package org.sagebionetworks;

import org.apache.commons.io.IOUtils;
import org.sagebionetworks.client.BaseClientImpl;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.auth.JSONWebTokenHelper;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.util.RetryException;
import org.sagebionetworks.util.TimeUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Holds helpers for setting up integration tests
 */
public class SynapseClientHelper {
		
	public static void setEndpoints(SynapseClient client) {
		client.setAuthEndpoint(StackConfigurationSingleton.singleton().getAuthenticationServicePrivateEndpoint());
		client.setRepositoryEndpoint(StackConfigurationSingleton.singleton().getRepositoryServiceEndpoint());
		client.setFileEndpoint(StackConfigurationSingleton.singleton().getFileServiceEndpoint());
		client.setDrsEndpoint(StackConfigurationSingleton.singleton().getDrsServiceEndpoint());
	}
	
	/**
	 * Creates a user that can login with a session token
	 * 
	 * @param newUserClient The client to log the new user in
	 * @return The ID of the user
	 */
	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient) throws SynapseException, JSONObjectAdapterException {
		return createUser(client, newUserClient, UUID.randomUUID().toString(), true, false);
	}

	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient, boolean acceptsTermsOfUse, boolean validateUser) throws SynapseException, JSONObjectAdapterException {
		return createUser(client, newUserClient, UUID.randomUUID().toString(), acceptsTermsOfUse, validateUser);
	}

	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient, String username, boolean acceptsTermsOfUse, boolean validateUser) throws SynapseException, JSONObjectAdapterException {
		return createUser(client, newUserClient, username, "password"+UUID.randomUUID().toString(), acceptsTermsOfUse, validateUser);
	}
	
	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient, String username, String password, boolean acceptsTermsOfUse, boolean validateUser) throws SynapseException, JSONObjectAdapterException {
		return createUser(client, newUserClient, username, password, UUID.randomUUID().toString() + "@sagebase.org", acceptsTermsOfUse, validateUser);
	}
	
	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient, 
			String username, String password, String email, boolean acceptsTermsOfUse, boolean validateUser) throws SynapseException, JSONObjectAdapterException {
		if (newUserClient == null) {
			newUserClient = new SynapseClientImpl();
		}
		setEndpoints(newUserClient);

		NewIntegrationTestUser nu = new NewIntegrationTestUser();
		nu.setTou(acceptsTermsOfUse);
		nu.setEmail(email);
		nu.setUsername(username);
		nu.setPassword(password);
		nu.setValidatedUser(validateUser);
		LoginResponse loginResponse = client.createIntegrationTestUser(nu);
		
		String accessTokenSubject = JSONWebTokenHelper.getSubjectFromJWTAccessToken(loginResponse.getAccessToken());
		Long principalId = Long.parseLong(accessTokenSubject);
		
		newUserClient.setBearerAuthorizationToken(loginResponse.getAccessToken());
		client.setCertifiedUserStatus(accessTokenSubject, true);
		
		return principalId;
	}

	/**
	 * Reads the content of the given pre-signed URL. A 403 {@code AccessDenied} response is retried
	 * with the same rule and budget as {@link SynapseClient#downloadFile}; any other error fails at once.
	 */
	public static String getPresignedUrlContent(URL url) throws IOException {
		try {
			return TimeUtils.waitForExponentialMaxRetry(BaseClientImpl.MAX_RETRY_SERVICE_UNAVAILABLE_COUNT, 1000, () -> {
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				try {
					if (connection.getResponseCode() != HttpURLConnection.HTTP_FORBIDDEN) {
						return IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
					}
					String body = connection.getErrorStream() == null ? ""
							: IOUtils.toString(connection.getErrorStream(), StandardCharsets.UTF_8);
					IOException forbidden = new IOException("Server returned HTTP response code: 403 for URL: " + url + " " + body);
					if (body.contains("AccessDenied")) {
						throw new RetryException(forbidden);
					}
					throw forbidden;
				} finally {
					connection.disconnect();
				}
			});
		} catch (RetryException e) {
			throw (IOException) e.getCause();
		} catch (IOException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
