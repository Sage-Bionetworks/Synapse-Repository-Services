package org.sagebionetworks.repo.manager.oauth;

import org.sagebionetworks.repo.model.auth.IdentityProviderName;

/**
 * What a validated access token says about its bearer.
 *
 * @param userId the Synapse user the token authenticates
 * @param identityProvider the name of the identity provider that authenticated that user, or null if
 *        none did — as for an anonymous access token, or one issued before this was recorded
 * @param originalUserId the user acting as {@code userId} (read-only impersonation), or null when the
 *        token was issued directly to {@code userId}
 */
public record ValidatedAccessToken(String userId, IdentityProviderName identityProvider, String originalUserId) {

	public ValidatedAccessToken(String userId, IdentityProviderName identityProvider) {
		this(userId, identityProvider, null);
	}
}
