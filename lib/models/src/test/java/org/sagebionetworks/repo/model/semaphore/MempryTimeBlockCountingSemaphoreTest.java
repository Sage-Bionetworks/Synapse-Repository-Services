package org.sagebionetworks.repo.model.semaphore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class MempryTimeBlockCountingSemaphoreTest {
	
	private MemoryTimeBlockCountingSemaphore memoryTimeBlockCountingSemaphore;
	
	@Mock
	private Map<String, SimpleSemaphore> keySemaphoreMap;
	
	@Mock
	private SimpleSemaphore mockSemaphore;
	
	private static String key = "some key";
	
	private static final int limit = 1;
	
	private static final int timeoutSec = 2;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		memoryTimeBlockCountingSemaphore = new MemoryTimeBlockCountingSemaphoreImpl();
		ReflectionTestUtils.setField(memoryTimeBlockCountingSemaphore, "keySemaphoreMap", keySemaphoreMap);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testAcquireLockNullKey(){
		memoryTimeBlockCountingSemaphore.attemptToAcquireLock(null, timeoutSec, limit);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testAcquireLockNegativeTimeoutSec(){
		memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, -1, limit);
		
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testAcquireLockNegativeMaxLock(){
		memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, timeoutSec, -1);
	}

	@Test
	public void testAcquireLockNoExistentSemaphore() {
		when(keySemaphoreMap.get(key)).thenReturn(null);
		
		assertTrue(memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, timeoutSec, limit));
		
		verify(keySemaphoreMap).get(key);
		ArgumentCaptor<SimpleSemaphore> semaphoreCaptor = ArgumentCaptor.forClass(SimpleSemaphore.class);
		verify(keySemaphoreMap).put(eq(key), semaphoreCaptor.capture());
		SimpleSemaphore capturedSemaphore = semaphoreCaptor.getValue();
		assertEquals(1, capturedSemaphore.getCount());
		verifyNoMoreInteractions(keySemaphoreMap);
	}
	
	@Test
	public void testAcquireLockExpiredSemaphore() throws InterruptedException {
		when(keySemaphoreMap.get(key)).thenReturn(mockSemaphore);
		when(mockSemaphore.isExpired()).thenReturn(true);
		
		assertTrue(memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, timeoutSec, limit));
		
		verify(keySemaphoreMap).get(key);
		verify(mockSemaphore).isExpired();
		verify(mockSemaphore).setExpiration(any(Long.class));
		verify(mockSemaphore).resetCount();
		verify(mockSemaphore).increment();
		
		verifyNoMoreInteractions(keySemaphoreMap,  mockSemaphore);
	}
	
	@Test
	public void testAcquireLockOverCountLimit(){
		when(keySemaphoreMap.get(key)).thenReturn(mockSemaphore);
		when(mockSemaphore.isExpired()).thenReturn(false);
		when(mockSemaphore.getCount()).thenReturn(limit);
		
		assertFalse(memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, timeoutSec, limit));
		
		verify(keySemaphoreMap).get(key);
		verify(mockSemaphore).isExpired();
		verify(mockSemaphore).getCount();
		verifyNoMoreInteractions(keySemaphoreMap,  mockSemaphore);
	}
	
	@Test
	public void testAcquireLockUnderCountLimit(){
		when(keySemaphoreMap.get(key)).thenReturn(mockSemaphore);
		when(mockSemaphore.isExpired()).thenReturn(false);
		when(mockSemaphore.getCount()).thenReturn(limit - 1);
		
		assertTrue(memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, timeoutSec, limit));
		
		verify(keySemaphoreMap).get(key);
		verify(mockSemaphore).isExpired();
		verify(mockSemaphore).getCount();
		verify(mockSemaphore).increment();
		verifyNoMoreInteractions(keySemaphoreMap,  mockSemaphore);

	}
	
	@Test
	public void testAcquireLockZeroMaxLocks(){
		assertFalse(memoryTimeBlockCountingSemaphore.attemptToAcquireLock(key, timeoutSec, 0));
		verify(keySemaphoreMap, never()).get(key);
		verify(mockSemaphore, never()).isExpired();
		verify(mockSemaphore, never()).getCount();
		verify(mockSemaphore, never()).increment();
		verify(mockSemaphore, never()).setExpiration(any(Long.class));
	}

}
