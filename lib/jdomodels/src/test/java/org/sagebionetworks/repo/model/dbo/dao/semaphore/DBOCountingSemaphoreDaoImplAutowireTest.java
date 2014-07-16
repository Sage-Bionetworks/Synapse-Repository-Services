package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.dao.semaphore.CountingSemaphoreDao;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOCountingSemaphoreDaoImplAutowireTest {

	@Autowired
	CountingSemaphoreDao countingSemaphoreDao;

	final int maxCount = 5;

	@Before
	public void before() throws Exception {
		((CountingSemaphoreDaoImpl) getTargetObject(countingSemaphoreDao)).forceReleaseAllLocks();
	}

	@Test
	public void testLockAndUnlock() throws Exception {
		((CountingSemaphoreDaoImpl) getTargetObject(countingSemaphoreDao)).setLockTimeoutMS(10000);
		((CountingSemaphoreDaoImpl) getTargetObject(countingSemaphoreDao)).setMaxCount(maxCount);

		String token = countingSemaphoreDao.attemptToAcquireLock();
		assertNotNull("could not aqcuire first lock", token);

		// get all available locks
		String[] tokens = new String[maxCount - 1];
		for (int i = 0; i < tokens.length; i++) {
			tokens[i] = countingSemaphoreDao.attemptToAcquireLock();
			assertNotNull("could not aqcuire token " + i, tokens[i]);
		}

		// We should not be able to acquire another one
		String secondToken = countingSemaphoreDao.attemptToAcquireLock();
		assertNull("We should not be able to get another lock.", secondToken);

		// release one
		countingSemaphoreDao.releaseLock(token);

		// and reaquire it
		token = countingSemaphoreDao.attemptToAcquireLock();
		assertNotNull("could not reaqcuire first lock", token);

		// We should not be able to acquire another one
		secondToken = countingSemaphoreDao.attemptToAcquireLock();
		assertNull("We should not be able to get another lock.", secondToken);

		// release all available locks
		for (int i = 0; i < tokens.length; i++) {
			countingSemaphoreDao.releaseLock(tokens[i]);
		}

		for (int i = 0; i < tokens.length; i++) {
			tokens[i] = countingSemaphoreDao.attemptToAcquireLock();
			assertNotNull("could not reaqcuire tokens " + i, tokens[i]);
		}

		// We should not be able to acquire another one
		secondToken = countingSemaphoreDao.attemptToAcquireLock();
		assertNull("We should not be able to get another lock.", secondToken);

		for (int i = 0; i < tokens.length; i++) {
			countingSemaphoreDao.releaseLock(tokens[i]);
		}
	}

	@Test
	public void testAcquireExpiredLock() throws Exception {
		// For this test we want to acquire a lock, let it expire. Once expired, we should be able to
		// acquire the lock even though it has not been released.
		((CountingSemaphoreDaoImpl) getTargetObject(countingSemaphoreDao)).setLockTimeoutMS(500);
		((CountingSemaphoreDaoImpl) getTargetObject(countingSemaphoreDao)).setMaxCount(1);

		// Get the lock and hold it for 1 second
		String originalToken = countingSemaphoreDao.attemptToAcquireLock();
		assertNotNull(originalToken);

		// We should not be able to acquire it yet
		String secondToken = countingSemaphoreDao.attemptToAcquireLock();
		assertNull("We should not be able to get another lock.", secondToken);

		// Now let the original lock expire
		Thread.sleep(1000);

		// We should now be able to acquire the lock even though it has not been released.
		secondToken = countingSemaphoreDao.attemptToAcquireLock();
		assertNotNull("Failed to acquire after the original lock expired ", secondToken);
		assertFalse("The second token should not equal the original token", secondToken.equals(originalToken));

		// Let it expire again
		Thread.sleep(1000);
		String thirdToken = countingSemaphoreDao.attemptToAcquireLock();
		assertNotNull("Failed to acquire after the second lock expired ", thirdToken);
		assertFalse("The third token should not equal the original token", thirdToken.equals(originalToken));
	}

	@SuppressWarnings("unchecked")
	protected <T> T getTargetObject(T proxy) throws Exception {
		if (AopUtils.isJdkDynamicProxy(proxy)) {
			return (T) ((Advised) proxy).getTargetSource().getTarget();
		} else {
			return proxy;
		}
	}
}
