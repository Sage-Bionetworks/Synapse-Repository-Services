package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleArchivalRequest;
import org.sagebionetworks.repo.model.file.FileHandleArchivalResponse;
import org.sagebionetworks.repo.model.file.FileHandleKeysArchiveRequest;
import org.sagebionetworks.repo.model.file.FileHandleStatus;

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
		
		// Call under test
		manager.archiveUnlinkedFileHandlesByKey(mockUser, bucket, key, modifiedBefore);
		
		verify(mockFileDao).updateStatusByBucketAndKey(bucket, key, FileHandleStatus.ARCHIVED, FileHandleStatus.UNLINKED, modifiedBefore);
		verify(mockFileDao).getAvailableOrEarlyUnlinkedFileHandlesCount(bucket, key, modifiedBefore);
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
}
