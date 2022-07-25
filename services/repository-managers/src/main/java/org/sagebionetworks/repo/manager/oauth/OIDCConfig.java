package org.sagebionetworks.repo.manager.oauth;

import java.io.IOException;

import org.json.JSONObject;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;
import org.springframework.http.HttpStatus;

public class OIDCConfig {

	static final String PROPERTY_AUTH_ENDPOINT = "authorization_endpoint";
	static final String PROPERTY_TOKEN_ENDPOINT = "token_endpoint";
	static final String PROPERTY_USERINFO_ENDPOINT = "userinfo_endpoint";

	private SimpleHttpClient httpClient;
	private String uri;
	private JSONObject cachedResponse;

	public OIDCConfig(SimpleHttpClient httpClient, String uri) {
		this.httpClient = httpClient;
		this.uri = uri;
	}

	public String getAuthorizationEndpoint() {
		return getConfigProperty(PROPERTY_AUTH_ENDPOINT);
	}

	public String getTokenEndpoint() {
		return getConfigProperty(PROPERTY_TOKEN_ENDPOINT);
	}

	public String getUserInfoEndpoint() {
		return getConfigProperty(PROPERTY_USERINFO_ENDPOINT);
	}

	String getConfigProperty(String propertyName) {
		if (cachedResponse == null) {
			SimpleHttpRequest request = new SimpleHttpRequest();
			request.setUri(uri);
			SimpleHttpResponse response;

			try {
				response = httpClient.get(request);
			} catch (IOException e) {
				throw new IllegalStateException("Could not fetch discovery document from " + uri + ": " + e.getMessage(), e);
			}

			if (HttpStatus.OK.value() != response.getStatusCode()) {
				throw new IllegalStateException("Could not fetch discovery document: " + response.getStatusCode()
						+ (response.getStatusReason() == null ? "" : " (Reason: " + response.getStatusReason() + ")"));
			}

			cachedResponse = new JSONObject(response.getContent());
		}

		if (!cachedResponse.has(propertyName)) {
			throw new IllegalStateException(
					"Could not fetch property name " + propertyName + " from discovery document: " + cachedResponse.toString());
		}

		return cachedResponse.getString(propertyName);
	}

}
