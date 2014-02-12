package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.dao.semaphore.ExclusiveOrSharedSemaphoreDao;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class ExclusiveOrSharedSemaphoreDaoImplTest {

	@Autowired
	ExclusiveOrSharedSemaphoreDao exclusiveOrSharedSemaphoreDao;
	
	@Before
	public void before(){
		// release all locks
		exclusiveOrSharedSemaphoreDao.releaseAllLocks();
	}
	
	@After
	public void after(){
		// release all locks
		exclusiveOrSharedSemaphoreDao.releaseAllLocks();
	}
	
	@Test
	public void testHappyReadLock(){
		long start = System.currentTimeMillis();
		String key = "123";
		// Should be able to get a read lock
		String token = exclusiveOrSharedSemaphoreDao.acquireSharedLock(key, 1000);
		assertNotNull(token);
		// We should be able to release the lock
		exclusiveOrSharedSemaphoreDao.releaseSharedLock(key, token);
		System.out.println("Shared lock timing: "+(System.currentTimeMillis()-start));
	}
	
	@Test
	public void testHappyWriteLock(){
		long start = System.currentTimeMillis();
		String key = "123";
		// First get the lock-precursor token
		String precursorToken = exclusiveOrSharedSemaphoreDao.acquireExclusiveLockPrecursor(key);
		assertNotNull(precursorToken);
		// Use it to get the actual token
		String lockToken = exclusiveOrSharedSemaphoreDao.acquireExclusiveLock(key, precursorToken, 1000);
		assertNotNull(lockToken);
		// We should be able to release the lock
		exclusiveOrSharedSemaphoreDao.releaseExclusiveLock(key, lockToken);
		System.out.println("Exclusive lock timing: "+(System.currentTimeMillis()-start));
		
		// We should now be able to get the lock again
		// First get the lock-precursor token
		precursorToken = exclusiveOrSharedSemaphoreDao.acquireExclusiveLockPrecursor(key);
		assertNotNull(precursorToken);
		// Use it to get the actual token
		lockToken = exclusiveOrSharedSemaphoreDao.acquireExclusiveLock(key, precursorToken, 1000);
		// We should be able to release the lock
		exclusiveOrSharedSemaphoreDao.releaseExclusiveLock(key, lockToken);
	}
	
	@Test
	public void testAcquireSharedLockWithOutstandingWritePrecursor() throws InterruptedException{
		// first get a read lock.
		String key = "123";
		// Should be able to get a read lock
		String readLockToken = exclusiveOrSharedSemaphoreDao.acquireSharedLock(key, 1000);
		assertNotNull(readLockToken);
		// Now acquire the write-lock-precursor
		String writeLockPrecursor = exclusiveOrSharedSemaphoreDao.acquireExclusiveLockPrecursor(key);
		assertNotNull(writeLockPrecursor);
		// Now we should not be should not be able to get a new read lock
		try{
			exclusiveOrSharedSemaphoreDao.acquireSharedLock(key, 1000);
			fail("Attempting to get a new read-lock when there is an outstanding write-lock-precursor should have failed.");
		}catch(LockUnavilableException e){
			// expected
		}
		// Now let the precursor expire and try again.
		Thread.sleep(ExclusiveOrSharedSemaphoreDaoImpl.WRITE_LOCK_PRECURSOR_TIMEOUT+10);
		// This time it should work
		String readLockTwo = exclusiveOrSharedSemaphoreDao.acquireSharedLock(key, 1000);
		assertNotNull(readLockTwo);
	}
	
	@Test
	public void testAcquireSharedLockWithOutstandingWriteLock() throws InterruptedException{
		// first get a read lock.
		String key = "123";
		// Should be able to get a read lock
		String readLockToken = exclusiveOrSharedSemaphoreDao.acquireSharedLock(key, 1000);
		assertNotNull(readLockToken);
		// Now acquire the write-lock-precursor
		String writeLockPrecursor = exclusiveOrSharedSemaphoreDao.acquireExclusiveLockPrecursor(key);
		assertNotNull(writeLockPrecursor);
		// Now attempt to acquire the actual write-lock.
		String writeLockToken = exclusiveOrSharedSemaphoreDao.acquireExclusiveLock(key, writeLockPrecursor, 1000);
		assertEquals("Should not be able to get the actual write-lock when there is an outstanding read-lock",null, writeLockToken);
		// Release the read-lock so we can get the write-lock
		exclusiveOrSharedSemaphoreDao.releaseSharedLock(key, readLockToken);
		// Now get the write-lock
		writeLockToken = exclusiveOrSharedSemaphoreDao.acquireExclusiveLock(key, writeLockPrecursor, 1000);
		assertNotNull("Should have been able to get the actual write-lock as there are no more outstanding read-lock", writeLockToken);
		
		// Now we should not be should not be able to get a new read lock
		try{
			exclusiveOrSharedSemaphoreDao.acquireSharedLock(key, 1000);
			fail("Attempting to get a new read-lock when there is an outstanding write-lock should have failed.");
		}catch(LockUnavilableException e){
			// expected
		}
		// Now release the write lock and try again
		exclusiveOrSharedSemaphoreDao.releaseExclusiveLock(key, writeLockToken);
		// This time it should work
		String readLockTwo = exclusiveOrSharedSemaphoreDao.acquireSharedLock(key, 1000);
		assertNotNull(readLockTwo);
	}
	
	@Test
	public void testAcquireSecondWriteLockPrecursor() throws InterruptedException{
		String key = "123";
		// Now acquire the write-lock-precursor
		String writeLockPrecursor = exclusiveOrSharedSemaphoreDao.acquireExclusiveLockPrecursor(key);
		assertNotNull(writeLockPrecursor);
		// Trying to get a precursor again should fail
		try{
			exclusiveOrSharedSemaphoreDao.acquireExclusiveLockPrecursor(key);
			fail("Attempting to get a second write-lock-precursor should fail when on is already outstanding.");
		}catch(LockUnavilableException e){
			// expected
		}
		// Now let the precursor expire and try again.
		Thread.sleep(ExclusiveOrSharedSemaphoreDaoImpl.WRITE_LOCK_PRECURSOR_TIMEOUT+10);
		// This time it should work
		writeLockPrecursor = exclusiveOrSharedSemaphoreDao.acquireExclusiveLockPrecursor(key);
		assertNotNull(writeLockPrecursor);
	}
	
	@Test
	public void testForcedReadLockRelease() throws InterruptedException{
		String key = "123";
		long timeoutOne = 2000;
		long timeoutTwo = 4000;
		long maxWaitMS = (timeoutOne+timeoutTwo)*2;
		// Get two read locks on that expires after two seconds and another the expires after 4
		String readLockOne = exclusiveOrSharedSemaphoreDao.acquireSharedLock(key, timeoutOne);
		assertNotNull(readLockOne);
		String readLockTwo = exclusiveOrSharedSemaphoreDao.acquireSharedLock(key, timeoutTwo);
		assertNotNull(readLockTwo);
		// Get the precursor
		String writeLockPrecursor = exclusiveOrSharedSemaphoreDao.acquireExclusiveLockPrecursor(key);
		assertNotNull(writeLockPrecursor);
		long start = System.currentTimeMillis();
		String writeLock = null;
		do{
			// try to get writeLock
			writeLock = exclusiveOrSharedSemaphoreDao.acquireExclusiveLock(key, writeLockPrecursor, 1000);
			assertTrue("Timed-out waiting for read-locks to expire", (System.currentTimeMillis()-start) < maxWaitMS);
			if(writeLock == null){
				System.out.println("Waiting for read-locks to expire...");
				Thread.sleep(1000);
			}
		}while(writeLock == null);
		// We should now have the write lock
		assertNotNull(writeLock);
	}
	
	@Test
	public void testForcedWriteLockRelease() throws InterruptedException{
		String key = "123";
		long timeoutOne = 4000;
		long maxWaitMS = timeoutOne*2;
		// First acquire a precursor
		String writeLockPrecursor = exclusiveOrSharedSemaphoreDao.acquireExclusiveLockPrecursor(key);
		assertNotNull(writeLockPrecursor);
		String writeLock = exclusiveOrSharedSemaphoreDao.acquireExclusiveLock(key, writeLockPrecursor, timeoutOne);
		assertNotNull(writeLock);
		long start = System.currentTimeMillis();
		String readLock = null;
		do{
			// try to get writeLock
			try {
				readLock = exclusiveOrSharedSemaphoreDao.acquireSharedLock(key, timeoutOne);
			} catch (LockUnavilableException e) {
				// This will occur as long as the write-lock is active.
				readLock = null;
			}
			assertTrue("Timed-out waiting for write-locks to expire", (System.currentTimeMillis()-start) < maxWaitMS);
			if(readLock == null){
				System.out.println("Waiting for write-locks to expire...");
				Thread.sleep(1000);
			}
		}while(readLock == null);
		// We should now have the write lock
		assertNotNull(readLock);
	}
}
