package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.OAuthRefreshTokenDao;
import org.sagebionetworks.repo.model.auth.SectorIdentifier;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:jdomodels-test-context.xml"})
public class OAuthAccessTokenDaoImplTest {
	
	private static final Long USER_ID = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	
	@Autowired
	private OAuthRefreshTokenDao oauthRefreshTokenDao;

	@Autowired
	private OAuthClientDao oauthClientDao;
	
	@Autowired
	private OAuthAccessTokenDao dao;
			
	@Test
	public void testCrud() {
		
		String tokenOneId = UUID.randomUUID().toString();
		String tokenTwoId = UUID.randomUUID().toString();
		String tokenThreeId = UUID.randomUUID().toString();
		
		Long otherUserId = 12345L;
		
		assertFalse(dao.isAccessTokenRecordExists(tokenOneId));
		assertFalse(dao.isAccessTokenRecordExists(tokenTwoId));
		assertFalse(dao.isAccessTokenRecordExists(tokenThreeId));
		
		Date now = new Date();
		
		OIDCAccessTokenData data = new OIDCAccessTokenData()
			.setTokenId(tokenOneId)
			.setPrincipalId(USER_ID)
			.setClientId(Long.valueOf(AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID))
			.setCreatedOn(now)
			.setExpiresOn(Date.from(now.toInstant().plus(1, ChronoUnit.HOURS)));			
		
		dao.storeAccessTokenRecord(data);
		dao.storeAccessTokenRecord(data.setTokenId(tokenTwoId));
		
		// Another user token
		dao.storeAccessTokenRecord(data.setPrincipalId(otherUserId).setTokenId(tokenThreeId).setSessionId(UUID.randomUUID().toString()));		
		
		assertTrue(dao.isAccessTokenRecordExists(tokenOneId));
		assertTrue(dao.isAccessTokenRecordExists(tokenTwoId));
		assertTrue(dao.isAccessTokenRecordExists(tokenThreeId));
		
		dao.deleteAccessTokenRecord(tokenOneId);
		
		assertFalse(dao.isAccessTokenRecordExists(tokenOneId));
		assertTrue(dao.isAccessTokenRecordExists(tokenTwoId));
		assertTrue(dao.isAccessTokenRecordExists(tokenThreeId));
		
		dao.deleteAccessTokenRecords(USER_ID);
		
		assertFalse(dao.isAccessTokenRecordExists(tokenOneId));
		assertFalse(dao.isAccessTokenRecordExists(tokenTwoId));
		assertTrue(dao.isAccessTokenRecordExists(tokenThreeId));
		
		dao.deleteAccessTokenRecords(otherUserId);
		
		assertFalse(dao.isAccessTokenRecordExists(tokenThreeId));
	}
	
	@Test
	public void testCrudWithRefreshToken() {
		
		SectorIdentifier sectorIdentifier = new SectorIdentifier();
		
		sectorIdentifier.setSectorIdentifierUri("https://foo.bar");
		sectorIdentifier.setSecret(UUID.randomUUID().toString());
		sectorIdentifier.setCreatedBy(Long.valueOf(USER_ID));
		sectorIdentifier.setCreatedOn((new Date()).getTime());
		
		oauthClientDao.createSectorIdentifier(sectorIdentifier);

		OAuthClient client = new OAuthClient();
		
		client.setClient_name(UUID.randomUUID().toString());
		client.setCreatedBy(USER_ID.toString());
		client.setSector_identifier("https://foo.bar");
		client.setSector_identifier_uri("https://client.uri.com/path/to/json/file");
		client.setVerified(true);
		client.setEtag(UUID.randomUUID().toString());
		
		client = oauthClientDao.createOAuthClient(client);
		
		OAuthRefreshTokenInformation refreshTokenMetadata = new OAuthRefreshTokenInformation();
		
		refreshTokenMetadata.setName(UUID.randomUUID().toString());
		refreshTokenMetadata.setPrincipalId(USER_ID.toString());
		refreshTokenMetadata.setClientId(client.getClient_id());
		refreshTokenMetadata.setScopes(Collections.singletonList(OAuthScope.view));
		refreshTokenMetadata.setClaims(new OIDCClaimsRequest());
		refreshTokenMetadata.setModifiedOn(new Date());
		refreshTokenMetadata.setLastUsed(new Date());
		refreshTokenMetadata.setAuthorizedOn(new Date());
		refreshTokenMetadata.setEtag(UUID.randomUUID().toString());

		OAuthRefreshTokenInformation refreshToken = oauthRefreshTokenDao.createRefreshToken("someHash", refreshTokenMetadata);
		
		String tokenOneId = UUID.randomUUID().toString();
		
		assertFalse(dao.isAccessTokenRecordExists(tokenOneId));
		
		Date now = new Date();
		
		OIDCAccessTokenData data = new OIDCAccessTokenData()
			.setTokenId(tokenOneId)
			.setRefreshTokenId(Long.valueOf(refreshToken.getTokenId()))
			.setPrincipalId(USER_ID)
			.setClientId(Long.valueOf(client.getClient_id()))
			.setCreatedOn(now)
			.setExpiresOn(Date.from(now.toInstant().plus(1, ChronoUnit.HOURS)));			
		
		dao.storeAccessTokenRecord(data);
		
		assertTrue(dao.isAccessTokenRecordExists(tokenOneId));
		
		// Deleting the refresh token also deletes the access token
		oauthRefreshTokenDao.deleteToken(refreshToken.getTokenId());
		
		assertFalse(dao.isAccessTokenRecordExists(tokenOneId));
		
		oauthClientDao.deleteOAuthClient(client.getClient_id());
		oauthClientDao.deleteSectorIdentifer("https://foo.bar");
		
	}
	
	
}
