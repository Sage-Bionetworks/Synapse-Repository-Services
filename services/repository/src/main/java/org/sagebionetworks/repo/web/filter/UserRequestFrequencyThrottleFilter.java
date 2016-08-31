package org.sagebionetworks.repo.web.filter;

import static org.sagebionetworks.repo.web.filter.ThrottleUtils.isMigrationAdmin;
import static org.sagebionetworks.repo.web.filter.ThrottleUtils.generateCloudwatchProfiledata;
import static org.sagebionetworks.repo.web.filter.ThrottleUtils.setResponseError;
import static org.sagebionetworks.repo.web.filter.ThrottleUtils.THROTTLED_HTTP_STATUS;


import java.io.IOException;
import java.util.Collections;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.semaphore.MemoryTimeBlockCountingSemaphore;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is an filter that throttles non-anonymous user requests by limiting
 * the frequency of their requests.
 * It will throw an unavailable exception when exceeded.
 * 
 */
public class UserRequestFrequencyThrottleFilter implements Filter {
	//From usage data in redash, normal users would not be affected with an average send 1 request per 1 second
	//Set to 600 requests / 60 seconds so that the filter could tolerate infrequent high bursts of request from users
	public static final long REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC =  60; //60 seconds
	public static final int MAX_REQUEST_FREQUENCY_LOCKS = 600;
	
	public static final String REASON_USER_THROTTLED_FREQ = 
			"{\"reason\": \"Requests are too frequent. Allowed "+MAX_REQUEST_FREQUENCY_LOCKS+" requests every "+REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC+" seconds.\"}";
	
	public static String CLOUDWATCH_EVENT_NAME = "RequestFrequencyLockUnavailable";
	
	@Autowired
	private Consumer consumer;
	
	@Autowired
	MemoryTimeBlockCountingSemaphore userThrottleMemoryTimeBlockSemaphore;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
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
			boolean frequencyLockAcquired = userThrottleMemoryTimeBlockSemaphore.attemptToAcquireLock(userId, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS);
			if(frequencyLockAcquired){
				//acquired lock. proceed to next filter
				chain.doFilter(request, response);
			}else{
				ProfileData report = generateCloudwatchProfiledata(CLOUDWATCH_EVENT_NAME, this.getClass().getName(), Collections.singletonMap("UserId", userId));
				consumer.addProfileData(report);
				setResponseError(response, THROTTLED_HTTP_STATUS, REASON_USER_THROTTLED_FREQ);
			}
		}
	}

	@Override
	public void destroy() {
	}

}
