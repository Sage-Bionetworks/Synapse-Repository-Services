package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleArchivalManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.file.FileHandleKeyArchiveResult;
import org.sagebionetworks.repo.model.file.FileHandleKeysArchiveRequest;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class FileHandleKeysArchiveWorkerTest {
	
	@Mock
	private UserManager mockUserManager;
	
	@Mock
	private FileHandleArchivalManager mockManager;
	
	@InjectMocks
	private FileHandleKeysArchiveWorker worker;
	
	@Mock
	private ProgressCallback mockCallback;
	
	@Mock
	private Message mockMessage;

	@Mock
	private FileHandleKeysArchiveRequest mockRequest;
	
	@Mock
	private FileHandleKeyArchiveResult mockResult;
	
	@Captor
	private ArgumentCaptor<String> keyCaptor;
	
	private UserInfo adminUser = new UserInfo(true);
	
	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		
		String bucket = "bucket";
		Long modifiedBefore = System.currentTimeMillis();
		List<String> keys = Arrays.asList("key1", "key2", "key3");
		
		when(mockUserManager.getUserInfo(any())).thenReturn(adminUser);
		when(mockRequest.getBucket()).thenReturn(bucket);
		when(mockRequest.getModifiedBefore()).thenReturn(modifiedBefore);
		when(mockRequest.getKeys()).thenReturn(keys);
		when(mockManager.archiveUnlinkedFileHandlesByKey(any(), any(), any(), any())).thenReturn(mockResult);
		
		// Call under test
		worker.run(mockCallback, mockMessage, mockRequest);
		
		verify(mockUserManager).getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		verify(mockManager, times(3)).archiveUnlinkedFileHandlesByKey(eq(adminUser), eq(bucket), keyCaptor.capture(), eq(Instant.ofEpochMilli(modifiedBefore)));
		
		assertEquals(keys, keyCaptor.getAllValues());
	}
	
	@Test
	public void testRunWithRecoverableException() throws RecoverableMessageException, Exception {
		
		String bucket = "bucket";
		Long modifiedBefore = System.currentTimeMillis();
		List<String> keys = Arrays.asList("key1", "key2", "key3");
		
		when(mockUserManager.getUserInfo(any())).thenReturn(adminUser);
		when(mockRequest.getBucket()).thenReturn(bucket);
		when(mockRequest.getModifiedBefore()).thenReturn(modifiedBefore);
		when(mockRequest.getKeys()).thenReturn(keys);
		
		RecoverableMessageException ex = new RecoverableMessageException();
		
		when(mockManager.archiveUnlinkedFileHandlesByKey(any(), any(), any(), any())).thenThrow(ex);
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			worker.run(mockCallback, mockMessage, mockRequest);
		});
		
		assertEquals(ex, result);
		
		verify(mockUserManager).getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		verify(mockManager).archiveUnlinkedFileHandlesByKey(adminUser, bucket, "key1", Instant.ofEpochMilli(modifiedBefore));
	}
	
	@Test
	public void testRunWithOtherException() throws RecoverableMessageException, Exception {
		
		String bucket = "bucket";
		Long modifiedBefore = System.currentTimeMillis();
		List<String> keys = Arrays.asList("key1", "key2", "key3");
		
		when(mockUserManager.getUserInfo(any())).thenReturn(adminUser);
		when(mockRequest.getBucket()).thenReturn(bucket);
		when(mockRequest.getModifiedBefore()).thenReturn(modifiedBefore);
		when(mockRequest.getKeys()).thenReturn(keys);
		
		when(mockManager.archiveUnlinkedFileHandlesByKey(any(), any(), any(), any())).thenReturn(mockResult);		
		when(mockManager.archiveUnlinkedFileHandlesByKey(any(), any(), eq("key2"), any())).thenThrow(RuntimeException.class);
		
		// Call under test
		worker.run(mockCallback, mockMessage, mockRequest);
		
		verify(mockUserManager).getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		verify(mockManager, times(3)).archiveUnlinkedFileHandlesByKey(eq(adminUser), eq(bucket), keyCaptor.capture(), eq(Instant.ofEpochMilli(modifiedBefore)));
		
		assertEquals(keys, keyCaptor.getAllValues());
	}
	
	@Test
	public void testRunWithEmptyKeys() throws RecoverableMessageException, Exception {
		
		String bucket = "bucket";
		Long modifiedBefore = System.currentTimeMillis();
		List<String> keys = Collections.emptyList();
		
		when(mockRequest.getBucket()).thenReturn(bucket);
		when(mockRequest.getModifiedBefore()).thenReturn(modifiedBefore);
		when(mockRequest.getKeys()).thenReturn(keys);
		
		// Call under test
		worker.run(mockCallback, mockMessage, mockRequest);
		
		verifyNoMoreInteractions(mockManager);
	}

}
