package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.manager.file.MultipartManagerV2;
import org.sagebionetworks.repo.manager.stack.StackStatusManager;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;

@ExtendWith(MockitoExtension.class)
public class MultipartCleanupWorkerUnitTest {
	
	@Mock
	private MultipartManagerV2 mockManager;
	
	@Mock
	private StackStatusManager mockStackStatusManager;
	
	@Mock
	private FeatureManager mockFeatureManager;
	
	@InjectMocks
	private MultipartCleanupWorker worker;
	
	@Mock
	private LoggerProvider mockLoggerProvider;
	
	@Mock
	private Logger mockLogger;
	
	@Mock
	private ProgressCallback mockCallback;
	
	@Mock
	private StackStatus mockStackStatus;
	
	@Captor
	private ArgumentCaptor<Instant> instantCaptor;
	
	@Captor
	private ArgumentCaptor<String> uploadsCaptor;
	
	@BeforeEach
	public void before() {
		when(mockLoggerProvider.getLogger(any())).thenReturn(mockLogger);
		worker.configureLogger(mockLoggerProvider);
	}
	
	@AfterEach
	public void after() {
		verify(mockLoggerProvider).getLogger(MultipartCleanupWorker.class.getName());
	}
		
	@Test
	public void testRun() throws Exception {
		
		boolean featureEnabled = true;
		List<String> uploads = Arrays.asList("up1", "up2");
		
		when(mockStackStatus.getStatus()).thenReturn(StatusEnum.READ_WRITE);
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		when(mockStackStatusManager.getCurrentStatus()).thenReturn(mockStackStatus);
		when(mockManager.getUploadsModifiedBefore(any(), anyLong())).thenReturn(uploads);
		
		// Call under test
		worker.run(mockCallback);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.MULTIPART_AUTO_CLEANUP);
		
		verify(mockManager).getUploadsModifiedBefore(instantCaptor.capture(), eq(MultipartCleanupWorker.BATCH_SIZE));
		
		Instant expectedInstantBefore = Instant.now().minus(MultipartManagerV2.EXPIRE_PERIOD).plus(1, ChronoUnit.SECONDS);
		Instant modifiedBefore = instantCaptor.getValue();
		
		assertTrue(modifiedBefore.isBefore(expectedInstantBefore));
		
		verify(mockManager, times(uploads.size())).clearMultipartUpload(uploadsCaptor.capture());
		assertEquals(uploads, uploadsCaptor.getAllValues());
		
		verify(mockLogger).info(eq("Processed {} multipart uploads (Errored: {}, Time: {} ms)."), eq(uploads.size()), eq(0), anyLong());
	}
	
	@Test
	public void testRunWithFeatureDisabled() throws Exception {
		
		boolean featureEnabled = false;
		
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		
		// Call under test
		worker.run(mockCallback);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.MULTIPART_AUTO_CLEANUP);
		
		verifyZeroInteractions(mockStackStatusManager);
		verifyZeroInteractions(mockManager);
		verifyZeroInteractions(mockLogger);
	}
	
	@Test
	public void testRunWithStackReadOnly() throws Exception {
		
		boolean featureEnabled = true;
		List<String> uploads = Arrays.asList("up1", "up2");
		
		when(mockStackStatus.getStatus()).thenReturn(StatusEnum.READ_WRITE, StatusEnum.READ_ONLY);
		
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		when(mockStackStatusManager.getCurrentStatus()).thenReturn(mockStackStatus);
		when(mockManager.getUploadsModifiedBefore(any(), anyLong())).thenReturn(uploads);
		
		// Call under test
		worker.run(mockCallback);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.MULTIPART_AUTO_CLEANUP);
		
		verify(mockManager).getUploadsModifiedBefore(instantCaptor.capture(), eq(MultipartCleanupWorker.BATCH_SIZE));
		
		Instant expectedInstantBefore = Instant.now().minus(MultipartManagerV2.EXPIRE_PERIOD).plus(1, ChronoUnit.SECONDS);
		Instant modifiedBefore = instantCaptor.getValue();
		
		assertTrue(modifiedBefore.isBefore(expectedInstantBefore));
		
		// Only the first was cleared as the stack went into read only mode
		verify(mockManager, times(1)).clearMultipartUpload(uploadsCaptor.capture());
		
		assertEquals(Arrays.asList(uploads.get(0)), uploadsCaptor.getAllValues());
		
		verify(mockLogger).info(eq("Processed {} multipart uploads (Errored: {}, Time: {} ms)."), eq(1), eq(0), anyLong());
	}
	
	@Test
	public void testRunWithException() throws Exception {
		
		boolean featureEnabled = true;
		List<String> uploads = Arrays.asList("up1", "up2");
		RuntimeException ex = new RuntimeException("Something went wrong");
		
		when(mockStackStatus.getStatus()).thenReturn(StatusEnum.READ_WRITE);
		when(mockFeatureManager.isFeatureEnabled(any())).thenReturn(featureEnabled);
		when(mockStackStatusManager.getCurrentStatus()).thenReturn(mockStackStatus);
		when(mockManager.getUploadsModifiedBefore(any(), anyLong())).thenReturn(uploads);
		
		// Throws on the first
		doThrow(ex).when(mockManager).clearMultipartUpload("up1");
		
		// Call under test
		worker.run(mockCallback);
		
		verify(mockFeatureManager).isFeatureEnabled(Feature.MULTIPART_AUTO_CLEANUP);
		
		verify(mockManager).getUploadsModifiedBefore(instantCaptor.capture(), eq(MultipartCleanupWorker.BATCH_SIZE));
		
		Instant expectedInstantBefore = Instant.now().minus(MultipartManagerV2.EXPIRE_PERIOD).plus(1, ChronoUnit.SECONDS);
		Instant modifiedBefore = instantCaptor.getValue();
		
		assertTrue(modifiedBefore.isBefore(expectedInstantBefore));
		
		// All of the uploads are processed
		verify(mockManager, times(uploads.size())).clearMultipartUpload(uploadsCaptor.capture());
		assertEquals(uploads, uploadsCaptor.getAllValues());
		
		verify(mockLogger).info(eq("Processed {} multipart uploads (Errored: {}, Time: {} ms)."), eq(uploads.size()), eq(1), anyLong());
	}

}
