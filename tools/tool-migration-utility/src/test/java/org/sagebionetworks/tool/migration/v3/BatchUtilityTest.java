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
	public void testAttemptBatchWithRetryEvenLengthOneFailure1() throws Exception{
		List<Long> batch = Arrays.asList(1l,2l,3l,4l);
		// Pass 1
		when(mockWorker.attemptBatch(batch)).thenThrow(new DaemonFailedException());
		// Pass 2
		when(mockWorker.attemptBatch(Arrays.asList(1l, 2l))).thenReturn(2L);
		when(mockWorker.attemptBatch(Arrays.asList(3l, 4l))).thenThrow(new DaemonFailedException("three failed"));
		// Pass 3
		when(mockWorker.attemptBatch(Arrays.asList(3l))).thenThrow(new DaemonFailedException("three failed"));
		when(mockWorker.attemptBatch(Arrays.asList(4l))).thenReturn(1L);

		try{
			BatchUtility.attemptBatchWithRetry(mockWorker, batch);
			fail("should have failed");
		}catch(DaemonFailedException e){
			// expected
			assertEquals("Failed ids in batch retry:	[3]", e.getMessage());
		}
		verify(mockWorker).attemptBatch(batch);
	}
	
	@Test
	public void testAttemptBatchWithRetryEvenLengthOneFailure2() throws Exception{
		List<Long> batch = Arrays.asList(1l,2l,3l,4l);
		// Pass 1
		when(mockWorker.attemptBatch(batch)).thenThrow(new DaemonFailedException());
		// Pass 2
		when(mockWorker.attemptBatch(Arrays.asList(1l, 2l))).thenReturn(2L);
		when(mockWorker.attemptBatch(Arrays.asList(3l, 4l))).thenThrow(new DaemonFailedException("three OK four failed"));
		// Pass 3
		when(mockWorker.attemptBatch(Arrays.asList(3l))).thenReturn(1L);
		when(mockWorker.attemptBatch(Arrays.asList(4l))).thenThrow(new DaemonFailedException("four failed"));

		try{
			BatchUtility.attemptBatchWithRetry(mockWorker, batch);
			fail("should have failed");
		}catch(DaemonFailedException e){
			// expected
			assertEquals("Failed ids in batch retry:	[4]", e.getMessage());
		}
		verify(mockWorker).attemptBatch(Arrays.asList(1l,2l,3l,4l));
	}
	
	@Test
	public void testAttemptBatchWithRetryOddLengthOneFailure1() throws Exception{
		List<Long> batch = Arrays.asList(1l,2l,3l,4l,5l);
		// The full list should fail.
		when(mockWorker.attemptBatch(batch)).thenThrow(new DaemonFailedException());
		// First two okay
		when(mockWorker.attemptBatch(Arrays.asList(1l,2l,3l))).thenReturn(3L);
		// Last next two okay
		when(mockWorker.attemptBatch(Arrays.asList(4l,5l))).thenThrow(new DaemonFailedException("four OK five failed"));
		// Last fail
		when(mockWorker.attemptBatch(Arrays.asList(4l))).thenReturn(1L);
		when(mockWorker.attemptBatch(Arrays.asList(5l))).thenThrow(new DaemonFailedException("five failed"));
		try{
			BatchUtility.attemptBatchWithRetry(mockWorker, batch);
			fail("should have failed");
		}catch(DaemonFailedException e){
			// expected
			assertEquals("Failed ids in batch retry:	[5]", e.getMessage());
		}
		verify(mockWorker).attemptBatch(Arrays.asList(5l));
		verify(mockWorker).attemptBatch(Arrays.asList(4l));
		

	}

	@Test
	public void testAttemptBatchWithRetryEvenLengthTwoFailures1() throws Exception{
		List<Long> batch = Arrays.asList(1l,2l,3l,4l,5l,6l);
		// The full list should fail.
		when(mockWorker.attemptBatch(batch)).thenThrow(new DaemonFailedException());
		// Pass 2
		// First three okay
		when(mockWorker.attemptBatch(Arrays.asList(1l, 2l, 3l))).thenReturn(3L);
		// Last next two not okay
		when(mockWorker.attemptBatch(Arrays.asList(4l, 5l, 6l))).thenThrow(new DaemonFailedException("four OK five and six failed"));
		
		// Pass 3
		when(mockWorker.attemptBatch(Arrays.asList(4l, 5l))).thenThrow(new DaemonFailedException("four OK five failed"));
		when(mockWorker.attemptBatch(Arrays.asList(6l))).thenThrow(new DaemonFailedException("six failed"));

		// Pass 4
		when(mockWorker.attemptBatch(Arrays.asList(4l))).thenReturn(1L);
		when(mockWorker.attemptBatch(Arrays.asList(5l))).thenThrow(new DaemonFailedException("five failed"));

		try{
			BatchUtility.attemptBatchWithRetry(mockWorker, batch);
			fail("should have failed");
		}catch(DaemonFailedException e){
			// expected
			assertEquals("Failed ids in batch retry:	[6]", e.getMessage());
		}
		verify(mockWorker).attemptBatch(Arrays.asList(1l,2l,3l));
		verify(mockWorker).attemptBatch(Arrays.asList(4l));
		verify(mockWorker).attemptBatch(Arrays.asList(5l));
		verify(mockWorker).attemptBatch(Arrays.asList(6l));

	}
	@Test
	public void testAttemptBatchWithRetryEvenLengthTwoFailures2() throws Exception{
		List<Long> batch = Arrays.asList(1l,2l,3l,4l,5l,6l);
		// The full list should fail.
		when(mockWorker.attemptBatch(batch)).thenThrow(new DaemonFailedException());
		// Pass 2
		// First three okay
		when(mockWorker.attemptBatch(Arrays.asList(1l, 2l, 3l))).thenThrow(new DaemonFailedException("one failed two and three OK"));
		// Last next two not okay
		when(mockWorker.attemptBatch(Arrays.asList(4l, 5l, 6l))).thenThrow(new DaemonFailedException("four and five OK six failed"));
		
		// Pass 3
		when(mockWorker.attemptBatch(Arrays.asList(1l, 2l))).thenThrow(new DaemonFailedException("one failed two OK"));
		when(mockWorker.attemptBatch(Arrays.asList(3l))).thenReturn(1L);
		when(mockWorker.attemptBatch(Arrays.asList(4l, 5l))).thenReturn(2L);
		when(mockWorker.attemptBatch(Arrays.asList(6l))).thenThrow(new DaemonFailedException("six failed"));

		// Pass 4
		when(mockWorker.attemptBatch(Arrays.asList(1l))).thenThrow(new DaemonFailedException("one failed"));
		when(mockWorker.attemptBatch(Arrays.asList(2l))).thenReturn(1L);
		when(mockWorker.attemptBatch(Arrays.asList(4l))).thenReturn(1L);
		when(mockWorker.attemptBatch(Arrays.asList(5l))).thenReturn(1L);

		try{
			BatchUtility.attemptBatchWithRetry(mockWorker, batch);
			fail("should have failed");
		}catch(DaemonFailedException e){
			// expected
			assertEquals("Failed ids in batch retry:	[6]", e.getMessage());
		}
		verify(mockWorker).attemptBatch(Arrays.asList(4l, 5l));
		verify(mockWorker).attemptBatch(Arrays.asList(1l));
		verify(mockWorker).attemptBatch(Arrays.asList(2l));
		verify(mockWorker).attemptBatch(Arrays.asList(3l));
		verify(mockWorker).attemptBatch(Arrays.asList(6l));
	}
}
