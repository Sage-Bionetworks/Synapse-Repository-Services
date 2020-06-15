package org.sagebionetworks.auth.filter;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.UserNameAndPassword;
import org.sagebionetworks.cloudwatch.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("cloudMailInAuthFilter")
public class CloudMailInAuthFilter extends BasicAuthenticationFilter {

	private String cloudMailInUser;
	private String cloudMailInPassword;

	@Autowired
	public CloudMailInAuthFilter(StackConfiguration config, Consumer consumer) {
		super(new FilterHelper(config, consumer));
		cloudMailInUser = config.getCloudMailInUser();
		cloudMailInPassword = config.getCloudMailInPassword();
	}
	
	@Override
	protected boolean credentialsRequired() {
		return true;
	}
	
	@Override
	protected boolean reportBadCredentialsMetric() {
		return true;
	}
	
	@Override
	protected void validateCredentialsAndDoFilterInternal(
			HttpServletRequest httpRequest, HttpServletResponse httpResponse, 
			FilterChain filterChain, Optional<UserNameAndPassword> credentials) throws IOException, ServletException {
		if (credentials.isPresent() && !validCredentials(credentials.get())) {
			filterHelper.rejectRequest(reportBadCredentialsMetric(), httpResponse, getInvalidCredentialsMessage());
			return;
		}
		filterChain.doFilter(httpRequest, httpResponse);
	}
	
	protected FilterHelper filterHelper() {return filterHelper;}

	private boolean validCredentials(UserNameAndPassword credentials) {
		return cloudMailInUser.equals(credentials.getUserName()) && cloudMailInPassword.equals(credentials.getPassword());
	}

}
