package org.sagebionetworks.repo.web.filter;

import static org.sagebionetworks.repo.web.filter.ThrottleUtils.generateCloudwatchProfiledata;
import static org.sagebionetworks.repo.web.filter.ThrottleUtils.isMigrationAdmin;
import static org.sagebionetworks.repo.web.filter.ThrottleUtils.setResponseError;
import static org.sagebionetworks.repo.web.filter.ThrottleUtils.THROTTLED_HTTP_STATUS;


import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.common.util.PathNormalizer;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.semaphore.MemoryTimeBlockCountingSemaphore;
import org.sagebionetworks.repo.model.throttle.ThrottleLimit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public class UserApiFrequencyThrottleFilter implements Filter{
	
	public static final String CLOUDWATCH_EVENT_NAME = "apiFrequencyLockUnavaliable";
	private static final String REASON_USER_THROTTLED_API_FORMAT = 
	"{\"reason\": \"Requests are too frequent for API call: %s. Allowed %d requests every %d seconds.\"}";
	
	@Autowired
	private Consumer consumer;
	
	@Autowired
	ThrottleRulesCache throttleRulesCache;
	
	@Autowired
	MemoryTimeBlockCountingSemaphore userApiThrottleMemoryTimeBlockSemaphore;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		//do nothing
	}


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		String userId = request.getParameter(AuthorizationConstants.USER_ID_PARAM);
		long userIdLong = Long.parseLong(userId);
		if (AuthorizationUtils.isUserAnonymous(userIdLong) || isMigrationAdmin(userIdLong) ) {
			//do not throttle anonymous users nor the admin responsible for migration.
			chain.doFilter(request, response);
		} else{
			HttpServletRequest httpServletRequest = (HttpServletRequest) request;
			String normalizedPath = PathNormalizer.normalizeMethodSignature(httpServletRequest.getRequestURI());
			ThrottleLimit limit = throttleRulesCache.getThrottleLimit(normalizedPath);
			if(limit == null){
				//no throttle exists for this URI
				chain.doFilter(request, response);
			}else{
				//this URI is throttled
				boolean lockAcquired = userApiThrottleMemoryTimeBlockSemaphore.attemptToAcquireLock(userId + ":" + normalizedPath, limit.getCallPeriodSec(), limit.getMaxCallsPerUserPerPeriod());
				if(lockAcquired){
					chain.doFilter(request, response);
				}else{
					//add extra dimensions for recording the throttled API
					Map<String, String> dimensions = new HashMap<String,String>();
					dimensions.put("UserId", userId);
					dimensions.put("ThrottledAPI", normalizedPath);
					
					ProfileData report = generateCloudwatchProfiledata( CLOUDWATCH_EVENT_NAME, this.getClass().getName(), Collections.unmodifiableMap(dimensions));
					
					consumer.addProfileData(report);
					setResponseError(response, THROTTLED_HTTP_STATUS, String.format(REASON_USER_THROTTLED_API_FORMAT, normalizedPath, limit.getMaxCallsPerUserPerPeriod(), limit.getCallPeriodSec()));
				}
			}
		}
		
	}
	
	@Override
	public void destroy() {
		//do nothing
	}

}
