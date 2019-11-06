package org.sagebionetworks.repo.web.filter.throttle;

import org.sagebionetworks.repo.web.HttpRequestIdentifier;

/**
 * Used by the {@link RequestThrottleFilter} to throttle HTTP requests
 */
public interface RequestThrottler {

	/**
	 * Determines whether the request should be throttled.
	 * @param httpRequestIdentifier Identifies the request
	 * @return object used to clean up the RequestThrottler after the http request has completed.
	 * Returning a {@link RequestThrottlerCleanup} object implies that the request was not throttled.
	 * @throws RequestThrottledException if the request should be throttled.
	 */
	public RequestThrottlerCleanup doThrottle(HttpRequestIdentifier httpRequestIdentifier) throws RequestThrottledException;
}
