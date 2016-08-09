package org.sagebionetworks.repo.web.filter;

import static org.sagebionetworks.repo.web.filter.UserThrottleFilter.CONCURRENT_CONNECTION_KEY_PREFIX;
import static org.sagebionetworks.repo.web.filter.UserThrottleFilter.REQUEST_FREQUENCY_KEY_PREFIX;

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
	public static final String CONCURRENT_CONNECTION_KEY_PREFIX = "ConcurrentConnectionsKey-";
	
	//limit users to on average send 1 request per 1 second
	public static final long REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC =  150; //150 seconds
	public static final int MAX_REQUEST_FREQUENCY_LOCKS = 150;
	public static final String REQUEST_FREQUENCY_KEY_PREFIX = "RequestFrequencyKey-";
	

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
			try {
				String concurrentKeyUserId = CONCURRENT_CONNECTION_KEY_PREFIX + userId;
				String frequencyKeyUserId = REQUEST_FREQUENCY_KEY_PREFIX + userId;
				
				//try to acquire the concurrent connections lock first
				String concurrentLockToken = userThrottleMemoryCountingSemaphore.attemptToAcquireLock(concurrentKeyUserId, CONCURRENT_CONNECTIONS_LOCK_TIMEOUT_SEC, MAX_CONCURRENT_LOCKS);
				if (concurrentLockToken != null) {
					//then try to acquire the request frequency lock
					boolean frequencyLockAcquired = userThrottleMemoryTimeBlockSemaphore.attemptToAcquireLock(frequencyKeyUserId, REQUEST_FREQUENCY_LOCK_TIMEOUT_SEC, MAX_REQUEST_FREQUENCY_LOCKS);
					if(frequencyLockAcquired){
						//acquired both locks. proceed to next filter
						try {
							chain.doFilter(request, response);
						} finally {
							try {
								//clean up by releasing locks
								userThrottleMemoryCountingSemaphore.releaseLock(concurrentKeyUserId, concurrentLockToken);
								//do not release frequency lock, allow it to timeout to enforce frequency limit.
							} catch (LockReleaseFailedException e) {
								// This happens when test force the release of all locks.
								log.info(e.getMessage());
							}
						}
					}else{
						//could not acquire request frequency lock
						try {
							//release the previously acquired concurrent connection lock
							userThrottleMemoryCountingSemaphore.releaseLock(concurrentKeyUserId, concurrentLockToken);
						} catch (LockReleaseFailedException e) {
							// This happens when test force the release of all locks.
							log.info(e.getMessage());
						}
						reportLockAcquireError(userId, response, "RequestFrequencyLockUnavailable", AuthorizationConstants.REASON_REQUESTS_TOO_FREQUENT);
					}
				} else{
					//could not acquire concurrent connections lock
					reportLockAcquireError(userId, response, "ConcurrentConnectionsLockUnavailable", AuthorizationConstants.REASON_TOO_MANY_CONCURRENT_REQUESTS);
				}
			} catch (IOException e) {
				throw e;
			} catch (ServletException e) {
				throw e;
			} catch (Exception e) {
				throw new ServletException(e.getMessage(), e);
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
