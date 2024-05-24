package org.sagebionetworks.workers.util.semaphore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.util.progress.ProgressListener;

@ExtendWith(MockitoExtension.class)
public class WriteLockImplTest {

	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private CountingSemaphore mockCountingSemaphore;
	@Captor
	private ArgumentCaptor<ProgressListener> listenerCaptor;
	private WriteReadSemaphore semaphore;

	private String lockKey;
	private long maxTimeout;
	private String context;
	private String lockToken;

	@BeforeEach
	public void before() {
		lockKey = "one";
		maxTimeout = 31L;
		context = "some context";
		lockToken = "lock_token";
		semaphore = new WriteReadSemaphoreImpl(mockCountingSemaphore, 1);
	}
	
	@Test
	public void testConstructorWithNullCallback() {
		mockCallback = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new WriteLockRequest(mockCallback, context, lockKey);
		}).getMessage();
		assertEquals("ProgressCallback cannot be null", message);
	}

	@Test
	public void testConstructorWithTimeoutTooLow() {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(1L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new WriteLockRequest(mockCallback, context, lockKey);
		}).getMessage();
		assertEquals("LockTimeout cannot be less than 2 seconds", message);
	}

	@Test
	public void testConstructorWithNullContext() {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		context = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new WriteLockRequest(mockCallback, context, lockKey);
		}).getMessage();
		assertEquals("Caller's context cannot be null", message);
	}

	@Test
	public void testConstructorWithNullLockKeys() {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		lockKey = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new WriteLockRequest(mockCallback, context, lockKey);
		}).getMessage();
		assertEquals("LockKey cannot be null", message);
	}

	@Test
	public void testConstructorWithNullSemaphore() {
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		mockCountingSemaphore = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new WriteLockImpl(mockCountingSemaphore, new WriteLockRequest(mockCallback, context, lockKey));
		}).getMessage();
		assertEquals("CountingSemaphore cannot be null", message);
	}
	
	@Test
	public void testConstructorWithNullRequest() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new WriteLockImpl(mockCountingSemaphore, null);
		}).getMessage();
		assertEquals("WriteLockRequest cannot be null", message);
	}
	
	@Test
	public void testAcquireLock() throws Exception {
		when(mockCountingSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.of(lockToken));
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		when(mockCountingSemaphore.getFirstUnexpiredLockContext(any())).thenReturn(Optional.of("one"), Optional.of("two"), Optional.empty());
		
		// call under test
		try(WriteLock lock = semaphore.getWriteLock(new WriteLockRequest(mockCallback, context, lockKey))){
			// call under test
			lock.getExistingReadLockContext();
			lock.getExistingReadLockContext();
			lock.getExistingReadLockContext();
		}
		
		verify(mockCountingSemaphore).attemptToAcquireLock("one_WRITER_LOCK", maxTimeout, Constants.WRITER_MAX_LOCKS, context);
		verify(mockCountingSemaphore, times(1)).attemptToAcquireLock(any(), anyLong(), anyInt(), any());
		
		verify(mockCountingSemaphore, times(3)).getFirstUnexpiredLockContext("one_READER_LOCK");
		verify(mockCountingSemaphore, times(3)).getFirstUnexpiredLockContext(any());
		
		verify(mockCallback).addProgressListener(listenerCaptor.capture());
		ProgressListener listener = listenerCaptor.getValue();
		assertNotNull(listener);
		// trigger progress made
		listener.progressMade();
		listener.progressMade();
		verify(mockCountingSemaphore, times(2)).refreshLockTimeout("one_WRITER_LOCK", lockToken, maxTimeout);
		verify(mockCountingSemaphore, times(2)).refreshLockTimeout(any(), any(), anyLong());

		// close checks
		verify(mockCallback).removeProgressListener(listener);
		verify(mockCountingSemaphore).releaseLock("one_WRITER_LOCK", lockToken);
		verify(mockCountingSemaphore, times(1)).releaseLock(any(), any());
	}
	
	@Test
	public void testAcquireLockWithFailure() throws Exception {
		when(mockCountingSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.empty());
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		when(mockCountingSemaphore.getFirstUnexpiredLockContext(any())).thenReturn(Optional.of("other holder"));
		
		String message = assertThrows(LockUnavilableException.class, ()->{
			// call under test
			try(WriteLock lock = semaphore.getWriteLock(new WriteLockRequest(mockCallback, context, lockKey))){
				// call under test
				lock.getExistingReadLockContext();
				lock.getExistingReadLockContext();
				lock.getExistingReadLockContext();
			}
		}).getMessage();
		
		assertEquals("Write lock unavailable for key: 'one'. Current lock holder's context: 'other holder'", message);
		
		verify(mockCountingSemaphore).attemptToAcquireLock("one_WRITER_LOCK", maxTimeout, Constants.WRITER_MAX_LOCKS, context);
		verify(mockCountingSemaphore, times(1)).attemptToAcquireLock(any(), anyLong(), anyInt(), any());
		
		verify(mockCountingSemaphore, times(1)).getFirstUnexpiredLockContext("one_WRITER_LOCK");
		verify(mockCountingSemaphore, times(1)).getFirstUnexpiredLockContext(any());
		
		verifyNoMoreInteractions(mockCountingSemaphore, mockCallback);
	}
	
	@Test
	public void testAcquireLockWithFailureWithNullContext() throws Exception {
		when(mockCountingSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.empty());
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		when(mockCountingSemaphore.getFirstUnexpiredLockContext(any())).thenReturn(Optional.empty());
		
		String message = assertThrows(LockUnavilableException.class, ()->{
			// call under test
			try(WriteLock lock = semaphore.getWriteLock(new WriteLockRequest(mockCallback, context, lockKey))){
				lock.getExistingReadLockContext();
				lock.getExistingReadLockContext();
				lock.getExistingReadLockContext();
			}
		}).getMessage();
		
		assertEquals("Write lock unavailable for key: 'one'. Current lock holder's context: 'null'", message);
		
		verify(mockCountingSemaphore).attemptToAcquireLock("one_WRITER_LOCK", maxTimeout, Constants.WRITER_MAX_LOCKS, context);
		verify(mockCountingSemaphore, times(1)).attemptToAcquireLock(any(), anyLong(), anyInt(), any());
		
		verify(mockCountingSemaphore, times(1)).getFirstUnexpiredLockContext("one_WRITER_LOCK");
		verify(mockCountingSemaphore, times(1)).getFirstUnexpiredLockContext(any());
		
		verifyNoMoreInteractions(mockCountingSemaphore, mockCallback);
	}
	
	@Test
	public void testAcquireLockWithRemoveListenerException() throws Exception {
		when(mockCountingSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.of(lockToken));
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		when(mockCountingSemaphore.getFirstUnexpiredLockContext(any())).thenReturn(Optional.of("one"), Optional.of("two"), Optional.empty());
		
		RuntimeException exception = new RuntimeException("Something went wrong");
		doThrow(exception).when(mockCallback).removeProgressListener(any());
		
		Throwable cause = assertThrows(IOException.class, ()->{
			// call under test
			try(WriteLock lock = semaphore.getWriteLock(new WriteLockRequest(mockCallback, context, lockKey))){
				lock.getExistingReadLockContext();
				lock.getExistingReadLockContext();
				lock.getExistingReadLockContext();
			}
		}).getCause();
		
		assertEquals(exception, cause);	

		
		verify(mockCountingSemaphore).attemptToAcquireLock("one_WRITER_LOCK", maxTimeout, Constants.WRITER_MAX_LOCKS, context);
		verify(mockCountingSemaphore, times(1)).attemptToAcquireLock(any(), anyLong(), anyInt(), any());
		
		verify(mockCountingSemaphore, times(3)).getFirstUnexpiredLockContext("one_READER_LOCK");
		verify(mockCountingSemaphore, times(3)).getFirstUnexpiredLockContext(any());
		
		verify(mockCallback).addProgressListener(listenerCaptor.capture());
		ProgressListener listener = listenerCaptor.getValue();
		assertNotNull(listener);
		// trigger progress made
		listener.progressMade();
		listener.progressMade();
		verify(mockCountingSemaphore, times(2)).refreshLockTimeout("one_WRITER_LOCK", lockToken, maxTimeout);
		verify(mockCountingSemaphore, times(2)).refreshLockTimeout(any(), any(), anyLong());

		// close checks
		verify(mockCallback).removeProgressListener(listener);
		verify(mockCountingSemaphore).releaseLock("one_WRITER_LOCK", lockToken);
		verify(mockCountingSemaphore, times(1)).releaseLock(any(), any());
	}
	
	@Test
	public void testAcquireLockWithReleaseException() throws Exception {
		when(mockCountingSemaphore.attemptToAcquireLock(any(), anyLong(), anyInt(), any())).thenReturn(Optional.of(lockToken));
		when(mockCallback.getLockTimeoutSeconds()).thenReturn(maxTimeout);
		when(mockCountingSemaphore.getFirstUnexpiredLockContext(any())).thenReturn(Optional.of("one"), Optional.of("two"), Optional.empty());
		
		RuntimeException exception = new RuntimeException("Something went wrong");
		doThrow(exception).when(mockCountingSemaphore).releaseLock(any(), any());
		
		
		Throwable cause = assertThrows(IOException.class, ()->{
			// call under test
			try(WriteLock lock = semaphore.getWriteLock(new WriteLockRequest(mockCallback, context, lockKey))){
				// call under test
				lock.getExistingReadLockContext();
				lock.getExistingReadLockContext();
				lock.getExistingReadLockContext();
			}
		}).getCause();
		
		assertEquals(exception, cause);	

		
		verify(mockCountingSemaphore).attemptToAcquireLock("one_WRITER_LOCK", maxTimeout, Constants.WRITER_MAX_LOCKS, context);
		verify(mockCountingSemaphore, times(1)).attemptToAcquireLock(any(), anyLong(), anyInt(), any());
		
		verify(mockCountingSemaphore, times(3)).getFirstUnexpiredLockContext("one_READER_LOCK");
		verify(mockCountingSemaphore, times(3)).getFirstUnexpiredLockContext(any());
		
		verify(mockCallback).addProgressListener(listenerCaptor.capture());
		ProgressListener listener = listenerCaptor.getValue();
		assertNotNull(listener);
		// trigger progress made
		listener.progressMade();
		listener.progressMade();
		verify(mockCountingSemaphore, times(2)).refreshLockTimeout("one_WRITER_LOCK", lockToken, maxTimeout);
		verify(mockCountingSemaphore, times(2)).refreshLockTimeout(any(), any(), anyLong());

		// close checks
		verify(mockCallback).removeProgressListener(listener);
		verify(mockCountingSemaphore).releaseLock("one_WRITER_LOCK", lockToken);
		verify(mockCountingSemaphore, times(1)).releaseLock(any(), any());
	}
	
}
