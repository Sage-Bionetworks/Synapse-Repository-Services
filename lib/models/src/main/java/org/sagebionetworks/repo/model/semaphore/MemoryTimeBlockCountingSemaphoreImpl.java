package org.sagebionetworks.repo.model.semaphore;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.util.ValidateArgument;

public class MemoryTimeBlockCountingSemaphoreImpl implements MemoryTimeBlockCountingSemaphore{
	private Map<String, SimpleSemaphore> keySemaphoreMap = new HashMap<String,SimpleSemaphore>();
	
	@Override
	public synchronized boolean attemptToAcquireLock(String key, long timeoutSec, long maxLockCount) {
		ValidateArgument.required(key, "key");
		ValidateArgument.requirement(timeoutSec >= 0, "timeoutSec must be a positive value");
		ValidateArgument.requirement(maxLockCount >= 0, "maxLockCount must be a positive value");
		
		if(maxLockCount == 0){
			//no need to track nor do anything if the max number of acquirable locks is 0
			return false;
		}
		
		SimpleSemaphore semaphore = keySemaphoreMap.get(key);
		
		if(semaphore == null){
			//no semaphore created for key yet
			//create new semaphore and increment
			SimpleSemaphore newSem = new SimpleSemaphore();
			resetAndIncrementSemaphore(newSem, timeoutSec);
			keySemaphoreMap.put(key, newSem);
			return true;
		}
		
		if(semaphore.isExpired()){
			//reset and increment if semaphore has expired
			resetAndIncrementSemaphore(semaphore, timeoutSec);
			return true;
		}
		
		if(semaphore.getCount() < maxLockCount){
			//current count not exceeding maximum. just increment
			semaphore.increment();
			return true;
		}
		
		return false;
	}
	
	//resets the semaphore, give it a new expiration and increment the count
	private static void resetAndIncrementSemaphore(SimpleSemaphore semaphore, long timeoutSec){
		long newExpiration = System.currentTimeMillis() + timeoutSec * 1000;
		semaphore.setExpiration(newExpiration);
		semaphore.resetCount();
		semaphore.increment();
	}

	@Override
	public synchronized void releaseAllLocks() {
		keySemaphoreMap.clear();
	}

}
