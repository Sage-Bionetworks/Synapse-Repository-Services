package org.sagebionetworks.repo.model.semaphore;

/**
 * semaphore used by SimpleMemoryCountingSemaphore 
 * 
 * @author zdong
 *
 */
public class SimpleSemaphore {
	private int count; //number of semaphores held
	private long expirationTimeMilis; //expiration time in milliseconds
	
	public void increment(){
		count++;
	}
	
	public void setExpiration(long time){
		this.expirationTimeMilis = time;
	}
	
	public void resetCount(){
		this.count = 0;
	}
	
	public int getCount(){
		return count;
	}
	
	public boolean isExpired(){
		return  System.currentTimeMillis() > expirationTimeMilis;
	}
}
