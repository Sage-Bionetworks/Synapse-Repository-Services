package org.sagebionetworks.workers.util.semaphore;

/**
 * An abstraction for a closeable lock. The lock should be
 * used in a try-with-resources block to ensure any acquired locks are
 * unconditionally released.
 *
 */
public interface Lock extends AutoCloseable {

}
