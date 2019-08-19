package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.SectorIdentifier;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class OAuthClientDaoImplTest {
	
	private static final String OAUTH_CLIENT_SHARED_SECRET ="oauth client shared secret";
	private static final String SECTOR_IDENTIFIER_ENCRYPTION_SECRET ="sector identifier secret";
	private static final String CLIENT_NAME = "Third paty app";
	private static final String SECTOR_IDENTIFIER = "https://foo.bar";
	private static final String CLIENT_URI = "https://client.uri.com/index.html";
	private static final String POLICY_URI = "https://client.uri.com/policy.html";
	private static final String TOS_URI = "https://client.uri.com/termsOfService.html";
	private static final List<String> REDIRCT_URIS = Collections.singletonList("https://client.com/redir");
	private static final String SECTOR_IDENTIFIER_URI = "https://client.uri.com/path/to/json/file";
	
	private List<String> idsToDelete;
	
	@Autowired
	private OAuthClientDao oauthClientDao;

	@Before
	public void setUp() throws Exception {
		idsToDelete = new ArrayList<String>();
		assertNotNull(oauthClientDao);
	}

	@After
	public void tearDown() throws Exception {
		for (String id: idsToDelete) {
			try {
				oauthClientDao.deleteOAuthClient(id);
			} catch (NotFoundException e) {
				// let pass
			}
		}
		try {
			oauthClientDao.deleteSectorIdentifer(SECTOR_IDENTIFIER);
		} catch (NotFoundException e) {
			// let pass
		}
	}
	
	private static OAuthClient newDTO() {
		OAuthClient result = new OAuthClient();
		Long userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		result.setClient_name(CLIENT_NAME);
		result.setCreatedBy(userId.toString());
		result.setSector_identifier(SECTOR_IDENTIFIER);
		result.setClient_uri(CLIENT_URI);
		result.setCreatedOn(new Date(System.currentTimeMillis()));
		result.setModifiedOn(new Date(System.currentTimeMillis()));
		result.setPolicy_uri(POLICY_URI);
		result.setRedirect_uris(REDIRCT_URIS);
		result.setSector_identifier(SECTOR_IDENTIFIER);
		result.setSector_identifier_uri(SECTOR_IDENTIFIER_URI);
		result.setTos_uri(TOS_URI);
		result.setUserinfo_signed_response_alg(OIDCSigningAlgorithm.RS256);
		result.setValidated(false);
		return result;
	}
	
	private static SectorIdentifier newSectorIdentifier() {
		SectorIdentifier result = new SectorIdentifier();
		Long userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		result.setCreatedBy(userId);
		result.setCreatedOn(System.currentTimeMillis());
		result.setSecret(SECTOR_IDENTIFIER_ENCRYPTION_SECRET);
		result.setSectorIdentifierUri(SECTOR_IDENTIFIER);
		return result;
	}

	private String createSectorIdentifierAndClient() {
		SectorIdentifier sectorIdentifier = newSectorIdentifier();
		oauthClientDao.createSectorIdentifier(sectorIdentifier);
		OAuthClient oauthClient = newDTO();
		String id = oauthClientDao.createOAuthClient(oauthClient, OAUTH_CLIENT_SHARED_SECRET);
		assertNotNull(id);
		idsToDelete.add(id);
		return id;
	}
	
	@Test
	public void testCreateOAuthClient() {
		createSectorIdentifierAndClient();
	}

	@Test
	public void testGetOAuthClientAndSecret() {
		String clientId = createSectorIdentifierAndClient();
		OAuthClient retrieved = oauthClientDao.getOAuthClient(clientId);
		assertEquals(CLIENT_NAME, retrieved.getClient_name());
		assertEquals(CLIENT_URI, retrieved.getClient_uri());
		assertEquals(clientId, retrieved.getClientId());
		assertEquals(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString(), retrieved.getCreatedBy());
		assertTrue(System.currentTimeMillis()-retrieved.getCreatedOn().getTime()<60000L);
		assertNotNull(retrieved.getEtag());
		assertTrue(System.currentTimeMillis()-retrieved.getModifiedOn().getTime()<60000L);
		assertEquals(POLICY_URI, retrieved.getPolicy_uri());
		assertEquals(REDIRCT_URIS, retrieved.getRedirect_uris());
		assertEquals(SECTOR_IDENTIFIER, retrieved.getSector_identifier());
		assertEquals(SECTOR_IDENTIFIER_URI, retrieved.getSector_identifier_uri());
		assertEquals(TOS_URI, retrieved.getTos_uri());
		assertEquals(OIDCSigningAlgorithm.RS256, retrieved.getUserinfo_signed_response_alg());
		assertFalse(retrieved.getValidated());
		
		String s = oauthClientDao.getOAuthClientSecret(clientId);
		assertEquals(OAUTH_CLIENT_SHARED_SECRET, s);
	}
	
	@Test
	public void testLookupSectorIdentifier() {
		// method under test
		assertFalse(oauthClientDao.doesSectorIdentifierExistForURI(SECTOR_IDENTIFIER));
		
		String clientId = createSectorIdentifierAndClient();
		
		// method under test
		assertTrue(oauthClientDao.doesSectorIdentifierExistForURI(SECTOR_IDENTIFIER));
		
		// method under test
		String secret = oauthClientDao.getSectorIdentifierSecretForClient(clientId);
		assertEquals(SECTOR_IDENTIFIER_ENCRYPTION_SECRET, secret);
	}

	@Test
	public void testDeleteOAuthClient() {
		String clientId = createSectorIdentifierAndClient();
		// method under test
		oauthClientDao.deleteOAuthClient(clientId);
		try {
			oauthClientDao.getOAuthClient(clientId);
			fail("deletion failed");
		} catch (NotFoundException e) {
			// as expected
		}
	}

	@Test
	public void testDeleteSectorIdentifer() {
		SectorIdentifier sectorIdentifier = newSectorIdentifier();
		oauthClientDao.createSectorIdentifier(sectorIdentifier);
		
		// method under test
		oauthClientDao.deleteSectorIdentifer(SECTOR_IDENTIFIER);

		assertFalse(oauthClientDao.doesSectorIdentifierExistForURI(SECTOR_IDENTIFIER));	
	}

	@Test
	public void testClientDboToDto() {
		//fail("Not yet implemented");
	}

	@Test
	public void testClientDtoToDbo() {
		//fail("Not yet implemented");
	}

	@Test
	public void testListOAuthClients() {
		//fail("Not yet implemented");
	}

	@Test
	public void testUpdateOAuthClient() {
		// fail("Not yet implemented");
	}

}
