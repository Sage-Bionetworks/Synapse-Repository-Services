package org.sagebionetworks.util.progress;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.util.progress.AutoProgressingRunner;
import org.sagebionetworks.util.progress.ProgressingRunner;
import org.sagebionetworks.util.progress.SynchronizedProgressCallback;

public class AutoProgressingRunnerTest {

	@Mock
	ProgressingRunner mockRunner;
	@Mock
	SynchronizedProgressCallback mockCallback;
	
	long progressFrequencyMs;
	
	@Before
	public void before() throws Exception{
		MockitoAnnotations.initMocks(this);
		
		progressFrequencyMs = 100;
		// Setup the runner to take 3 times the progress frequency.
		doAnswer(new Answer<Void>(){

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(progressFrequencyMs*3+1);
				return null;
			}}).when(mockRunner).run(mockCallback);
	}
	
	@Test
	public void testRunner() throws Exception{
		AutoProgressingRunner autoRunner = new AutoProgressingRunner(mockRunner, progressFrequencyMs);
		autoRunner.run(mockCallback);
		// progress should be made at least three times
		verify(mockCallback, atLeast(3)).fireProgressMade();
		verify(mockRunner).run(mockCallback);
	}

}
