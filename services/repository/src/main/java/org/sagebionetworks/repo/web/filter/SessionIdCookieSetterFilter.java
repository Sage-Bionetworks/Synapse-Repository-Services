package org.sagebionetworks.repo.web.filter;

import org.sagebionetworks.repo.web.HttpRequestIdentifierUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

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
			sessionIdCookie.setHttpOnly(true);
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
