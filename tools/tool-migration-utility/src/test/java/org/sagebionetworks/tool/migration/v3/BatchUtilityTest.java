package org.sagebionetworks.tool.migration.v3;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test for BatchUtility
 * 
 * @author John
 *
 */
public class BatchUtilityTest {

	
	private BatchWorker mockWorker;
	
	@Before
	public void before(){
		mockWorker = Mockito.mock(BatchWorker.class);
	}
	
	@Test
	public void testAttemptBatchWithRetryHappyCase() throws Exception{
		List<Long> batch = Arrays.asList(1l,2l);
		// Only one attempt should be made
		when(mockWorker.attemptBatch(batch)).thenReturn(2L);
		BatchUtility.attemptBatchWithRetry(mockWorker, batch);
		verify(mockWorker, times(1)).attemptBatch(batch);
	}
	
	@Test
	public void testAttemptBatchWithRetryRetryLessThanList() throws Exception{
		List<Long> batch = Arrays.asList(1l,2l,3l,4l);
		// The full list should fail.
		when(mockWorker.attemptBatch(batch)).thenThrow(new DaemonFailedException());
		// One is okay
		when(mockWorker.attemptBatch(Arrays.asList(1l))).thenReturn(1L);
		// two should fail
		when(mockWorker.attemptBatch(Arrays.asList(2l))).thenThrow(new DaemonFailedException("two failed"));
		// three and four are okay
		when(mockWorker.attemptBatch(Arrays.asList(3l))).thenReturn(1L);
		when(mockWorker.attemptBatch(Arrays.asList(4l))).thenReturn(1L);
		try{
			BatchUtility.attemptBatchWithRetry(mockWorker, batch);
			fail("should have failed");
		}catch(DaemonFailedException e){
			// expected
			assertEquals("Failed ids in batch retry:	[2]", e.getMessage());
		}
		verify(mockWorker).attemptBatch(Arrays.asList(1l,2l,3l,4l));

	}
	
	@Test
	public void testAttemptBatchWithRetryOdd() throws Exception{
		List<Long> batch = Arrays.asList(1l,2l,3l,4l,5l);
		// The full list should fail.
		when(mockWorker.attemptBatch(batch)).thenThrow(new DaemonFailedException());
		// First two okay
		when(mockWorker.attemptBatch(Arrays.asList(1l,2l))).thenReturn(2L);
		// Last next two okay
		when(mockWorker.attemptBatch(Arrays.asList(3l,4l))).thenReturn(2L);
		// Last fail
		when(mockWorker.attemptBatch(Arrays.asList(5l))).thenThrow(new DaemonFailedException("five failed"));
		try{
			BatchUtility.attemptBatchWithRetry(mockWorker, batch);
			fail("should have failed");
		}catch(DaemonFailedException e){
			// expected
			assertEquals("Failed ids in batch retry:	[5]", e.getMessage());
		}

	}

	@Test
	public void testAttemptBatchWithMultipleFailsOnRetry() throws Exception{
		List<Long> batch = Arrays.asList(1l,2l,3l,4l,5l,6l);
		// The full list should fail.
		when(mockWorker.attemptBatch(batch)).thenThrow(new DaemonFailedException());
		// First two okay
		when(mockWorker.attemptBatch(Arrays.asList(1l,2l))).thenReturn(2L);
		// Last next two okay
		when(mockWorker.attemptBatch(Arrays.asList(3l,4l))).thenReturn(2L);
		// Last 2 fail
		when(mockWorker.attemptBatch(Arrays.asList(5l))).thenThrow(new DaemonFailedException("five failed"));
		when(mockWorker.attemptBatch(Arrays.asList(6l))).thenThrow(new DaemonFailedException("six failed"));
		try{
			BatchUtility.attemptBatchWithRetry(mockWorker, batch);
			fail("should have failed");
		}catch(DaemonFailedException e){
			// expected
			assertEquals("Failed ids in batch retry:	[5, 6]", e.getMessage());
		}

	}
}
