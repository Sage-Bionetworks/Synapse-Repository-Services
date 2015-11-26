package org.sagebionetworks.repo.web.filter;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.ThreadContext;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.web.controller.AccessInterceptor;
/**
 * Gathers basic data from an http request.
 * 
 * @author John
 *
 */
public class HttpServletRequestData {
	
	String sessionToken;
	String uri;
	String method;
	String userIdString;
	long threadId;
	String sessionId;
	
	/**
	 * Gather basic date from the request.
	 * @param request
	 */
	public HttpServletRequestData(HttpServletRequest request){
		userIdString = request.getParameter(AuthorizationConstants.USER_ID_PARAM);
		uri = request.getRequestURI();
		method = request.getMethod();
		threadId = Thread.currentThread().getId();
		sessionToken = request.getHeader(AuthorizationConstants.SESSION_TOKEN_PARAM);
		sessionId = ThreadContext.get(AccessInterceptor.SESSION_ID);
	}
	
	
	public String getSessionToken() {
		return sessionToken;
	}

	public String getMethod() {
		return method;
	}

	public String getUserIdString() {
		return userIdString;
	}


	public long getThreadId() {
		return threadId;
	}

	public String getUri() {
		return uri;
	}


	public String getSessionId() {
		return sessionId;
	}


	@Override
	public String toString() {
		return "HttpServletRequestData [sessionToken=" + sessionToken
				+ ", url=" + uri + ", method=" + method + ", userIdString="
				+ userIdString + ", threadId=" + threadId
				+ ", accessRecordSessionId=" + sessionId + "]";
	}

}