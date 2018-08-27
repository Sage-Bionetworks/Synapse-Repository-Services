package org.sagebionetworks.repo.web.filter.throttle;

import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.generateCloudwatchProfiledata;
import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.setResponseError;
import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.THROTTLED_HTTP_STATUS;


import java.io.IOException;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.semaphore.LockReleaseFailedException;
import org.sagebionetworks.repo.model.semaphore.MemoryCountingSemaphore;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is an filter that throttles non-anonymous user requests. It does this by limiting the number of concurrent
 * requests for one user to a fixed number and will throw an unavailable exception when exceeded.
 * 
 */
public class UserConcurrentConnectionThrottleFilter extends AbstractRequestThrottleFilter {
	
	public static final long CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC = 60*10; // 10 MINS
	// The maximum number of concurrent locks a user can have per machine.
	public static final int MAX_CONCURRENT_LOCKS = 3;
	
	public static final String REASON_USER_THROTTLED_CONCURRENT = 
			"{\"reason\": \"Too many concurrent requests. Allowed "+ MAX_CONCURRENT_LOCKS +" concurrent connections at any time.\"}";
	
	public static final String CLOUDWATCH_EVENT_NAME = "ConcurrentConnectionsLockUnavailable";
	
	private static Logger log = LogManager.getLogger(UserConcurrentConnectionThrottleFilter.class);
	
	@Autowired
	MemoryCountingSemaphore userThrottleMemoryCountingSemaphore;

	@Override
	protected void throttle(ServletRequest request, String userId) throws RequestThrottledException{
		String concurrentLockToken = null;
		try {
			concurrentLockToken = userThrottleMemoryCountingSemaphore.attemptToAcquireLock(userId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS);

			if(concurrentLockToken == null){
				ProfileData report = generateCloudwatchProfiledata( CLOUDWATCH_EVENT_NAME, this.getClass().getName(), Collections.singletonMap("UserId", userId));
				throw new RequestThrottledException(REASON_USER_THROTTLED_CONCURRENT, report);
			}
		}finally {
			//clean up by releasing concurrent lock regardless if frequency lock was acquired
			if(concurrentLockToken != null){
				try {
					userThrottleMemoryCountingSemaphore.releaseLock(userId, concurrentLockToken);
				} catch (LockReleaseFailedException e) {
					// This happens when test force the release of all locks.
					log.info(e.getMessage());
				}
			}
		}
	}


}
