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

import org.apache.http.HttpStatus;
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

/**
 * This is an filter that throttles non-anonymous user requests. It does this by limiting the number of concurrent
 * requests for one user to a fixed number and will throw an unavailable exception when exceeded.
 * 
 */
public class UserThrottleFilter implements Filter {
	
	public static final long CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC = 60*10; // 10 MINS
	// The maximum number of concurrent locks a user can have per machine.
	public static final int MAX_CONCURRENT_LOCKS = 3;
	
	//limit users to on average send 1 request per 1 second
	public static final long REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC =  150; //150 seconds
	
	//If MAX_REQUEST_FREQUENCY_LOCKS < 150 this filter will throttle the Integration Tests and the build will fail
	//not sure of the exact value (150 was from trial and error) but anything < 100 definitely does now work
	public static final int MAX_REQUEST_FREQUENCY_LOCKS = 150;
	

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
		if (AuthorizationUtils.isUserAnonymous(userIdLong)) {
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
						reportLockAcquireError(userId, response, "RequestFrequencyLockUnavailable", AuthorizationConstants.REASON_USER_THROTTLED);
					}
				}else{
					reportLockAcquireError(userId, response, "ConcurrentConnectionsLockUnavailable", AuthorizationConstants.REASON_USER_THROTTLED);
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
		httpResponse.setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
		httpResponse.getWriter().println(reason);
	}
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

}
