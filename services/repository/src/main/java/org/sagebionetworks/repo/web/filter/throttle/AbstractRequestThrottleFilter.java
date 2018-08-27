package org.sagebionetworks.repo.web.filter.throttle;

import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.THROTTLED_HTTP_STATUS;
import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.isMigrationAdmin;

import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

//TODO: test
//TODO: make this not abstract and
//TODO: use Composition where it contains an interface throttler class. Make that interface also implement AutoCloseable so that locks can be released after response is sent(if necessary)
public abstract class AbstractRequestThrottleFilter implements Filter {

	@Autowired
	private Consumer consumer;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		String userId = request.getParameter(AuthorizationConstants.USER_ID_PARAM);
		long userIdLong = Long.parseLong(userId);
		try {
			if (!isMigrationAdmin(userIdLong) && !AuthorizationUtils.isUserAnonymous(userIdLong) ) {   //do not throttle the admin responsible for migration.
				throttle(request, userId);
			}
			chain.doFilter(request, response);
		} catch (RequestThrottledException e){
			consumer.addProfileData(e.getProfileData());
			ThrottleUtils.setResponseError(response,THROTTLED_HTTP_STATUS,e.getMessage());
		} catch (Exception e){
			throw new ServletException(e);
		}

		//proceed to next filter
	}

	protected abstract void throttle(ServletRequest request, String userId) throws RequestThrottledException;

	@Override
	public void init(FilterConfig filterConfig) {
		//do nothing
	}

	@Override
	public void destroy() {
		//do nothing
	}
}
