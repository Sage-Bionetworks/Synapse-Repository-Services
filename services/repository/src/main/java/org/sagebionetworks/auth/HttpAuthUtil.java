package org.sagebionetworks.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.http.HttpStatus;

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
		return getBearerTokenFromAuthorizationHeader(header);
	}
	
	public static String getBearerTokenFromAuthorizationHeader(String header) {
		if (header==null || !header.startsWith(AuthorizationConstants.BEARER_TOKEN_HEADER)) return null;
		return header.substring(AuthorizationConstants.BEARER_TOKEN_HEADER.length());
	}
	
	/*
	 * Set the given bearerToken as an Authorization header, overwriting any other Authorization headers
	 */
	public static void setBearerTokenHeader(Map<String, String[]> headers, String bearerToken) {
		headers.put(AuthorizationConstants.AUTHORIZATION_HEADER_NAME, 
				new String[] {AuthorizationConstants.BEARER_TOKEN_HEADER+bearerToken});
	}
	
	private static final List<String> AUTHORIZATION_HEADERS = 
			Arrays.asList(new String[] {}); // TODO Authorization, sessionToken, HMAC, time stamp, user name, what else??
	/*
	 * Get all the request headers *except* the authorization headers used by Synapse
	 */
	public static Map<String, String[]> filterAuthorizationHeaders(HttpServletRequest request) {
		Map<String, String[]> result = new HashMap<String, String[]> ();
		for (Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
			String headerName = e.nextElement();
			if (AUTHORIZATION_HEADERS.contains(headerName)) {
				continue;
			}
			List<String> headerValues = new ArrayList<String>();
			for (Enumeration<String> n = request.getHeaders(headerName); n.hasMoreElements();) {
				String headerValue = n.nextElement();
				headerValues.add(headerValue);
			}
			result.put(headerName, headerValues.toArray(new String[] {}));
		}
		return result;
	}	
	
	public static boolean isDigitalSignaturePresent(HttpServletRequest request) {
		return false; // TODO
	}
	
	public static long getDigitalSignaturePrincipalId(HttpServletRequest request) {
		return 0l; // TODO throw unauthenticated exception if signature is invalid
	}
	
	public static void reject(HttpServletResponse resp, String reason) throws IOException {
		reject(resp, reason, HttpStatus.UNAUTHORIZED);
	}
	
	public static void reject(HttpServletResponse resp, String reason, HttpStatus status) throws IOException {
		resp.setStatus(status.value());

		// This header is required according to RFC-2612
		// See: http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.2
		//      http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.47
		//      http://www.ietf.org/rfc/rfc2617.txt
		resp.setContentType("application/json");
		resp.setHeader("WWW-Authenticate", "\"Digest\" your email");
		ErrorResponse er = new ErrorResponse();
		er.setReason(reason);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		try {
			er.writeToJSONObject(joa);
			resp.getWriter().println(joa.toJSONString());
		} catch (JSONObjectAdapterException e) {
			// give up here, use old method, so we at least send something back
			resp.getWriter().println("{\"reason\": \"" + reason + "\"}");
		}
		resp.getOutputStream().flush();
	}
}
