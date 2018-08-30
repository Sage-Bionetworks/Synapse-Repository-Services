package org.sagebionetworks.repo.web;

import java.util.StringJoiner;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.Triple;
import org.sagebionetworks.common.util.PathNormalizer;
import org.sagebionetworks.repo.model.AuthorizationConstants;

public class HttpRequestIdentifierUtils {

	public static final String SESSION_ID_COOKIE_NAME = "sessionID";

	public static String getSessionId(HttpServletRequest request){
		if (request.getCookies() == null){
			return null;
		}
		for(Cookie cookie : request.getCookies()){
			if (SESSION_ID_COOKIE_NAME.equals(cookie.getName())){
				return cookie.getValue();
			}
		}
		return null;
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
