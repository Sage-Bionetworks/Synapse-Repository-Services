package org.sagebionetworks.repo.web.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.semaphore.LockReleaseFailedException;
import org.sagebionetworks.repo.model.semaphore.MemoryCountingSemaphore;
import org.sagebionetworks.repo.model.semaphore.MemoryTimeBlockCountingSemaphore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * This is an filter that throttles non-anonymous user requests. It does this by limiting the number of concurrent
 * requests for one user to a fixed number and will throw an unavailable exception when exceeded.
 * 
 */
public class UserThrottleFilter implements Filter {
	
	public static final long CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC = 60*10; // 10 MINS
	// The maximum number of concurrent locks a user can have per machine.
	public static final int MAX_CONCURRENT_LOCKS = 3;
	
	//From usage data in redash, normal users would not be affected with an average send 1 request per 1 second
	//Set to 600 requests / 60 seconds so that the filter could tolerate infrequent high bursts of request from users
	public static final long REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC =  60; //60 seconds
	public static final int MAX_REQUEST_FREQUENCY_LOCKS = 600;
	
	public static final String REASON_USER_THROTTLED_CONCURRENT = 
			"{\"reason\": \"Too many concurrent requests. Allowed "+ MAX_CONCURRENT_LOCKS +" concurrent connections at any time.\"}";
	public static final String REASON_USER_THROTTLED_FREQUENT = 
			"{\"reason\": \"Requests are too frequent. Allowed "+MAX_REQUEST_FREQUENCY_LOCKS+" requests every "+REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC+" seconds.\"}";

	private static Logger log = LogManager.getLogger(UserThrottleFilter.class);

	@Autowired
	private Consumer consumer;
	
	@Autowired
	MemoryCountingSemaphore userThrottleMemoryCountingSemaphore;
	
	@Autowired
	MemoryTimeBlockCountingSemaphore userThrottleMemoryTimeBlockSemaphore;

	@Override
	public void destroy() {
	}

	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException,
			ServletException {

		String userId = request.getParameter(AuthorizationConstants.USER_ID_PARAM);
		long userIdLong = Long.parseLong(userId);
		if (AuthorizationUtils.isUserAnonymous(userIdLong) || isMigrationAdmin(userIdLong) ) {
			//do not throttle anonymous users nor the admin responsible for migration.
			chain.doFilter(request, response);
		} else {
			String concurrentLockToken = null;
			try {
				
				//first try to acquire the concurrent connections lock
				concurrentLockToken = userThrottleMemoryCountingSemaphore.attemptToAcquireLock(userId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS);
				
				//then try to acquire the request frequency lock
				if(concurrentLockToken != null){
					boolean frequencyLockAcquired = userThrottleMemoryTimeBlockSemaphore.attemptToAcquireLock(userId, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS);
					if(frequencyLockAcquired){
						//acquired both locks. proceed to next filter
						chain.doFilter(request, response);
					}else{
						reportLockAcquireError(userId, response, "RequestFrequencyLockUnavailable", REASON_USER_THROTTLED_FREQUENT);
					}
				}else{
					reportLockAcquireError(userId, response, "ConcurrentConnectionsLockUnavailable", REASON_USER_THROTTLED_CONCURRENT);
				}
				
			
			} catch (IOException e) {
				throw e;
			} catch (ServletException e) {
				throw e;
			} catch (Exception e) {
				throw new ServletException(e.getMessage(), e);
			}finally {
				//clean up by releasing concurrent lock regardless if frequency lock was acquired
				//do not release frequency lock(if acquired), allow it to timeout to enforce frequency limit.
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
	
	/**
	 * reports to cloudwatch that a lock could not be acquired and sets
	 * @param userId id of user
	 * @param response 
	 * @param eventName name of event to be reported to cloudwatch
	 * @param reason reason for user to see
	 * @throws IOException 
	 */
	private void reportLockAcquireError(String userId, ServletResponse response, String eventName, String reason) throws IOException{
		
		ProfileData lockUnavailableEvent = new ProfileData();
		lockUnavailableEvent.setNamespace(this.getClass().getName());
		lockUnavailableEvent.setName(eventName);
		lockUnavailableEvent.setValue(1.0);
		lockUnavailableEvent.setUnit("Count");
		lockUnavailableEvent.setTimestamp(new Date());
		lockUnavailableEvent.setDimension(Collections.singletonMap("UserId", userId));
		consumer.addProfileData(lockUnavailableEvent);
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		//TODO: Switch to 429 http code once clients have been implemented to expect that code
		httpResponse.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
		httpResponse.getWriter().println(reason);
	}
	
	//returns whether the userId is that of the admin used for migration
	private boolean isMigrationAdmin(long userId){
		return userId == AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	}
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

}
