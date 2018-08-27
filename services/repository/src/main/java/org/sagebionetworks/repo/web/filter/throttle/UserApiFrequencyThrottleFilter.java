package org.sagebionetworks.repo.web.filter.throttle;

import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.generateCloudwatchProfiledata;
import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.setResponseError;
import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.THROTTLED_HTTP_STATUS;


import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.common.util.PathNormalizer;
import org.sagebionetworks.repo.model.semaphore.MemoryTimeBlockCountingSemaphore;
import org.sagebionetworks.repo.model.throttle.ThrottleLimit;
import org.springframework.beans.factory.annotation.Autowired;

public class UserApiFrequencyThrottleFilter extends AbstractRequestThrottleFilter {
	
	public static final String CLOUDWATCH_EVENT_NAME = "apiFrequencyLockUnavaliable";
	private static final String REASON_USER_THROTTLED_API_FORMAT = 
	"{\"reason\": \"Requests are too frequent for API call: %s. Allowed %d requests every %d seconds.\"}";
	
	@Autowired
	ThrottleRulesCache throttleRulesCache;
	
	@Autowired
	MemoryTimeBlockCountingSemaphore userApiThrottleMemoryTimeBlockSemaphore;

	@Override
	protected void throttle(ServletRequest request, String userId) throws RequestThrottledException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;
		String normalizedPath = PathNormalizer.normalizeMethodSignature(httpServletRequest.getRequestURI());
		ThrottleLimit limit = throttleRulesCache.getThrottleLimit(normalizedPath);
		if(limit == null){
			//no throttle exists for this URI
		}else{
			//this URI is throttled
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
		}
	}


}
