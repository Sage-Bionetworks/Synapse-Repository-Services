package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationScannerJobManager;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationScannerNotifier;
import org.sagebionetworks.repo.model.exception.RecoverableException;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class FileHandleAssociationScanRangeWorkerTest {

	@Mock
	private FileHandleAssociationScannerJobManager mockManager;
	
	@Mock
	private FileHandleAssociationScannerNotifier mockNotifier;
	
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
		
		when(mockNotifier.fromSqsMessage(any())).thenReturn(request);
		
		// Call under test
		worker.run(mockCallback, mockMessage);
		
		verify(mockNotifier).fromSqsMessage(mockMessage);
		verify(mockManager).processScanRangeRequest(request);
		Map<String, String> expectedDimensions = Collections.singletonMap("AssociationType", "FileEntity");
		verify(mockWorkerLogger).logWorkerTimeMetric(eq(FileHandleAssociationScanRangeWorker.class), anyLong(), eq(expectedDimensions));
	}
	
	@Test
	public void testRunWithRecoverableException() throws RecoverableMessageException, Exception {
		
		when(mockNotifier.fromSqsMessage(any())).thenReturn(request);
		
		RecoverableException ex = new RecoverableException("Some ex");
		
		doThrow(ex).when(mockManager).processScanRangeRequest(any());
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			worker.run(mockCallback, mockMessage);
		});
		
		assertEquals(ex, result.getCause());
		
		verify(mockNotifier).fromSqsMessage(mockMessage);
		verify(mockManager).processScanRangeRequest(request);
		verify(mockWorkerLogger).logWorkerCountMetric(FileHandleAssociationScanRangeWorker.class, "JobRetryCount");
		Map<String, String> expectedDimensions = Collections.singletonMap("AssociationType", "FileEntity");
		verify(mockWorkerLogger).logWorkerTimeMetric(eq(FileHandleAssociationScanRangeWorker.class), anyLong(), eq(expectedDimensions));
	}
	
	@Test
	public void testRunWithUnrecoverableException() throws RecoverableMessageException, Exception {
		
		when(mockNotifier.fromSqsMessage(any())).thenReturn(request);
		
		RuntimeException ex = new RuntimeException("Some ex");
		
		doThrow(ex).when(mockManager).processScanRangeRequest(any());
		
		// Call under test
		worker.run(mockCallback, mockMessage);
				
		verify(mockNotifier).fromSqsMessage(mockMessage);
		verify(mockManager).processScanRangeRequest(request);
		verify(mockWorkerLogger).logWorkerCountMetric(FileHandleAssociationScanRangeWorker.class, "JobFailedCount");
		Map<String, String> expectedDimensions = Collections.singletonMap("AssociationType", "FileEntity");
		verify(mockWorkerLogger).logWorkerTimeMetric(eq(FileHandleAssociationScanRangeWorker.class), anyLong(), eq(expectedDimensions));
	}
	
	@Test
	public void testRunWithMessageParsingError() throws RecoverableMessageException, Exception {
		
		RuntimeException ex = new RuntimeException("Some error");
		
		doThrow(ex).when(mockNotifier).fromSqsMessage(any());
		
		// Call under test
		worker.run(mockCallback, mockMessage);
		
		verify(mockNotifier).fromSqsMessage(mockMessage);
		verify(mockWorkerLogger).logWorkerCountMetric(FileHandleAssociationScanRangeWorker.class, "ParseMessageErrorCount");
		verifyZeroInteractions(mockManager);
	}

}
