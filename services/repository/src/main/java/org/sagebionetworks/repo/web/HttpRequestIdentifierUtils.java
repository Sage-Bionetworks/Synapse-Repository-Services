package org.sagebionetworks.repo.web;

import java.util.StringJoiner;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import org.sagebionetworks.repo.model.AuthorizationConstants;

public class HttpRequestIdentifierUtils {

	public static final String SESSION_ID_COOKIE_NAME = "sessionID";

	//TODO: maybe merge w/ IpAddressUtils?
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

	public static String getRequestIdentifier(ServletRequest request){
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String userId = httpRequest.getParameter(AuthorizationConstants.USER_ID_PARAM);
		String sessionId = getSessionId(httpRequest);
		String ipAddress = IpAddressUtil.getIpAddress(httpRequest);
		return String.join(":", userId, sessionId, ipAddress); //TODO: test null
	}

}
