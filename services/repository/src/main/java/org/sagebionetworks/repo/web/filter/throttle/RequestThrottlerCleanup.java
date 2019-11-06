package org.sagebionetworks.repo.web.filter.throttle;

/**
 * A class that is returned by Throttler.doThrottle() so that
 * cleanup for the Throttler occurs at the end of a HTTP request
 * after all business logic related to the HTTP request is completed,
 * not at the end of the Throttle.doThrottle() check
 */
public interface RequestThrottlerCleanup extends AutoCloseable{
	/**
	 * 	AutoCloseable has one method:
	 * 	public void close()
	 */
}
