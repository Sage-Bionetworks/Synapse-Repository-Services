package org.sagebionetworks.auth;

import javax.servlet.http.HttpServletRequest;

import com.sun.jersey.core.util.Base64;

public class HttpAuthUtil {

	private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
	public static final String BASIC_PREFIX = "Basic ";
	public static final String BEARER_PREFIX = "Bearer ";

	public static UserNameAndPassword getBasicAuthenticationCredentials(HttpServletRequest httpRequest) {
		String header = httpRequest.getHeader(AUTHORIZATION_HEADER_NAME);
		if (header==null || !header.startsWith(BASIC_PREFIX)) return null;

		String base64EncodedCredentials = header.substring(BASIC_PREFIX.length());
		String basicCredentials = Base64.base64Decode(base64EncodedCredentials);
		int colon = basicCredentials.indexOf(":");
		if (colon>0 && colon<basicCredentials.length()-1) {
			String name = basicCredentials.substring(0, colon);
			String password = basicCredentials.substring(colon+1);
			return new UserNameAndPassword(name, password);
		}
		return null;
	}

	public static String getBearerToken(HttpServletRequest httpRequest) {
		String header = httpRequest.getHeader(AUTHORIZATION_HEADER_NAME);
		if (header==null || !header.startsWith(BEARER_PREFIX)) return null;
		return header.substring(BASIC_PREFIX.length());
	}

}
