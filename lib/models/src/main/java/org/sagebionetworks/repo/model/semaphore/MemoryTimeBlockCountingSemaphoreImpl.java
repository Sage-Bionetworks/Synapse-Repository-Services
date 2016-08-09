package org.sagebionetworks.repo.model.semaphore;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.util.ValidateArgument;

public class MemoryTimeBlockCountingSemaphoreImpl implements MemoryTimeBlockCountingSemaphore{
	private Map<String, SimpleSemaphore> keySemaphoreMap = new HashMap<String,SimpleSemaphore>();
	
	@Override
	public synchronized boolean attemptToAcquireLock(String key, long timeoutSec, int maxLockCount) {
		ValidateArgument.required(key, "key");
		ValidateArgument.requirement(timeoutSec >= 0, "timeoutSec must be a positive value");
		ValidateArgument.requirement(maxLockCount >= 0, "maxLockCount must be a positive value");
		
		SimpleSemaphore semaphore = keySemaphoreMap.get(key);
		
		if(semaphore == null){
			//no semaphore created for key yet
			//create new semaphore and increment
			SimpleSemaphore newSem = new SimpleSemaphore();
			newSem.setExpiration(calcExpirationTime(timeoutSec));
			newSem.increment();
			keySemaphoreMap.put(key, newSem);
			return true;
		}
		
		if(semaphore.isExpired()){
			//reset and increment if semaphore has expired
			semaphore.setExpiration(calcExpirationTime(timeoutSec));
			semaphore.resetCount();
			semaphore.increment();
			return true;
		}
		
		if(semaphore.getCount() < maxLockCount){
			//current count not exceeding maximum. just increment
			semaphore.increment();
			return true;
		}
		
		return false;
	}
	
	private static long calcExpirationTime(long timeoutSec){
		return System.currentTimeMillis() + timeoutSec * 1000;
	}

	@Override
	public synchronized void releaseAllLocks() {
		keySemaphoreMap.clear();
	}

}
