package org.sagebionetworks.docusign;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.client.auth.OAuth;

/**
 * Thread-safe cache for a DocuSign access token. The token is lazily fetched on
 * first access and refreshed when it expires or is explicitly invalidated.
 *
 * <p>Unlike Guava's LoadingCache (which requires a fixed TTL at construction),
 * this class supports dynamic TTL — each token response specifies its own
 * expiry, which may vary between fetches.
 */
@Service
class DocuSignAccessTokenProvider {

	static final long JWT_EXPIRES_IN_SECONDS = 3600L;
	static final long TOKEN_EXPIRY_BUFFER_MILLIS = 60_000L;
	static final List<String> SCOPES = Arrays.asList("signature", "impersonation");

	private final DocuSignClientConfig config;

	private String cachedAccessToken;
	private long cachedAccessTokenExpiryMillis;

	DocuSignAccessTokenProvider(DocuSignClientConfig config) {
		this.config = config;
	}

	synchronized String getAccessToken() {
		long now = System.currentTimeMillis();
		if (cachedAccessToken != null && now + TOKEN_EXPIRY_BUFFER_MILLIS < cachedAccessTokenExpiryMillis) {
			return cachedAccessToken;
		}
		OAuth.OAuthToken fresh = requestJwtUserToken();
		cachedAccessToken = fresh.getAccessToken();
		cachedAccessTokenExpiryMillis = System.currentTimeMillis() + (fresh.getExpiresIn() * 1000L);
		return cachedAccessToken;
	}

	synchronized void invalidateAccessToken() {
		cachedAccessToken = null;
		cachedAccessTokenExpiryMillis = 0L;
	}

	OAuth.OAuthToken requestJwtUserToken() {
		ApiClient apiClient = new ApiClient(config.getBasePath());
		apiClient.setOAuthBasePath(config.getOAuthBasePath());
		try {
			return apiClient.requestJWTUserToken(
					config.getIntegrationKey(),
					config.getUserId(),
					SCOPES,
					config.getPrivateKeyBytes(),
					JWT_EXPIRES_IN_SECONDS);
		} catch (ApiException e) {
			throw new IllegalStateException("Failed to obtain DocuSign access token.", e);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read DocuSign private key.", e);
		}
	}
}
