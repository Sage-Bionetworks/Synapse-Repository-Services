package org.sagebionetworks.repo.web;

import java.util.StringJoiner;
import java.util.UUID;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.Triple;
import org.sagebionetworks.common.util.PathNormalizer;
import org.sagebionetworks.repo.model.AuthorizationConstants;

public class HttpRequestIdentifierUtils {
	public static final String SESSION_HEADER_NAME = "sessionId";

	public static String getSessionId(HttpServletRequest request){
		//check http headers
		return request.getHeader(SESSION_HEADER_NAME);
	}

	public static String generateSessionId(){
		return UUID.randomUUID().toString();
	}


	public static HttpRequestIdentifier getRequestIdentifier(ServletRequest request){
		HttpServletRequest httpRequest = (HttpServletRequest) request;

		Long userId = Long.parseLong(httpRequest.getParameter(AuthorizationConstants.USER_ID_PARAM));
		String sessionId = getSessionId(httpRequest);
		String ipAddress = IpAddressUtil.getIpAddress(httpRequest);
		String requestPath = httpRequest.getRequestURI();

		return new HttpRequestIdentifier(userId, sessionId, ipAddress, requestPath);
	}

}
