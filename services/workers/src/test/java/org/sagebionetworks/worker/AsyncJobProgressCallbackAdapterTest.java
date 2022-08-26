package org.sagebionetworks.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressListener;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;

@ExtendWith(MockitoExtension.class)
public class AsyncJobProgressCallbackAdapterTest {
	
	@Mock
	private AsynchJobStatusManager mockManager;
	
	@Mock
	private ProgressCallback mockCallback;
	
	private String jobId = "123";
	
	private AsyncJobProgressCallbackAdapter adapter;
	
	@Mock
	private ProgressListener mockListener;
	
	@BeforeEach
	public void before() {
		adapter = new AsyncJobProgressCallbackAdapter(mockManager, mockCallback, jobId);
	}

	@Test
	public void testUpdateProgress() {
		// Call under test
		adapter.updateProgress("message", 33L, 100L);
		
		verify(mockManager).updateJobProgress(jobId, 33L, 100L, "message");
		verifyNoMoreInteractions(mockManager);
	}
	
	@Test
	public void testAddProgressListener() {
		// Call under test
		adapter.addProgressListener(mockListener);
		
		verify(mockCallback).addProgressListener(mockListener);
		verifyNoMoreInteractions(mockCallback);
		verifyNoMoreInteractions(mockManager);
	}
	
	@Test
	public void testRemoveProgressListener() {
		// Call under test
		adapter.removeProgressListener(mockListener);
		
		verify(mockCallback).removeProgressListener(mockListener);
		verifyNoMoreInteractions(mockCallback);
		verifyNoMoreInteractions(mockManager);
	}
	
	@Test
	public void testGetLockTimeoutSeconds() {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(123L);
		
		// Call under test
		assertEquals(123L, adapter.getLockTimeoutSeconds());
		
		verify(mockCallback).getLockTimeoutSeconds();
		verifyNoMoreInteractions(mockCallback);
		verifyNoMoreInteractions(mockManager);
	}
	

}
