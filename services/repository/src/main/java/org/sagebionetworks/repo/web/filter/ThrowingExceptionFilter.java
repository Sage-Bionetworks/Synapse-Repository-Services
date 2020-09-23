package org.sagebionetworks.repo.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A filter that throws a given exception, used for testing
 * 
 * @author Marco Marasca
 *
 */
@Component("throwingExceptionFilter")
public class ThrowingExceptionFilter extends OncePerRequestFilter {
	
	private static final String DEFAULT_EXCEPTION_NAME = IllegalStateException.class.getName();
	private static final String DEFAULT_EXCEPTION_MSG = DEFAULT_EXCEPTION_NAME;
	private static final String PARAM_EXCEPTION = "ex";
	private static final String PARAM_EXCEPTION_MSG = "exMsg";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		// This goes through an authentication filter, the user must be present
		String userId = request.getParameter(AuthorizationConstants.USER_ID_PARAM);
		
		if (!BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString().equals(userId)) {
			HttpAuthUtil.reject(response, "Unauthorized.");
			return;
		}
		
		String className = request.getParameter(PARAM_EXCEPTION);
		
		if (className == null) {
			className = DEFAULT_EXCEPTION_NAME;
		}
		
		Class<?> clazz;
		
		try {
			clazz = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new ServletException(e);
		}

		if (!RuntimeException.class.isAssignableFrom(clazz)) {
			throw new ServletException(clazz.getName() + " is not a RuntimeException.");
		}

		String exMessage = request.getParameter(PARAM_EXCEPTION_MSG);
		
		if (exMessage == null) {
			exMessage = DEFAULT_EXCEPTION_MSG;
		}
		
		RuntimeException throwable;
		
		try {

			throwable = (RuntimeException) clazz.getConstructor(String.class).newInstance(exMessage);
			
		} catch (Exception e) {
			throw new ServletException(e);
		}
				
		throw throwable;
	}

}
