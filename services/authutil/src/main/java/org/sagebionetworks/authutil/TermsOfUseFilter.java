package org.sagebionetworks.authutil;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.http.HttpStatus;

// TODO delete this class, no longer used
public class TermsOfUseFilter implements Filter {

	// maps userIds to a boolean value which says whether the user has signed the terms of use
	private static Map<String,Boolean> touCache = null; 
	private static Long cacheTimeout = null;
	private static Date lastCacheDump = null;

	private void initCache() {
		touCache = Collections.synchronizedMap(new HashMap<String,Boolean>());
		lastCacheDump = new Date();
		String s = System.getProperty(AuthorizationConstants.AUTH_CACHE_TIMEOUT_MILLIS);
		if (s!=null && s.length()>0) {
			cacheTimeout = Long.parseLong(s);
		} else {
			cacheTimeout = AuthorizationConstants.AUTH_CACHE_TIMEOUT_DEFAULT;
		}
	}
	
	private void checkCacheDump() {
		Date now = new Date();
		if (lastCacheDump.getTime()+cacheTimeout<now.getTime()) {
			touCache.clear();
			lastCacheDump = now;
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		initCache();
	}

	@Override
	public void destroy() {
		// nothing to do
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
			FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest)servletRequest;
		HttpServletResponse rsp = (HttpServletResponse)servletResponse;
		// get the user id
		String userId = req.getParameter(AuthorizationConstants.USER_ID_PARAM);
		if (userId==null) {
			rsp.setStatus(HttpStatus.UNAUTHORIZED.value());
	 		rsp.getWriter().println("{\"reason\":\"User name, or 'anonymous', is required.\"}");
			return;
		}
		// if user id is ANONYMOUS allow request to continue
		// otherwise check that the user has signed the terms of use
		if (!AuthorizationConstants.ANONYMOUS_USER_ID.equals(userId)) {
			// check local cache for user attributes
			Boolean passes = null; // is admin or agrees to terms of use
			if (cacheTimeout>0) { // then use cache
				checkCacheDump();
				passes = touCache.get(userId);
			}
			if (passes==null) { // not using cache or not found in cache
				// look up whether user agrees to ToU
				passes = false;
				try {
					Map<String, Collection<String>> attrs = CrowdAuthUtil.getUserAttributes(userId);
					passes  = acceptsTermsOfUse(attrs)  || CrowdAuthUtil.isAdmin(userId);
				} catch (NotFoundException e) {
					passes = false;
				}
				if (cacheTimeout>0) {
					touCache.put(userId, passes);
				}
			}
			// if not, throw authentication exception
			if (!passes) {
				reject(req, rsp);
				return;
			}
		}
		// if so, continue
		filterChain.doFilter(servletRequest, servletResponse);
	}
	
	public static boolean acceptsTermsOfUse(Map<String, Collection<String>> attrs) {
		Collection<String> values = attrs.get(AuthorizationConstants.ACCEPTS_TERMS_OF_USE_ATTRIBUTE);
		if (values==null || values.size()==0) return false;
		return Boolean.valueOf(values.iterator().next());
	}
	
	private static final String authEndpoint = StackConfiguration.getAuthenticationServicePublicEndpoint();
	
	public static String termsOfUseURL(HttpServletRequest req) {
	       return authEndpoint+AuthorizationConstants.TERMS_OF_USE_URI;
	}

	public static void reject(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setStatus(HttpStatus.FORBIDDEN.value());
 		resp.getWriter().println("{\"reason\":\"You must agree to Terms of Use.\", \"url\":\""+termsOfUseURL(req)+"\"}");
	}
	
}
