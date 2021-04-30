package org.sagebionetworks.athena.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.athena.RecurrentAthenaQueryManager;
import org.sagebionetworks.repo.model.athena.RecurrentAthenaQueryResult;
import org.sagebionetworks.repo.model.exception.RecoverableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class RecurrentAthenaQueryWorkerTest {
	
	@Mock
	private ProgressCallback mockCallback;
	
	@Mock
	private Message mockMessage;
	
	@Mock
	private AmazonSQSClient mockSqsClient;
	
	@Mock
	private RecurrentAthenaQueryManager mockManager;
	
	@InjectMocks
	private RecurrentAthenaQueryWorker worker;
	
	@Mock
	private RecurrentAthenaQueryResult mockResult;
	
	private String queueUrl;
	
	@BeforeEach
	public void before() {
		this.queueUrl = "queueUrl";
		this.worker.setQueueName("queueName");		
		when(mockSqsClient.getQueueUrl(anyString())).thenReturn(new GetQueueUrlResult().withQueueUrl(queueUrl));
		this.worker.configure();
	}

	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		
		when(mockManager.fromSqsMessage(any())).thenReturn(mockResult);
				
		// Call under test
		worker.run(mockCallback, mockMessage);
		
		verify(mockManager).fromSqsMessage(mockMessage);
		verify(mockManager).processRecurrentAthenaQueryResult(mockResult, queueUrl);
	}
	
	@Test
	public void testRunWithRecoverableException() throws RecoverableMessageException, Exception {
		
		when(mockManager.fromSqsMessage(any())).thenReturn(mockResult);
		
		RecoverableException ex = new RecoverableException("Recover");
		
		doThrow(ex).when(mockManager).processRecurrentAthenaQueryResult(any(), any());
				
		String message = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			worker.run(mockCallback, mockMessage);
		}).getMessage();
		
		assertEquals("Recover", message);
		
		verify(mockManager).fromSqsMessage(mockMessage);
		verify(mockManager).processRecurrentAthenaQueryResult(mockResult, queueUrl);
	}

}
