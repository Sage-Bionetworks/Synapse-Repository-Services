package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationScannerJobManager;
import org.sagebionetworks.repo.model.IdRange;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class FileHandleAssociationScanRangeWorkerTest {

	@Mock
	private FileHandleAssociationScannerJobManager mockManager;
		
	@Mock
	private WorkerLogger mockWorkerLogger;
	
	@Mock
	private ProgressCallback mockCallback;
	
	@Mock
	private Message mockMessage;
	
	@InjectMocks
	private FileHandleAssociationScanRangeWorker worker;
	
	private FileHandleAssociationScanRangeRequest request;
	
	@BeforeEach
	public void before() {
		request = new FileHandleAssociationScanRangeRequest()
				.withJobId(123L)
				.withAssociationType(FileHandleAssociateType.FileEntity)
				.withIdRange(new IdRange(1, 10000));
	}
	
	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		
		boolean allJobsCompleted = false;
		
		when(mockManager.isScanJobCompleted(anyLong())).thenReturn(allJobsCompleted);
		
		// Call under test
		worker.run(mockCallback, mockMessage, request);
		
		verify(mockManager).processScanRangeRequest(request);
		Map<String, String> expectedDimensions = Collections.singletonMap("AssociationType", "FileEntity");
		verify(mockWorkerLogger).logWorkerCountMetric(FileHandleAssociationScanRangeWorker.class, "JobCompletedCount");
		verify(mockWorkerLogger).logWorkerTimeMetric(eq(FileHandleAssociationScanRangeWorker.class), anyLong(), eq(expectedDimensions));
		verifyNoMoreInteractions(mockWorkerLogger);
	}
	
	@Test
	public void testRunWithAllJobsCompleted() throws RecoverableMessageException, Exception {
		
		boolean allJobsCompleted = true;
		
		when(mockManager.isScanJobCompleted(anyLong())).thenReturn(allJobsCompleted);
		
		// Call under test
		worker.run(mockCallback, mockMessage, request);
		
		verify(mockManager).processScanRangeRequest(request);
		Map<String, String> expectedDimensions = Collections.singletonMap("AssociationType", "FileEntity");
		verify(mockWorkerLogger).logWorkerCountMetric(FileHandleAssociationScanRangeWorker.class, "JobCompletedCount");
		verify(mockWorkerLogger).logWorkerCountMetric(FileHandleAssociationScanRangeWorker.class, "AllJobsCompletedCount");
		verify(mockWorkerLogger).logWorkerTimeMetric(eq(FileHandleAssociationScanRangeWorker.class), anyLong(), eq(expectedDimensions));
		verifyNoMoreInteractions(mockWorkerLogger);
	}
	
	@Test
	public void testRunWithRecoverableException() throws RecoverableMessageException, Exception {
				
		RecoverableMessageException ex = new RecoverableMessageException("Some ex");
		
		doThrow(ex).when(mockManager).processScanRangeRequest(any());
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			worker.run(mockCallback, mockMessage, request);
		});
		
		assertEquals(ex, result);
		
		verify(mockManager).processScanRangeRequest(request);
		verify(mockWorkerLogger).logWorkerCountMetric(FileHandleAssociationScanRangeWorker.class, "JobRetryCount");
		Map<String, String> expectedDimensions = Collections.singletonMap("AssociationType", "FileEntity");
		verify(mockWorkerLogger).logWorkerTimeMetric(eq(FileHandleAssociationScanRangeWorker.class), anyLong(), eq(expectedDimensions));
	}
	
	@Test
	public void testRunWithUnrecoverableException() throws RecoverableMessageException, Exception {
		
		RuntimeException ex = new RuntimeException("Some ex");
		
		doThrow(ex).when(mockManager).processScanRangeRequest(any());
		
		// Call under test
		worker.run(mockCallback, mockMessage, request);
			
		verify(mockManager).processScanRangeRequest(request);
		verify(mockWorkerLogger).logWorkerCountMetric(FileHandleAssociationScanRangeWorker.class, "JobFailedCount");
		Map<String, String> expectedDimensions = Collections.singletonMap("AssociationType", "FileEntity");
		verify(mockWorkerLogger).logWorkerTimeMetric(eq(FileHandleAssociationScanRangeWorker.class), anyLong(), eq(expectedDimensions));
	}
}
