package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class OAuthClientDaoImplTest {
	
	private static final String SHARED_SECRET ="a secret";
	private static final String SECTOR_IDENTIFIER = "https://foo.bar";
	private List<String> idsToDelete;
	
	@Autowired
	private OAuthClientDao oauthClientDao;

	@Before
	public void setUp() throws Exception {
		idsToDelete = new ArrayList<String>();
		assertNotNull(oauthClientDao);
		oauthClientDao.deleteSectorIdentifer(SECTOR_IDENTIFIER);
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
	}
	
	private static OAuthClient newOAuthClient() {
		OAuthClient result = new OAuthClient();
		Long userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		result.setCreatedBy(userId.toString());
		result.setSector_identifier(SECTOR_IDENTIFIER);
		return result;
	}

	@Test
	public void testCreateOAuthClient() {
		OAuthClient oauthClient = newOAuthClient();
		String id = oauthClientDao.createOAuthClient(oauthClient, SHARED_SECRET);
		assertNotNull(id);
		idsToDelete.add(id);
	}

	@Test
	public void testGetOAuthClientAndSecret() {
		OAuthClient oauthClient = newOAuthClient();
		String id = oauthClientDao.createOAuthClient(oauthClient, SHARED_SECRET);
		assertNotNull(id);
		idsToDelete.add(id);
		OAuthClient retrieved = oauthClientDao.getOAuthClient(id);
		assertEquals(oauthClient, retrieved);
		String s = oauthClientDao.getOAuthClientSecret(id);
		assertEquals(SHARED_SECRET, s);
	}
	
	@Test
	public void testDeleteOAuthClient() {
		OAuthClient oauthClient = newOAuthClient();
		String id = oauthClientDao.createOAuthClient(oauthClient, SHARED_SECRET);
		assertNotNull(id);
		idsToDelete.add(id);
		oauthClientDao.deleteOAuthClient(id);
		try {
			oauthClientDao.getOAuthClient(id);
			fail("deletion failed");
		} catch (NotFoundException e) {
			// as expected
		}
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
	public void testSectorIdentifierDboToDto() {
		//fail("Not yet implemented");
	}

	@Test
	public void testGetSectorIdentifier() {
		// fail("Not yet implemented");
	}

	@Test
	public void testDeleteSectorIdentifer() {
		// fail("Not yet implemented");
	}

	@Test
	public void testGetOAuthClientSecret() {
		// fail("Not yet implemented");
	}

	@Test
	public void testUpdateOAuthClient() {
		// fail("Not yet implemented");
	}

}
