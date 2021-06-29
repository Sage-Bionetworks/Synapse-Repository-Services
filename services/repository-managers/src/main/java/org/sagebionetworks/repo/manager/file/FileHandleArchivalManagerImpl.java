package org.sagebionetworks.repo.manager.file;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.CannotDetermineBucketLocationException;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.BucketAndKey;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleMetadataType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;
import org.sagebionetworks.repo.model.file.FileHandleKeyArchiveResult;
import org.sagebionetworks.repo.model.file.FileHandleKeysArchiveRequest;
import org.sagebionetworks.repo.model.file.FileHandleStatus;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Tag;
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

	static final int S3_DELETE_BATCH_SIZE = 1000;
	
	private AmazonSQS sqsClient;
	private SynapseS3Client s3Client;
	private ObjectMapper objectMapper;
	private FileHandleDao fileHandleDao;
	private DBOBasicDao basicDao;
	
	private String processQueueUrl;
	private String bucketName;
	
	@Autowired
	public FileHandleArchivalManagerImpl(AmazonSQS sqsClient, SynapseS3Client s3Client, ObjectMapper objectMapper, FileHandleDao fileHandleDao, DBOBasicDao basicDao) {
		this.sqsClient = sqsClient;
		this.s3Client = s3Client;
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
	public FileHandleKeysArchiveRequest parseArchiveKeysRequestFromSqsMessage(Message message) {
		ValidateArgument.required(message, "The message");
		
		try {
			return objectMapper.readValue(message.getBody(), FileHandleKeysArchiveRequest.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not deserialize FileHandleKeysArchiveRequest message: " + e.getMessage(), e);
		}
	}
	
	@Override
	@WriteTransaction
	public FileHandleKeyArchiveResult archiveUnlinkedFileHandlesByKey(UserInfo user, String bucketName, String key, Instant modifedBefore) {
		ValidateArgument.required(user, "The userInfo");
		ValidateArgument.requiredNotBlank(bucketName, "The bucketName");
		ValidateArgument.requiredNotBlank(key, "The key");
		ValidateArgument.required(modifedBefore, "The modifiedBefore");
		
		if (!user.isAdmin()) {
			throw new UnauthorizedException("Only administrators can access this service.");
		}
		
		final int archived = fileHandleDao.updateStatusByBucketAndKey(bucketName, key, FileHandleStatus.ARCHIVED, FileHandleStatus.UNLINKED, modifedBefore);
		
		if (archived <= 0) {
			return new FileHandleKeyArchiveResult(archived, false);
		}
		
		final int availableAfterUpdate = fileHandleDao.getAvailableOrEarlyUnlinkedFileHandlesCount(bucketName, key, modifedBefore);
		
		boolean tagged = false;
		boolean keyUnavailable = false;
		
		// The key is not referenced anymore by any available (or unlinked but too early) file handles, we can proceed and tag the objects in S3
		if (availableAfterUpdate <= 0) {
			try {
				tagged = tagObjectForArchival(bucketName, key);
			} catch (AmazonServiceException ex) {
				if (ex instanceof AmazonS3Exception && HttpStatus.SC_NOT_FOUND == ex.getStatusCode()) {
					LOG.warn("Attempted to tag key {} in bucket {} for archival but the object didn't exist: {}", key, bucketName, ex.getMessage());
					keyUnavailable = true;
				} else if (ErrorType.Service.equals(ex.getErrorType())) {
					throw new RecoverableMessageException(ex);
				} else {
					throw ex;
				}
			} catch (CannotDetermineBucketLocationException ex) {
				LOG.warn("Attempted to tag key {} in bucket {} for archival but the bucket didn't exist: {}", key, bucketName, ex.getMessage());
				keyUnavailable = true;
			}
			
		}
		
		cleanupArchivedFileHandlesPreviews(bucketName, key);
		
		if (keyUnavailable) {
			fileHandleDao.deleteUnavailableByBucketAndKey(bucketName, key);
		}
		
		return new FileHandleKeyArchiveResult(archived, tagged);
	}

	boolean tagObjectForArchival(String bucketName, String key) {
		List<Tag> objectTags = s3Client.getObjectTags(bucketName, key);

		if (objectTags == null || objectTags.isEmpty()) {
			objectTags = new ArrayList<>();
		} else {
			objectTags = new ArrayList<>(objectTags);
		}

		if (objectTags.stream().filter(tag -> tag.getKey().equals(S3_TAG_ARCHIVED.getKey())).findFirst().isPresent()) {
			return false;
		}
		
		objectTags.add(S3_TAG_ARCHIVED);
		
		s3Client.setObjectTags(bucketName, key, objectTags);
		
		return true;
	}
	
	void cleanupArchivedFileHandlesPreviews(String bucketName, String key) {
		Set<Long> clearedPreviewIds = fileHandleDao.clearPreviewByKeyAndStatus(bucketName, key, FileHandleStatus.ARCHIVED);
		
		if (clearedPreviewIds.isEmpty()) {
			return;
		}
		
		Set<Long> referencedPreviewIds = fileHandleDao.getReferencedPreviews(clearedPreviewIds);
		
		Set<Long> unreferencedPreviewIds = clearedPreviewIds.stream().filter(id -> !referencedPreviewIds.contains(id)).collect(Collectors.toSet());

		if (unreferencedPreviewIds.isEmpty()) {
			return;
		}
		
		Set<BucketAndKey> bucketAndKeys = fileHandleDao.getBucketAndKeyBatch(unreferencedPreviewIds);
		
		fileHandleDao.deleteBatch(unreferencedPreviewIds);
		
		for (BucketAndKey bucketAndKey : bucketAndKeys) {
			String previewBucket = bucketAndKey.getBucket();
			String previewKey = bucketAndKey.getKey();
			
			if (fileHandleDao.getNumberOfReferencesToFile(FileHandleMetadataType.S3, previewBucket, previewKey) > 0) {
				continue;
			}
			
			try {
				s3Client.deleteObject(previewBucket, previewKey);
			}  catch (AmazonServiceException ex) {
				if (ErrorType.Service.equals(ex.getErrorType())) {
					throw new RecoverableMessageException(ex);
				}
				throw ex;
			} catch (CannotDetermineBucketLocationException ex) {
				LOG.warn("Attempted to delete preview key {} in bucket {} but the bucket didn't exist: {}", previewKey, previewBucket, ex.getMessage());
			}
		}
		
	}
	
	private void pushAndClearBatch(Instant modifiedBefore, String bucketName, List<String> keysBatch) {
		if (keysBatch.isEmpty()) {
			return;
		}
		
		FileHandleKeysArchiveRequest request = new FileHandleKeysArchiveRequest()
				.withBucket(bucketName)
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
	
	

}
