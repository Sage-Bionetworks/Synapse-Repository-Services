package org.sagebionetworks.auth;

import org.sagebionetworks.repo.model.auth.LoginCredentials;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.util.ValidateArgument;

public class DeprecatedUtils {

	/**
	 * Create a deprecated Session from a LoginResponse.
	 * @param response
	 * @return
	 */
	@Deprecated
	public static Session createSession(LoginResponse response) {
		ValidateArgument.required(response, "LoginResponse");
		Session session = new Session();
		session.setAcceptsTermsOfUse(response.getAcceptsTermsOfUse());
		session.setSessionToken(response.getSessionToken());
		return session;
	}
	
	/**
	 * Create a LoginRequest from LoginCredentials.
	 * The resulting LoginRequest.authenticationReceipt will be null.
	 * @param credentials
	 * @return
	 */
	@Deprecated
	public static LoginRequest createLoginRequest(LoginCredentials credentials) {
		ValidateArgument.required(credentials, "LoginCredentials");
		ValidateArgument.required(credentials.getEmail(), "LoginCredentials.email");
		ValidateArgument.required(credentials.getPassword(), "LoginCredentials.password");
		LoginRequest request = new LoginRequest();
		request.setUsername(credentials.getEmail());
		request.setPassword(credentials.getPassword());
		return request;
	}
	
}
