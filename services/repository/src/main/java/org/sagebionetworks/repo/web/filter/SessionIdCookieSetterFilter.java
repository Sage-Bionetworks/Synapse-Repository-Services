package org.sagebionetworks.repo.web.filter;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.web.HttpRequestIdentifierUtils;

/**
 * Provide the response with a session ID if one did not come in with the request.
 */
public class SessionIdCookieSetterFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		//sessionId is either the session ID that came in with the request or a newly generated sessionId
		String requestSessionId = HttpRequestIdentifierUtils.getSessionId((HttpServletRequest) request);
		if (requestSessionId == null){
			//add cookie to the response
			Cookie sessionIdCookie = new Cookie(HttpRequestIdentifierUtils.SESSION_ID_COOKIE_NAME, UUID.randomUUID().toString());
			((HttpServletResponse) response).addCookie(sessionIdCookie);
		}

		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		///do nothing
	}

	@Override
	public void destroy() {
		//do nothing
	}
}
