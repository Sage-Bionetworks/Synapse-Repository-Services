package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.file.FileHandleAssociationScanRangeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FileHandleAssociationScannerNotifierImpl implements FileHandleAssociationScannerNotifier {
	
	private static final String QUEUE_NAME = "FILE_HANDLE_SCAN_REQUEST";
	
	private ObjectMapper objectMapper;
	private AmazonSQS sqsClient;
	private String sqsQueueUrl;

	@Autowired
	public FileHandleAssociationScannerNotifierImpl(ObjectMapper objectMapper, AmazonSQS sqsClient) {
		this.objectMapper = objectMapper;
		this.sqsClient = sqsClient;
	}
	
	@Autowired
	public void configureQueue(StackConfiguration config) {
		this.sqsQueueUrl = sqsClient.getQueueUrl(config.getQueueName(QUEUE_NAME)).getQueueUrl();
	}
	
	@Override
	public String getQueueUrl() {
		return sqsQueueUrl;
	}

	@Override
	public FileHandleAssociationScanRangeRequest fromSqsMessage(Message message) {
		try {
			return objectMapper.readValue(message.getBody(), FileHandleAssociationScanRangeRequest.class);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Could not parse FileHandleAssociationScanRangeRequest from message: " + e.getMessage(), e);
		}
	}

	@Override
	public void sendScanRequest(FileHandleAssociationScanRangeRequest request, int delay) {
		String messageBody;
		
		try {
			messageBody = objectMapper.writeValueAsString(request);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Could not serialize FileHandleAssociationScanRangeRequest message: " + e.getMessage(), e);
		}
		
		SendMessageRequest sendMessageRequest = new SendMessageRequest()
				.withQueueUrl(sqsQueueUrl)
				.withMessageBody(messageBody)
				.withDelaySeconds(delay);
		
		sqsClient.sendMessage(sendMessageRequest);
	}

}
