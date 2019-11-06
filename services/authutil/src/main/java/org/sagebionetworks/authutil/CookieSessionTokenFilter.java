package org.sagebionetworks.authutil;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.AuthorizationConstants;

/**
 * This is a simple filter that looks for the session token cookie and 
 * if it exists it adds the session token to the header.
 * 
 * @author John
 *
 */
public class CookieSessionTokenFilter implements Filter {
	
	static private Logger log = LogManager.getLogger(CookieSessionTokenFilter.class);

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res,	FilterChain chain) throws IOException, ServletException {
		// Look for the cookie
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		Cookie[] cookies = request.getCookies();
		String sessionToken = null;
		if(cookies != null){
			log.debug("Found: "+cookies.length+" cookies");
			for(Cookie cookie: cookies){
				if(AuthorizationConstants.SESSION_TOKEN_COOKIE_NAME.equals(cookie.getName())){
					sessionToken = cookie.getValue();
					log.debug("Found a cookie with the session token: "+sessionToken);
				}
			}
		}
		// add it to the header.
		if(sessionToken != null){
			// override the request
			HeaderRequestWraper wapper = new HeaderRequestWraper(request);
			wapper.addHeader(AuthorizationConstants.SESSION_TOKEN_PARAM, sessionToken);
			wapper.addHeader("Accept", "application/json");
			request = wapper;
		}
		// pass it along.
		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}

}
