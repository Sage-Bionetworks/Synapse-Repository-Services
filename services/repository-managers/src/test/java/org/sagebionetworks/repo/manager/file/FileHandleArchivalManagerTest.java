package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static  org.sagebionetworks.repo.manager.file.FileHandleArchivalManagerImpl.ARCHIVE_BUFFER_DAYS;
import static  org.sagebionetworks.repo.manager.file.FileHandleArchivalManagerImpl.DEFAULT_ARCHIVE_LIMIT;
import static  org.sagebionetworks.repo.manager.file.FileHandleArchivalManagerImpl.KEYS_PER_MESSAGE;
import static  org.sagebionetworks.repo.manager.file.FileHandleArchivalManagerImpl.PROCESS_QUEUE_NAME;
import static  org.sagebionetworks.repo.manager.file.FileHandleArchivalManagerImpl.SCAN_WINDOW_DAYS;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.ListUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class FileHandleArchivalManagerTest {
	
	@Mock
	private StackConfiguration mockConfig;
	
	@Mock
	private ObjectMapper mockMapper;
	
	@Mock
	private AmazonSQS mockSqs;
	
	@Mock
	private SynapseS3Client mockS3Client;
	
	@Mock
	private DBOBasicDao mockBasicDao;
	
	@Mock
	private FileHandleDao mockFileDao;
	
	@InjectMocks
	private FileHandleArchivalManagerImpl manager;
	
	@Mock
	private UserInfo mockUser;
	
	@Mock
	private FileHandleArchivalRequest mockRequest;
	
	@Mock
	private FileHandleKeysArchiveRequest mockKeysArchiveRequest;
	
	@Mock
	private Message mockMessage;
	
	@Captor
	private ArgumentCaptor<FileHandleKeysArchiveRequest> requestCaptor;
	
	private String queueUrl = "queueUrl";
	private String bucket = "bucket";
	
	@BeforeEach
	public void setup() {
		when(mockConfig.getS3Bucket()).thenReturn(bucket);
		when(mockConfig.getQueueName(anyString())).thenReturn("queueName");
		when(mockSqs.getQueueUrl(anyString())).thenReturn(new GetQueueUrlResult().withQueueUrl(queueUrl));
		
		// This is invoked automatically by spring
		manager.configureQueue(mockConfig);
		
		verify(mockConfig).getQueueName(PROCESS_QUEUE_NAME);
		verify(mockSqs).getQueueUrl("queueName");
	}
	
	@Test
	public void testProcessArchivalRequest() throws JsonProcessingException {
		
		int limit = 100_000;
		long timestamp = 1624584423000L;
		List<String> keys = Arrays.asList("key1", "key2");
		
		when(mockBasicDao.getDatabaseTimestampMillis()).thenReturn(timestamp);
		when(mockRequest.getLimit()).thenReturn(Long.valueOf(limit));
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockFileDao.getUnlinkedKeysForBucket(any(), any(), any(), anyInt())).thenReturn(keys);
		when(mockMapper.writeValueAsString(any())).thenReturn("messageBody");
		
		FileHandleArchivalResponse expectedResponse = new FileHandleArchivalResponse().setCount(Long.valueOf(keys.size()));
		Instant expectedModifiedBefore = Instant.ofEpochMilli(timestamp).minus(ARCHIVE_BUFFER_DAYS, ChronoUnit.DAYS);
		Instant expectedModifiedAfter = expectedModifiedBefore.minus(SCAN_WINDOW_DAYS, ChronoUnit.DAYS);
		FileHandleKeysArchiveRequest expectedMessage = new FileHandleKeysArchiveRequest()
				.withBucket(bucket)
				.withModifiedBefore(expectedModifiedBefore.toEpochMilli())
				.withKeys(keys);
		
		// Call under test
		FileHandleArchivalResponse response = manager.processFileHandleArchivalRequest(mockUser, mockRequest);
		
		assertEquals(expectedResponse, response);
		
		verify(mockFileDao).getUnlinkedKeysForBucket(bucket, expectedModifiedBefore, expectedModifiedAfter, limit);
		verify(mockMapper).writeValueAsString(expectedMessage);
		verify(mockSqs).sendMessage(queueUrl, "messageBody");
		
	}
	
	@Test
	public void testProcessArchivalRequestWithDefaultLimit() throws JsonProcessingException {
		
		long timestamp = 1624584423000L;
		List<String> keys = Arrays.asList("key1", "key2");
		
		when(mockBasicDao.getDatabaseTimestampMillis()).thenReturn(timestamp);
		when(mockRequest.getLimit()).thenReturn(null);
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockFileDao.getUnlinkedKeysForBucket(any(), any(), any(), anyInt())).thenReturn(keys);
		when(mockMapper.writeValueAsString(any())).thenReturn("messageBody");
		
		FileHandleArchivalResponse expectedResponse = new FileHandleArchivalResponse().setCount(Long.valueOf(keys.size()));
		Instant expectedModifiedBefore = Instant.ofEpochMilli(timestamp).minus(ARCHIVE_BUFFER_DAYS, ChronoUnit.DAYS);
		Instant expectedModifiedAfter = expectedModifiedBefore.minus(SCAN_WINDOW_DAYS, ChronoUnit.DAYS);
		FileHandleKeysArchiveRequest expectedMessage = new FileHandleKeysArchiveRequest()
				.withBucket(bucket)
				.withModifiedBefore(expectedModifiedBefore.toEpochMilli())
				.withKeys(keys);
		
		// Call under test
		FileHandleArchivalResponse response = manager.processFileHandleArchivalRequest(mockUser, mockRequest);
		
		assertEquals(expectedResponse, response);
		
		verify(mockFileDao).getUnlinkedKeysForBucket(bucket, expectedModifiedBefore, expectedModifiedAfter, DEFAULT_ARCHIVE_LIMIT);
		verify(mockMapper).writeValueAsString(expectedMessage);
		verify(mockSqs).sendMessage(queueUrl, "messageBody");
		
	}
	
	@Test
	public void testProcessArchivalRequestWithMultipleKeysBatches() throws JsonProcessingException {
		
		int limit = 100_000;
		long timestamp = 1624584423000L;
		
		List<String> keys = IntStream.range(1, KEYS_PER_MESSAGE * 3 + 1).boxed().map(i -> "key_" + i).collect(Collectors.toList());
		
		when(mockBasicDao.getDatabaseTimestampMillis()).thenReturn(timestamp);
		when(mockRequest.getLimit()).thenReturn(Long.valueOf(limit));
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockFileDao.getUnlinkedKeysForBucket(any(), any(), any(), anyInt())).thenReturn(keys);
		when(mockMapper.writeValueAsString(any())).thenReturn("messageBody");
		
		FileHandleArchivalResponse expectedResponse = new FileHandleArchivalResponse().setCount(Long.valueOf(keys.size()));
		Instant expectedModifiedBefore = Instant.ofEpochMilli(timestamp).minus(ARCHIVE_BUFFER_DAYS, ChronoUnit.DAYS);
		Instant expectedModifiedAfter = expectedModifiedBefore.minus(SCAN_WINDOW_DAYS, ChronoUnit.DAYS);
		List<FileHandleKeysArchiveRequest> expectedRequests = ListUtils.partition(keys, KEYS_PER_MESSAGE).stream()
				.map( batch -> new FileHandleKeysArchiveRequest().withKeys(batch).withBucket(bucket).withModifiedBefore(expectedModifiedBefore.toEpochMilli()))
				.collect(Collectors.toList());
		
		// Call under test
		FileHandleArchivalResponse response = manager.processFileHandleArchivalRequest(mockUser, mockRequest);
		
		assertEquals(expectedResponse, response);
		
		verify(mockFileDao).getUnlinkedKeysForBucket(bucket, expectedModifiedBefore, expectedModifiedAfter, limit);
		verify(mockMapper, times(keys.size()/KEYS_PER_MESSAGE)).writeValueAsString(requestCaptor.capture());
		assertEquals(expectedRequests, requestCaptor.getAllValues());
		verify(mockSqs, times(keys.size()/KEYS_PER_MESSAGE)).sendMessage(queueUrl, "messageBody");
		
	}
		
	@Test
	public void testProcessArchivalRequestWithSerializeException() throws JsonProcessingException {
		
		int limit = 100_000;
		long timestamp = 1624584423000L;
		List<String> keys = Arrays.asList("key1", "key2");
		
		when(mockBasicDao.getDatabaseTimestampMillis()).thenReturn(timestamp);
		when(mockRequest.getLimit()).thenReturn(Long.valueOf(limit));
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockFileDao.getUnlinkedKeysForBucket(any(), any(), any(), anyInt())).thenReturn(keys);
		
		JsonProcessingException ex = new JsonParseException(null, "error");
		
		doThrow(ex).when(mockMapper).writeValueAsString(any());
		
		Instant expectedModifiedBefore = Instant.ofEpochMilli(timestamp).minus(ARCHIVE_BUFFER_DAYS, ChronoUnit.DAYS);
		Instant expectedModifiedAfter = expectedModifiedBefore.minus(SCAN_WINDOW_DAYS, ChronoUnit.DAYS);
		FileHandleKeysArchiveRequest expectedMessage = new FileHandleKeysArchiveRequest()
				.withBucket(bucket)
				.withModifiedBefore(expectedModifiedBefore.toEpochMilli())
				.withKeys(keys);
		
		assertThrows(IllegalStateException.class, () -> {
			// Call under test
			manager.processFileHandleArchivalRequest(mockUser, mockRequest);
		});
		
		verify(mockFileDao).getUnlinkedKeysForBucket(bucket, expectedModifiedBefore, expectedModifiedAfter, limit);
		verify(mockMapper).writeValueAsString(expectedMessage);
		verifyNoMoreInteractions(mockSqs);
		
	}
	
	@Test
	public void testProcessArchivalRequestWithNotAdmin() {
		
		when(mockRequest.getLimit()).thenReturn(null);
		when(mockUser.isAdmin()).thenReturn(false);
		
		UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.processFileHandleArchivalRequest(mockUser, mockRequest);			
		});
		
		assertEquals("Only administrators can access this service.", ex.getMessage());
				
	}
	
	@Test
	public void testProcessArchivalRequestWithNoUser() {
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processFileHandleArchivalRequest(null, mockRequest);			
		});
		
		assertEquals("The user is required.", ex.getMessage());
				
	}
	
	@Test
	public void testProcessArchivalRequestWithNoRequest() {
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processFileHandleArchivalRequest(mockUser, null);			
		});
		
		assertEquals("The request is required.", ex.getMessage());
				
	}
	
	@Test
	public void testProcessArchivalRequestWithNegativeLimit() {
		
		when(mockRequest.getLimit()).thenReturn(-1L);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processFileHandleArchivalRequest(mockUser, mockRequest);			
		});
		
		assertEquals("If supplied the limit must be in the range (0, 100000]", ex.getMessage());
				
	}
	
	@Test
	public void testProcessArchivalRequestWithExceedLimit() {
		
		when(mockRequest.getLimit()).thenReturn(DEFAULT_ARCHIVE_LIMIT + 1L);
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.processFileHandleArchivalRequest(mockUser, mockRequest);			
		});
		
		assertEquals("If supplied the limit must be in the range (0, 100000]", ex.getMessage());
				
	}
	
	@Test
	public void testParseArchiveKeysRequestFromSqsMessage() throws JsonProcessingException {
		
		when(mockMessage.getBody()).thenReturn("Message");
		when(mockMapper.readValue(anyString(), any(Class.class))).thenReturn(mockKeysArchiveRequest);
		
		// Call under test
		FileHandleKeysArchiveRequest result = manager.parseArchiveKeysRequestFromSqsMessage(mockMessage);
		
		assertEquals(mockKeysArchiveRequest, result);
		
		verify(mockMapper).readValue("Message", FileHandleKeysArchiveRequest.class);
	}
	
	@Test
	public void testParseArchiveKeysRequestFromSqsMessageWithParseEx() throws JsonProcessingException {
		
		when(mockMessage.getBody()).thenReturn("Message");
		
		JsonProcessingException ex = new JsonParseException(null, "Some error");
		
		doThrow(ex).when(mockMapper).readValue(anyString(), any(Class.class));
		
		IllegalStateException result = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			manager.parseArchiveKeysRequestFromSqsMessage(mockMessage);
		});
		
		assertEquals(ex, result.getCause());
		assertEquals("Could not deserialize FileHandleKeysArchiveRequest message: Some error", result.getMessage());
		verify(mockMapper).readValue("Message", FileHandleKeysArchiveRequest.class);
		
	}
	
	@Test
	public void testArchiveUnlinkedFileHandlesByKey() {
		String key = "key1";
		
		Instant modifiedBefore = Instant.parse("2021-02-03T10:00:00.00Z");
		int updated = 2;
		int availableAfterUpdate = 0;
		
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockFileDao.updateStatusByBucketAndKey(anyString(), anyString(), any(), any(), any())).thenReturn(updated);
		when(mockFileDao.getAvailableOrEarlyUnlinkedFileHandlesCount(anyString(), anyString(), any())).thenReturn(availableAfterUpdate);
		when(mockS3Client.getObjectTags(anyString(), anyString())).thenReturn(Collections.emptyList());
		when(mockFileDao.clearPreviewByKeyAndStatus(any(), any(), any())).thenReturn(Collections.emptySet());
		
		FileHandleKeyArchiveResult expected = new FileHandleKeyArchiveResult(updated, true);
		
		// Call under test
		FileHandleKeyArchiveResult result = manager.archiveUnlinkedFileHandlesByKey(mockUser, bucket, key, modifiedBefore);
		
		assertEquals(expected, result);
		
		verify(mockFileDao).updateStatusByBucketAndKey(bucket, key, FileHandleStatus.ARCHIVED, FileHandleStatus.UNLINKED, modifiedBefore);
		verify(mockFileDao).getAvailableOrEarlyUnlinkedFileHandlesCount(bucket, key, modifiedBefore);
		verify(mockS3Client).getObjectTags(bucket, key);
		verify(mockS3Client).setObjectTags(bucket, key, Arrays.asList(FileHandleArchivalManagerImpl.S3_TAG_ARCHIVED));
		verify(mockFileDao).clearPreviewByKeyAndStatus(bucket, key, FileHandleStatus.ARCHIVED);
		verifyNoMoreInteractions(mockS3Client);
	}

	@Test
	public void testArchiveUnlinkedFileHandlesByKeyWithObjectNotFoundExceptionWhileTagging() {
		String key = "key1";
		
		Instant modifiedBefore = Instant.parse("2021-02-03T10:00:00.00Z");
		int updated = 2;
		int availableAfterUpdate = 0;
		
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockFileDao.updateStatusByBucketAndKey(anyString(), anyString(), any(), any(), any())).thenReturn(updated);
		when(mockFileDao.getAvailableOrEarlyUnlinkedFileHandlesCount(anyString(), anyString(), any())).thenReturn(availableAfterUpdate);
		
		AmazonS3Exception ex = new AmazonS3Exception("Key not found");
		
		ex.setStatusCode(HttpStatus.SC_NOT_FOUND);
		
		doThrow(ex).when(mockS3Client).getObjectTags(any(), any());

		FileHandleKeyArchiveResult expected = new FileHandleKeyArchiveResult(updated, false);
		
		// Call under test
		FileHandleKeyArchiveResult result = manager.archiveUnlinkedFileHandlesByKey(mockUser, bucket, key, modifiedBefore);
		
		assertEquals(expected, result);
		
		verify(mockFileDao).updateStatusByBucketAndKey(bucket, key, FileHandleStatus.ARCHIVED, FileHandleStatus.UNLINKED, modifiedBefore);
		verify(mockFileDao).getAvailableOrEarlyUnlinkedFileHandlesCount(bucket, key, modifiedBefore);
		verify(mockS3Client).getObjectTags(bucket, key);
		verifyNoMoreInteractions(mockS3Client);
	}
	
	@Test
	public void testArchiveUnlinkedFileHandlesByKeyWithBucketNotFoundExceptionWhileTagging() {
		String key = "key1";
		
		Instant modifiedBefore = Instant.parse("2021-02-03T10:00:00.00Z");
		int updated = 2;
		int availableAfterUpdate = 0;
		
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockFileDao.updateStatusByBucketAndKey(anyString(), anyString(), any(), any(), any())).thenReturn(updated);
		when(mockFileDao.getAvailableOrEarlyUnlinkedFileHandlesCount(anyString(), anyString(), any())).thenReturn(availableAfterUpdate);
		
		CannotDetermineBucketLocationException ex = new CannotDetermineBucketLocationException("Key not found");
		
		doThrow(ex).when(mockS3Client).getObjectTags(any(), any());

		FileHandleKeyArchiveResult expected = new FileHandleKeyArchiveResult(updated, false);
		
		// Call under test
		FileHandleKeyArchiveResult result = manager.archiveUnlinkedFileHandlesByKey(mockUser, bucket, key, modifiedBefore);
		
		assertEquals(expected, result);
		
		verify(mockFileDao).updateStatusByBucketAndKey(bucket, key, FileHandleStatus.ARCHIVED, FileHandleStatus.UNLINKED, modifiedBefore);
		verify(mockFileDao).getAvailableOrEarlyUnlinkedFileHandlesCount(bucket, key, modifiedBefore);
		verify(mockS3Client).getObjectTags(bucket, key);
		verifyNoMoreInteractions(mockS3Client);
	}
	
	@Test
	public void testArchiveUnlinkedFileHandlesByKeyWithRecoverableExceptionWhileTagging() {
		String key = "key1";
		
		Instant modifiedBefore = Instant.parse("2021-02-03T10:00:00.00Z");
		int updated = 2;
		int availableAfterUpdate = 0;
		
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockFileDao.updateStatusByBucketAndKey(anyString(), anyString(), any(), any(), any())).thenReturn(updated);
		when(mockFileDao.getAvailableOrEarlyUnlinkedFileHandlesCount(anyString(), anyString(), any())).thenReturn(availableAfterUpdate);
		
		AmazonS3Exception ex = new AmazonS3Exception("Some error");
		
		ex.setErrorType(ErrorType.Service);
		
		doThrow(ex).when(mockS3Client).getObjectTags(any(), any());
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {
			manager.archiveUnlinkedFileHandlesByKey(mockUser, bucket, key, modifiedBefore);
		});
		
		assertEquals(ex, result.getCause());
		
		verify(mockFileDao).updateStatusByBucketAndKey(bucket, key, FileHandleStatus.ARCHIVED, FileHandleStatus.UNLINKED, modifiedBefore);
		verify(mockFileDao).getAvailableOrEarlyUnlinkedFileHandlesCount(bucket, key, modifiedBefore);
		verify(mockS3Client).getObjectTags(bucket, key);
		verifyNoMoreInteractions(mockS3Client);
		verifyNoMoreInteractions(mockFileDao);
	}
	
	@Test
	public void testArchiveUnlinkedFileHandlesByKeyWithUnrecoverableExceptionWhileTagging() {
		String key = "key1";
		
		Instant modifiedBefore = Instant.parse("2021-02-03T10:00:00.00Z");
		int updated = 2;
		int availableAfterUpdate = 0;
		
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockFileDao.updateStatusByBucketAndKey(anyString(), anyString(), any(), any(), any())).thenReturn(updated);
		when(mockFileDao.getAvailableOrEarlyUnlinkedFileHandlesCount(anyString(), anyString(), any())).thenReturn(availableAfterUpdate);
		
		AmazonS3Exception ex = new AmazonS3Exception("Some error");
		
		ex.setErrorType(ErrorType.Client);
		
		doThrow(ex).when(mockS3Client).getObjectTags(any(), any());
		
		AmazonS3Exception result = assertThrows(AmazonS3Exception.class, () -> {
			manager.archiveUnlinkedFileHandlesByKey(mockUser, bucket, key, modifiedBefore);
		});
		
		assertEquals(ex, result);
		
		verify(mockFileDao).updateStatusByBucketAndKey(bucket, key, FileHandleStatus.ARCHIVED, FileHandleStatus.UNLINKED, modifiedBefore);
		verify(mockFileDao).getAvailableOrEarlyUnlinkedFileHandlesCount(bucket, key, modifiedBefore);
		verify(mockS3Client).getObjectTags(bucket, key);
		verifyNoMoreInteractions(mockS3Client);
		verifyNoMoreInteractions(mockFileDao);
	}
	
	@Test
	public void testArchiveUnlinkedFileHandlesByKeyWithNoUpdates() {
		String key = "key1";
		
		Instant modifiedBefore = Instant.parse("2021-02-03T10:00:00.00Z");
		int updated = 0;
		
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockFileDao.updateStatusByBucketAndKey(anyString(), anyString(), any(), any(), any())).thenReturn(updated);
		
		FileHandleKeyArchiveResult expected = new FileHandleKeyArchiveResult(updated, false);
		
		// Call under test
		FileHandleKeyArchiveResult result = manager.archiveUnlinkedFileHandlesByKey(mockUser, bucket, key, modifiedBefore);
		
		assertEquals(expected, result);
		
		verify(mockFileDao).updateStatusByBucketAndKey(bucket, key, FileHandleStatus.ARCHIVED, FileHandleStatus.UNLINKED, modifiedBefore);
		verifyNoMoreInteractions(mockFileDao);
		verifyZeroInteractions(mockS3Client);
	}
	
	@Test
	public void testArchiveUnlinkedFileHandlesByKeyWithStillAvailable() {
		String key = "key1";
		
		Instant modifiedBefore = Instant.parse("2021-02-03T10:00:00.00Z");
		int updated = 2;
		int availableAfterUpdate = 1;
		
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockFileDao.updateStatusByBucketAndKey(anyString(), anyString(), any(), any(), any())).thenReturn(updated);
		when(mockFileDao.getAvailableOrEarlyUnlinkedFileHandlesCount(anyString(), anyString(), any())).thenReturn(availableAfterUpdate);
		
		FileHandleKeyArchiveResult expected = new FileHandleKeyArchiveResult(updated, false);
		
		// Call under test
		FileHandleKeyArchiveResult result = manager.archiveUnlinkedFileHandlesByKey(mockUser, bucket, key, modifiedBefore);
		
		assertEquals(expected, result);
		
		verify(mockFileDao).updateStatusByBucketAndKey(bucket, key, FileHandleStatus.ARCHIVED, FileHandleStatus.UNLINKED, modifiedBefore);
		verify(mockFileDao).getAvailableOrEarlyUnlinkedFileHandlesCount(bucket, key, modifiedBefore);
		
		verifyNoMoreInteractions(mockS3Client);
	}
	
	@Test
	public void testArchiveUnlinkedFileHandlesByKeyWithNotAdmin() {
		String key = "key1";
		
		Instant modifiedAfter = Instant.parse("2021-02-03T10:00:00.00Z");
		
		when(mockUser.isAdmin()).thenReturn(false);
		
		UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			manager.archiveUnlinkedFileHandlesByKey(mockUser, bucket, key, modifiedAfter);
		});
		
		assertEquals("Only administrators can access this service.", ex.getMessage());
		
		verifyZeroInteractions(mockFileDao);
	}
	
	@Test
	public void testArchiveUnlinkedFileHandlesByKeyWithEmptyBucket() {
		String key = "key1";
		
		Instant modifiedAfter = Instant.parse("2021-02-03T10:00:00.00Z");
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.archiveUnlinkedFileHandlesByKey(mockUser, "", key, modifiedAfter);
		});
		
		assertEquals("The bucketName is required and must not be the empty string.", ex.getMessage());
		
		verifyZeroInteractions(mockFileDao);
	}
	
	@Test
	public void testArchiveUnlinkedFileHandlesByKeyWithEmptyKey() {
		String key = "";
		
		Instant modifiedAfter = Instant.parse("2021-02-03T10:00:00.00Z");
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.archiveUnlinkedFileHandlesByKey(mockUser, bucket, key, modifiedAfter);
		});
		
		assertEquals("The key is required and must not be the empty string.", ex.getMessage());
		
		verifyZeroInteractions(mockFileDao);
	}
	
	@Test
	public void testArchiveUnlinkedFileHandlesByKeyWithNullModifiedBefore() {
		String key = "key1";
		
		Instant modifiedAfter = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.archiveUnlinkedFileHandlesByKey(mockUser, bucket, key, modifiedAfter);
		});
		
		assertEquals("The modifiedBefore is required.", ex.getMessage());
		
		verifyZeroInteractions(mockFileDao);
	}
	
	@Test
	public void testTagObjectForArchivalWithEmptyTags() {
		String key = "key";
		
		when(mockS3Client.getObjectTags(any(), any())).thenReturn(Collections.emptyList());
		
		// Call under test
		boolean result = manager.tagObjectForArchival(bucket, key);
		
		assertTrue(result);
		
		verify(mockS3Client).getObjectTags(bucket, key);
		verify(mockS3Client).setObjectTags(bucket, key, Arrays.asList(FileHandleArchivalManagerImpl.S3_TAG_ARCHIVED));
	}
	
	@Test
	public void testTagObjectForArchivalWithNullTags() {
		String key = "key";
		
		when(mockS3Client.getObjectTags(any(), any())).thenReturn(null);
		
		// Call under test
		boolean result = manager.tagObjectForArchival(bucket, key);
		
		assertTrue(result);
		
		verify(mockS3Client).getObjectTags(bucket, key);
		verify(mockS3Client).setObjectTags(bucket, key, Arrays.asList(FileHandleArchivalManagerImpl.S3_TAG_ARCHIVED));
	}
	
	@Test
	public void testTagObjectForArchivalWithExistingAndNotMatchingTags() {
		String key = "key";
		
		when(mockS3Client.getObjectTags(any(), any())).thenReturn(Arrays.asList(new Tag("key", "value")));
		
		// Call under test
		boolean result = manager.tagObjectForArchival(bucket, key);
		
		assertTrue(result);
		
		verify(mockS3Client).getObjectTags(bucket, key);
		verify(mockS3Client).setObjectTags(bucket, key, Arrays.asList(new Tag("key", "value"), FileHandleArchivalManagerImpl.S3_TAG_ARCHIVED));
	}
	
	@Test
	public void testTagObjectForArchivalWithExistingAndMatchingTags() {
		String key = "key";
		
		when(mockS3Client.getObjectTags(any(), any())).thenReturn(Arrays.asList(FileHandleArchivalManagerImpl.S3_TAG_ARCHIVED));
		
		// Call under test
		boolean result = manager.tagObjectForArchival(bucket, key);
		
		assertFalse(result);
		
		verify(mockS3Client).getObjectTags(bucket, key);
		verifyNoMoreInteractions(mockS3Client);
	}
	
	@Test
	public void testCleanupArchivedFileHandlesPreview() {
		String key = "key";
		
		when(mockFileDao.clearPreviewByKeyAndStatus(any(), any(), any())).thenReturn(new HashSet<>(Arrays.asList(1L, 2L)));
		when(mockFileDao.getReferencedPreviews(any())).thenReturn(Collections.emptySet());
		
		when(mockFileDao.getBucketAndKeyBatch(any())).thenReturn(new HashSet<>(Arrays.asList(
			new BucketAndKey().withBucket(bucket).withtKey("preview_key1"),
			new BucketAndKey().withBucket(bucket).withtKey("preview_key2")
		)));
		
		when(mockFileDao.getNumberOfReferencesToFile(any(), any(), any())).thenReturn(0L);
		
		// Call under test
		manager.cleanupArchivedFileHandlesPreviews(bucket, key);
		
		verify(mockFileDao).clearPreviewByKeyAndStatus(bucket, key, FileHandleStatus.ARCHIVED);
		verify(mockFileDao).getReferencedPreviews(new HashSet<>(Arrays.asList(1L, 2L)));
		verify(mockFileDao).getBucketAndKeyBatch(new HashSet<>(Arrays.asList(1L, 2L)));
		verify(mockFileDao).deleteBatch(new HashSet<>(Arrays.asList(1L, 2L)));
		
		List<String> keys = Arrays.asList("preview_key1", "preview_key2");
		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		
		verify(mockFileDao, times(2)).getNumberOfReferencesToFile(eq(FileHandleMetadataType.S3), eq(bucket), keyCaptor.capture());
		assertEquals(new HashSet<>(keys), new HashSet<>(keyCaptor.getAllValues()));
		
		verify(mockS3Client, times(2)).deleteObject(eq(bucket), keyCaptor.capture());
		assertEquals(new HashSet<>(keys), new HashSet<>(keyCaptor.getAllValues()));
		
	}
	
	@Test
	public void testCleanupArchivedFileHandlesPreviewWithReferencedPreviews() {
		String key = "key";
		
		when(mockFileDao.clearPreviewByKeyAndStatus(any(), any(), any())).thenReturn(new HashSet<>(Arrays.asList(1L, 2L, 3L)));
		when(mockFileDao.getReferencedPreviews(any())).thenReturn(Collections.singleton(2L));
		
		when(mockFileDao.getBucketAndKeyBatch(any())).thenReturn(new HashSet<>(Arrays.asList(
			new BucketAndKey().withBucket(bucket).withtKey("preview_key1")
		)));
		
		when(mockFileDao.getNumberOfReferencesToFile(any(), any(), any())).thenReturn(0L);
		
		// Call under test
		manager.cleanupArchivedFileHandlesPreviews(bucket, key);
		
		verify(mockFileDao).clearPreviewByKeyAndStatus(bucket, key, FileHandleStatus.ARCHIVED);
		verify(mockFileDao).getReferencedPreviews(new HashSet<>(Arrays.asList(1L, 2L, 3L)));
		verify(mockFileDao).getBucketAndKeyBatch(new HashSet<>(Arrays.asList(1L, 3L)));
		verify(mockFileDao).deleteBatch(new HashSet<>(Arrays.asList(1L, 3L)));
		
		List<String> keys = Arrays.asList("preview_key1");
		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		
		verify(mockFileDao).getNumberOfReferencesToFile(eq(FileHandleMetadataType.S3), eq(bucket), keyCaptor.capture());
		assertEquals(new HashSet<>(keys), new HashSet<>(keyCaptor.getAllValues()));
		
		verify(mockS3Client).deleteObject(eq(bucket), keyCaptor.capture());
		assertEquals(new HashSet<>(keys), new HashSet<>(keyCaptor.getAllValues()));
		
	}
	
	@Test
	public void testCleanupArchivedFileHandlesPreviewWithReferencedPreviewsKeys() {
		String key = "key";
		
		when(mockFileDao.clearPreviewByKeyAndStatus(any(), any(), any())).thenReturn(new HashSet<>(Arrays.asList(1L, 2L)));
		when(mockFileDao.getReferencedPreviews(any())).thenReturn(Collections.emptySet());
		
		when(mockFileDao.getBucketAndKeyBatch(any())).thenReturn(new HashSet<>(Arrays.asList(
			new BucketAndKey().withBucket(bucket).withtKey("preview_key1"),
			new BucketAndKey().withBucket(bucket).withtKey("preview_key2")
		)));
		
		when(mockFileDao.getNumberOfReferencesToFile(FileHandleMetadataType.S3, bucket, "preview_key1")).thenReturn(0L);
		when(mockFileDao.getNumberOfReferencesToFile(FileHandleMetadataType.S3, bucket, "preview_key2")).thenReturn(1L);
		
		// Call under test
		manager.cleanupArchivedFileHandlesPreviews(bucket, key);
		
		verify(mockFileDao).clearPreviewByKeyAndStatus(bucket, key, FileHandleStatus.ARCHIVED);
		verify(mockFileDao).getReferencedPreviews(new HashSet<>(Arrays.asList(1L, 2L)));
		verify(mockFileDao).getBucketAndKeyBatch(new HashSet<>(Arrays.asList(1L, 2L)));
		verify(mockFileDao).deleteBatch(new HashSet<>(Arrays.asList(1L, 2L)));
		
		verify(mockFileDao).getNumberOfReferencesToFile(FileHandleMetadataType.S3, bucket, "preview_key1");
		verify(mockFileDao).getNumberOfReferencesToFile(FileHandleMetadataType.S3, bucket, "preview_key2");
		
		verify(mockS3Client).deleteObject(bucket, "preview_key1");
		
	}
	
	@Test
	public void testCleanupArchivedFileHandlesPreviewWithRecoverableException() {
		String key = "key";
		
		when(mockFileDao.clearPreviewByKeyAndStatus(any(), any(), any())).thenReturn(new HashSet<>(Arrays.asList(1L)));
		when(mockFileDao.getReferencedPreviews(any())).thenReturn(Collections.emptySet());
		
		when(mockFileDao.getBucketAndKeyBatch(any())).thenReturn(new HashSet<>(Arrays.asList(
			new BucketAndKey().withBucket(bucket).withtKey("preview_key1")
		)));
		
		when(mockFileDao.getNumberOfReferencesToFile(any(), any(), any())).thenReturn(0L);
		
		AmazonS3Exception ex = new AmazonS3Exception("Something wrong");
		ex.setErrorType(ErrorType.Service);
		
		doThrow(ex).when(mockS3Client).deleteObject(any(), any());
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {
			// Call under test
			manager.cleanupArchivedFileHandlesPreviews(bucket, key);
		});
		
		assertEquals(ex, result.getCause());
		
		verify(mockFileDao).clearPreviewByKeyAndStatus(bucket, key, FileHandleStatus.ARCHIVED);
		verify(mockFileDao).getReferencedPreviews(new HashSet<>(Arrays.asList(1L)));
		verify(mockFileDao).getBucketAndKeyBatch(new HashSet<>(Arrays.asList(1L)));
		verify(mockFileDao).deleteBatch(new HashSet<>(Arrays.asList(1L)));
		verify(mockFileDao).getNumberOfReferencesToFile(FileHandleMetadataType.S3, bucket, "preview_key1");
		verify(mockS3Client).deleteObject(bucket, "preview_key1");
		
	}
	
	@Test
	public void testCleanupArchivedFileHandlesPreviewWithUnrecoverableException() {
		String key = "key";
		
		when(mockFileDao.clearPreviewByKeyAndStatus(any(), any(), any())).thenReturn(new HashSet<>(Arrays.asList(1L)));
		when(mockFileDao.getReferencedPreviews(any())).thenReturn(Collections.emptySet());
		
		when(mockFileDao.getBucketAndKeyBatch(any())).thenReturn(new HashSet<>(Arrays.asList(
			new BucketAndKey().withBucket(bucket).withtKey("preview_key1")
		)));
		
		when(mockFileDao.getNumberOfReferencesToFile(any(), any(), any())).thenReturn(0L);
		
		AmazonS3Exception ex = new AmazonS3Exception("Something wrong");
		
		doThrow(ex).when(mockS3Client).deleteObject(any(), any());
		
		AmazonS3Exception result = assertThrows(AmazonS3Exception.class, () -> {
			// Call under test
			manager.cleanupArchivedFileHandlesPreviews(bucket, key);
		});
		
		assertEquals(ex, result);
		
		verify(mockFileDao).clearPreviewByKeyAndStatus(bucket, key, FileHandleStatus.ARCHIVED);
		verify(mockFileDao).getReferencedPreviews(new HashSet<>(Arrays.asList(1L)));
		verify(mockFileDao).getBucketAndKeyBatch(new HashSet<>(Arrays.asList(1L)));
		verify(mockFileDao).deleteBatch(new HashSet<>(Arrays.asList(1L)));
		verify(mockFileDao).getNumberOfReferencesToFile(FileHandleMetadataType.S3, bucket, "preview_key1");
		verify(mockS3Client).deleteObject(bucket, "preview_key1");
		
	}
	
	@Test
	public void testCleanupArchivedFileHandlesPreviewWithBucketNotFoundException() {
		String key = "key";
		
		when(mockFileDao.clearPreviewByKeyAndStatus(any(), any(), any())).thenReturn(new HashSet<>(Arrays.asList(1L, 2L)));
		when(mockFileDao.getReferencedPreviews(any())).thenReturn(Collections.emptySet());
		
		when(mockFileDao.getBucketAndKeyBatch(any())).thenReturn(new HashSet<>(Arrays.asList(
			new BucketAndKey().withBucket(bucket).withtKey("preview_key1"),
			new BucketAndKey().withBucket(bucket).withtKey("preview_key2")
		)));
		
		when(mockFileDao.getNumberOfReferencesToFile(any(), any(), any())).thenReturn(0L);
		
		CannotDetermineBucketLocationException ex = new CannotDetermineBucketLocationException("Something wrong");
		
		doThrow(ex).when(mockS3Client).deleteObject(bucket, "preview_key1");
		
		// Call under test
		manager.cleanupArchivedFileHandlesPreviews(bucket, key);
		
		verify(mockFileDao).clearPreviewByKeyAndStatus(bucket, key, FileHandleStatus.ARCHIVED);
		verify(mockFileDao).getReferencedPreviews(new HashSet<>(Arrays.asList(1L, 2L)));
		verify(mockFileDao).getBucketAndKeyBatch(new HashSet<>(Arrays.asList(1L, 2L)));
		verify(mockFileDao).deleteBatch(new HashSet<>(Arrays.asList(1L, 2L)));
		verify(mockFileDao).getNumberOfReferencesToFile(FileHandleMetadataType.S3, bucket, "preview_key1");
		verify(mockFileDao).getNumberOfReferencesToFile(FileHandleMetadataType.S3, bucket, "preview_key2");
		verify(mockS3Client).deleteObject(bucket, "preview_key1");
		verify(mockS3Client).deleteObject(bucket, "preview_key2");
	}
	
}
