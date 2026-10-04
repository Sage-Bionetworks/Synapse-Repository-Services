package org.sagebionetworks.repo.manager.oauth;

/**
 * What a validated access token says about its bearer.
 *
 * @param userId the Synapse user the token authenticates
 * @param identityProvider the name of the identity provider that authenticated that user, or null if
 *        none did — as for an anonymous access token, or one issued before this was recorded
 */
public record ValidatedAccessToken(String userId, String identityProvider) {
}
