package org.sagebionetworks.repo.web.filter.throttle;

import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.generateCloudwatchProfiledata;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.semaphore.MemoryTimeBlockCountingSemaphore;
import org.sagebionetworks.repo.web.HttpRequestIdentifier;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is an filter that throttles non-anonymous user requests by limiting
 * the frequency of their requests.
 * It will throw an unavailable exception when exceeded.
 * 
 */
public class UserRequestFrequencyThrottler implements RequestThrottler {
	//From usage data in redash, normal users would not be affected with an average send 1 request per 1 second
	//Set to 600 requests / 60 seconds so that the filter could tolerate infrequent high bursts of request from users
	public static final long REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC =  60; //60 seconds
	public static final int MAX_REQUEST_FREQUENCY_LOCKS = 600;
	
	public static final String REASON_USER_THROTTLED_FREQ = 
			"{\"reason\": \"Requests are too frequent. Allowed "+MAX_REQUEST_FREQUENCY_LOCKS+" requests every "+REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC+" seconds.\"}";
	public static final String CLOUDWATCH_EVENT_NAME = "RequestFrequencyLockUnavailable";

	private static final RequestThrottlerCleanup NO_OP_THROTTLER_CLEANUP = new RequestThrottlerCleanupNoOpImpl();

	@Autowired
	MemoryTimeBlockCountingSemaphore userThrottleMemoryTimeBlockSemaphore;

	@Override
	public RequestThrottlerCleanup doThrottle(HttpRequestIdentifier httpRequestIdentifier) throws RequestThrottledException {
		boolean frequencyLockAcquired = userThrottleMemoryTimeBlockSemaphore.attemptToAcquireLock(httpRequestIdentifier.getUserMachineIdentifierString(), REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS);
		if(!frequencyLockAcquired){
			Map<String, String> dimensions = new HashMap<>();
			dimensions.put("UserId", String.valueOf(httpRequestIdentifier.getUserId()));
			dimensions.put("IpAddress", httpRequestIdentifier.getIpAddress());
			dimensions.put("sessionId", httpRequestIdentifier.getSessionId());
			ProfileData report = generateCloudwatchProfiledata(CLOUDWATCH_EVENT_NAME, this.getClass().getName(), dimensions);
			throw new RequestThrottledException(REASON_USER_THROTTLED_FREQ, report);
		}

		return NO_OP_THROTTLER_CLEANUP;
	}

}
