package org.sagebionetworks.repo.web.filter;

import static org.sagebionetworks.repo.web.filter.ThrottleUtils.generateCloudwatchProfiledata;
import static org.sagebionetworks.repo.web.filter.ThrottleUtils.isMigrationAdmin;
import static org.sagebionetworks.repo.web.filter.ThrottleUtils.setResponseError;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.core.appender.rolling.action.IfAccumulatedFileCount;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.semaphore.MemoryTimeBlockCountingSemaphore;
import org.sagebionetworks.repo.model.throttle.ThrottleLimit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

public class UserApiFrequencyThrottleFilter implements Filter{
	
	private static final String CLOUDWATCH_EVENT_NAME = "apiFrequencyLockUnavaliable";
	private static final String REASON_USER_THROTTLED_API_FORMAT = 
	"{\"reason\": \"Requests are too frequent for API call: %s. Allowed %l requests every %l seconds.\"}";
	
	@Autowired
	private Consumer consumer;
	
	@Autowired
	ThrottleRulesCache rulesCache;
	
	//TODO: use same semaphore or different one?
	@Autowired
	MemoryTimeBlockCountingSemaphore userThrottleMemoryTimeBlockSemaphore;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		//do nothing
	}


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		// TODO Auto-generated method stub
		String userId = request.getParameter(AuthorizationConstants.USER_ID_PARAM);
		long userIdLong = Long.parseLong(userId);
		if (AuthorizationUtils.isUserAnonymous(userIdLong) || isMigrationAdmin(userIdLong) ) {
			//do not throttle anonymous users nor the admin responsible for migration.
			chain.doFilter(request, response);
		} else{
			HttpServletRequest httpServletRequest = (HttpServletRequest) request;
			String normalizedURI = getNormalizedURI(httpServletRequest.getRequestURI(), httpServletRequest.getMethod());
			ThrottleLimit limit = rulesCache.getThrottleLimit(normalizedURI);
			if(limit == null){
				//no throttle exists for this URI
				chain.doFilter(request, response);
			}else{
				//this URI is throttled
				boolean lockAcquired = userThrottleMemoryTimeBlockSemaphore.attemptToAcquireLock(userId + normalizedURI, limit.getCallPeriodSec(), limit.getMaxCalls());
				if(lockAcquired){
					chain.doFilter(request, response);
				}else{
					ProfileData report = generateCloudwatchProfiledata(userId, CLOUDWATCH_EVENT_NAME, this.getClass().getName());
					consumer.addProfileData(report);
					setResponseError(response, HttpStatus.SERVICE_UNAVAILABLE.value(), String.format(REASON_USER_THROTTLED_API_FORMAT, normalizedURI, limit.getMaxCalls(), limit.getCallPeriodSec()));
				}
			}
		}
		
	}
	
	private String getNormalizedURI(String requestURI, String method){
		//TODO: need this from AccessRecordUtils
		return null;
	}
	
	@Override
	public void destroy() {
		//do nothing
	}

}
