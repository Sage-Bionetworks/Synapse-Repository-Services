package org.sagebionetworks.repo.manager;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.semaphore.MemoryCountingSemaphore;
import org.springframework.test.util.ReflectionTestUtils;

public class SemaphoreManagerImplTest {
	
	CountingSemaphore mockSemaphoreDao;
	MemoryCountingSemaphore mockMemoryCountingSemaphore;
	SemaphoreManagerImpl manager;
	
	@Before
	public void before(){
		mockSemaphoreDao = Mockito.mock(CountingSemaphore.class);
		mockMemoryCountingSemaphore = Mockito.mock(MemoryCountingSemaphore.class);
		manager = new SemaphoreManagerImpl();
		ReflectionTestUtils.setField(manager,"countingSemaphore", mockSemaphoreDao);
		ReflectionTestUtils.setField(manager,"userThrottleMemoryCountingSemaphore", mockMemoryCountingSemaphore);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testReleaseAllLocksAsAdminNull(){
		manager.releaseAllLocksAsAdmin(null);
	}
	
	@Test
	public void testReleaseAllLocksAsAdminHappy(){
		manager.releaseAllLocksAsAdmin(new UserInfo(true));
		verify(mockSemaphoreDao, times(1)).releaseAllLocks();
		verify(mockMemoryCountingSemaphore, times(1)).releaseAllLocks();
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testReleaseAllLocksAsAdminUnauthorized(){
		manager.releaseAllLocksAsAdmin(new UserInfo(false));
		verify(mockSemaphoreDao, never()).releaseAllLocks();
		verify(mockMemoryCountingSemaphore, never()).releaseAllLocks();
	}

}
