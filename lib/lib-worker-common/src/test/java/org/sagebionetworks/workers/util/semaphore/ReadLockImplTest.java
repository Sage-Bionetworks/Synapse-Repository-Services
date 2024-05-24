package org.sagebionetworks.workers.util.semaphore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.database.semaphore.LockReleaseFailedException;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressListener;

@ExtendWith(MockitoExtension.class)
public class ReadLockImplTest {

	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private CountingSemaphore mockCountingSemaphore;
	@Captor
	private ArgumentCaptor<ProgressListener> listenerCaptor;
	private WriteReadSemaphore semaphore;

	private int maxNumberOfReaders;
	private String[] keys;
	private long maxTimeout;
	private String context;

	@BeforeEach
	public void before() {
		maxNumberOfReaders = 4;
		keys = new String[] { "one", "two", "three" };
		maxTimeout = 31L;
		context = "some context";
		semaphore = new WriteReadSemaphoreImpl(mockCountingSemaphore, maxNumberOfReaders);
	}

	@Test
	public void testConstructorWithNullCallback() {
		mockCallback = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new ReadLockRequest(mockCallback, context, keys);
		}).getMessage();
		assertEquals("ProgressCallback cannot be null", message);
	}

	@Test
	public void testConstructorWithTimeoutTooLow() {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(1L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new ReadLockRequest(mockCallback, context, keys);
		}).getMessage();
		assertEquals("LockTimeout cannot be less than 2 seconds", message);
	}

	@Test
	public void testConstructorWithNullContext() {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		context = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new ReadLockRequest(mockCallback, context, keys);
		}).getMessage();
		assertEquals("Caller's context cannot be null", message);
	}

	@Test
	public void testConstructorWithNullLockKeys() {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		keys = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new ReadLockRequest(mockCallback, context, keys);
		}).getMessage();
		assertEquals("LockKey cannot be null", message);
	}

	@Test
	public void testConstructorWithEmptyLockKeys() {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		keys = new String[0];
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new ReadLockRequest(mockCallback, context, keys);
		}).getMessage();
		assertEquals("Must include at least one lock key", message);
	}

	@Test
	public void testConstructorWithNullInLockKeys() {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		keys = new String[] { "one", null, "two" };
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new ReadLockRequest(mockCallback, context, keys);
		}).getMessage();
		assertEquals("Lock key cannot be null", message);
	}

	@Test
	public void testConstructorWithNullSemaphore() {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		mockCountingSemaphore = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new ReadLockImpl(mockCountingSemaphore, maxNumberOfReaders, new ReadLockRequest(mockCallback, context, keys));
		}).getMessage();
		assertEquals("CountingSemaphore cannot be null", message);
	}
	
	@Test
	public void testConstructorWithNullRequest() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new ReadLockImpl(mockCountingSemaphore, maxNumberOfReaders, null);
		}).getMessage();
		assertEquals("ReadLockRequest cannot be null", message);
	}

	@Test
	public void testAcqurieLockAndClose() throws LockUnavilableException, Exception {
		when(mockCountingSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(
				Optional.of("tokenOne"), Optional.of("tokenTwo"),
				Optional.of("tokenThree"));
		when(mockCountingSemaphore.getFirstUnexpiredLockContext(any())).thenReturn(Optional.empty());
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);

		// call under test
		try (ReadLock lock = semaphore.getReadLock(new ReadLockRequest(mockCallback, context, keys))) {
			
		}

		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("one_WRITER_LOCK");
		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("two_WRITER_LOCK");
		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("three_WRITER_LOCK");
		verify(mockCountingSemaphore, times(3)).getFirstUnexpiredLockContext(any());

		verify(mockCountingSemaphore).attemptToAcquireLock("one_READER_LOCK", maxTimeout, maxNumberOfReaders, context);
		verify(mockCountingSemaphore).attemptToAcquireLock("two_READER_LOCK", maxTimeout, maxNumberOfReaders, context);
		verify(mockCountingSemaphore).attemptToAcquireLock("three_READER_LOCK", maxTimeout, maxNumberOfReaders, context);
		verify(mockCountingSemaphore, times(3)).attemptToAcquireLock(any(), anyLong(), anyInt(),any());

		verify(mockCallback).addProgressListener(listenerCaptor.capture());
		ProgressListener listener = listenerCaptor.getValue();
		assertNotNull(listener);
		// trigger progress made
		listener.progressMade();
		verify(mockCountingSemaphore).refreshLockTimeout("one_READER_LOCK", "tokenOne", maxTimeout);
		verify(mockCountingSemaphore).refreshLockTimeout("two_READER_LOCK", "tokenTwo", maxTimeout);
		verify(mockCountingSemaphore).refreshLockTimeout("three_READER_LOCK", "tokenThree", maxTimeout);
		verify(mockCountingSemaphore, times(3)).refreshLockTimeout(any(), any(), anyLong());

		// close checks
		verify(mockCallback).removeProgressListener(listener);
		verify(mockCountingSemaphore).releaseLock("one_READER_LOCK", "tokenOne");
		verify(mockCountingSemaphore).releaseLock("two_READER_LOCK", "tokenTwo");
		verify(mockCountingSemaphore).releaseLock("three_READER_LOCK", "tokenThree");
		verify(mockCountingSemaphore, times(3)).releaseLock(any(), any());
	}

	@Test
	public void testAcqurieLockWithUnexpiredLock() {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		when(mockCountingSemaphore.getFirstUnexpiredLockContext(any())).thenReturn(
				Optional.empty(), Optional.of("some write context"), Optional.empty());

		String message = assertThrows(LockUnavilableException.class, () -> {
			// call under test
			try (ReadLock lock = semaphore.getReadLock(new ReadLockRequest(mockCallback, context, keys))) {
				
			}
		}).getMessage();

		assertEquals("Write lock unavailable for key: 'two'. Current lock holder's context: 'some write context'", message);

		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("one_WRITER_LOCK");
		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("two_WRITER_LOCK");
		verify(mockCountingSemaphore, never()).getFirstUnexpiredLockContext("three_WRITER_LOCK");
		verify(mockCountingSemaphore, times(2)).getFirstUnexpiredLockContext(any());

		verifyNoMoreInteractions(mockCountingSemaphore);
		verifyNoMoreInteractions(mockCallback);

	}

	@Test
	public void testAcqurieLockAndCloseWithFailedReadLock() throws IOException {
		// null signals a failed lock attempt.
		when(mockCountingSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(
				Optional.of("tokenOne"), Optional.of("tokenTwo"),
				Optional.empty());
		when(mockCountingSemaphore.getFirstUnexpiredLockContext(any())).thenReturn(
				Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("locked by someone else"));

		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);

		String message = assertThrows(LockUnavilableException.class, () -> {
			// call under test
			try (ReadLock lock = semaphore.getReadLock(new ReadLockRequest(mockCallback, context, keys))) {
				
			}
		}).getMessage();
		
		assertEquals("Read lock unavailable for key: 'three'. Current lock holder's context: 'locked by someone else'", message);

		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("one_WRITER_LOCK");
		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("two_WRITER_LOCK");
		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("three_WRITER_LOCK");
		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("three_READER_LOCK");
		verify(mockCountingSemaphore, times(4)).getFirstUnexpiredLockContext(any());

		verify(mockCountingSemaphore).attemptToAcquireLock("one_READER_LOCK", maxTimeout, maxNumberOfReaders, context);
		verify(mockCountingSemaphore).attemptToAcquireLock("two_READER_LOCK", maxTimeout, maxNumberOfReaders, context);
		verify(mockCountingSemaphore).attemptToAcquireLock("three_READER_LOCK", maxTimeout, maxNumberOfReaders, context);
		verify(mockCountingSemaphore, times(3)).attemptToAcquireLock(any(), anyLong(), anyInt(), any());

		verify(mockCallback, never()).addProgressListener(any());

		// close checks
		verify(mockCallback, never()).removeProgressListener(any());
		// the first two locks must be released even though the third lock attempt failed.
		verify(mockCountingSemaphore).releaseLock("one_READER_LOCK", "tokenOne");
		verify(mockCountingSemaphore).releaseLock("two_READER_LOCK", "tokenTwo");
		verify(mockCountingSemaphore, times(2)).releaseLock(any(), any());
	}
	
	@Test
	public void testAcqurieLockAndCloseWithReleaseFailed() throws IOException {
		when(mockCountingSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(
				Optional.of("tokenOne"), Optional.of("tokenTwo"),
				Optional.of("tokenThree"));
		when(mockCountingSemaphore.getFirstUnexpiredLockContext(any())).thenReturn(Optional.empty());
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		// release failure on the first lock should still release the other locks.
		LockReleaseFailedException releaseException = new LockReleaseFailedException("failed to release");
		doThrow(releaseException).when(mockCountingSemaphore).releaseLock("one_READER_LOCK", "tokenOne");

		IOException exception = assertThrows(IOException.class, ()->{
			// call under test
			// call under test
			try (ReadLock lock = semaphore.getReadLock(new ReadLockRequest(mockCallback, context, keys))) {
				
			}
		});
		assertEquals(releaseException, exception.getCause());

		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("one_WRITER_LOCK");
		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("two_WRITER_LOCK");
		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("three_WRITER_LOCK");
		verify(mockCountingSemaphore, times(3)).getFirstUnexpiredLockContext(any());

		verify(mockCountingSemaphore).attemptToAcquireLock("one_READER_LOCK", maxTimeout, maxNumberOfReaders, context);
		verify(mockCountingSemaphore).attemptToAcquireLock("two_READER_LOCK", maxTimeout, maxNumberOfReaders, context);
		verify(mockCountingSemaphore).attemptToAcquireLock("three_READER_LOCK", maxTimeout, maxNumberOfReaders, context);
		verify(mockCountingSemaphore, times(3)).attemptToAcquireLock(any(), anyLong(), anyInt(), any());

		verify(mockCallback).addProgressListener(listenerCaptor.capture());
		ProgressListener listener = listenerCaptor.getValue();
		assertNotNull(listener);
		// trigger progress made
		listener.progressMade();
		verify(mockCountingSemaphore).refreshLockTimeout("one_READER_LOCK", "tokenOne", maxTimeout);
		verify(mockCountingSemaphore).refreshLockTimeout("two_READER_LOCK", "tokenTwo", maxTimeout);
		verify(mockCountingSemaphore).refreshLockTimeout("three_READER_LOCK", "tokenThree", maxTimeout);
		verify(mockCountingSemaphore, times(3)).refreshLockTimeout(any(), any(), anyLong());

		// close checks
		verify(mockCallback).removeProgressListener(listener);
		verify(mockCountingSemaphore).releaseLock("one_READER_LOCK", "tokenOne");
		verify(mockCountingSemaphore).releaseLock("two_READER_LOCK", "tokenTwo");
		verify(mockCountingSemaphore).releaseLock("three_READER_LOCK", "tokenThree");
		verify(mockCountingSemaphore, times(3)).releaseLock(any(), any());
	}
	
	@Test
	public void testAcqurieLockAndCloseWithNullcontext() throws IOException {
		when(mockCountingSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(
				Optional.empty());
		when(mockCountingSemaphore.getFirstUnexpiredLockContext(any())).thenReturn(Optional.empty());
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);

		String message = assertThrows(LockUnavilableException.class, () -> {
			// call under test
			try (ReadLock lock = semaphore.getReadLock(new ReadLockRequest(mockCallback, context, keys))) {
				
			}
		}).getMessage();
		
		assertEquals("Read lock unavailable for key: 'one'. Current lock holder's context: 'null'", message);

		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("one_WRITER_LOCK");
		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("two_WRITER_LOCK");
		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("three_WRITER_LOCK");
		verify(mockCountingSemaphore).getFirstUnexpiredLockContext("one_READER_LOCK");
		verify(mockCountingSemaphore, times(4)).getFirstUnexpiredLockContext(any());

		verify(mockCountingSemaphore).attemptToAcquireLock("one_READER_LOCK", maxTimeout, maxNumberOfReaders, context);
		verify(mockCountingSemaphore, times(1)).attemptToAcquireLock(any(), anyLong(), anyInt(), any());

		verifyNoMoreInteractions(mockCountingSemaphore);
		verifyNoMoreInteractions(mockCallback);
	}

}
