package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.dao.semaphore.SemaphoreDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOSemaphoreDaoImplAutowireTest {

	@Autowired
	SemaphoreDao semaphoreDao;
	
	@Before
	public void before(){
		semaphoreDao.forceReleaseAllLocks();
	}
	
	@After
	public void after() {
		semaphoreDao.forceReleaseAllLocks();
	}

	@Test
	public void testLockAndUnlock(){
		String key = "UNSENT_MESSAGE_WORKER";
		// Get the lock and hold it for 1 second
		String token = semaphoreDao.attemptToAcquireLock(key, 1000);
		assertNotNull(token);
		System.out.println(token);
		// While holding the token we should not be able to get it again
		String secondToken = semaphoreDao.attemptToAcquireLock(key, 1000);
		assertEquals("We should not be able to get the lock while another is holding it.",null, secondToken);
		// We should not be able to release the lock without the correc token
		assertFalse("Lock was released with a fake token",semaphoreDao.releaseLock(key, "bogus-token"));
		// Now release the lock
		assertTrue("Failed to release the lock with a valid token",semaphoreDao.releaseLock(key, token));
		// We should now be able to acquire the lock again.
		token = semaphoreDao.attemptToAcquireLock(key, 1);
		assertNotNull("Could not acquire the lock after releasing it", token);
		assertTrue("Failed to release the lock with a valid token",semaphoreDao.releaseLock(key, token));
	}
	
	@Test
	public void testAcquireExpiredLock() throws InterruptedException{
		// For this test we want to acquire a lock, let it expire.  Once expired, we should be able to
		// acquire the lock even though it has not been released.
		String key = "UNSENT_MESSAGE_WORKER";
		// Get the lock and hold it for 1 second
		String originalToken = semaphoreDao.attemptToAcquireLock(key, 1000);
		assertNotNull(originalToken);
		// We should not be able to acquire it yet
		String secondToken = semaphoreDao.attemptToAcquireLock(key, 1000);
		assertEquals("We should not be able to get the lock while another is holding it.",null, secondToken);
		// Now let the original lock expire
		Thread.sleep(1500);
		// We should now be able to acquire the lock even though it has not been released.
		secondToken = semaphoreDao.attemptToAcquireLock(key, 1000);
		assertNotNull("Failed to acquire after the original lock expired ",secondToken);
		assertFalse("The second token should not equal the original token", secondToken.equals(originalToken));
		
		// Let it expire again
		Thread.sleep(1500);
		String thirdToken = semaphoreDao.attemptToAcquireLock(key, 1000);
		assertNotNull("Failed to acquire after the second lock expired ",thirdToken);
		assertFalse("The third token should not equal the original token", thirdToken.equals(originalToken));
		
		// Now releasing the original lock should fail, since they lost the lock
		assertFalse("The original token lock expired so we should not be able to release it. ",semaphoreDao.releaseLock(key, originalToken));
		assertFalse("The second token locked expired so we should not be able to release it.",semaphoreDao.releaseLock(key, secondToken));
		assertTrue("The third token should be valid so we should have been able to release it.",semaphoreDao.releaseLock(key, thirdToken));
	}
	
	@Test
	public void testForceReleaseAllLocks() throws InterruptedException{
		String key = "UNSENT_MESSAGE_WORKER";
		// Get the lock and hold it for 1 second
		String originalToken = semaphoreDao.attemptToAcquireLock(key, 1000);
		assertNotNull(originalToken);
		// Force the release of the lock
		semaphoreDao.forceReleaseAllLocks();
		// Now releasing the original lock should fail, since they lost the lock
		assertFalse("The original token lock was force released so we should not be able to release it. ",semaphoreDao.releaseLock(key, originalToken));
	}
	
	@Test
	public void testForceReleaseAllLocksEmpty() throws InterruptedException{
		// needs to work even when there are no locks.
		semaphoreDao.forceReleaseAllLocks();
		semaphoreDao.forceReleaseAllLocks();
	}
	
	@Test
	public void testRefefrshLockHappy() throws Exception {
		String key = "UNSENT_MESSAGE_WORKER";
		// Get the lock and hold it for 1 second
		long lockTimeout = 3000;
		String token = semaphoreDao.attemptToAcquireLock(key, lockTimeout);
		assertNotNull(token);
		// We should not be able to acquire it yet
		String secondToken = semaphoreDao.attemptToAcquireLock(key, lockTimeout);
		assertEquals("We should not be able to get the lock while another is holding it.",null, secondToken);
		// Now refresh the lock until the original time expires
		for (int i = 0; i < lockTimeout / 1000 + 1; i++) {
			Thread.sleep(1000L);
			semaphoreDao.refreshLockTimeout(key, token, lockTimeout);
		}
		// The original lock would now be timed-out if we had not refreshed it.
		// To prove we are still holding attempt to acquire it again.
		secondToken = semaphoreDao.attemptToAcquireLock(key, lockTimeout);
		assertEquals("We should not be able to get the lock because it should have been refreshed.",null, secondToken);
	}
	
	@Test
	public void testRefefrshExpiresLockLockHappy() throws InterruptedException{
		String key = "UNSENT_MESSAGE_WORKER";
		// Get the lock and hold it for 1 second
		long lockTimeout = 10;
		String token = semaphoreDao.attemptToAcquireLock(key, lockTimeout);
		assertNotNull(token);
		// Let the lock expires
		Thread.sleep(lockTimeout+10);
		// Try to refresh the expired lock.
		try {
			semaphoreDao.refreshLockTimeout(key, token, lockTimeout);
			fail("Should have failed to refresh an expired lock");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("lock has already expired"));
		}
	}
	
	@Test
	public void testRefefrshExpiresLockBadKey() throws InterruptedException{
		String key = "UNSENT_MESSAGE_WORKER";
		// Get the lock and hold it for 1 second
		long lockTimeout = 1000;
		String token = semaphoreDao.attemptToAcquireLock(key, lockTimeout);
		assertNotNull(token);
		// Try to refresh the expired lock.
		try {
			semaphoreDao.refreshLockTimeout(key+"bad", token, lockTimeout);
			fail("Should have failed to refresh an expired lock");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Cannot refresh the lock for key"));
		}
	}
	
	@Test
	public void testRefefrshExpiresLockBadToken() throws InterruptedException{
		String key = "UNSENT_MESSAGE_WORKER";
		// Get the lock and hold it for 1 second
		long lockTimeout = 1000;
		String token = semaphoreDao.attemptToAcquireLock(key, lockTimeout);
		assertNotNull(token);
		// Try to refresh the expired lock.
		try {
			semaphoreDao.refreshLockTimeout(key, token+"bad", lockTimeout);
			fail("Should have failed to refresh an expired lock");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("Cannot refresh the lock for key"));
		}
	}
	
	
}
