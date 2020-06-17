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
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;


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

		// Call under test
		OAuthRefreshTokenAndMetadata token = refreshTokenManager.createRefreshToken(user1.getId().toString(), client1.getClient_id(), scopes, claims);
		assertNotNull(token);
		assertTrue(StringUtils.isNotBlank(token.getRefreshToken()));
		OAuthRefreshTokenInformation tokenMetadata = token.getMetadata();
		assertNotNull(tokenMetadata);
		assertEquals(user1.getId().toString(), tokenMetadata.getPrincipalId());
		assertEquals(client1.getClient_id(), tokenMetadata.getClientId());
		assertNotNull(tokenMetadata.getName());
		assertNotNull(tokenMetadata.getEtag());
		assertNotNull(tokenMetadata.getModifiedOn());
		assertNotNull(tokenMetadata.getLastUsed());
		assertNotNull(tokenMetadata.getAuthorizedOn());
		assertEquals(scopes, tokenMetadata.getScopes());
		assertEquals(claims, tokenMetadata.getClaims());

		// Retrieving the token with the token ID as a user
		// Call under test
		OAuthRefreshTokenInformation retrievedViaId = refreshTokenManager.getRefreshTokenMetadata(user1, token.getMetadata().getTokenId());
		assertNotNull(retrievedViaId);
		assertEquals(token.getMetadata(), retrievedViaId);

		// Retrieving the token with the token ID as a verified client
		// Call under test
		retrievedViaId = refreshTokenManager.getRefreshTokenMetadata(client1.getClient_id(), token.getMetadata().getTokenId());
		assertNotNull(retrievedViaId);
		assertEquals(token.getMetadata(), retrievedViaId);


		// Retrieve the token metadata with the token
		OAuthRefreshTokenInformation retrievedViaMetadata = refreshTokenManager.getRefreshTokenMetadataWithToken(client1.getClient_id(), token.getRefreshToken());
		assertEquals(retrievedViaId, retrievedViaMetadata);

		// Rotate the token
		// Call under test
		OAuthRefreshTokenAndMetadata rotatedToken = refreshTokenManager.rotateRefreshToken(token.getRefreshToken());
		assertNotNull(rotatedToken);
		assertTrue(StringUtils.isNotBlank(rotatedToken.getRefreshToken()));
		assertNotEquals(rotatedToken.getRefreshToken(), token.getRefreshToken()); // The token should be different
		assertNotNull(rotatedToken.getMetadata());
		// Last used and Etag will have changed
		assertNotEquals(token.getMetadata().getEtag(), rotatedToken.getMetadata().getEtag());
		assertTrue(rotatedToken.getMetadata().getLastUsed().getTime() >= token.getMetadata().getLastUsed().getTime());
		// Everything else will be equal
		token.getMetadata().setEtag(rotatedToken.getMetadata().getEtag());
		token.getMetadata().setLastUsed(rotatedToken.getMetadata().getLastUsed());
		assertEquals(token.getMetadata(), rotatedToken.getMetadata());

		// Should get IllegalArgumentException when trying to retrieve via the old hash
		assertThrows(IllegalArgumentException.class, () -> refreshTokenManager.rotateRefreshToken(token.getRefreshToken()));

		// Other user should get unauthz on attempt to retrieve via ID
		assertThrows(UnauthorizedException.class, () -> refreshTokenManager.getRefreshTokenMetadata(user2, token.getMetadata().getTokenId()));

		// Other client should get unauthz on attempt to retrieve via ID or token itself
		assertThrows(UnauthorizedException.class, () -> refreshTokenManager.getRefreshTokenMetadata(client2.getClient_id(), token.getMetadata().getTokenId()));
		assertThrows(UnauthorizedException.class, () -> refreshTokenManager.getRefreshTokenMetadataWithToken(client2.getClient_id(), rotatedToken.getRefreshToken()));


		// Update a refresh token's metadata
		OAuthRefreshTokenInformation metadata = rotatedToken.getMetadata();
		String customName = "my token name";
		metadata.setName(customName);
		// Call under test
		OAuthRefreshTokenInformation updatedMetadata = refreshTokenManager.updateRefreshTokenMetadata(user1, metadata);
		assertEquals(customName, updatedMetadata.getName());
		assertNotEquals(metadata.getEtag(), updatedMetadata.getEtag());
		assertTrue(updatedMetadata.getModifiedOn().getTime() >= metadata.getModifiedOn().getTime());
		// Everything else should be equal
		metadata.setName(updatedMetadata.getName());
		metadata.setEtag(updatedMetadata.getEtag());
		metadata.setModifiedOn(updatedMetadata.getModifiedOn());
		assertEquals(metadata, updatedMetadata);

		// Other user should get unauthz on attempt to update
		assertThrows(UnauthorizedException.class, () -> refreshTokenManager.updateRefreshTokenMetadata(user2, updatedMetadata));

		// Revoke a refresh token
		// Test unauthz first
		assertThrows(UnauthorizedException.class, () -> refreshTokenManager.revokeRefreshToken(user2, token.getMetadata().getTokenId()));
		// Call under test
		refreshTokenManager.revokeRefreshToken(user1, token.getMetadata().getTokenId());

		// Should get NFE because token is revoked/deleted.
		assertThrows(NotFoundException.class, () -> refreshTokenManager.getRefreshTokenMetadata(user1, token.getMetadata().getTokenId()));
		// Should get IllegalArgumentException when trying to retrieve via hash
		assertThrows(IllegalArgumentException.class, () -> refreshTokenManager.rotateRefreshToken(rotatedToken.getRefreshToken()));

	}

	@Test
	public void testAuditAndRevokeTokensForUserClientPair() throws Exception {
		// Create two tokens for client 1, one token for client 2
		OAuthRefreshTokenAndMetadata client1Token1 = refreshTokenManager.createRefreshToken(user1.getId().toString(), client1.getClient_id(), scopes, claims);
		OAuthRefreshTokenAndMetadata client1Token2 = refreshTokenManager.createRefreshToken(user1.getId().toString(), client1.getClient_id(), scopes, claims);
		OAuthRefreshTokenAndMetadata client2Token = refreshTokenManager.createRefreshToken(user1.getId().toString(), client2.getClient_id(), scopes, claims);

		// Audit clients
		// Call under test
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
		assertNull(client1History.getClient().getCreatedBy()); // Private fields should have been removed
		assertNotNull(client2History);
		assertNull(client2History.getClient().getCreatedBy()); // Private fields should have been removed

		// Call under test
		OAuthClientAuthorizationHistoryList user2HistoryList = refreshTokenManager.getAuthorizedClientHistory(user2, null);
		assertTrue(user2HistoryList.getResults().isEmpty());
		assertNull(user2HistoryList.getNextPageToken());

		// Audit tokens
		// Call under test
		OAuthRefreshTokenInformationList client1TokenList = refreshTokenManager.getMetadataForActiveRefreshTokens(user1, client1.getClient_id(), null);
		assertEquals(2, client1TokenList.getResults().size());
		assertNull(client1TokenList.getNextPageToken());
		OAuthRefreshTokenInformation token1FromList = null;
		OAuthRefreshTokenInformation token2FromList = null;
		for (OAuthRefreshTokenInformation tokenInformation : client1TokenList.getResults()) {
			if (tokenInformation.getTokenId().equals(client1Token1.getMetadata().getTokenId())) {
				token1FromList = tokenInformation;
			} else if (tokenInformation.getTokenId().equals(client1Token2.getMetadata().getTokenId())) {
				token2FromList = tokenInformation;
			} else {
				fail("Found an unexpected OAuthClientAuthorizationHistory record.");
			}
		}
		assertNotNull(token1FromList);
		assertNotNull(token2FromList);

		// Call under test
		OAuthRefreshTokenInformationList client2TokenList = refreshTokenManager.getMetadataForActiveRefreshTokens(user1, client2.getClient_id(), null);
		assertEquals(1, client2TokenList.getResults().size());
		assertNull(client2TokenList.getNextPageToken());
		assertEquals(client2Token.getMetadata().getTokenId(), client2TokenList.getResults().get(0).getTokenId());

		// Call under test
		OAuthRefreshTokenInformationList user2TokenList = refreshTokenManager.getMetadataForActiveRefreshTokens(user2, client1.getClient_id(), null);
		assertTrue(user2TokenList.getResults().isEmpty());
		assertNull(client2TokenList.getNextPageToken());

		// Revoke tokens
		// Create a token between client 1 and user 2 (to ensure it isn't accidentally revoked when user 1 revokes client 1's tokens)
		OAuthRefreshTokenAndMetadata user2Token = refreshTokenManager.createRefreshToken(user2.getId().toString(), client1.getClient_id(), scopes, claims);

		// Call under test
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
		client2History = historyList.getResults().get(0);
		assertEquals(client2.getClient_id(), client2History.getClient().getClient_id());
	}

	@Test
	public void testIsRefreshTokenActive() {
		OAuthRefreshTokenAndMetadata token = refreshTokenManager.createRefreshToken(user1.getId().toString(), client1.getClient_id(), scopes, claims);

		// Call under test
		assertTrue(refreshTokenManager.isRefreshTokenActive(token.getMetadata().getTokenId()));

		// revoke the token
		refreshTokenManager.revokeRefreshToken(user1, token.getMetadata().getTokenId());

		// Call under test
		assertFalse(refreshTokenManager.isRefreshTokenActive(token.getMetadata().getTokenId()));
	}

	@Test
	public void testRotateToken_MandatoryWriteTransaction() {
		assertThrows(IllegalTransactionStateException.class,() ->
				refreshTokenManager.rotateRefreshToken("token")
		);
	}


}
