package org.sagebionetworks.auth;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;

public class CloudMailInAuthFilter implements Filter {

	private String cloudMailInUser;
	private String cloudMailInPassword;

	public CloudMailInAuthFilter() {
		StackConfiguration config = StackConfigurationSingleton.singleton();
		cloudMailInUser = config.getCloudMailInUser();
		cloudMailInPassword = config.getCloudMailInPassword();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest)request;

		UserNameAndPassword up = BasicAuthUtils.getBasicAuthenticationCredentials(httpRequest);

		if (up!=null && 
				cloudMailInUser.equals(up.getUserName()) && 
				cloudMailInPassword.equals(up.getPassword())) {
			// We are now authenticated!!
			chain.doFilter(httpRequest, response);
			return;

		}

		HttpServletResponse httpResponse = (HttpServletResponse)response;
		httpResponse.setStatus(HttpStatus.SC_UNAUTHORIZED);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Nothing to do
	}

	@Override
	public void destroy() {
		// Nothing to do
	}

}
