package org.sagebionetworks.workers.util.semaphore;

/**
 * Represents an acquired read lock. 
 * <p>
 * Note: This lock must be acquired using try-with-resources to ensure that the
 * lock is unconditionally released.
 */
public interface ReadLock extends Lock {

}
