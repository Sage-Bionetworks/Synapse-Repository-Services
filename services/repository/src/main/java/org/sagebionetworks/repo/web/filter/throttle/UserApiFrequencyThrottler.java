package org.sagebionetworks.repo.web.filter.throttle;

import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.generateCloudwatchProfiledata;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.common.util.PathNormalizer;
import org.sagebionetworks.repo.model.semaphore.MemoryTimeBlockCountingSemaphore;
import org.sagebionetworks.repo.model.throttle.ThrottleLimit;
import org.sagebionetworks.repo.web.HttpRequestIdentifier;
import org.springframework.beans.factory.annotation.Autowired;

public class UserApiFrequencyThrottler implements RequestThrottler {
	
	public static final String CLOUDWATCH_EVENT_NAME = "apiFrequencyLockUnavaliable";
	private static final String REASON_USER_THROTTLED_API_FORMAT = 
	"{\"reason\": \"Requests are too frequent for API call: %s. Allowed %d requests every %d seconds.\"}";
	
	@Autowired
	ThrottleRulesCache throttleRulesCache;
	
	@Autowired
	MemoryTimeBlockCountingSemaphore userApiThrottleMemoryTimeBlockSemaphore;


	RequestThrottlerCleanup noOpRequestThrottlerCleanup = new RequestThrottlerCleanupNoOpImpl(); //TODO: Autowire?

	@Override
	public RequestThrottlerCleanup doThrottle(HttpRequestIdentifier httpRequestIdentifier) throws RequestThrottledException {
		String userId = httpRequestIdentifier.getUserId().toString();
		String normalizedPath = PathNormalizer.normalizeMethodSignature(httpRequestIdentifier.getRequestPath());

		ThrottleLimit limit = throttleRulesCache.getThrottleLimit(normalizedPath);
		if(limit == null){
			//no throttle exists for this URI
			return noOpRequestThrottlerCleanup;
		}
		boolean lockAcquired = userApiThrottleMemoryTimeBlockSemaphore.attemptToAcquireLock(userId + ":" + normalizedPath, limit.getCallPeriodSec(), limit.getMaxCallsPerUserPerPeriod());
		if(!lockAcquired){
			//add extra dimensions for recording the throttled API
			Map<String, String> dimensions = new HashMap<>();
			dimensions.put("UserId", userId);
			dimensions.put("ThrottledAPI", normalizedPath);
			ProfileData report = generateCloudwatchProfiledata( CLOUDWATCH_EVENT_NAME, this.getClass().getName(), Collections.unmodifiableMap(dimensions));

			throw new RequestThrottledException(String.format(REASON_USER_THROTTLED_API_FORMAT, normalizedPath, limit.getMaxCallsPerUserPerPeriod(), limit.getCallPeriodSec()),
												report);
		}

		return noOpRequestThrottlerCleanup;
	}


}
