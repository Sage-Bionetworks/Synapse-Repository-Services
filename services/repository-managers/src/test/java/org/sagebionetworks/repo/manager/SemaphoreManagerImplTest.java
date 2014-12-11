package org.sagebionetworks.repo.manager;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.semaphore.SemaphoreDao;
import org.springframework.test.util.ReflectionTestUtils;

public class SemaphoreManagerImplTest {
	
	SemaphoreDao mockSemaphoreDao;
	SemaphoreManagerImpl manager;
	
	@Before
	public void before(){
		mockSemaphoreDao = Mockito.mock(SemaphoreDao.class);
		manager = new SemaphoreManagerImpl();
		ReflectionTestUtils.setField(manager,"semaphoreDao", mockSemaphoreDao);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testReleaseAllLocksAsAdminNull(){
		manager.releaseAllLocksAsAdmin(null);
	}
	
	@Test
	public void testReleaseAllLocksAsAdminHappy(){
		manager.releaseAllLocksAsAdmin(new UserInfo(true));
		verify(mockSemaphoreDao, times(1)).forceReleaseAllLocks();
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testReleaseAllLocksAsAdminUnauthorized(){
		manager.releaseAllLocksAsAdmin(new UserInfo(false));
		verify(mockSemaphoreDao, never()).forceReleaseAllLocks();
	}

}
