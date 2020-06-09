package org.sagebionetworks.repo.manager.oauth;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientAuthorizationHistory;
import org.sagebionetworks.repo.model.oauth.OAuthClientAuthorizationHistoryList;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformationList;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OAuthTokenRevocationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.model.oauth.TokenTypeHint;
import org.sagebionetworks.repo.util.StringUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;

import junit.framework.AssertionFailedError;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class OAuthRefreshTokenManagerAutowiredTest {
	private static final String CLIENT_1_NAME = "some client";
	private static final String CLIENT_1_URI = "https://client1.uri.com/index.html";
	private static final String CLIENT_2_NAME = "a different client";
	private static final String CLIENT_2_URI = "https://client2.uri.com/index.html";
	private static final String POLICY_URI = "https://client.uri.com/policy.html";
	private static final String TOS_URI = "https://client.uri.com/termsOfService.html";
	private static final List<String> REDIRCT_URIS = Collections.singletonList("https://client.com/redir");

	@Autowired
	private UserManager userManager;

	@Autowired
	OAuthClientManager oauthClientManager;
	
	@Autowired
	OIDCTokenHelper tokenHelper;

	@Autowired
	OAuthRefreshTokenManager refreshTokenManager;
	
	private UserInfo adminUserInfo;
	private UserInfo user1;
	private UserInfo user2;
	private OAuthClient client1;
	private OAuthClient client2;

	private List<OAuthScope> scopes;
	private OIDCClaimsRequest claims;

	private OAuthClient createOAuthClient(UserInfo owner, String name, String uri) throws Exception {
		OAuthClient toCreate = new OAuthClient();
		toCreate.setClient_name(name);
		toCreate.setClient_uri(uri);
		toCreate.setCreatedOn(new Date(System.currentTimeMillis()));
		toCreate.setModifiedOn(new Date(System.currentTimeMillis()));
		toCreate.setPolicy_uri(POLICY_URI);
		toCreate.setRedirect_uris(REDIRCT_URIS);
		toCreate.setTos_uri(TOS_URI);
		toCreate.setUserinfo_signed_response_alg(OIDCSigningAlgorithm.RS256);

		return oauthClientManager.createOpenIDConnectClient(user1, toCreate);
	}

	@BeforeEach
	public void beforeEach() throws Exception {
		// Scopes and claims just need placeholder values
		scopes = Arrays.asList(OAuthScope.openid, OAuthScope.modify);
		claims = new OIDCClaimsRequest();
		OIDCClaimsRequestDetails claimDetail = new OIDCClaimsRequestDetails();
		claimDetail.setEssential(true);
		claimDetail.setValue("532523");
		claims.setUserinfo(Collections.singletonMap(OIDCClaimName.team.name(), claimDetail));
		claims.setId_token(Collections.emptyMap());

		// Create two users
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		DBOCredential cred = new DBOCredential();
		cred.setSecretKey("");
		NewUser nu1 = new NewUser();
		nu1.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu1.setUserName(UUID.randomUUID().toString());

		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setAgreesToTermsOfUse(Boolean.TRUE);

		NewUser nu2 = new NewUser();
		nu2.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu2.setUserName(UUID.randomUUID().toString());

		user1 = userManager.createOrGetTestUser(adminUserInfo, nu1, cred, tou);
		user2 = userManager.createOrGetTestUser(adminUserInfo, nu2, cred, tou);

		// Create two clients
		client1 = createOAuthClient(user1, CLIENT_1_NAME, CLIENT_1_URI);
		client2 = createOAuthClient(user1, CLIENT_2_NAME, CLIENT_2_URI);

	}
	
	@AfterEach
	public void afterEach() throws Exception {
		try {
			oauthClientManager.deleteOpenIDConnectClient(adminUserInfo, client1.getClient_id());
			oauthClientManager.deleteOpenIDConnectClient(adminUserInfo, client2.getClient_id());
		} catch (NotFoundException e) {
			// stale ID, no deletion necessary
		}
		try {
			userManager.deletePrincipal(adminUserInfo, user1.getId());
			userManager.deletePrincipal(adminUserInfo, user2.getId());
		} catch (DataAccessException e) {
		}
	}


	@Transactional
	@Test
	public void testRefreshTokenRoundTrip() throws Exception {
		// Create a refresh token
		OAuthRefreshTokenAndId token = refreshTokenManager.createRefreshToken(user1.getId().toString(), client1.getClient_id(), scopes, claims);
		assertNotNull(token);
		assertTrue(StringUtils.isNotBlank(token.getRefreshToken()));
		assertTrue(StringUtils.isNotBlank(token.getTokenId()));

		// Retrieve the token
		OAuthRefreshTokenInformation retrievedByToken = refreshTokenManager.getRefreshTokenMetadataForUpdate(token.getRefreshToken(), client1.getClient_id())
				.orElseThrow(AssertionFailedError::new);

		assertNotNull(retrievedByToken);
		assertEquals(token.getTokenId(), retrievedByToken.getTokenId());
		assertEquals(user1.getId().toString(), retrievedByToken.getPrincipalId());
		assertEquals(client1.getClient_id(), retrievedByToken.getClientId());
		assertNotNull(retrievedByToken.getName());
		assertNotNull(retrievedByToken.getEtag());
		assertNotNull(retrievedByToken.getModifiedOn());
		assertNotNull(retrievedByToken.getLastUsed());
		assertNotNull(retrievedByToken.getAuthorizedOn());
		assertEquals(scopes, retrievedByToken.getScopes());
		assertEquals(claims, retrievedByToken.getClaims());

		// Retrieving the same token by ID should yield the same result
		OAuthRefreshTokenInformation retrievedById = refreshTokenManager.getRefreshTokenMetadata(user1, token.getTokenId());
		assertNotNull(retrievedById);
		assertEquals(retrievedByToken, retrievedById);

		// Other user should get unauthz on attempt to retrieve
		assertThrows(UnauthorizedException.class, () -> refreshTokenManager.getRefreshTokenMetadata(user2, token.getTokenId()));

		// Update a refresh token
		String customName = "my token name";
		retrievedByToken.setName(customName);
		OAuthRefreshTokenInformation updated = refreshTokenManager.updateRefreshTokenMetadata(user1, retrievedByToken);
		assertEquals(customName, updated.getName());
		assertNotEquals(retrievedByToken.getEtag(), updated.getEtag());
		assertNotEquals(retrievedByToken.getModifiedOn(), updated.getModifiedOn());
		// Everything else should be equal
		retrievedByToken.setName(updated.getName());
		retrievedByToken.setEtag(updated.getEtag());
		retrievedByToken.setModifiedOn(updated.getModifiedOn());
		assertEquals(retrievedByToken, updated);

		// Other user should get unauthz on attempt to update
		assertThrows(UnauthorizedException.class, () -> refreshTokenManager.updateRefreshTokenMetadata(user2, updated));

		// Rotate a refresh token
		String oldToken = token.getRefreshToken();
		String newToken = refreshTokenManager.rotateRefreshToken(token.getTokenId());

		assertFalse(refreshTokenManager.getRefreshTokenMetadataForUpdate(oldToken, client1.getClient_id()).isPresent());
		OAuthRefreshTokenInformation rotatedToken = refreshTokenManager.getRefreshTokenMetadataForUpdate(newToken, client1.getClient_id())
				.orElseThrow(AssertionFailedError::new);

		// Last used and etag should be the only fields to update
		assertNotEquals(updated.getLastUsed(), rotatedToken.getLastUsed());
		assertNotEquals(updated.getEtag(), rotatedToken.getEtag());
		updated.setLastUsed(rotatedToken.getLastUsed());
		updated.setEtag(rotatedToken.getEtag());
		assertEquals(updated, rotatedToken);

		// Revoke a refresh token
		// Test unauthz first
		assertThrows(UnauthorizedException.class, () -> refreshTokenManager.revokeRefreshToken(user2, token.getTokenId()));
		refreshTokenManager.revokeRefreshToken(user1, token.getTokenId());
		// Should get NFE because token is revoked/deleted.
		assertThrows(NotFoundException.class, () -> refreshTokenManager.getRefreshTokenMetadata(user1, token.getTokenId()));
		
	}

	@Test
	public void testAuditAndRevokeTokensForUserClientPair() throws Exception {
		// Create two tokens for client 1, one token for client 2
		OAuthRefreshTokenAndId client1Token1 = refreshTokenManager.createRefreshToken(user1.getId().toString(), client1.getClient_id(), scopes, claims);
		OAuthRefreshTokenAndId client1Token2 = refreshTokenManager.createRefreshToken(user1.getId().toString(), client1.getClient_id(), scopes, claims);
		OAuthRefreshTokenAndId client2Token = refreshTokenManager.createRefreshToken(user1.getId().toString(), client2.getClient_id(), scopes, claims);

		// Audit clients
		OAuthClientAuthorizationHistoryList historyList = refreshTokenManager.getAuthorizedClientHistory(user1, null);
		assertEquals(2, historyList.getResults().size());
		assertNull(historyList.getNextPageToken());
		OAuthClientAuthorizationHistory client1History = null;
		OAuthClientAuthorizationHistory client2History = null;
		for (OAuthClientAuthorizationHistory history : historyList.getResults()) {
			if (history.getClient().getClient_id().equals(client1.getClient_id())) {
				client1History = history;
			} else if (history.getClient().getClient_id().equals(client2.getClient_id())) {
				client2History = history;
			} else {
				fail("Found an unexpected OAuthClientAuthorizationHistory record.");
			}
		}
		assertNotNull(client1History);
		assertNull(client1History.getClient().getCreatedBy()); // Public fields should have been removed
		assertNotNull(client2History);
		assertNull(client1History.getClient().getCreatedBy()); // Public fields should have been removed

		OAuthClientAuthorizationHistoryList user2HistoryList = refreshTokenManager.getAuthorizedClientHistory(user2, null);
		assertTrue(user2HistoryList.getResults().isEmpty());
		assertNull(user2HistoryList.getNextPageToken());

		// Audit tokens
		OAuthRefreshTokenInformationList client1TokenList = refreshTokenManager.getMetadataForActiveRefreshTokens(user1, client1.getClient_id(), null);
		assertEquals(2, client1TokenList.getResults().size());
		assertNull(client1TokenList.getNextPageToken());
		OAuthRefreshTokenInformation token1FromList = null;
		OAuthRefreshTokenInformation token2FromList = null;
		for (OAuthRefreshTokenInformation tokenInformation : client1TokenList.getResults()) {
			if (tokenInformation.getTokenId().equals(client1Token1.getTokenId())) {
				token1FromList = tokenInformation;
			} else if (tokenInformation.getTokenId().equals(client1Token2.getTokenId())) {
				token2FromList = tokenInformation;
			} else {
				fail("Found an unexpected OAuthClientAuthorizationHistory record.");
			}
		}
		assertNotNull(token1FromList);
		assertNotNull(token2FromList);

		OAuthRefreshTokenInformationList client2TokenList = refreshTokenManager.getMetadataForActiveRefreshTokens(user1, client2.getClient_id(), null);
		assertEquals(1, client2TokenList.getResults().size());
		assertNull(client2TokenList.getNextPageToken());
		assertEquals(client2Token.getTokenId(), client2TokenList.getResults().get(0).getTokenId());

		OAuthRefreshTokenInformationList user2TokenList = refreshTokenManager.getMetadataForActiveRefreshTokens(user2, client1.getClient_id(), null);
		assertTrue(user2TokenList.getResults().isEmpty());
		assertNull(client2TokenList.getNextPageToken());

		// Revoke tokens
		// Create a token between client 1 and user 2 (to ensure it isn't accidentally revoked when user 1 revokes client 1's tokens)
		OAuthRefreshTokenAndId user2Token = refreshTokenManager.createRefreshToken(user2.getId().toString(), client1.getClient_id(), scopes, claims);

		refreshTokenManager.revokeRefreshTokensForUserClientPair(user1, client1.getClient_id());

		// Verify tokens are gone
		client1TokenList = refreshTokenManager.getMetadataForActiveRefreshTokens(user1, client1.getClient_id(), null);
		assertTrue(client1TokenList.getResults().isEmpty());

		// Verify that tokens between other clients or users are not gone
		client2TokenList = refreshTokenManager.getMetadataForActiveRefreshTokens(user1, client2.getClient_id(), null);
		assertFalse(client2TokenList.getResults().isEmpty());

		user2TokenList = refreshTokenManager.getMetadataForActiveRefreshTokens(user2, client1.getClient_id(), null);
		assertFalse(user2TokenList.getResults().isEmpty());

		// Audit clients -- client 1 should be gone from user 1's list
		historyList = refreshTokenManager.getAuthorizedClientHistory(user1, null);
		assertEquals(1, historyList.getResults().size());
		assertNull(historyList.getNextPageToken());
		client1History = null;
		client2History = null;
		for (OAuthClientAuthorizationHistory history : historyList.getResults()) {
			if (history.getClient().getClient_id().equals(client1.getClient_id())) {
				client1History = history;
			} else if (history.getClient().getClient_id().equals(client2.getClient_id())) {
				client2History = history;
			} else {
				fail("Found an unexpected OAuthClientAuthorizationHistory record.");
			}
		}
		assertNull(client1History);
		assertNotNull(client2History);
	}

	@Test
	public void testRevokeTokenWithRefreshToken() throws Exception {
		OAuthRefreshTokenAndId tokenAndId = refreshTokenManager.createRefreshToken(user1.getId().toString(), client1.getClient_id(), scopes, claims);

		OAuthTokenRevocationRequest revocationRequest = new OAuthTokenRevocationRequest();
		revocationRequest.setToken(tokenAndId.getRefreshToken());
		revocationRequest.setToken_type_hint(TokenTypeHint.refresh_token);
		// Call under test
		refreshTokenManager.revokeRefreshToken(client1.getClient_id(), revocationRequest);

		assertThrows(NotFoundException.class, () -> refreshTokenManager.getRefreshTokenMetadata(user1, tokenAndId.getTokenId()));
	}

	@Test
	public void testRevokeTokenWithAccessToken() throws Exception {
		OAuthRefreshTokenAndId tokenAndId = refreshTokenManager.createRefreshToken(user1.getId().toString(), client1.getClient_id(), scopes, claims);

		// We must create a signed JWT containing the `refresh_token_id` claim -- the only argument here that matters is the refresh token ID
		String accessToken  = tokenHelper.createOIDCaccessToken("placeholder", "placeholder", client1.getClient_id(), System.currentTimeMillis(), new Date(), tokenAndId.getTokenId(), UUID.randomUUID().toString(), scopes, Collections.emptyMap());

		OAuthTokenRevocationRequest revocationRequest = new OAuthTokenRevocationRequest();
		revocationRequest.setToken(accessToken);
		revocationRequest.setToken_type_hint(TokenTypeHint.access_token);

		// Call under test
		refreshTokenManager.revokeRefreshToken(client1.getClient_id(), revocationRequest);

		assertThrows(NotFoundException.class, () -> refreshTokenManager.getRefreshTokenMetadata(user1, tokenAndId.getTokenId()));
	}



	@Test
	public void testMandatoryWriteTransactions() {
		assertThrows(IllegalTransactionStateException.class,() ->
				refreshTokenManager.getRefreshTokenMetadataForUpdate("token", client1.getClient_id())
		);

		assertThrows(IllegalTransactionStateException.class,() ->
				refreshTokenManager.rotateRefreshToken("tokenId")
		);
	}


}
