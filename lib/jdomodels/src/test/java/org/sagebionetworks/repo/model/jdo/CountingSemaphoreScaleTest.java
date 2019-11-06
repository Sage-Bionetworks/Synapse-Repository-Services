package org.sagebionetworks.repo.model.jdo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test for PLFM-4027.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class CountingSemaphoreScaleTest {

	@Autowired
	CountingSemaphore countingSemaphore;
		
	int poolSize;
	ExecutorService threadPool;
	int numberOfAttempts;
	int numberOfLockKeys;
	
	@Before
	public void before(){
		poolSize = 25;
		numberOfAttempts = 1000;
		numberOfLockKeys = 15;
		threadPool = Executors.newFixedThreadPool(poolSize);
		countingSemaphore.releaseAllLocks();

	}
	
	@Ignore
	@Test
	public void testMultipleThreads() throws InterruptedException{
		for(int i=0; i<numberOfAttempts; i++){
			int mod = i%numberOfLockKeys;
			String key = "testKey"+mod;
			threadPool.execute(new SimpleRunner(countingSemaphore, key));
		}
		// wait for the submitted jobs to finish.
		threadPool.shutdown();
		threadPool.awaitTermination(5, TimeUnit.MINUTES);
	}
	
	private static class SimpleRunner implements Runnable{
		
		private long timeoutSec = 40;
		private int maxLockCount = 8;
		private String key;
		
		CountingSemaphore countingSemaphore;
		
		
		public SimpleRunner(CountingSemaphore countingSemaphore, String key) {
			super();
			this.countingSemaphore = countingSemaphore;
			this.key = key;
		}


		@Override
		public void run() {
			String token = countingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount);
			if(token != null){
				System.out.println("Lock acquired: "+token+" on thread: "+Thread.currentThread().getId()+" key: "+key);
				// refresh the lock then release the lock
				countingSemaphore.refreshLockTimeout(key, token, timeoutSec);
				// release the token
				countingSemaphore.releaseLock(key, token);
			}else{
				System.out.println("Failed to acquire lock on thread: "+Thread.currentThread().getId()+" key: "+key);
			}
		}
		
	}

}
