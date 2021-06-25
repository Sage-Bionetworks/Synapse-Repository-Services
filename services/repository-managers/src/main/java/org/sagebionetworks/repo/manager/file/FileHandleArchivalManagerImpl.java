package org.sagebionetworks.repo.manager.file;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;
import org.sagebionetworks.repo.model.file.FileHandleKeysArchiveRequest;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FileHandleArchivalManagerImpl implements FileHandleArchivalManager {
	
	private static final Logger LOG = LogManager.getLogger(FileHandleArchivalManagerImpl.class);
	
	static final int ARCHIVE_BUFFER_DAYS = 30;
	static final int SCAN_WINDOW_DAYS = 7;
	
	static final int DEFAULT_ARCHIVE_LIMIT = 100_000;
	
	static final int KEYS_PER_MESSAGE = 100;
	static final String PROCESS_QUEUE_NAME = "FILE_KEY_ARCHIVE";

	private AmazonSQS sqsClient;
	private ObjectMapper objectMapper;
	private FileHandleDao fileHandleDao;
	private DBOBasicDao basicDao;
	private String processQueueUrl;
	private String bucketName;
	
	@Autowired
	public FileHandleArchivalManagerImpl(AmazonSQS sqsClient, ObjectMapper objectMapper, FileHandleDao fileHandleDao, DBOBasicDao basicDao) {
		this.sqsClient = sqsClient;
		this.objectMapper = objectMapper;
		this.fileHandleDao = fileHandleDao;
		this.basicDao = basicDao;
	}
	
	@Autowired
	public void configureQueue(StackConfiguration config) {
		this.processQueueUrl = sqsClient.getQueueUrl(config.getQueueName(PROCESS_QUEUE_NAME)).getQueueUrl();
		this.bucketName = config.getS3Bucket();
	}

	@Override
	public FileHandleArchivalResponse processFileHandleArchivalRequest(UserInfo user, FileHandleArchivalRequest request) {
		ValidateArgument.required(user, "The user");
		ValidateArgument.required(request, "The request");
		ValidateArgument.requirement(request.getLimit() == null || (request.getLimit() > 0 && request.getLimit() <= DEFAULT_ARCHIVE_LIMIT), "If supplied the limit must be in the range (0, " + DEFAULT_ARCHIVE_LIMIT + "]");
		
		if (!user.isAdmin()) {
			throw new UnauthorizedException("Only administrators can access this service.");
		}
		
		int limit = request.getLimit() == null ? DEFAULT_ARCHIVE_LIMIT : request.getLimit().intValue();
		
		long timestamp = basicDao.getDatabaseTimestampMillis();
		
		Instant now = Instant.ofEpochMilli(timestamp);
		Instant modifiedBefore = now.minus(ARCHIVE_BUFFER_DAYS, ChronoUnit.DAYS);
		Instant modifiedAfter = modifiedBefore.minus(SCAN_WINDOW_DAYS, ChronoUnit.DAYS);
		
		List<String> unlinkedKeys = fileHandleDao.getUnlinkedKeysForBucket(bucketName, modifiedBefore, modifiedAfter, limit);
		List<String> keysBatch = new ArrayList<>(KEYS_PER_MESSAGE);

		for (String key : unlinkedKeys) {
			keysBatch.add(key);
			if (keysBatch.size() >= KEYS_PER_MESSAGE) {
				pushAndClearBatch(modifiedBefore, bucketName, keysBatch);
			}
		}

		pushAndClearBatch(modifiedBefore, bucketName, keysBatch);
		
		return new FileHandleArchivalResponse().setCount(Long.valueOf(unlinkedKeys.size()));
	}
	
	@Override
	public void processFileHandleKeyArchiveRequest(Message message) {
		FileHandleKeysArchiveRequest request = fromSqsMessage(message);

		LOG.info(request);
		
		// TODO
	}
	
	void pushAndClearBatch(Instant modifiedBefore, String bucket, List<String> keysBatch) {
		if (keysBatch.isEmpty()) {
			return;
		}
		
		FileHandleKeysArchiveRequest request = new FileHandleKeysArchiveRequest()
				.withBucket(bucket)
				.withModifiedBefore(modifiedBefore.toEpochMilli())
				.withKeys(new ArrayList<>(keysBatch));
		
		String messageBody;
		
		try {
			messageBody = objectMapper.writeValueAsString(request);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not serialize FileHandleKeysArchiveRequest message: " + e.getMessage(), e);
		}
		
		sqsClient.sendMessage(processQueueUrl, messageBody);
		
		keysBatch.clear();
	}
	
	FileHandleKeysArchiveRequest fromSqsMessage(Message message) {
		ValidateArgument.required(message, "The message");
		
		try {
			return objectMapper.readValue(message.getBody(), FileHandleKeysArchiveRequest.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not deserialize FileHandleKeysArchiveRequest message: " + e.getMessage(), e);
		}
	}

}
