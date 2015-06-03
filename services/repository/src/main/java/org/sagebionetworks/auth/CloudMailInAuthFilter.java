package org.sagebionetworks.auth;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.sagebionetworks.StackConfiguration;

import com.sun.jersey.core.util.Base64;

public class CloudMailInAuthFilter implements Filter {
	
	private static final String BASIC_PREFIX = "Basic ";
	
	private static final Map<String,String> ACCEPT_PLAIN_TEXT_HEADER;
	
	static {
		ACCEPT_PLAIN_TEXT_HEADER = new HashMap<String,String>();
		ACCEPT_PLAIN_TEXT_HEADER.put("Accept", ContentType.TEXT_PLAIN.getMimeType());
	}
	
	private String cloudMailInUser;
	private String cloudMailInPassword;
	
	public CloudMailInAuthFilter() {
		cloudMailInUser = StackConfiguration.getCloudMailInUser();
		cloudMailInPassword = StackConfiguration.getCloudMailInPassword();
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		
		String header = httpRequest.getHeader("Authorization");
		
		if (header != null && header.startsWith(BASIC_PREFIX)) {
			String base64EncodedCredentials = header.substring(BASIC_PREFIX.length());
			String basicCredentials = Base64.base64Decode(base64EncodedCredentials);
			int colon = basicCredentials.indexOf(":");
			if (colon>0 && colon<basicCredentials.length()-1) {
				String name = basicCredentials.substring(0, colon);
				String password = basicCredentials.substring(colon+1);
				if (cloudMailInUser.equals(name) && cloudMailInPassword.equals(password)) {
					// authenticated!!
					ExtraHeadersHttpServletRequest plainTextRequest = 
							new ExtraHeadersHttpServletRequest(httpRequest, ACCEPT_PLAIN_TEXT_HEADER);
					chain.doFilter(plainTextRequest, response);
					return;
				}
			}
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
