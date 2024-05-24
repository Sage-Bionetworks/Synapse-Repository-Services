package org.sagebionetworks.asynchronous.workers.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.util.progress.ProgressListener;

@ExtendWith(MockitoExtension.class)
public class ConcurrentProgressCallbackTest {

	@Mock
	private ProgressListener mockListenerOne;
	@Mock
	private ProgressListener mockListenerTwo;

	@Test
	public void testTimeout() {
		int timeout = 123;
		ConcurrentProgressCallback callback = new ConcurrentProgressCallback(timeout);
		assertEquals(timeout, callback.getLockTimeoutSeconds());
	}

	@Test
	public void testProgressMade() {
		int timeout = 123;
		ConcurrentProgressCallback callback = new ConcurrentProgressCallback(timeout);
		callback.addProgressListener(mockListenerOne);
		callback.addProgressListener(mockListenerTwo);

		// call under test
		callback.progressMade();
		verify(mockListenerOne).progressMade();
		verify(mockListenerTwo).progressMade();
	}

	@Test
	public void testProgressMadeWithException() {

		int timeout = 123;
		ConcurrentProgressCallback callback = new ConcurrentProgressCallback(timeout);
		callback.addProgressListener(mockListenerOne);
		callback.addProgressListener(mockListenerTwo);

		doNothing().when(mockListenerOne).progressMade();
		doThrow(new IllegalArgumentException("nope")).when(mockListenerTwo).progressMade();
		;

		// call under test
		callback.progressMade();
		verify(mockListenerOne).progressMade();
		verify(mockListenerTwo).progressMade();

		reset(mockListenerOne);
		reset(mockListenerTwo);

		// call under test
		callback.progressMade();

		// second call will
		verify(mockListenerOne).progressMade();
		// should not be called since an exception was thrown the first time.
		verify(mockListenerTwo, never()).progressMade();

	}
}
