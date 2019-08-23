package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.auth.SectorIdentifier;
import org.sagebionetworks.repo.model.dbo.persistence.DBOOAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
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
	private static final String SECOND_SECTOR_IDENTIFIER = "https://newsi.com";

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
		try {
			oauthClientDao.deleteSectorIdentifer(SECOND_SECTOR_IDENTIFIER);
		} catch (NotFoundException e) {
			// let pass
		}
	}
	
	private static OAuthClient newDTO(Long userId, String clientName) {
		OAuthClient result = new OAuthClient();
		result.setClient_name(clientName);
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
	
	private static OAuthClient newDTO() {
		Long userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		return newDTO(userId, CLIENT_NAME);
	}
	
	private static SectorIdentifier newSectorIdentifier() {
		return newSectorIdentifier(SECTOR_IDENTIFIER);
	}
	
	private static SectorIdentifier newSectorIdentifier(String uri) {
		SectorIdentifier result = new SectorIdentifier();
		Long userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		result.setCreatedBy(userId);
		result.setCreatedOn(System.currentTimeMillis());
		result.setSecret(SECTOR_IDENTIFIER_ENCRYPTION_SECRET);
		result.setSectorIdentifierUri(uri);
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
	public void testClientDtoToDbo() {
		OAuthClient dto = newDTO();
		Long clientId=101L;
		dto.setClientId(clientId.toString());
		String etag = "999";
		dto.setEtag(etag);
		dto.setCreatedOn(new Date());
		dto.setModifiedOn(new Date());
		DBOOAuthClient dbo = OAuthClientDaoImpl.clientDtoToDbo(dto);
		// elsewhere we test the round trip, DTO->DBO->DTO.  Here we just want to make sure the DBO fields 
		// are getting populated
		assertEquals(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId(), dbo.getCreatedBy());
		assertTrue(System.currentTimeMillis()-dbo.getCreatedOn()<60000L);
		assertTrue(System.currentTimeMillis()-dbo.getModifiedOn()<60000L);
		assertEquals(etag, dbo.geteTag());
		assertEquals(clientId, dbo.getId());
		assertEquals(CLIENT_NAME, dbo.getName());
		assertNotNull(dbo.getProperties());
		assertEquals(SECTOR_IDENTIFIER, dbo.getSectorIdentifierUri());
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
	public void testNameUniqueness() {
		createSectorIdentifierAndClient();
		OAuthClient oauthClient = newDTO();
		try {
			oauthClientDao.createOAuthClient(oauthClient, OAUTH_CLIENT_SHARED_SECRET);
			fail("name uniqueness is not enforced");
		} catch (IllegalArgumentException e) {
			assertEquals("OAuth client already exists with name "+oauthClient.getClient_name(), e.getMessage());
		}
	}

	@Test
	public void testListOAuthClients() {
		createSectorIdentifierAndClient();
		long numClients = 2;
		// At the manager level 'anonymous' can't make a client but it's OK to use the ID for testing the DAO
		Long userId2 = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId(); 
		Set<String> expectedClientNames = new HashSet<String>();
		for (int i=0; i<numClients; i++) {
			String clientName = "ANOTHER CLIENT "+i;
			expectedClientNames.add(clientName);
			OAuthClient oauthClient = newDTO(userId2, clientName);
			String id = oauthClientDao.createOAuthClient(oauthClient, OAUTH_CLIENT_SHARED_SECRET);
			assertNotNull(id);
			idsToDelete.add(id);
		}
		
		// method under test
		OAuthClientList list = oauthClientDao.listOAuthClients(null, userId2);
		
		assertNull(list.getNextPageToken());
		List<OAuthClient> clients = list.getResults();
		// we only see our own clients
		Set<String> actualClientNames = new HashSet<String>();
		assertEquals(numClients, clients.size());
		for (OAuthClient client : clients) {
			actualClientNames.add(client.getClient_name());
		}
		assertEquals(expectedClientNames, actualClientNames);
		
		// now let's check pagination
		NextPageToken firstPage = new NextPageToken(/*limit*/1, /*offset*/0);
		
		// method under test
		list = oauthClientDao.listOAuthClients(firstPage.toToken(), userId2);
		assertEquals(1, list.getResults().size());
		NextPageToken nextPage = new NextPageToken(list.getNextPageToken());
		assertEquals(1L, nextPage.getOffset());
		assertEquals(2L, nextPage.getLimitForQuery());
		
		// method under test
		list = oauthClientDao.listOAuthClients(nextPage.toToken(), userId2);
		assertEquals(1, list.getResults().size());
		assertNull(list.getNextPageToken()); // no more results	
	}

	@Test
	public void testUpdateOAuthClient() {
		String id = createSectorIdentifierAndClient();
		OAuthClient clientToUpdate = oauthClientDao.getOAuthClient(id);
		String newName = "new name";
		clientToUpdate.setClient_name(newName);
		String newClientUri = "https://new/index.html";
		clientToUpdate.setClient_uri(newClientUri);
		String newPolicyUri = "https://new/policy";
		clientToUpdate.setPolicy_uri(newPolicyUri);
		String newTOS = "https://new/tos";
		clientToUpdate.setTos_uri(newTOS);
		List<String> newRedir = Collections.singletonList("https://new/redir");
		clientToUpdate.setRedirect_uris(newRedir);
		clientToUpdate.setSector_identifier(SECOND_SECTOR_IDENTIFIER);
		String newSIURI = "https://new/uri";
		clientToUpdate.setSector_identifier_uri(newSIURI);
		clientToUpdate.setUserinfo_signed_response_alg(null);
		clientToUpdate.setValidated(true);
		
		SectorIdentifier sectorIdentifier = newSectorIdentifier(SECOND_SECTOR_IDENTIFIER);
		oauthClientDao.createSectorIdentifier(sectorIdentifier);
		
		// method under test
		OAuthClient updated = oauthClientDao.updateOAuthClient(clientToUpdate);
		assertEquals(newName, updated.getClient_name());
		assertEquals(newClientUri, updated.getClient_uri());
		assertEquals(id, updated.getClientId());
		assertEquals(clientToUpdate.getCreatedBy(), updated.getCreatedBy());
		assertEquals(clientToUpdate.getCreatedOn(), updated.getCreatedOn());
		assertNotNull(updated.getEtag());
		assertNotEquals(clientToUpdate.getEtag(), updated.getEtag());
		assertNotNull(updated.getModifiedOn());
		assertEquals(newPolicyUri, updated.getPolicy_uri());
		assertEquals(newRedir, updated.getRedirect_uris());
		assertEquals(SECOND_SECTOR_IDENTIFIER, updated.getSector_identifier());
		assertEquals(newSIURI, updated.getSector_identifier_uri());
		assertEquals(newTOS, updated.getTos_uri());
		assertNull(updated.getUserinfo_signed_response_alg());
		assertTrue(updated.getValidated());
	}

}
