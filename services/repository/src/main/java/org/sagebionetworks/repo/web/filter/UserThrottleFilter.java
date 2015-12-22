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
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is an filter that throttles non-anonymous user requests. It does this by limiting the number of concurrent
 * requests for one user to a fixed number and will throw an unavailable exception when exceeded.
 * 
 */
public class UserThrottleFilter implements Filter {
	
	public static final long LOCK_TIMOUTE_SEC = 60*10; // 10 MINS
	// The maximum number of concurrent locks a user can have per machine.
	public static final int MAX_CONCURRENT_LOCKS = 3;

	private static Logger log = LogManager.getLogger(UserThrottleFilter.class);

	@Autowired
	private Consumer consumer;
	
	@Autowired
	MemoryCountingSemaphore userThrottleGate;

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
				String lockToken = userThrottleGate.attemptToAcquireLock(userId, LOCK_TIMOUTE_SEC, MAX_CONCURRENT_LOCKS);
				if (lockToken != null) {
					try {
						chain.doFilter(request, response);
					} finally {
						try {
							userThrottleGate.releaseLock(userId, lockToken);
						} catch (LockReleaseFailedException e) {
							// This happens when test force the release of all locks.
							log.info(e.getMessage());
						}
					}
				} else {
					ProfileData lockUnavailableEvent = new ProfileData();
					lockUnavailableEvent.setNamespace(this.getClass().getName());
					lockUnavailableEvent.setName("LockUnavailable");
					lockUnavailableEvent.setValue(1.0);
					lockUnavailableEvent.setUnit("Count");
					lockUnavailableEvent.setTimestamp(new Date());
					lockUnavailableEvent.setDimension(Collections.singletonMap("UserId", userId));
					consumer.addProfileData(lockUnavailableEvent);
					HttpServletResponse httpResponse = (HttpServletResponse) response;
					httpResponse.setStatus(HttpStatus.SC_SERVICE_UNAVAILABLE);
					httpResponse.getWriter().println(AuthorizationConstants.REASON_TOO_MANY_CONCURRENT_REQUESTS);
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

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

}
