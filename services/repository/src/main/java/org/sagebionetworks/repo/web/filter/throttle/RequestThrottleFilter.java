package org.sagebionetworks.repo.web.filter.throttle;

import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.THROTTLED_HTTP_STATUS;
import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.isMigrationAdmin;

import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.repo.web.HttpRequestIdentifier;
import org.sagebionetworks.repo.web.HttpRequestIdentifierUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class RequestThrottleFilter implements Filter {

	@Autowired
	private Consumer consumer;

	@Autowired
	RequestThrottler requestThrottler;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpRequestIdentifier httpRequestIdentifier = HttpRequestIdentifierUtils.getRequestIdentifier(request);

		if ( isMigrationAdmin(httpRequestIdentifier.getUserId()) ) { //do not throttle the admin responsible for migration.
			//proceed to next filter and exit early
			chain.doFilter(request, response);
			return;
		}

		try (RequestThrottlerCleanup requestThrottlerCleanup = requestThrottler.doThrottle(httpRequestIdentifier)){
			chain.doFilter(request, response); //proceed to next filter
		} catch(RequestThrottledException e){
			//The request needs to be throttled. log throttling in CloudWatch and return HTTP response
			consumer.addProfileData(e.getProfileData());
			ThrottleUtils.setResponseError(response, THROTTLED_HTTP_STATUS, e.getMessage());
		} catch(Exception e){
			throw new ServletException(e);
		}
	}

	@Override
	public void init(FilterConfig filterConfig) {
		//do nothing
	}

	@Override
	public void destroy() {
		//do nothing
	}
}
