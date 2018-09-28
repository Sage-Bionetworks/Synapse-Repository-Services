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

	public static final String SESSION_ID_COOKIE_NAME = "sessionId";

	public static final String SESSION_HEADER_NAME = "sessionId"; //TODO: change??

	public static String getSessionId(HttpServletRequest request){
		//TODO: if this starts getting more complex we will need a session id provider chain
		//first check http headers
		String idFromHeader = request.getHeader(SESSION_HEADER_NAME);

		if(idFromHeader != null ){
			return idFromHeader;
		}

		//then check cookies
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
