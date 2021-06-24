package org.sagebionetworks.repo.manager.file;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FileHandleArchivalManagerImpl implements FileHandleArchivalManager {
	
	private static final String PROCESS_QUEUE_NAME = "FILE_KEY_ARCHIVE";

	private AmazonSQS sqsClient;
	private ObjectMapper objectMapper;
	private FileHandleDao fileHandleDao;
	private String processQueueUrl;
	
	@Autowired
	public FileHandleArchivalManagerImpl(AmazonSQS sqsClient, ObjectMapper objectMapper, FileHandleDao fileHandleDao) {
		this.sqsClient = sqsClient;
		this.fileHandleDao = fileHandleDao;
	}
	
	@Autowired
	public void configureQueue(StackConfiguration config) {
		this.processQueueUrl = sqsClient.getQueueUrl(config.getQueueName(PROCESS_QUEUE_NAME)).getQueueUrl();
	}

	@Override
	public FileHandleArchivalResponse processFileHandleArchivalRequest(UserInfo user, FileHandleArchivalRequest request) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(request, "The request");
		
		if (!user.isAdmin()) {
			throw new UnauthorizedException("Only administrators can access this service.");
		}
		
		Long count = 0L;
		
		// TODO
		return new FileHandleArchivalResponse().setCount(count);
	}

}
