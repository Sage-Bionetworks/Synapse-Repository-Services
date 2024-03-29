package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.kinesis.AwsKinesisDeliveryException;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationRecord;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleScannerUtils;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.model.IdRange;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.dbo.dao.files.DBOFilesScannerStatus;
import org.sagebionetworks.repo.model.dbo.dao.files.FilesScannerStatusDao;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;
import org.sagebionetworks.repo.model.file.FileHandleStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class FileHandleAssociationScannerJobManagerUnitTest {

	@Mock
	private FileHandleAssociationManager mockAssociationManager;

	@Mock
	private AwsKinesisFirehoseLogger mockKinesisLogger;

	@Mock
	private StackStatusDao mockStackStatusDao;

	@Mock
	private FilesScannerStatusDao mockStatusDao;
	
	@Mock
	private FileHandleDao mockFileHandleDao;
	
	@Mock
	private FileHandleAssociationScannerNotifier mockNotifier;

	@Mock
	private Clock mockClock;

	@InjectMocks
	private FileHandleAssociationScannerJobManagerImpl manager;
	
	@Captor
	private ArgumentCaptor<List<FileHandleAssociationRecord>> recordsCaptor;
	
	@Captor
	private ArgumentCaptor<List<Long>> fileIdsCaptor;
	
	@Mock
	private DBOFilesScannerStatus mockStatus;

	private FileHandleAssociationScanRangeRequest scanRangeRequest;
	private FileHandleAssociateType associationType = FileHandleAssociateType.FileEntity;
	private Long jobId = 123L;
	private IdRange idRange = new IdRange(0, 100);

	@BeforeEach
	public void before() {
		scanRangeRequest = new FileHandleAssociationScanRangeRequest()
				.withAssociationType(associationType)
				.withJobId(jobId)
				.withIdRange(idRange);
	}
	
	@Test
	public void processScanRangeRequest() throws RecoverableMessageException {
		
		List<ScannedFileHandleAssociation> associations = Arrays.asList(
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(2L, 2L)
		);
		
		long batchTimestamp = 123L;
		
		when(mockStatusDao.exist(anyLong())).thenReturn(true);
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(true);
		when(mockAssociationManager.scanRange(any(), any())).thenReturn(associations);
		when(mockFileHandleDao.hasStatusBatch(anyList(), any())).thenReturn(false);
		when(mockClock.currentTimeMillis()).thenReturn(batchTimestamp, 456L);
		
		Set<FileHandleAssociationRecord> expectedRecords = 
			associations.stream().flatMap( a -> {
				return FileHandleScannerUtils.mapAssociation(associationType, a, batchTimestamp).stream();
			}).collect(Collectors.toSet());
		
		Set<Long> expectedFileIds = expectedRecords.stream().map(FileHandleAssociationRecord::getFileHandleId).collect(Collectors.toSet());
		
		// Call under test
		int result = manager.processScanRangeRequest(scanRangeRequest);
		
		assertEquals(expectedRecords.size(), result);
		
		verify(mockStackStatusDao, times(2)).isStackReadWrite();
		verify(mockAssociationManager).scanRange(scanRangeRequest.getAssociationType(), scanRangeRequest.getIdRange());
		verify(mockFileHandleDao).hasStatusBatch(fileIdsCaptor.capture(), eq(FileHandleStatus.UNLINKED));
		assertEquals(expectedFileIds, fileIdsCaptor.getValue().stream().collect(Collectors.toSet()));	
		verify(mockFileHandleDao, never()).updateStatusForBatch(anyList(), any(), any(), anyInt());
		verify(mockKinesisLogger).logBatch(eq(FileHandleAssociationRecord.STREAM_NAME), recordsCaptor.capture());
		verify(mockClock).currentTimeMillis();
		verify(mockStatusDao).increaseJobCompletedCount(scanRangeRequest.getJobId(), expectedRecords.size(), 0);
		
		List<FileHandleAssociationRecord> records = recordsCaptor.getValue();
		
		assertEquals(expectedRecords.size(), records.size());
		assertEquals(expectedRecords, new HashSet<>(records));
		
	}

	@Test
	public void processScanRangeRequestWithDuplicates() throws RecoverableMessageException {
		
		List<ScannedFileHandleAssociation> associations = Arrays.asList(
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(2L, 2L),
				new ScannedFileHandleAssociation(1L, 1L)
		);
		
		long batchTimestamp = 123L;
		
		when(mockStatusDao.exist(anyLong())).thenReturn(true);
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(true);
		when(mockAssociationManager.scanRange(any(), any())).thenReturn(associations);
		when(mockFileHandleDao.hasStatusBatch(anyList(), any())).thenReturn(false);
		when(mockClock.currentTimeMillis()).thenReturn(batchTimestamp, 456L);
		
		Set<FileHandleAssociationRecord> expectedRecords = associations
				.stream()
				.flatMap( a -> FileHandleScannerUtils.mapAssociation(associationType, a, batchTimestamp).stream())
				.collect(Collectors.toSet());

		Set<Long> expectedFileIds = expectedRecords.stream().map(FileHandleAssociationRecord::getFileHandleId).collect(Collectors.toSet());
		
		// Call under test
		int result = manager.processScanRangeRequest(scanRangeRequest);
		
		assertEquals(expectedRecords.size(), result);
		
		verify(mockStackStatusDao, times(2)).isStackReadWrite();
		verify(mockAssociationManager).scanRange(scanRangeRequest.getAssociationType(), scanRangeRequest.getIdRange());
		verify(mockFileHandleDao).hasStatusBatch(fileIdsCaptor.capture(), eq(FileHandleStatus.UNLINKED));
		assertEquals(expectedFileIds, fileIdsCaptor.getValue().stream().collect(Collectors.toSet()));
		verify(mockFileHandleDao, never()).updateStatusForBatch(anyList(), any(), any(), anyInt());
		verify(mockKinesisLogger).logBatch(eq(FileHandleAssociationRecord.STREAM_NAME), recordsCaptor.capture());
		verify(mockClock).currentTimeMillis();
		
		List<FileHandleAssociationRecord> records = recordsCaptor.getValue();
		
		assertEquals(expectedRecords.size(), records.size());
		assertEquals(expectedRecords, new HashSet<>(records));
		
		verify(mockStatusDao).increaseJobCompletedCount(scanRangeRequest.getJobId(), expectedRecords.size(), 0);
		
	}
	
	@Test
	public void processScanRangeRequestWithMultipleBatches() throws InterruptedException, RecoverableMessageException {
		
		// 3 batches, the last batch is not full
		List<ScannedFileHandleAssociation> associations = IntStream.range(0, FileHandleAssociationScannerJobManagerImpl.KINESIS_BATCH_SIZE * 3 - 1).boxed().map(i ->
			new ScannedFileHandleAssociation(Long.valueOf(i), Long.valueOf(i))
		).collect(Collectors.toList());
		
		when(mockStatusDao.exist(anyLong())).thenReturn(true);
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(true);
		when(mockAssociationManager.scanRange(any(), any())).thenReturn(associations);
		when(mockFileHandleDao.hasStatusBatch(anyList(), any())).thenReturn(false);
		
		doNothing().when(mockClock).sleep(anyLong());
		
		final long initialTimestamp = 1234L;
		
		long batchTimestamp = initialTimestamp;
		
		// 3 batches, should have 3 different timestamps
		when(mockClock.currentTimeMillis()).thenReturn(batchTimestamp++, batchTimestamp++, batchTimestamp);
		
		List<Set<FileHandleAssociationRecord>> expectedBatches = new ArrayList<>();

		batchTimestamp = initialTimestamp;
		
		for (List<ScannedFileHandleAssociation> batch : ListUtils.partition(associations, FileHandleAssociationScannerJobManagerImpl.KINESIS_BATCH_SIZE)) {
			final long timestamp = batchTimestamp++;
			expectedBatches.add(
				batch.stream()
					.flatMap( a -> FileHandleScannerUtils.mapAssociation(associationType, a, timestamp).stream())
					.collect(Collectors.toSet())
			);
		}
		
		// Call under test
		int result = manager.processScanRangeRequest(scanRangeRequest);
		
		assertEquals(associations.size(), result);
		
		verify(mockStackStatusDao, times(expectedBatches.size() + 1)).isStackReadWrite();
		verify(mockAssociationManager).scanRange(scanRangeRequest.getAssociationType(), scanRangeRequest.getIdRange());
		verify(mockKinesisLogger, times(expectedBatches.size())).logBatch(eq(FileHandleAssociationRecord.STREAM_NAME), recordsCaptor.capture());
		verify(mockClock, times(expectedBatches.size())).currentTimeMillis();
		verify(mockClock, times(expectedBatches.size() - 1)).sleep(FileHandleAssociationScannerJobManagerImpl.FLUSH_DELAY_MS);
		verify(mockFileHandleDao, times(expectedBatches.size())).hasStatusBatch(fileIdsCaptor.capture(), eq(FileHandleStatus.UNLINKED));
		
		List<List<FileHandleAssociationRecord>> batches = recordsCaptor.getAllValues();
		List<List<Long>> idsBatches = fileIdsCaptor.getAllValues();

		for (int i = 0; i < batches.size(); i++) {
			Set<FileHandleAssociationRecord> expectedRecords = expectedBatches.get(i);
			List<FileHandleAssociationRecord> batch = batches.get(i);
			
			assertEquals(expectedRecords.size(), batch.size());
			assertEquals(expectedRecords, new HashSet<>(batch));
			
			Set<Long> expectedFileIds = expectedRecords.stream().map(FileHandleAssociationRecord::getFileHandleId).collect(Collectors.toSet());
			
			assertEquals(expectedFileIds, idsBatches.get(i).stream().collect(Collectors.toSet()));
		}
		
		verify(mockFileHandleDao, never()).updateStatusForBatch(anyList(), any(), any(), anyInt());
		verify(mockStatusDao).increaseJobCompletedCount(scanRangeRequest.getJobId(), associations.size(), 0);
		
	}
	
	@Test
	public void processScanRangeRequestWithRelinked() throws RecoverableMessageException {
		
		List<ScannedFileHandleAssociation> associations = Arrays.asList(
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(2L, 2L)
		);
		
		long batchTimestamp = 123L;
		
		when(mockStatusDao.exist(anyLong())).thenReturn(true);
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(true);
		when(mockAssociationManager.scanRange(any(), any())).thenReturn(associations);
		when(mockFileHandleDao.hasStatusBatch(anyList(), any())).thenReturn(true);
		when(mockFileHandleDao.updateStatusForBatch(anyList(), any(), any(), anyInt())).thenReturn(Arrays.asList(2L));
		when(mockClock.currentTimeMillis()).thenReturn(batchTimestamp, 456L);
		
		Set<FileHandleAssociationRecord> expectedRecords = 
			associations.stream().flatMap( a -> {
				return FileHandleScannerUtils.mapAssociation(associationType, a, batchTimestamp).stream();
			}).collect(Collectors.toSet());
		
		Set<Long> expectedFileIds = expectedRecords.stream().map(FileHandleAssociationRecord::getFileHandleId).collect(Collectors.toSet());
		
		// Call under test
		int result = manager.processScanRangeRequest(scanRangeRequest);
		
		assertEquals(expectedRecords.size(), result);
		
		verify(mockStackStatusDao, times(2)).isStackReadWrite();
		verify(mockAssociationManager).scanRange(scanRangeRequest.getAssociationType(), scanRangeRequest.getIdRange());
		verify(mockFileHandleDao).hasStatusBatch(fileIdsCaptor.capture(), eq(FileHandleStatus.UNLINKED));
		List<Long> capturedIds = fileIdsCaptor.getValue();
		assertEquals(expectedFileIds, capturedIds.stream().collect(Collectors.toSet()));
		verify(mockFileHandleDao).updateStatusForBatch(capturedIds, FileHandleStatus.AVAILABLE, FileHandleStatus.UNLINKED, 0);
		verify(mockKinesisLogger).logBatch(eq(FileHandleAssociationRecord.STREAM_NAME), recordsCaptor.capture());
		verify(mockClock).currentTimeMillis();
		verify(mockStatusDao).increaseJobCompletedCount(scanRangeRequest.getJobId(), expectedRecords.size(), 1);
		
		List<FileHandleAssociationRecord> records = recordsCaptor.getValue();
		
		assertEquals(expectedRecords.size(), records.size());
		assertEquals(expectedRecords, new HashSet<>(records));
		
	}
	
	@Test
	public void processScanRangeRequestWithRecoverableException() {
		
		Iterator<ScannedFileHandleAssociation> mockIterator = Mockito.mock(Iterator.class);
		Iterable<ScannedFileHandleAssociation> mockIterable = Mockito.mock(Iterable.class);
		
		RecoverableMessageException ex = new RecoverableMessageException("Something wrong");
		
		when(mockStatusDao.exist(anyLong())).thenReturn(true);
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(true);
		when(mockIterator.hasNext()).thenReturn(true);
		when(mockIterator.next()).thenThrow(ex);
		when(mockIterable.iterator()).thenReturn(mockIterator);
		when(mockAssociationManager.scanRange(any(), any())).thenReturn(mockIterable);
		when(mockClock.currentTimeMillis()).thenReturn(456L);		
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, ()-> {			
			// Call under test
			manager.processScanRangeRequest(scanRangeRequest);
		});
		
		assertEquals(ex, result);
		
		verify(mockStackStatusDao).isStackReadWrite();
		verify(mockAssociationManager).scanRange(scanRangeRequest.getAssociationType(), scanRangeRequest.getIdRange());
		verify(mockClock).currentTimeMillis();
		verifyZeroInteractions(mockKinesisLogger);
		verifyZeroInteractions(mockStackStatusDao);
		
	}
	
	@Test
	public void processScanRangeRequestWithKinesisException() throws RecoverableMessageException {
		
		List<ScannedFileHandleAssociation> associations = Arrays.asList(
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(2L, 2L)
		);
		
		long batchTimestamp = 123L;
		
		AwsKinesisDeliveryException ex = new AwsKinesisDeliveryException("Could not deliver");
		
		when(mockStatusDao.exist(anyLong())).thenReturn(true);
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(true);
		when(mockAssociationManager.scanRange(any(), any())).thenReturn(associations);
		when(mockClock.currentTimeMillis()).thenReturn(batchTimestamp, 456L);
		doThrow(ex).when(mockKinesisLogger).logBatch(any(), any());
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			manager.processScanRangeRequest(scanRangeRequest);
		});
		
		assertEquals(ex, result.getCause());
		
		verify(mockStackStatusDao, times(2)).isStackReadWrite();
		verify(mockAssociationManager).scanRange(scanRangeRequest.getAssociationType(), scanRangeRequest.getIdRange());
		verify(mockKinesisLogger).logBatch(eq(FileHandleAssociationRecord.STREAM_NAME), anyList());
		verify(mockClock).currentTimeMillis();
		verifyZeroInteractions(mockStatusDao);
	}
	
	@Test
	public void processScanRangeRequestWithNoRequest() {

		scanRangeRequest = null;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processScanRangeRequest(scanRangeRequest);
		}).getMessage();

		assertEquals("The request is required.", errorMessage);
	}
	
	@Test
	public void processScanRangeRequestWithNoJobId() {

		scanRangeRequest.withJobId(null);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processScanRangeRequest(scanRangeRequest);
		}).getMessage();

		assertEquals("The request.jobId is required.", errorMessage);
	}

	@Test
	public void processScanRangeRequestWithNoAssociationType() {

		scanRangeRequest.withAssociationType(null);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processScanRangeRequest(scanRangeRequest);
		}).getMessage();

		assertEquals("The request.associationType is required.", errorMessage);
	}
	
	@Test
	public void processScanRangeRequestWithNoIdRange() {

		scanRangeRequest.withIdRange(null);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processScanRangeRequest(scanRangeRequest);
		}).getMessage();

		assertEquals("The request.idRange is required.", errorMessage);
	}
	
	@Test
	public void processScanRangeRequestWithReadOnly() {

		when(mockStatusDao.exist(anyLong())).thenReturn(true);
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(false); 
		
		String errorMessage = assertThrows(RecoverableMessageException .class, () -> {
			// Call under test
			manager.processScanRangeRequest(scanRangeRequest);
		}).getMessage();

		assertEquals("The stack was in read-only mode.", errorMessage);
	}
	
	@Test
	public void processScanRangeRequestWithNotFound() {

		when(mockStatusDao.exist(anyLong())).thenReturn(false); 
		
		String errorMessage = assertThrows(NotFoundException.class, () -> {
			// Call under test
			manager.processScanRangeRequest(scanRangeRequest);
		}).getMessage();

		assertEquals("A job with id " + scanRangeRequest.getJobId() + " does not exist.", errorMessage);
	}
	
	@Test
	public void testIsScanJobIdle() {
		int daysNum = 5;
		boolean exists = true;
		
		when(mockStatusDao.existsWithinLast(anyInt())).thenReturn(exists);
		
		// Call under test
		assertFalse(manager.isScanJobIdle(daysNum));
		
		verify(mockStatusDao).existsWithinLast(daysNum);
	}
	
	@Test
	public void testIsScanJobIdleAndNotExists() {
		int daysNum = 5;
		boolean exists = false;
		
		when(mockStatusDao.existsWithinLast(anyInt())).thenReturn(exists);
		
		// Call under test
		assertTrue(manager.isScanJobIdle(daysNum));
		
		verify(mockStatusDao).existsWithinLast(daysNum);
	}
	
	@Test
	public void testStartScanJob() {
		
		long maxIdRange = 1000;
		IdRange idRange = new IdRange(2, 10);
		
		when(mockStatusDao.create()).thenReturn(mockStatus);
		when(mockStatus.getId()).thenReturn(jobId);
		when(mockAssociationManager.getIdRange(any())).thenReturn(new IdRange(-1, -1));
		when(mockAssociationManager.getIdRange(FileHandleAssociateType.FileEntity)).thenReturn(idRange);
		when(mockAssociationManager.getMaxIdRangeSize(FileHandleAssociateType.FileEntity)).thenReturn(maxIdRange);
		
		// Call under test
		manager.startScanJob();
		
		verify(mockStatusDao).create();
		
		for (FileHandleAssociateType type : FileHandleAssociateType.values()) {
			verify(mockAssociationManager).getIdRange(type);
		}
		
		verify(mockAssociationManager).getMaxIdRangeSize(FileHandleAssociateType.FileEntity);
		verify(mockNotifier).sendScanRequest(eq(new FileHandleAssociationScanRangeRequest().withJobId(jobId).withAssociationType(FileHandleAssociateType.FileEntity).withIdRange(new IdRange(2, 1001))), anyInt());
		verify(mockStatusDao).setStartedJobsCount(jobId, 1);
		
	}
	
	@Test
	public void testStartScanJobWithDispatchingException() {
		
		long maxIdRange = 1000;
		IdRange idRange = new IdRange(2, 10);
		
		when(mockStatusDao.create()).thenReturn(mockStatus);
		when(mockStatus.getId()).thenReturn(jobId);
		when(mockAssociationManager.getIdRange(any())).thenReturn(new IdRange(-1, -1));
		when(mockAssociationManager.getIdRange(FileHandleAssociateType.FileEntity)).thenReturn(idRange);
		when(mockAssociationManager.getMaxIdRangeSize(FileHandleAssociateType.FileEntity)).thenReturn(maxIdRange);
		
		RuntimeException ex = new RuntimeException();
		
		doThrow(ex).when(mockNotifier).sendScanRequest(any(), anyInt());
		
		RuntimeException result = assertThrows(RuntimeException.class, () -> {
			// Call under test
			manager.startScanJob();
		});
		
		assertEquals(ex, result);
		
		verify(mockStatusDao).create();
		verify(mockAssociationManager).getIdRange(FileHandleAssociateType.FileEntity);
		verify(mockAssociationManager).getMaxIdRangeSize(FileHandleAssociateType.FileEntity);
		
		verify(mockNotifier).sendScanRequest(eq(new FileHandleAssociationScanRangeRequest().withJobId(jobId).withAssociationType(FileHandleAssociateType.FileEntity).withIdRange(new IdRange(2, 1001))), anyInt());
		verify(mockStatusDao).delete(jobId);
		
	}

	@Test
	public void testStartScanJobWithMultiple() {
		
		long maxIdRange = 5;
		IdRange idRange = new IdRange(2, 10);
		
		when(mockStatusDao.create()).thenReturn(mockStatus);
		when(mockStatus.getId()).thenReturn(jobId);
		when(mockAssociationManager.getIdRange(any())).thenReturn(new IdRange(-1, -1));
		when(mockAssociationManager.getIdRange(FileHandleAssociateType.FileEntity)).thenReturn(idRange);
		when(mockAssociationManager.getMaxIdRangeSize(FileHandleAssociateType.FileEntity)).thenReturn(maxIdRange);
		
		// Call under test
		manager.startScanJob();
		
		verify(mockStatusDao).create();
		
		for (FileHandleAssociateType type : FileHandleAssociateType.values()) {
			verify(mockAssociationManager).getIdRange(type);
		}
		
		verify(mockAssociationManager).getMaxIdRangeSize(FileHandleAssociateType.FileEntity);
		
		verify(mockNotifier).sendScanRequest(eq(new FileHandleAssociationScanRangeRequest().withJobId(jobId).withAssociationType(FileHandleAssociateType.FileEntity).withIdRange(new IdRange(2, 6))), anyInt());
		verify(mockNotifier).sendScanRequest(eq(new FileHandleAssociationScanRangeRequest().withJobId(jobId).withAssociationType(FileHandleAssociateType.FileEntity).withIdRange(new IdRange(7, 11))), anyInt());
		verify(mockStatusDao).setStartedJobsCount(jobId, 2);
		
	}
	
	@Test
	public void testStartScanJobWithOne() {
		
		long maxIdRange = 5;
		IdRange idRange = new IdRange(2, 2);
		
		when(mockStatusDao.create()).thenReturn(mockStatus);
		when(mockStatus.getId()).thenReturn(jobId);
		when(mockAssociationManager.getIdRange(any())).thenReturn(new IdRange(-1, -1));
		when(mockAssociationManager.getIdRange(FileHandleAssociateType.FileEntity)).thenReturn(idRange);
		when(mockAssociationManager.getMaxIdRangeSize(FileHandleAssociateType.FileEntity)).thenReturn(maxIdRange);
		
		// Call under test
		manager.startScanJob();
		
		verify(mockStatusDao).create();
		
		for (FileHandleAssociateType type : FileHandleAssociateType.values()) {
			verify(mockAssociationManager).getIdRange(type);
		}
		
		verify(mockAssociationManager).getMaxIdRangeSize(FileHandleAssociateType.FileEntity);
		
		verify(mockNotifier).sendScanRequest(eq(new FileHandleAssociationScanRangeRequest().withJobId(jobId).withAssociationType(FileHandleAssociateType.FileEntity).withIdRange(new IdRange(2, 6))), anyInt());
		verify(mockStatusDao).setStartedJobsCount(jobId, 1);
		
	}
	
	@Test
	public void testStartScanJobWithFitRange() {
		
		long maxIdRange = 3;
		IdRange idRange = new IdRange(1, 9);
		
		when(mockStatusDao.create()).thenReturn(mockStatus);
		when(mockStatus.getId()).thenReturn(jobId);
		when(mockAssociationManager.getIdRange(any())).thenReturn(new IdRange(-1, -1));
		when(mockAssociationManager.getIdRange(FileHandleAssociateType.FileEntity)).thenReturn(idRange);
		when(mockAssociationManager.getMaxIdRangeSize(FileHandleAssociateType.FileEntity)).thenReturn(maxIdRange);
		
		// Call under test
		manager.startScanJob();
		
		verify(mockStatusDao).create();
		
		for (FileHandleAssociateType type : FileHandleAssociateType.values()) {
			verify(mockAssociationManager).getIdRange(type);
		}
		
		verify(mockAssociationManager).getMaxIdRangeSize(FileHandleAssociateType.FileEntity);
		
		verify(mockNotifier).sendScanRequest(eq(new FileHandleAssociationScanRangeRequest().withJobId(jobId).withAssociationType(FileHandleAssociateType.FileEntity).withIdRange(new IdRange(1, 3))), anyInt());
		verify(mockNotifier).sendScanRequest(eq(new FileHandleAssociationScanRangeRequest().withJobId(jobId).withAssociationType(FileHandleAssociateType.FileEntity).withIdRange(new IdRange(4, 6))), anyInt());
		verify(mockNotifier).sendScanRequest(eq(new FileHandleAssociationScanRangeRequest().withJobId(jobId).withAssociationType(FileHandleAssociateType.FileEntity).withIdRange(new IdRange(7, 9))), anyInt());
		
		verify(mockStatusDao).setStartedJobsCount(jobId, 3);
		
	}
	
	@Test
	public void testIsScanJobCompletedWithNoJobStarted() {
		
		Long jobStartedCount = 0L;
		
		when(mockStatus.getJobsStartedCount()).thenReturn(jobStartedCount);
		when(mockStatusDao.get(jobId)).thenReturn(mockStatus);
	
		// Call under test
		boolean result = manager.isScanJobCompleted(jobId);
		
		assertFalse(result);
		
		verify(mockStatusDao).get(jobId);
	}
	
	@Test
	public void testIsScanJobCompletedWithNoJobsCompleted() {
		
		Long jobStartedCount = 10L;
		Long jobCompletedCount = 0L;
		
		when(mockStatus.getJobsStartedCount()).thenReturn(jobStartedCount);
		when(mockStatus.getJobsCompletedCount()).thenReturn(jobCompletedCount);
		
		when(mockStatusDao.get(jobId)).thenReturn(mockStatus);
	
		// Call under test
		boolean result = manager.isScanJobCompleted(jobId);
		
		assertFalse(result);
		
		verify(mockStatusDao).get(jobId);
	}
	
	@Test
	public void testIsScanJobCompletedWithExact() {
		
		Long jobStartedCount = 10L;
		Long jobCompletedCount = 10L;
		
		when(mockStatus.getJobsStartedCount()).thenReturn(jobStartedCount);
		when(mockStatus.getJobsCompletedCount()).thenReturn(jobCompletedCount);
		
		when(mockStatusDao.get(jobId)).thenReturn(mockStatus);
	
		// Call under test
		boolean result = manager.isScanJobCompleted(jobId);
		
		assertTrue(result);
		
		verify(mockStatusDao).get(jobId);
	}
	
	@Test
	public void testIsScanJobCompletedWithMore() {
		
		Long jobStartedCount = 10L;
		Long jobCompletedCount = 11L;
		
		when(mockStatus.getJobsStartedCount()).thenReturn(jobStartedCount);
		when(mockStatus.getJobsCompletedCount()).thenReturn(jobCompletedCount);
		
		when(mockStatusDao.get(jobId)).thenReturn(mockStatus);
	
		// Call under test
		boolean result = manager.isScanJobCompleted(jobId);
		
		assertTrue(result);
		
		verify(mockStatusDao).get(jobId);
	}

}
