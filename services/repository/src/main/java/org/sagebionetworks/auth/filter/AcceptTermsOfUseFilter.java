package org.sagebionetworks.auth.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.service.auth.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**

 * This filter checks that the authenticated Syanpse user has agreed to the Synapse Terms Of Use.
 * Anonymous users are simply let through.
 */
@Component("acceptTermsOfUseFilter")
public class AcceptTermsOfUseFilter implements Filter {
	private static final String TOU_UNSIGNED_REASON = "Login to https://synapse.org to accept the latest Terms of Service.";
	
	@Autowired
	private AuthenticationService authenticationService;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {

		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			throw new ServletException("Only HTTP requests are supported");
		}

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		
		String userIdParam = httpRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		
		if (userIdParam==null) {
			// There must at least be an anonymous user id.  If not, we have misconfigured the filter
			HttpAuthUtil.rejectWithErrorResponse(httpResponse, "Missing user id.", HttpStatus.INTERNAL_SERVER_ERROR);
			return;
		}
		
		Long userId = Long.parseLong(userIdParam);
		
		// If the user is not anonymous, check if they have accepted the terms of use
		boolean isAnonymous = HttpAuthUtil.isAnonymous(httpRequest);
		if (!isAnonymous) {
			if (!authenticationService.hasUserAcceptedTermsOfService(userId)) {
				HttpAuthUtil.rejectWithErrorResponse(httpResponse, TOU_UNSIGNED_REASON, HttpStatus.FORBIDDEN);
				return;
			}
		}

		filterChain.doFilter(httpRequest, httpResponse);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// nothing to do
	}

	@Override
	public void destroy() {
		// nothing to do
	}

}
