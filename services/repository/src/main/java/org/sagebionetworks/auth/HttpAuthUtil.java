package org.sagebionetworks.auth;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AuthorizationConstants;

import com.sun.jersey.core.util.Base64;

public class HttpAuthUtil {

	public static UserNameAndPassword getBasicAuthenticationCredentials(HttpServletRequest httpRequest) {
		String header = httpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME);
		if (header==null || !header.startsWith(AuthorizationConstants.BASIC_PREFIX)) return null;

		String base64EncodedCredentials = header.substring(AuthorizationConstants.BASIC_PREFIX.length());
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
		
		String header = httpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME);
		if (header==null || !header.startsWith(AuthorizationConstants.BEARER_TOKEN_HEADER)) return null;
		return header.substring(AuthorizationConstants.BEARER_TOKEN_HEADER.length());
	}

}
