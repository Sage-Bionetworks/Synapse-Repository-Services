package org.sagebionetworks.snapshot.workers.writers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.audit.DeletedNode;
import org.sagebionetworks.repo.model.audit.NodeRecord;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class NodeObjectRecordWriterTest {

	@Mock
	private NodeDAO mockNodeDAO;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private AccessRequirementDAO mockAccessRequirementDao;
	@Mock
	private EntityAuthorizationManager mockEntityAuthorizationManager;
	@Mock
	private UserEntityPermissions mockPermissions;
	@Mock
	private UserInfo mockUserInfo;
	@Mock
	private ObjectRecordDAO mockObjectRecordDao;
	@Mock
	private AwsKinesisFirehoseLogger mockKinesisLogger;
	@Mock
	private ProgressCallback mockCallback;
	
	@InjectMocks
	private NodeObjectRecordWriter writer;
	
	private NodeRecord node;
	private AccessRequirementStats stats;
	private boolean canPublicRead;
	
	@Captor
	private ArgumentCaptor<List<KinesisObjectSnapshotRecord<?>>> recordCaptor;

	@BeforeEach
	public void setup() {

		node = new NodeRecord();
		node.setId("123");
		
		stats = new AccessRequirementStats();
		stats.setHasACT(true);
		stats.setHasToU(false);
		canPublicRead = true;

	}

	@Test
	public void deleteChangeMessage() throws IOException {
		Long timestamp = Instant.now().toEpochMilli();
		
		String nodeId = "123";
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, nodeId, ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		
		DeletedNode deletedNode = new DeletedNode();
		deletedNode.setId(nodeId);
		
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(deletedNode, timestamp);
		
		verify(mockObjectRecordDao).saveBatch(eq(Arrays.asList(expected)), eq(expected.getJsonClassName()));

		KinesisObjectSnapshotRecord<?> expectedRecord = KinesisObjectSnapshotRecord.map(changeMessage, new NodeRecord().setId(nodeId));
		
		verify(mockKinesisLogger).logBatch(eq("nodeSnapshots"), recordCaptor.capture());
		
		expectedRecord.withSnapshotTimestamp(recordCaptor.getValue().get(0).getSnapshotTimestamp());
		
		assertEquals(recordCaptor.getValue(), List.of(expectedRecord));
	}

	@Test
	public void invalidObjectType() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "123", ObjectType.TABLE, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		
		assertThrows(IllegalArgumentException.class, () -> {
			writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		});
		
		verifyZeroInteractions(mockObjectRecordDao);
		verifyZeroInteractions(mockKinesisLogger);
	}

	@Test
	public void publicRestrictedAndControlledTest() throws IOException {
		
		when(mockNodeDAO.getNode("123")).thenReturn(node);

		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()))
				.thenReturn(mockUserInfo);
		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(mockUserInfo, node.getId()))
				.thenReturn(mockPermissions);
		when(mockNodeDAO.getEntityPathIds(node.getId())).thenReturn(Arrays.asList(KeyFactory.stringToKey(node.getId())));
		when(mockAccessRequirementDao.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(node.getId())), RestrictableObjectType.ENTITY))
				.thenReturn(stats);
		when(mockPermissions.getCanPublicRead()).thenReturn(canPublicRead);
		
		Long timestamp = System.currentTimeMillis();
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, "123", ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);

		node.setIsPublic(canPublicRead);
		node.setIsControlled(stats.getHasACT());
		node.setIsRestricted(stats.getHasToU());
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(node, timestamp);

		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		
		verify(mockNodeDAO).getNode(eq("123"));
		verify(mockObjectRecordDao).saveBatch(eq(Arrays.asList(expected)), eq(expected.getJsonClassName()));
		
		KinesisObjectSnapshotRecord<NodeRecord> expectedRecord = KinesisObjectSnapshotRecord.map(changeMessage, node);
		
		verify(mockKinesisLogger).logBatch(eq("nodeSnapshots"), recordCaptor.capture());
		
		expectedRecord.withSnapshotTimestamp(recordCaptor.getValue().get(0).getSnapshotTimestamp());
		
		assertEquals(recordCaptor.getValue(), List.of(expectedRecord));
	}
	
	@Test
	public void buildNodeRecordTest() {
		Node node = new Node();
		node.setId("id");
		node.setParentId("parentId");
		node.setNodeType(EntityType.file);
		node.setCreatedOn(new Date(0));
		node.setCreatedByPrincipalId(1L);
		node.setModifiedOn(new Date());
		node.setModifiedByPrincipalId(2L);
		node.setVersionNumber(3L);
		node.setFileHandleId("fileHandleId");
		node.setName("name");
		NodeRecord record = NodeObjectRecordWriter.buildNodeRecord(node,"benefactorId", "projectId");
		assertEquals(node.getId(), record.getId());
		assertEquals("benefactorId", record.getBenefactorId());
		assertEquals("projectId", record.getProjectId());
		assertEquals(node.getParentId(), record.getParentId());
		assertEquals(node.getNodeType(), record.getNodeType());
		assertEquals(node.getCreatedOn(), record.getCreatedOn());
		assertEquals(node.getCreatedByPrincipalId(), record.getCreatedByPrincipalId());
		assertEquals(node.getModifiedOn(), record.getModifiedOn());
		assertEquals(node.getModifiedByPrincipalId(), record.getModifiedByPrincipalId());
		assertEquals(node.getVersionNumber(), record.getVersionNumber());
		assertEquals(node.getFileHandleId(), record.getFileHandleId());
		assertEquals(node.getName(), record.getName());
	}

	@Test
	public void testNodeInTrashCan() throws IOException {
		
		when(mockNodeDAO.getNode("123")).thenReturn(node);
		when(mockUserManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()))
				.thenReturn(mockUserInfo);
		when(mockEntityAuthorizationManager.getUserPermissionsForEntity(mockUserInfo, node.getId()))
				.thenReturn(mockPermissions);
				
		EntityInTrashCanException exception = new EntityInTrashCanException("");
		when(mockPermissions.getCanPublicRead()).thenThrow(exception);

		Long timestamp = System.currentTimeMillis();
		String nodeId = "123";
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, nodeId, ObjectType.ENTITY, "etag", timestamp);
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);

		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		DeletedNode deletedNode = new DeletedNode();
		deletedNode.setId(nodeId);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(deletedNode, timestamp);
		verify(mockObjectRecordDao).saveBatch(eq(Arrays.asList(expected)), eq(expected.getJsonClassName()));
				
		KinesisObjectSnapshotRecord<?> expectedRecord = KinesisObjectSnapshotRecord.map(changeMessage, new NodeRecord().setId(nodeId));
		
		verify(mockKinesisLogger).logBatch(eq("nodeSnapshots"), recordCaptor.capture());
		
		expectedRecord.withSnapshotTimestamp(recordCaptor.getValue().get(0).getSnapshotTimestamp());
		
		assertEquals(recordCaptor.getValue(), List.of(expectedRecord));
	}
}
