package org.sagebionetworks.repo.web.filter.throttle;

import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.generateCloudwatchProfiledata;


import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.semaphore.MemoryCountingSemaphore;
import org.sagebionetworks.repo.web.HttpRequestIdentifier;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is an filter that throttles non-anonymous user requests. It does this by limiting the number of concurrent
 * requests for one user to a fixed number and will throw an unavailable exception when exceeded.
 * 
 */
public class UserConcurrentConnectionThrottler implements RequestThrottler {
	
	public static final long CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC = 60*10; // 10 MINS
	// The maximum number of concurrent locks a user can have per machine.
	public static final int MAX_CONCURRENT_LOCKS = 3;
	
	public static final String REASON_USER_THROTTLED_CONCURRENT = 
			"{\"reason\": \"Too many concurrent requests. Allowed "+ MAX_CONCURRENT_LOCKS +" concurrent connections at any time.\"}";
	
	public static final String CLOUDWATCH_EVENT_NAME = "ConcurrentConnectionsLockUnavailable";
	
	private static Logger log = LogManager.getLogger(UserConcurrentConnectionThrottler.class);
	
	@Autowired
	MemoryCountingSemaphore userThrottleMemoryCountingSemaphore;

	@Override
	public RequestThrottlerCleanup doThrottle(HttpRequestIdentifier httpRequestIdentifier) throws RequestThrottledException{
		String userId = httpRequestIdentifier.getUserId().toString();
		final String concurrentLockToken = userThrottleMemoryCountingSemaphore.attemptToAcquireLock(userId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS);

		if(concurrentLockToken == null){
			ProfileData report = generateCloudwatchProfiledata( CLOUDWATCH_EVENT_NAME, this.getClass().getName(), Collections.singletonMap("UserId", userId));
			throw new RequestThrottledException(REASON_USER_THROTTLED_CONCURRENT, report);
		}

		return () -> userThrottleMemoryCountingSemaphore.releaseLock(userId, concurrentLockToken);
	}


}
