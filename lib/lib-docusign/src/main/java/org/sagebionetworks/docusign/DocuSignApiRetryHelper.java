package org.sagebionetworks.docusign;

import com.docusign.esign.client.ApiException;

class DocuSignApiRetryHelper {

	@FunctionalInterface
	interface ApiCall<T> {
		T execute(String accessToken) throws ApiException;
	}

	private final DocuSignAccessTokenProvider accessTokenProvider;

	DocuSignApiRetryHelper(DocuSignAccessTokenProvider accessTokenProvider) {
		this.accessTokenProvider = accessTokenProvider;
	}

	<T> T executeWithRetry(ApiCall<T> action) {
		try {
			return action.execute(accessTokenProvider.getAccessToken());
		} catch (ApiException e) {
			if (e.getCode() == 401) {
				accessTokenProvider.invalidateAccessToken();
				try {
					return action.execute(accessTokenProvider.getAccessToken());
				} catch (ApiException retryEx) {
					throw convertApiException(retryEx);
				}
			}
			throw convertApiException(e);
		}
	}

	static RuntimeException convertApiException(ApiException e) {
		int code = e.getCode();
		if (code == 401) {
			return new DocuSignUnauthorizedException("DocuSign rejected the access token.", e);
		}
		// The response body carries DocuSign's errorCode and message (e.g. why a send failed);
		// include it so the cause is visible in logs and the surfaced error rather than a bare code.
		String responseBody = e.getResponseBody();
		String message = "DocuSign API error " + code + ".";
		if (responseBody != null && !responseBody.isBlank()) {
			message += " " + responseBody;
		}
		return new IllegalStateException(message, e);
	}
}
