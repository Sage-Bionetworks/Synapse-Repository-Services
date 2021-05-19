package org.sagebionetworks.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.repo.model.OAuthErrorResponse;
import org.sagebionetworks.repo.web.OAuthErrorCode;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.http.HttpStatus;

public class HttpAuthUtil {
	private static final Decoder BASE64_DECODER = Base64.getDecoder();
	private static final String INVALID_AUTH_MSG_FORMAT = "Invalid Authorization header for basic authentication (%s)"; 

	public static boolean usesBasicAuthentication(HttpServletRequest httpRequest) {
		String header = httpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME);
		if (StringUtils.isBlank(header)) {
			return false;
		}

		return header.startsWith(AuthorizationConstants.BASIC_PREFIX);
	}

	/**
	 * Extracts the credentials from the Authorization header following the basic authentication scheme
	 * 
	 * @param httpRequest The http request
	 * @return An optional that contains the credentials if present in the Authorization header
	 * @throws IllegalArgumentException If the authorization header does not follow the basic authentication scheme and/or it's invalid
	 */
	public static Optional<UserNameAndPassword> getBasicAuthenticationCredentials(HttpServletRequest httpRequest) throws IllegalArgumentException {
		String header = httpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME);

		if (StringUtils.isBlank(header)) {
			return Optional.empty();
		}

		if (!header.startsWith(AuthorizationConstants.BASIC_PREFIX)) {
			throw new IllegalArgumentException(String.format(INVALID_AUTH_MSG_FORMAT,
					"Missing \"" + AuthorizationConstants.BASIC_PREFIX + "\" prefix"));
		}

		String base64EncodedCredentials = header.substring(AuthorizationConstants.BASIC_PREFIX.length()).trim();
		String basicCredentials = null;

		try {
			basicCredentials = new String(BASE64_DECODER.decode(base64EncodedCredentials), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
					String.format(INVALID_AUTH_MSG_FORMAT, "Malformed Base64 encoding: " + e.getMessage()), e);
		}

		int colon = basicCredentials.indexOf(":");

		if (colon > 0 && colon < basicCredentials.length() - 1) {
			String name = basicCredentials.substring(0, colon);
			String password = basicCredentials.substring(colon + 1);
			return Optional.of(new UserNameAndPassword(name, password));
		}

		throw new IllegalArgumentException(
				String.format(INVALID_AUTH_MSG_FORMAT, "Decoded credentials should be colon separated"));
	}

	public static String getBearerTokenFromStandardAuthorizationHeader(HttpServletRequest httpRequest) {
		String header = httpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME);
		return getBearerTokenFromAuthorizationHeader(header);
	}

	public static String getBearerTokenFromAuthorizationHeader(String header) {
		if (StringUtils.isBlank(header) || !header.startsWith(AuthorizationConstants.BEARER_TOKEN_HEADER)) return null;
		return header.substring(AuthorizationConstants.BEARER_TOKEN_HEADER.length()).trim();
	}
	
	/*
	 * Set the given bearerToken as an Authorization header, using the standard *internal* 
	 * Synapse authorization header name
	 */
	public static void setBearerTokenHeader(Map<String, String[]> headers, String bearerToken) {
		headers.put(AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME, 
				new String[] {AuthorizationConstants.BEARER_TOKEN_HEADER+bearerToken});
	}
	
	/**
	 * Sets the given service name as the value of the special synapse header service name
	 * 
	 * @param headers
	 * @param serviceName
	 */
	public static void setServiceNameHeader(Map<String, String[]> headers, String serviceName) {
		headers.put(AuthorizationConstants.SYNAPSE_HEADER_SERVICE_NAME, new String[] { serviceName } );
	}

	private static final List<String> AUTHORIZATION_HEADERS_LOWER_CASE = 
			Arrays.asList(new String[] {
					AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME.toLowerCase(),
					AuthorizationConstants.SESSION_TOKEN_PARAM.toLowerCase(),
					AuthorizationConstants.USER_ID_HEADER.toLowerCase(),
					AuthorizationConstants.SIGNATURE_TIMESTAMP.toLowerCase(),
					AuthorizationConstants.SIGNATURE.toLowerCase(),
					AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_HEADER.toLowerCase(),
					AuthorizationConstants.SYNAPSE_HEADER_SERVICE_NAME.toLowerCase()
			});
	
	/*
	 * Get all the request headers *except* the authorization headers used by Synapse
	 */
	public static Map<String, String[]> filterAuthorizationHeaders(HttpServletRequest request) {
		Map<String, String[]> result = new HashMap<String, String[]> ();
		for (Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
			String headerName = e.nextElement();
			if (AUTHORIZATION_HEADERS_LOWER_CASE.contains(headerName.toLowerCase())) {
				continue;
			}
			List<String> headerValues = new ArrayList<String>();
			for (Enumeration<String> n = request.getHeaders(headerName); n.hasMoreElements();) {
				String headerValue = n.nextElement();
				headerValues.add(headerValue);
			}
			result.put(headerName, headerValues.toArray(new String[headerValues.size()]));
		}
		return result;
	}	
		
	public static void reject(HttpServletResponse resp, String reason) throws IOException {
		rejectWithErrorResponse(resp, reason, HttpStatus.UNAUTHORIZED);
	}

	public static void reject(HttpServletResponse resp, JSONEntity responseBody, HttpStatus status) throws IOException {
		resp.setStatus(status.value());

		// This header is required according to RFC-2612
		// See: http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.2
		//      http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.47
		//      http://www.ietf.org/rfc/rfc2617.txt
		resp.setContentType("application/json");
		if (status.equals(HttpStatus.UNAUTHORIZED)) {
			resp.setHeader("WWW-Authenticate", "\"Digest\" your email");
		}
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		try {
			responseBody.writeToJSONObject(joa);
			resp.getWriter().println(joa.toJSONString());
		} catch (JSONObjectAdapterException e) {
			// give up here, use old method, so we at least send something back
			resp.getWriter().println(responseBody.toString());
		}
		resp.getWriter().flush();
	}
	
	public static void rejectWithErrorResponse(HttpServletResponse resp, String reason, HttpStatus status) throws IOException {
		ErrorResponse er = new ErrorResponse();
		er.setReason(reason);
		reject(resp, er, status);
	}

	/**
	 * OAuth errors don't have the same JSON format as our regular error, so this method may be used for
	 * sending error responses caused by OAuth services
	 */
	public static void rejectWithOAuthError(HttpServletResponse resp, OAuthErrorCode errorCode, String description, HttpStatus status) throws IOException {
		OAuthErrorResponse er = new OAuthErrorResponse();
		er.setError(errorCode.name());
		er.setError_description(description);
		if (description != null) {
			er.setReason(errorCode.name() + ". " + description);
		} else {
			er.setReason(errorCode.name());
		}
		reject(resp, er, status);
	}

}
