package org.sagebionetworks.repo.manager.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationRequest;
import org.sagebionetworks.repo.model.auth.AccessTokenRecord;
import org.sagebionetworks.repo.model.auth.AccessTokenRecordList;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class PersonalAccessTokenManagerAutowiredTest {

	@Autowired
	private PersonalAccessTokenManager personalAccessTokenManager;

	@Autowired
	private UserManager userManager;

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	private UserInfo adminUserInfo;
	private UserInfo userInfo;

	private static final String OAUTH_ENDPOINT = "http://synapse.org/";

	private List<String> tokenIdsToDelete;
	
	private String fullAccessToken;

	@BeforeEach
	void beforeEach() {
		// Create an admin user and a regular user
		adminUserInfo = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		DBOCredential cred = new DBOCredential();
		cred.setSecretKey("");
		NewUser newUser = new NewUser();
		newUser.setEmail(UUID.randomUUID().toString() + "@test.com");
		newUser.setUserName(UUID.randomUUID().toString());
		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setAgreesToTermsOfUse(Boolean.TRUE);
		userInfo = userManager.createOrGetTestUser(adminUserInfo, newUser, cred, tou);

		tokenIdsToDelete = new ArrayList<>();
		
		fullAccessToken = oidcTokenHelper.createTotalAccessToken(userInfo.getId());
	}

	@AfterEach
	void afterEach() {
		for (String t : tokenIdsToDelete) {
			personalAccessTokenManager.revokeToken(adminUserInfo, t);
		}

		userManager.deletePrincipal(adminUserInfo, userInfo.getId());
	}

	private String getTokenIdFromJwt(String jwt) {
		Jwt<JwsHeader, Claims> parsed = oidcTokenHelper.parseJWT(jwt);
		return parsed.getBody().getId();
	}

	// Helper for getting token and retrieving the ID
	private String createTokenAndGetId() {
		String tokenId = getTokenIdFromJwt(personalAccessTokenManager.issueToken(userInfo, fullAccessToken, new AccessTokenGenerationRequest(), OAUTH_ENDPOINT).getToken());
		tokenIdsToDelete.add(tokenId);
		return tokenId;
	}

	@Test
	void testIssueAndRevokeToken() {
		// method under test -- create
		String token = personalAccessTokenManager.issueToken(userInfo, fullAccessToken, new AccessTokenGenerationRequest(), OAUTH_ENDPOINT).getToken();
		assertTrue(StringUtils.isNotBlank(token));

		String tokenId = this.getTokenIdFromJwt(token);

		// call under test - token is valid
		assertTrue(personalAccessTokenManager.isTokenActive(tokenId));

		// call under test - individual token retrieval
		AccessTokenRecord record = personalAccessTokenManager.getTokenRecord(userInfo, tokenId);
		assertEquals(tokenId, record.getId());

		// call under test - revoke a token and verify it is invalid
		personalAccessTokenManager.revokeToken(userInfo, tokenId);
		assertFalse(personalAccessTokenManager.isTokenActive(tokenId));
	}

	@Test // PLFM-6494
	void testIssueTokenWithDuplicateName() {
		AccessTokenGenerationRequest request = new AccessTokenGenerationRequest();
		request.setName("token name");

		// Create a token
		String token = personalAccessTokenManager.issueToken(userInfo, fullAccessToken, request, OAUTH_ENDPOINT).getToken();
		String tokenId = this.getTokenIdFromJwt(token);

		// Method under test: a user should not be able to create two tokens with the same name, and should get a specific error message
		assertThrows(IllegalArgumentException.class, () -> personalAccessTokenManager.issueToken(userInfo, fullAccessToken, request, OAUTH_ENDPOINT).getToken(), PersonalAccessTokenManagerImpl.DUPLICATE_TOKEN_NAME_MSG);

		// Cleanup -- delete the created token
		personalAccessTokenManager.revokeToken(userInfo, tokenId);
	}

	@Test
	void testGetListOfTokens() {
		String tokenId1 = createTokenAndGetId();
		String tokenId2 = createTokenAndGetId();

		long limit = 1L;
		long offset = 0L;
		NextPageToken npt = new NextPageToken(limit, offset);

		// method under test
		AccessTokenRecordList pageOne = personalAccessTokenManager.getTokenRecords(userInfo, npt.toToken());
		assertEquals(1, pageOne.getResults().size());
		assertNotNull(pageOne.getNextPageToken());

		// method under test
		AccessTokenRecordList pageTwo = personalAccessTokenManager.getTokenRecords(userInfo, pageOne.getNextPageToken());
		assertEquals(1, pageTwo.getResults().size());
		assertNull(pageTwo.getNextPageToken());

		if (pageOne.getResults().get(0).getId().equals(tokenId1)) {
			assertEquals(tokenId2, pageTwo.getResults().get(0).getId());
		} else {
			assertEquals(tokenId1, pageTwo.getResults().get(0).getId());
			assertEquals(tokenId2, pageOne.getResults().get(0).getId());
		}
	}

	@Test
	void testUpdateLastUsedTime() throws Exception {
		// Create a token
		String tokenId1 = createTokenAndGetId();

		AccessTokenRecord preUpdate = personalAccessTokenManager.getTokenRecord(userInfo, tokenId1);

		Thread.sleep(100L);
		// method under test
		personalAccessTokenManager.updateLastUsedTime(tokenId1);

		AccessTokenRecord postUpdate = personalAccessTokenManager.getTokenRecord(userInfo, tokenId1);
		// we update no more than once per minute
		assertTrue(postUpdate.getLastUsed().equals(preUpdate.getLastUsed()));
	}

}
