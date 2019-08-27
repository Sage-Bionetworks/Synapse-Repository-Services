package org.sagebionetworks.repo.manager.statistics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsFileActionType;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsFileEvent;
import org.sagebionetworks.repo.manager.statistics.records.StatisticsEventLogRecord;
import org.sagebionetworks.repo.manager.statistics.records.StatisticsFileEventLogRecord;
import org.sagebionetworks.repo.manager.statistics.records.StatisticsFileEventLogRecordProvider;
import org.sagebionetworks.repo.manager.statistics.records.StatisticsLogRecordProviderFactory;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableList;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StatisticsEventsCollectorAutowireTest {

	public static final long TEST_FILE_SIZE = 1234567l;
	
	private static final String STREAM_UPLOAD = StatisticsFileEventLogRecordProvider.ASSOCIATED_STREAMS.get(StatisticsFileActionType.FILE_UPLOAD);

	private static final String STREAM_DOWNLOAD = StatisticsFileEventLogRecordProvider.ASSOCIATED_STREAMS.get(StatisticsFileActionType.FILE_DOWNLOAD);

	
	@Autowired
	private StatisticsLogRecordProviderFactory logRecordProviderFactory;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private StatisticsEventsCollectorClient collectorClient;
	
	@Autowired
	private TransactionSynchronizationProxy transactionSynchronization;
	
	@Mock
	private AwsKinesisFirehoseLogger firehoseLogger;

	private StatisticsEventsCollectorImpl statsEventsCollector;
	
	private Long creatorUserId;

	private Node project;
	
	private Node file;
	
	private FileHandle fileHandle;
	
	private List<String> nodesToDelete;
	private List<String> filehandlesToDelete;

	@BeforeEach
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		nodesToDelete = new ArrayList<String>();
		filehandlesToDelete = new ArrayList<>();
		creatorUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		// Spies on the transaction synchronization so that we can verify calls on it
		transactionSynchronization = Mockito.spy(transactionSynchronization);
		// We mock the firehose logger
		statsEventsCollector = new StatisticsEventsCollectorImpl(firehoseLogger, logRecordProviderFactory, transactionSynchronization);
		// Replace the autowired collector with ours so that we do not use firehose
		collectorClient.setEventsCollector(statsEventsCollector);
		
		fileHandle = createFileHandle("Test handle", creatorUserId.toString());
		project = createProject(creatorUserId);
		file = createFile(creatorUserId, project.getId(), fileHandle.getId());

	}
	
	@AfterEach
	public void after() throws Exception {
		for(String id : nodesToDelete) {
			nodeDao.delete(id);
		}
		for(String id : filehandlesToDelete) {
			fileHandleDao.delete(id);
		}
	}

	@Test
	public void testCollectEventWithNullEvent() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			statsEventsCollector.collectEvent(null);
		});
	}

	@Test
	public void testCollectFileDownloadEvent() {
		StatisticsFileEvent event = new StatisticsFileEvent(
				StatisticsFileActionType.FILE_DOWNLOAD, 
				creatorUserId, 
				fileHandle.getId(), file.getId(),
				FileHandleAssociateType.FileEntity);
		
		StatisticsEventLogRecord expectedRecord = convertToRecord(event);

		// Call under test
		statsEventsCollector.collectEvent(event);
		// Simulates the background timer call
		statsEventsCollector.flush();

		verify(firehoseLogger, times(1)).logBatch(eq(STREAM_DOWNLOAD), eq(Collections.singletonList(expectedRecord)));

	}

	@Test
	public void testCollectFileUploadEvent() {
		StatisticsFileEvent event = new StatisticsFileEvent(
				StatisticsFileActionType.FILE_UPLOAD, 
				creatorUserId, 
				fileHandle.getId(), file.getId(),
				FileHandleAssociateType.FileEntity);
		
		StatisticsEventLogRecord expectedRecord = convertToRecord(event);

		// Call under test
		statsEventsCollector.collectEvent(event);
		// Simulates the background timer call
		statsEventsCollector.flush();

		verify(firehoseLogger, times(1)).logBatch(eq(STREAM_UPLOAD), eq(Collections.singletonList(expectedRecord)));

	}
	
	@Test
	public void testCollectEvents() {
		
		StatisticsFileEvent downloadEvent1 = new StatisticsFileEvent(
				StatisticsFileActionType.FILE_DOWNLOAD,
				creatorUserId, 
				fileHandle.getId(), 
				file.getId(), 
				FileHandleAssociateType.FileEntity);

		StatisticsFileEvent downloadEvent2 = new StatisticsFileEvent(
				StatisticsFileActionType.FILE_DOWNLOAD,
				creatorUserId, 
				fileHandle.getId(), 
				file.getId(), 
				FileHandleAssociateType.FileEntity);

		List<StatisticsFileEvent> events = ImmutableList.of(downloadEvent1, downloadEvent2);

		List<StatisticsFileEventLogRecord> expectedRecords = ImmutableList.of(
			convertToRecord(downloadEvent1),
			convertToRecord(downloadEvent2)
		);
		
		// Call under test
		statsEventsCollector.collectEvents(events);
		// Simulates the background timer call
		statsEventsCollector.flush();
		
		// Verifies that the logger is invoked only once
		verify(firehoseLogger, times(1)).logBatch(eq(STREAM_DOWNLOAD), eq(expectedRecords));
	}
	
	@Test
	public void testCollectEventsWithDifferentStreams() {
		
		StatisticsFileEvent downloadEvent = new StatisticsFileEvent(
				StatisticsFileActionType.FILE_DOWNLOAD,
				creatorUserId, 
				fileHandle.getId(), 
				file.getId(), 
				FileHandleAssociateType.FileEntity);

		StatisticsFileEvent uploadEvent = new StatisticsFileEvent(
				StatisticsFileActionType.FILE_UPLOAD,
				creatorUserId, 
				fileHandle.getId(), 
				file.getId(), 
				FileHandleAssociateType.FileEntity);
		
		List<StatisticsFileEvent> events = ImmutableList.of(downloadEvent, uploadEvent);

		List<StatisticsFileEventLogRecord> expectedRecords1 = ImmutableList.of(
			convertToRecord(downloadEvent)
		);
		
		List<StatisticsFileEventLogRecord> expectedRecords2 = ImmutableList.of(
			convertToRecord(uploadEvent)
		);
		
		// Call under test
		statsEventsCollector.collectEvents(events);
		// Simulates the background timer call
		statsEventsCollector.flush();
		
		// Verifies that the logger is invoked once per stream type
		verify(firehoseLogger, times(1)).logBatch(eq(STREAM_DOWNLOAD), eq(expectedRecords1));
		verify(firehoseLogger, times(1)).logBatch(eq(STREAM_UPLOAD), eq(expectedRecords2));
	}

	@Test
	public void testCollectEventWithoutSending() {
		StatisticsFileEvent event = new StatisticsFileEvent(
				StatisticsFileActionType.FILE_DOWNLOAD, 
				creatorUserId, 
				"123", "123",
				FileHandleAssociateType.TeamAttachment);

		// Call under test
		statsEventsCollector.collectEvent(event);
		// Simulates the background timer call
		statsEventsCollector.flush();

		verify(firehoseLogger, never()).logBatch(any(), any());
	}
	
	@Test
	public void testInvokationWithoutTransaction() {
		StatisticsFileEvent event = new StatisticsFileEvent(
				StatisticsFileActionType.FILE_DOWNLOAD,
				creatorUserId, 
				fileHandle.getId(), 
				file.getId(), 
				FileHandleAssociateType.FileEntity);

		// Call under test
		collectorClient.collectEventWithoutTransaction(event);
		// Simulates the background timer call
		statsEventsCollector.flush();

		verify(transactionSynchronization, times(1)).isActualTransactionActive();
		verify(transactionSynchronization, never()).registerSynchronization(any());
		verify(firehoseLogger, times(1)).logBatch(any(), any());

	}
	
	@Test
	public void testInvokationWithTransaction() {
		StatisticsFileEvent event = new StatisticsFileEvent(
				StatisticsFileActionType.FILE_DOWNLOAD,
				creatorUserId, 
				fileHandle.getId(), 
				file.getId(), 
				FileHandleAssociateType.FileEntity);

		// Call under test
		collectorClient.collectEventWithTransaction(event);
		// Simulates the background timer call
		statsEventsCollector.flush();

		verify(transactionSynchronization, times(1)).isActualTransactionActive();
		verify(transactionSynchronization, times(1)).registerSynchronization(any());
		verify(firehoseLogger, times(1)).logBatch(any(), any());

	}
	
	private StatisticsFileEventLogRecord convertToRecord(StatisticsFileEvent event) {
		return new StatisticsFileEventLogRecord()
				.withUserId(event.getUserId())
				.withAssociation(event.getAssociationType(), event.getAssociationId())
				.withFileHandleId(event.getFileHandleId())
				.withProjectId(KeyFactory.stringToKey(project.getId()))
				.withTimestamp(event.getTimestamp());
	}
	
	private Node createProject(Long creatorId) {
		Node node = new Node();
		node.setId(KeyFactory.keyToString(idGenerator.generateNewId(IdType.ENTITY_ID)));
		node.setName("Project" + "-" + new Random().nextInt());
		node.setCreatedByPrincipalId(creatorId);
		node.setModifiedByPrincipalId(creatorId);
		node.setCreatedOn(new Date());
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(EntityType.project);
		node.setParentId(StackConfigurationSingleton.singleton().getRootFolderEntityId());
		node = nodeDao.createNewNode(node);
		nodesToDelete.add(node.getId());
		return node;
	}
	
	private Node createFile(Long creatorId, String projectId, String fileHandleId) {
		Node node = new Node();
		node.setId(KeyFactory.keyToString(idGenerator.generateNewId(IdType.ENTITY_ID)));
		node.setName("File" + "-" + new Random().nextInt());
		node.setCreatedByPrincipalId(creatorId);
		node.setModifiedByPrincipalId(creatorId);
		node.setCreatedOn(new Date());
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(EntityType.file);
		node.setParentId(projectId);
		node.setFileHandleId(fileHandleId);
		node = nodeDao.createNewNode(node);
		nodesToDelete.add(node.getId());
		return node;
	}
	
	private S3FileHandle createFileHandle(String fileName, String createdById){
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setBucketName("bucket");
		fileHandle.setKey("key");
		fileHandle.setCreatedBy(createdById);
		fileHandle.setFileName(fileName);
		fileHandle.setContentMd5(fileName);
		fileHandle.setContentSize(TEST_FILE_SIZE);
		fileHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fileHandle.setEtag(UUID.randomUUID().toString());
		fileHandle = (S3FileHandle) fileHandleDao.createFile(fileHandle);
		filehandlesToDelete.add(fileHandle.getId());
		return fileHandle;
	}
}
