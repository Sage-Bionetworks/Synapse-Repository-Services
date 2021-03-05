package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
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
import org.sagebionetworks.repo.model.exception.RecoverableException;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;
import org.sagebionetworks.repo.model.file.IdRange;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class FileHandleAssociationScanRangeWorkerTest {

	@Mock
	private FileHandleAssociationScannerJobManager mockManager;
	
	@Mock
	private ObjectMapper mockObjectMapper;
	
	@Mock
	private WorkerLogger mockWorkerLogger;
	
	@Mock
	private ProgressCallback mockCallback;
	
	@Mock
	private Message mockMessage;
	
	@InjectMocks
	private FileHandleAssociationScanRangeWorker worker;
	
	private String messageBody;
	private FileHandleAssociationScanRangeRequest request;
	
	@BeforeEach
	public void before() {
		messageBody = "{ \"jobId\": 123, \"associationType\": \"FileEntity\", \"idRange\": { \"minId\": 1, \"maxId\": 10000 } }";
		request = new FileHandleAssociationScanRangeRequest()
				.withJobId(123L)
				.withAssociationType(FileHandleAssociateType.FileEntity)
				.withIdRange(new IdRange(1, 10000));
		
		when(mockMessage.getBody()).thenReturn(messageBody);
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRun() throws RecoverableMessageException, Exception {
		
		when(mockObjectMapper.readValue(anyString(), any(Class.class))).thenReturn(request);
		
		// Call under test
		worker.run(mockCallback, mockMessage);
		
		verify(mockObjectMapper).readValue(messageBody, FileHandleAssociationScanRangeRequest.class);
		verify(mockManager).processScanRangeRequest(request);
		Map<String, String> expectedDimensions = Collections.singletonMap("AssociationType", "FileEntity");
		verify(mockWorkerLogger).logWorkerTimeMetric(eq(FileHandleAssociationScanRangeWorker.class), anyLong(), eq(expectedDimensions));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRunWithRecoverableException() throws RecoverableMessageException, Exception {
		
		when(mockObjectMapper.readValue(anyString(), any(Class.class))).thenReturn(request);
		
		RecoverableException ex = new RecoverableException("Some ex");
		
		doThrow(ex).when(mockManager).processScanRangeRequest(any());
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			worker.run(mockCallback, mockMessage);
		});
		
		assertEquals(ex, result.getCause());
		
		verify(mockObjectMapper).readValue(messageBody, FileHandleAssociationScanRangeRequest.class);
		verify(mockManager).processScanRangeRequest(request);
		verify(mockWorkerLogger).logWorkerCountMetric(FileHandleAssociationScanRangeWorker.class, "JobRetryCount");
		Map<String, String> expectedDimensions = Collections.singletonMap("AssociationType", "FileEntity");
		verify(mockWorkerLogger).logWorkerTimeMetric(eq(FileHandleAssociationScanRangeWorker.class), anyLong(), eq(expectedDimensions));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRunWithUnrecoverableException() throws RecoverableMessageException, Exception {
		
		when(mockObjectMapper.readValue(anyString(), any(Class.class))).thenReturn(request);
		
		RuntimeException ex = new RuntimeException("Some ex");
		
		doThrow(ex).when(mockManager).processScanRangeRequest(any());
		
		RuntimeException result = assertThrows(RuntimeException.class, () -> {			
			// Call under test
			worker.run(mockCallback, mockMessage);
		});
		
		assertEquals(ex, result);
		
		verify(mockObjectMapper).readValue(messageBody, FileHandleAssociationScanRangeRequest.class);
		verify(mockManager).processScanRangeRequest(request);
		verify(mockWorkerLogger).logWorkerCountMetric(FileHandleAssociationScanRangeWorker.class, "JobFailedCount");
		Map<String, String> expectedDimensions = Collections.singletonMap("AssociationType", "FileEntity");
		verify(mockWorkerLogger).logWorkerTimeMetric(eq(FileHandleAssociationScanRangeWorker.class), anyLong(), eq(expectedDimensions));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testRunWithMessageParsingError() throws RecoverableMessageException, Exception {
		
		RuntimeException ex = new RuntimeException("Some error");
		
		doThrow(ex).when(mockObjectMapper).readValue(anyString(), any(Class.class));
		
		RuntimeException result = assertThrows(RuntimeException.class, () -> {
			// Call under test
			worker.run(mockCallback, mockMessage);
		});
		
		assertEquals(ex, result);
		
		verify(mockObjectMapper).readValue(messageBody, FileHandleAssociationScanRangeRequest.class);
		verify(mockWorkerLogger).logWorkerCountMetric(FileHandleAssociationScanRangeWorker.class, "ParseMessageErrorCount");
	}

}
