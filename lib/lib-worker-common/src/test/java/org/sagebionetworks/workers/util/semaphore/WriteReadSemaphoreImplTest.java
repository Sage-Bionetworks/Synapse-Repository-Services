package org.sagebionetworks.workers.util.semaphore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.database.semaphore.LockReleaseFailedException;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.progress.ProgressCallback;

@ExtendWith(MockitoExtension.class)
public class WriteReadSemaphoreImplTest {

	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private CountingSemaphore mockCountingSemaphore;
	@Mock
	private WriteLockImpl mockWriteLock;
	@Mock
	private ReadLockImpl mockReadLock;
	@Mock
	private Clock mockClock;

	private WriteReadSemaphoreImpl semaphore;

	private int maxNumberOfReaders;
	private String[] keys;
	private long maxTimeout;
	private String context;
	private ReadLockRequest readRequest;
	private WriteLockRequest writeLockRequest;

	@BeforeEach
	public void before() {
		maxNumberOfReaders = 4;
		keys = new String[] { "one" };
		maxTimeout = 31L;
		context = "some context";
		semaphore = Mockito.spy(new WriteReadSemaphoreImpl(mockCountingSemaphore, maxNumberOfReaders, mockClock));
	}

	@Test
	public void testSemaphoreWithNullCounting() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new WriteReadSemaphoreImpl(null, maxNumberOfReaders, mockClock);
		}).getMessage();
		assertEquals("CountingSemaphore cannot be null", message);
	}

	@Test
	public void testGetReadLockProviderWithNullRequest() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			semaphore.getReadLock(null);
		}).getMessage();
		assertEquals("Request cannot be null", message);
	}

	@Test
	public void testGetWriteLockProviderWithNullRequest() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			semaphore.getWriteLock(null);
		}).getMessage();
		assertEquals("Request cannot be null", message);
	}

	@Test
	public void testGetReadLock() throws IOException {

		doReturn(mockReadLock).when(semaphore).createReadLock(any());

		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		readRequest = new ReadLockRequest(mockCallback, context, keys);

		// call under test
		ReadLock readLock = semaphore.getReadLock(readRequest);

		assertEquals(mockReadLock, readLock);
		verify(semaphore).createReadLock(readRequest);
		verify(mockReadLock).attemptToAcquireLock();
		verify(mockReadLock, never()).close();
	}

	@Test
	public void testGetReadLockWithLockUnavailableException() throws Exception {

		doReturn(mockReadLock).when(semaphore).createReadLock(any());
		LockUnavilableException exception = new LockUnavilableException(LockType.Read, "123", context);
		doThrow(exception).when(mockReadLock).attemptToAcquireLock();

		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		readRequest = new ReadLockRequest(mockCallback, context, keys);

		Exception thrown = assertThrows(LockUnavilableException.class, () -> {
			// call under test
			semaphore.getReadLock(readRequest);
		});
		assertEquals(exception, thrown);

		verify(semaphore).createReadLock(readRequest);
		verify(mockReadLock).attemptToAcquireLock();
		verify(mockReadLock).close();
	}

	@Test
	public void testGetReadLockWithLockCloseException() throws Exception {

		doReturn(mockReadLock).when(semaphore).createReadLock(any());
		LockUnavilableException exception = new LockUnavilableException(LockType.Read, "123", context);
		doThrow(exception).when(mockReadLock).attemptToAcquireLock();
		// also want to throw an exception when the lock is closed
		doThrow(new LockReleaseFailedException("cannot release")).when(mockReadLock).close();

		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		readRequest = new ReadLockRequest(mockCallback, context, keys);

		Exception thrown = assertThrows(LockUnavilableException.class, () -> {
			// call under test
			semaphore.getReadLock(readRequest);
		});
		assertEquals(exception, thrown);

		verify(semaphore).createReadLock(readRequest);
		verify(mockReadLock).attemptToAcquireLock();
		verify(mockReadLock).close();
	}

	@Test
	public void testGetReadLockWithOtherException() throws Exception {

		doReturn(mockReadLock).when(semaphore).createReadLock(any());
		IllegalArgumentException exception = new IllegalArgumentException("some other exception");
		doThrow(exception).when(mockReadLock).attemptToAcquireLock();

		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		readRequest = new ReadLockRequest(mockCallback, context, keys);

		Throwable thrown = assertThrows(RuntimeException.class, () -> {
			// call under test
			semaphore.getReadLock(readRequest);
		}).getCause();
		assertEquals(exception, thrown);

		verify(semaphore).createReadLock(readRequest);
		verify(mockReadLock).attemptToAcquireLock();
		verify(mockReadLock).close();
	}

	///

	@Test
	public void testGetWriteLock() throws Exception {

		doReturn(mockWriteLock).when(semaphore).createWriteLock(any());

		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		writeLockRequest = new WriteLockRequest(mockCallback, context, keys[0]);

		// call under test
		WriteLock writeLock = semaphore.getWriteLock(writeLockRequest);

		assertEquals(mockWriteLock, writeLock);
		verify(semaphore).createWriteLock(writeLockRequest);
		verify(mockWriteLock).attemptToAcquireLock();
		verify(mockWriteLock, never()).close();
	}

	@Test
	public void testGetWriteLockWithLockUnavailableException() throws Exception {

		doReturn(mockWriteLock).when(semaphore).createWriteLock(any());
		LockUnavilableException exception = new LockUnavilableException(LockType.Write, "123", context);
		doThrow(exception).when(mockWriteLock).attemptToAcquireLock();

		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		writeLockRequest = new WriteLockRequest(mockCallback, context, keys[0]);

		assertThrows(LockUnavilableException.class, () -> {
			// call under test
			semaphore.getWriteLock(writeLockRequest);
		});

		verify(semaphore).createWriteLock(writeLockRequest);
		verify(mockWriteLock).attemptToAcquireLock();
		verify(mockWriteLock).close();
	}

	@Test
	public void testGetWriteLockWithCloseException() throws Exception {

		doReturn(mockWriteLock).when(semaphore).createWriteLock(any());
		LockUnavilableException exception = new LockUnavilableException(LockType.Write, "123", context);
		doThrow(exception).when(mockWriteLock).attemptToAcquireLock();
		// also want to throw an exception when the lock is closed
		doThrow(new LockReleaseFailedException("cannot release")).when(mockWriteLock).close();

		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		writeLockRequest = new WriteLockRequest(mockCallback, context, keys[0]);

		assertThrows(LockUnavilableException.class, () -> {
			// call under test
			semaphore.getWriteLock(writeLockRequest);
		});

		verify(semaphore).createWriteLock(writeLockRequest);
		verify(mockWriteLock).attemptToAcquireLock();
		verify(mockWriteLock).close();
	}

	@Test
	public void testGetWriteLockWithOtherException() throws Exception {

		doReturn(mockWriteLock).when(semaphore).createWriteLock(any());
		IllegalArgumentException exception = new IllegalArgumentException("bad args");
		doThrow(exception).when(mockWriteLock).attemptToAcquireLock();

		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		writeLockRequest = new WriteLockRequest(mockCallback, context, keys[0]);

		Throwable cause = assertThrows(RuntimeException.class, () -> {
			// call under test
			semaphore.getWriteLock(writeLockRequest);
		}).getCause();
		assertEquals(exception, cause);

		verify(semaphore).createWriteLock(writeLockRequest);
		verify(mockWriteLock).attemptToAcquireLock();
		verify(mockWriteLock).close();
	}

	@Test
	public void testTryRunWithReadLock() throws Exception {
		doReturn(mockReadLock).when(semaphore).createReadLock(any());
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		readRequest = new ReadLockRequest(mockCallback, context, keys);

		// call under test
		String returned = semaphore.tryRunWithReadLock(readRequest, (cb) -> {
			return "results";
		});
		assertEquals("results", returned);

		verify(semaphore).createReadLock(readRequest);
		verify(mockReadLock).attemptToAcquireLock();
		verify(mockReadLock).close();
	}

	@Test
	public void testTryRunWithReadLockWithException() throws Exception {
		doReturn(mockReadLock).when(semaphore).createReadLock(any());
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		readRequest = new ReadLockRequest(mockCallback, context, keys);
		Exception e = new Exception("no");
		Exception thrown = assertThrows(Exception.class, () -> {
			// call under test
			semaphore.tryRunWithReadLock(readRequest, (cb) -> {
				throw e;
			});
		});
		assertEquals(e, thrown.getCause());

		verify(semaphore).createReadLock(readRequest);
		verify(mockReadLock).attemptToAcquireLock();
		verify(mockReadLock).close();
	}

	@Test
	public void testTryRunWithReadLockWithRuntimeException() throws Exception {
		doReturn(mockReadLock).when(semaphore).createReadLock(any());
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		readRequest = new ReadLockRequest(mockCallback, context, keys);
		RuntimeException e = new RuntimeException("no");
		Exception thrown = assertThrows(RuntimeException.class, () -> {
			// call under test
			semaphore.tryRunWithReadLock(readRequest, (cb) -> {
				throw e;
			});
		});
		assertEquals(e, thrown);

		verify(semaphore).createReadLock(readRequest);
		verify(mockReadLock).attemptToAcquireLock();
		verify(mockReadLock).close();
	}

	@Test
	public void testTryRunWithWriteLock() throws Exception {
		doReturn(mockWriteLock).when(semaphore).createWriteLock(any());
		when(mockWriteLock.getExistingReadLockContext()).thenReturn(Optional.empty());

		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		writeLockRequest = new WriteLockRequest(mockCallback, context, keys[0]);

		// call under test
		String returned = semaphore.tryRunWithWriteLock(writeLockRequest, (p) -> {
			return "results";
		});
		assertEquals("results", returned);

		verify(semaphore).createWriteLock(writeLockRequest);
		verify(mockWriteLock).attemptToAcquireLock();
		verify(mockWriteLock).close();
		verifyZeroInteractions(mockClock);
	}

	@Test
	public void testTryRunWithWriteLockWithRuntimeException() throws Exception {
		doReturn(mockWriteLock).when(semaphore).createWriteLock(any());
		when(mockWriteLock.getExistingReadLockContext()).thenReturn(Optional.empty());

		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		writeLockRequest = new WriteLockRequest(mockCallback, context, keys[0]);
		RuntimeException e = new RuntimeException("not");

		RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
			// call under test
			semaphore.tryRunWithWriteLock(writeLockRequest, (p) -> {
				throw e;
			});
		});
		assertEquals(e, thrown);

		verify(semaphore).createWriteLock(writeLockRequest);
		verify(mockWriteLock).attemptToAcquireLock();
		verify(mockWriteLock).close();
		verifyZeroInteractions(mockClock);
	}

	@Test
	public void testTryRunWithWriteLockWithException() throws Exception {
		doReturn(mockWriteLock).when(semaphore).createWriteLock(any());
		when(mockWriteLock.getExistingReadLockContext()).thenReturn(Optional.empty());

		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		writeLockRequest = new WriteLockRequest(mockCallback, context, keys[0]);
		Exception e = new Exception("not");

		Exception thrown = assertThrows(Exception.class, () -> {
			// call under test
			semaphore.tryRunWithWriteLock(writeLockRequest, (p) -> {
				throw e;
			});
		});
		assertEquals(e, thrown.getCause());

		verify(semaphore).createWriteLock(writeLockRequest);
		verify(mockWriteLock).attemptToAcquireLock();
		verify(mockWriteLock).close();
		verifyZeroInteractions(mockClock);
	}

	@Test
	public void testTryRunWithWriteLockWithExistingReadLocks() throws Exception {
		doReturn(mockWriteLock).when(semaphore).createWriteLock(any());
		when(mockWriteLock.getExistingReadLockContext()).thenReturn(Optional.of("one"), Optional.of("two"),
				Optional.empty());

		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		writeLockRequest = new WriteLockRequest(mockCallback, context, keys[0]).setWaitForReaderMS(101L)
				.setMaxReaderWaitAttempts(10);

		// call under test
		String returned = semaphore.tryRunWithWriteLock(writeLockRequest, (p) -> {
			return "results";
		});
		assertEquals("results", returned);

		verify(semaphore).createWriteLock(writeLockRequest);
		verify(mockWriteLock).attemptToAcquireLock();
		verify(mockWriteLock).close();
		verify(mockClock, times(2)).sleep(101L);
	}

	@Test
	public void testTryRunWithWriteLockWithExistingReadLocksExceeded() throws Exception {
		doReturn(mockWriteLock).when(semaphore).createWriteLock(any());
		when(mockWriteLock.getExistingReadLockContext()).thenReturn(Optional.of("three"), Optional.of("two"),
				Optional.of("one"), Optional.empty());

		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		writeLockRequest = new WriteLockRequest(mockCallback, context, keys[0]).setWaitForReaderMS(101L)
				.setMaxReaderWaitAttempts(2);

		String message = assertThrows(LockUnavilableException.class, () -> {
			// call under test
			semaphore.tryRunWithWriteLock(writeLockRequest, (p) -> {
				return "results";
			});
		}).getMessage();
		assertEquals("Write lock unavailable for key: 'one'. Current lock holder's context: 'some context'", message);

		verify(semaphore).createWriteLock(writeLockRequest);
		verify(mockWriteLock).attemptToAcquireLock();
		verify(mockWriteLock).close();
		verify(mockClock, times(2)).sleep(101L);
	}

}
