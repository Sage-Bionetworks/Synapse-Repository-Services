package org.sagebionetworks.repo.model.semaphore;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.common.util.Clock;
import org.sagebionetworks.util.ValidateArgument;

/**
 * An in-memory implementation of a counting semaphore. This class is designed
 * to be a thread-safe singleton.
 * 
 * 
 */
public class MemoryCountingSemaphoreImpl implements MemoryCountingSemaphore {
	
	/*
	 * Note: This is not a synchronized map. All access to this map must occur in a synchronized method
	 * making this singleton the lock (not the map).
	 */
	private Map<String, List<Lock>> keyTokenMap = new HashMap<String, List<Lock>>();
	// abstraction from the system clock.
	private Clock clock;
	
	/**
	 * Create a new semaphore given a clock.
	 * @param clock
	 */
	public MemoryCountingSemaphoreImpl(Clock clock) {
		super();
		this.clock = clock;
	}

	@Override
	public synchronized String attemptToAcquireLock(String key, long timeoutSec,
			int maxLockCount) {
		ValidateArgument.required(key, "key");
		// Is there an entry in the map for this key?
		List<Lock> locks = keyTokenMap.get(key);
		if(locks == null){
			locks = new LinkedList<Lock>();
			keyTokenMap.put(key, locks);
		}
		// remove expired locks
		long now = clock.currentTimeMillis();
		Iterator<Lock> it = locks.iterator();
		while(it.hasNext()){
			Lock token = it.next();
			if(now > token.getExpiresTimeMs()){
				it.remove();
			}
		}
		// are we out of locks for this key?
		if(locks.size() < maxLockCount){
			// a new lock can be issued
			Lock token = new Lock();
			token.setExpiresTimeMs(now+(timeoutSec*1000));
			token.setToken(UUID.randomUUID().toString());
			locks.add(token);
			return token.getToken();
		}
		// a new token could not be issued.
		return null;
	}

	@Override
	public synchronized void refreshLockTimeout(String key, String tokenString, long timeoutSec) {
		ValidateArgument.required(key, "key");
		ValidateArgument.required(tokenString, "token");
		// Get the tokens for this key
		List<Lock> locks = keyTokenMap.get(key);
		boolean refreshed = false;
		if(locks != null){
			Iterator<Lock> it = locks.iterator();
			while(it.hasNext()){
				Lock token = it.next();
				if(token.getToken().equals(tokenString)){
					// found a match.
					long now = clock.currentTimeMillis();
					token.setExpiresTimeMs(now+(timeoutSec*1000));
					refreshed = true;
					break;
				}
			}
		}
		if(!refreshed){
			throw new LockReleaseFailedException("Key: " + key + " token: "	+ tokenString + " has expired.");
		}
	}

	@Override
	public synchronized void releaseLock(String key, String tokenString) {
		ValidateArgument.required(key, "key");
		ValidateArgument.required(tokenString, "token");
		// Get the tokens for this key
		List<Lock> locks = keyTokenMap.get(key);
		boolean released = false;
		if(locks != null){
			Iterator<Lock> it = locks.iterator();
			while(it.hasNext()){
				Lock token = it.next();
				if(token.getToken().equals(tokenString)){
					// found a match.
					it.remove();
					released = true;
					break;
				}
			}
		}
		if(!released){
			throw new LockReleaseFailedException("Key: " + key + " token: "	+ tokenString + " has expired.");
		}
	}

	@Override
	public synchronized void releaseAllLocks() {
		keyTokenMap.clear();
	}

}
