package org.sagebionetworks.repo.init;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.dao.semaphore.CountingSemaphoreDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-delayed-initializer-context.spb.xml" })
public class DelayedInitializerAutowireTest {

	private static final AtomicInteger count = new AtomicInteger(0);
	private static final int TOTAL_COUNT = 4;

	public static class StubCountingSemaphoreDaoImpl implements CountingSemaphoreDao {
		int gate = 0;
		@Override
		public String attemptToAcquireLock() {
			return gate++ % 2 == 0 ? null : "xx";
		}

		@Override
		public void releaseLock(String token) {
		}

		@Override
		public String attemptToAcquireLock(String extraKey) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void releaseLock(String token, String extraKey) {
			// TODO Auto-generated method stub

		}

		@Override
		public void extendLockLease(String token) throws NotFoundException {
			// TODO Auto-generated method stub

		}

		@Override
		public long getLockTimeoutMS() {
			// TODO Auto-generated method stub
			return 0;
		}
	}

	public static boolean postInitialize() {
		if (count.incrementAndGet() == TOTAL_COUNT) {
			return true;
		} else {
			return false;
		}
	}

	@Test
	public void testWaitForWorker() throws Exception {
		// wait long enough for the counter to overshoot if there was a bug
		Thread.sleep(2000);
		assertEquals(TOTAL_COUNT, count.get());
	}
}
