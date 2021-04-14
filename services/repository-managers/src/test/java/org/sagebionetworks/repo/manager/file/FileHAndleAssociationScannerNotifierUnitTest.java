package org.sagebionetworks.repo.manager.file;

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
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;
import org.sagebionetworks.repo.model.file.IdRange;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class FileHAndleAssociationScannerNotifierUnitTest {

	@Mock
	private StackConfiguration mockConfig;
	
	@Mock
	private ObjectMapper mockObjectMapper;
	
	@Mock
	private AmazonSQS mockSqsClient;
	
	@Mock
	private GetQueueUrlResult mockQueueUrlResult;
	
	@InjectMocks
	private FileHandleAssociationScannerNotifierImpl notifier;
	
	@Mock
	private Message mockMessage;
	
	private String queueUrl;
	private String messageBody;
	private FileHandleAssociationScanRangeRequest request;
	
	@BeforeEach
	public void before() {
		queueUrl = "QueueUrl";
		messageBody = "{ \"jobId\": 123, \"associationType\": \"FileEntity\", \"idRange\": { \"minId\": 1, \"maxId\": 10000 } }";
		request = new FileHandleAssociationScanRangeRequest()
				.withJobId(123L)
				.withAssociationType(FileHandleAssociateType.FileEntity)
				.withIdRange(new IdRange(1, 10000));
		
		when(mockConfig.getQueueName(any())).thenReturn("QueueName");
		when(mockSqsClient.getQueueUrl(anyString())).thenReturn(mockQueueUrlResult);
		when(mockQueueUrlResult.getQueueUrl()).thenReturn(queueUrl);
		
		notifier.configureQueue(mockConfig);
		
		verify(mockConfig).getQueueName("FILE_HANDLE_SCAN_REQUEST");
		verify(mockSqsClient).getQueueUrl("QueueName");
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testFromSqsMessage() throws JsonProcessingException {
		when(mockMessage.getBody()).thenReturn(messageBody);
		when(mockObjectMapper.readValue(anyString(), any(Class.class))).thenReturn(request);
		
		// Call under test
		FileHandleAssociationScanRangeRequest result = notifier.fromSqsMessage(mockMessage);
		
		assertEquals(request, result);
		
		verify(mockMessage).getBody();
		verify(mockObjectMapper).readValue(messageBody, FileHandleAssociationScanRangeRequest.class);
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testFromSqsMessageWithException() throws JsonProcessingException {
		when(mockMessage.getBody()).thenReturn(messageBody);
		
		JsonProcessingException ex = new JsonParseException(null, "Some error");
		
		doThrow(ex).when(mockObjectMapper).readValue(anyString(), any(Class.class));
		
		IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			notifier.fromSqsMessage(mockMessage);			
		});
		
		assertEquals(ex, result.getCause());
		assertEquals("Could not parse FileHandleAssociationScanRangeRequest from message: Some error", result.getMessage());
		
	}
	
	@Test
	public void testSendScanRequest() throws JsonProcessingException {
		
		when(mockObjectMapper.writeValueAsString(any())).thenReturn(messageBody);
		
		int delay = 10;
		
		// Call under test
		notifier.sendScanRequest(request, delay);

		SendMessageRequest expectedRequest = new SendMessageRequest()
				.withQueueUrl(queueUrl)
				.withMessageBody(messageBody)
				.withDelaySeconds(delay);
		
		verify(mockObjectMapper).writeValueAsString(request);
		verify(mockSqsClient).sendMessage(expectedRequest);
	}
	
	@Test
	public void testSendScanRequestWithException() throws JsonProcessingException {
		
		JsonProcessingException ex = new JsonParseException(null, "Some error");
		
		doThrow(ex).when(mockObjectMapper).writeValueAsString(any());
		
		int delay = 10;
		
		IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			notifier.sendScanRequest(request, delay);
		});
		
		assertEquals(ex, result.getCause());
		assertEquals("Could not serialize FileHandleAssociationScanRangeRequest message: Some error", result.getMessage());
	}

}
