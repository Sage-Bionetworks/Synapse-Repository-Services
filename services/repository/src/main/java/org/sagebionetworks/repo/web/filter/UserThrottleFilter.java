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
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.dao.semaphore.CountingSemaphoreDao;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is an filter that throttles non-anonymous user requests. It does this by limiting the number of concurrent
 * requests for one user to a fixed number and will throw an unavailable exception when exceeded.
 * 
 */
public class UserThrottleFilter implements Filter {

	private CountingSemaphoreDao userThrottleGate;

	@Autowired
	private Consumer consumer;

	public void setUserThrottleGate(CountingSemaphoreDao userThrottleGate) {
		this.userThrottleGate = userThrottleGate;
	}

	@Override
	public void destroy() {
	}

	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException,
			ServletException {

		String userId = request.getParameter(AuthorizationConstants.USER_ID_PARAM);
		if (AuthorizationUtils.isUserAnonymous(Long.parseLong(userId))) {
			chain.doFilter(request, response);
		} else {
			try {
				String lockToken = userThrottleGate.attemptToAcquireLock(userId);
				if (lockToken != null) {
					try {
						chain.doFilter(request, response);
					} finally {
						userThrottleGate.releaseLock(lockToken, userId);
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
					httpResponse.getWriter().println("{\"reason\": \"Too many concurrent requests\"}");
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
