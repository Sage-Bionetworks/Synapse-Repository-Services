package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationRecord;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleScannerUtils;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.dbo.dao.files.FilesScannerStatusDao;
import org.sagebionetworks.repo.model.exception.ReadOnlyException;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.util.Clock;

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
	private Clock mockClock;

	@InjectMocks
	private FileHandleAssociationScannerJobManagerImpl manager;
	
	@Captor
	private ArgumentCaptor<List<FileHandleAssociationRecord>> recordsCaptor;

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
	public void processScanRangeRequest() {
		
		List<ScannedFileHandleAssociation> associations = Arrays.asList(
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(2L, 2L)
		);
		
		long timestamp = System.currentTimeMillis();
		
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(true);
		when(mockAssociationManager.scanRange(any(), any())).thenReturn(associations);
		when(mockClock.currentTimeMillis()).thenReturn(timestamp);
		
		Set<FileHandleAssociationRecord> expectedRecords = 
			associations.stream().flatMap( a -> {
				return FileHandleScannerUtils.mapAssociation(associationType, a, timestamp).stream();
			}).collect(Collectors.toSet());
		
		// Call under test
		int result = manager.processScanRangeRequest(scanRangeRequest);
		
		assertEquals(expectedRecords.size(), result);
		
		verify(mockStackStatusDao, times(2)).isStackReadWrite();
		verify(mockAssociationManager).scanRange(scanRangeRequest.getAssociationType(), scanRangeRequest.getIdRange());
		verify(mockKinesisLogger).logBatch(eq(FileHandleAssociationRecord.KINESIS_STREAM_NAME), recordsCaptor.capture());
		verify(mockStatusDao).increaseJobCompletedCount(scanRangeRequest.getJobId());
		
		List<FileHandleAssociationRecord> records = recordsCaptor.getValue();
		
		assertEquals(expectedRecords.size(), records.size());
		assertEquals(expectedRecords, new HashSet<>(records));
		
	}

	@Test
	public void processScanRangeRequestWithDuplicates() {
		
		List<ScannedFileHandleAssociation> associations = Arrays.asList(
				new ScannedFileHandleAssociation(1L, 1L),
				new ScannedFileHandleAssociation(2L, 2L),
				new ScannedFileHandleAssociation(1L, 1L)
		);
		
		long timestamp = System.currentTimeMillis();
		
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(true);
		when(mockAssociationManager.scanRange(any(), any())).thenReturn(associations);
		when(mockClock.currentTimeMillis()).thenReturn(timestamp);
		
		Set<FileHandleAssociationRecord> expectedRecords = associations
				.stream()
				.flatMap( a -> FileHandleScannerUtils.mapAssociation(associationType, a, timestamp).stream())
				.collect(Collectors.toSet());
		
		// Call under test
		int result = manager.processScanRangeRequest(scanRangeRequest);
		
		assertEquals(expectedRecords.size(), result);
		
		verify(mockStackStatusDao, times(2)).isStackReadWrite();
		verify(mockAssociationManager).scanRange(scanRangeRequest.getAssociationType(), scanRangeRequest.getIdRange());
		verify(mockKinesisLogger).logBatch(eq(FileHandleAssociationRecord.KINESIS_STREAM_NAME), recordsCaptor.capture());
		
		List<FileHandleAssociationRecord> records = recordsCaptor.getValue();
		
		assertEquals(expectedRecords.size(), records.size());
		assertEquals(expectedRecords, new HashSet<>(records));
		
		verify(mockStatusDao).increaseJobCompletedCount(scanRangeRequest.getJobId());
		
	}
	
	@Test
	public void processScanRangeRequestWithMultipleBatches() {
		
		// 3 batches, the last batch is not full
		List<ScannedFileHandleAssociation> associations = IntStream.range(0, FileHandleAssociationScannerJobManagerImpl.KINESIS_BATCH_SIZE * 3 - 1).boxed().map(i ->
			new ScannedFileHandleAssociation(Long.valueOf(i), Long.valueOf(i))
		).collect(Collectors.toList());
		
		long timestamp = System.currentTimeMillis();
		
		when(mockStackStatusDao.isStackReadWrite()).thenReturn(true);
		when(mockAssociationManager.scanRange(any(), any())).thenReturn(associations);
		when(mockClock.currentTimeMillis()).thenReturn(timestamp);
		
		List<Set<FileHandleAssociationRecord>> expectedBatches = ListUtils.partition(associations, FileHandleAssociationScannerJobManagerImpl.KINESIS_BATCH_SIZE).stream().map(partition -> 
			// Each partition is mapped to a set of records
			partition.stream().flatMap( a -> FileHandleScannerUtils.mapAssociation(associationType, a, timestamp).stream()).collect(Collectors.toSet())
		).collect(Collectors.toList());
		
		// Call under test
		int result = manager.processScanRangeRequest(scanRangeRequest);
		
		assertEquals(associations.size(), result);
		
		verify(mockStackStatusDao, times(expectedBatches.size() + 1)).isStackReadWrite();
		verify(mockAssociationManager).scanRange(scanRangeRequest.getAssociationType(), scanRangeRequest.getIdRange());
		verify(mockKinesisLogger, times(expectedBatches.size())).logBatch(eq(FileHandleAssociationRecord.KINESIS_STREAM_NAME), recordsCaptor.capture());
		
		List<List<FileHandleAssociationRecord>> batches = recordsCaptor.getAllValues();

		for (int i = 0; i < batches.size(); i++) {
			Set<FileHandleAssociationRecord> expectedRecords = expectedBatches.get(i);
			List<FileHandleAssociationRecord> batch = batches.get(i);
			
			assertEquals(expectedRecords.size(), batch.size());
			assertEquals(expectedRecords, new HashSet<>(batch));
			
		}
		
		verify(mockStatusDao).increaseJobCompletedCount(scanRangeRequest.getJobId());
		
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

		when(mockStackStatusDao.isStackReadWrite()).thenReturn(false); 
		
		String errorMessage = assertThrows(ReadOnlyException.class, () -> {
			// Call under test
			manager.processScanRangeRequest(scanRangeRequest);
		}).getMessage();

		assertEquals("The stack was in read-only mode.", errorMessage);
	}

}
