package org.sagebionetworks.repo.web.filter.throttle;

import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.THROTTLED_HTTP_STATUS;
import static org.sagebionetworks.repo.web.filter.throttle.ThrottleUtils.isMigrationAdmin;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.web.HttpRequestIdentifier;
import org.sagebionetworks.repo.web.HttpRequestIdentifierUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class RequestThrottleFilter implements Filter {

	@Autowired
	private Consumer consumer;
	
	@Autowired
	private OpenIDConnectManager oidcManager;


	RequestThrottler requestThrottler;

	public RequestThrottleFilter(RequestThrottler requestThrottler){
		this.requestThrottler = requestThrottler;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		String synapseAuthorizationHeader = ((HttpServletRequest)request).getHeader(AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME);
		String accessToken= HttpAuthUtil.getBearerTokenFromAuthorizationHeader(synapseAuthorizationHeader);
		Long userId = Long.parseLong(oidcManager.getUserId(accessToken));
		HttpRequestIdentifier httpRequestIdentifier = HttpRequestIdentifierUtils.getRequestIdentifier(request, userId);

		//TODO: remove exception for anonymous users once java client has a way to get session id from cookies
		if ( isMigrationAdmin(httpRequestIdentifier.getUserId()) || AuthorizationUtils.isUserAnonymous(httpRequestIdentifier.getUserId())) { //do not throttle the admin responsible for migration.
			//proceed to next filter and exit early
			chain.doFilter(request, response);
			return;
		}

		//Have the
		try (RequestThrottlerCleanup requestThrottlerCleanup = requestThrottler.doThrottle(httpRequestIdentifier)){
			//No throttling is required so proceed to next filter.
			chain.doFilter(request, response);
		} catch(RequestThrottledException e){//This exception indicates request needs to be throttled
			//log throttling in CloudWatch and return HTTP response
			consumer.addProfileData(e.getProfileData());
			ThrottleUtils.setResponseError(response, THROTTLED_HTTP_STATUS, e.getMessage());
		} catch(Exception e){
			//Thrown when the RequestThrottlerCleanup.close() encounters an exception.
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
