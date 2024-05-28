package org.sagebionetworks.workers.util.semaphore;

/**
 * An abstraction for a runner that is gated by acquiring a lock from a
 * semaphore. When a lock is acquired, the wrapped runner will be run. The lock
 * is unconditionally released if when the runner terminate for any reason.
 *
 */
public interface SemaphoreGatedRunner extends Runnable {

}
