package org.sagebionetworks.repo.model.semaphore;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.common.util.Clock;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.database.semaphore.LockReleaseFailedException;
import org.sagebionetworks.util.ValidateArgument;

/**
 * An in-memory implementation of a counting semaphore. This class is designed
 * to be a thread-safe singleton.
 * 
 * 
 */
public class MemoryCountingSemaphore implements CountingSemaphore {
	
	/*
	 * Note: This is not a synchronized map. All access to this map must occur in a synchronized method
	 * making this singleton the lock (not the map).
	 */
	private Map<String, List<Token>> keyTokenMap = new HashMap<String, List<Token>>();
	// abstraction from the system clock.
	private Clock clock;
	
	/**
	 * Create a new semaphore given a clock.
	 * @param clock
	 */
	public MemoryCountingSemaphore(Clock clock) {
		super();
		this.clock = clock;
	}

	@Override
	public synchronized String attemptToAcquireLock(String key, long timeoutSec,
			int maxLockCount) {
		ValidateArgument.required(key, "key");
		// Is there an entry in the map for this key?
		List<Token> tokens = keyTokenMap.get(key);
		if(tokens == null){
			tokens = new LinkedList<Token>();
			keyTokenMap.put(key, tokens);
		}
		// remove expired locks
		long now = clock.currentTimeMillis();
		Iterator<Token> it = tokens.iterator();
		while(it.hasNext()){
			Token token = it.next();
			if(now > token.getExpiresTimeMs()){
				it.remove();
			}
		}
		// are we out of locks for this key?
		if(tokens.size() < maxLockCount){
			// a new lock can be issued
			Token token = new Token();
			token.setExpiresTimeMs(now+(timeoutSec*1000));
			token.setToken(UUID.randomUUID().toString());
			tokens.add(token);
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
		List<Token> tokens = keyTokenMap.get(key);
		boolean refreshed = false;
		if(tokens != null){
			Iterator<Token> it = tokens.iterator();
			while(it.hasNext()){
				Token token = it.next();
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
		List<Token> tokens = keyTokenMap.get(key);
		boolean released = false;
		if(tokens != null){
			Iterator<Token> it = tokens.iterator();
			while(it.hasNext()){
				Token token = it.next();
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
