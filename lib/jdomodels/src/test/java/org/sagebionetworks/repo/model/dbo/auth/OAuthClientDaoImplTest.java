package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
class OAuthClientDaoImplTest {
	
	private static final String SHARED_SECRET ="a secret";
	
	private List<String> idsToDelete;
	
	@Autowired
	private OAuthClientDao oauthClientDao;

	@BeforeEach
	void setUp() throws Exception {
		idsToDelete = new ArrayList<String>();
	}

	@AfterEach
	void tearDown() throws Exception {
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
		return result;
	}

	@Test
	void testCreateOAuthClient() {
		OAuthClient oauthClient = newOAuthClient();
		String id = oauthClientDao.createOAuthClient(oauthClient, SHARED_SECRET);
		assertNotNull(id);
		idsToDelete.add(id);
	}

	@Test
	void testGetOAuthClientAndSecret() {
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
	void testDeleteOAuthClient() {
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
	void testClientDboToDto() {
		//fail("Not yet implemented");
	}

	@Test
	void testClientDtoToDbo() {
		//fail("Not yet implemented");
	}

	@Test
	void testListOAuthClients() {
		//fail("Not yet implemented");
	}

	@Test
	void testSectorIdentifierDboToDto() {
		//fail("Not yet implemented");
	}

	@Test
	void testGetSectorIdentifier() {
		// fail("Not yet implemented");
	}

	@Test
	void testDeleteSectorIdentifer() {
		// fail("Not yet implemented");
	}

	@Test
	void testGetOAuthClientSecret() {
		// fail("Not yet implemented");
	}

	@Test
	void testUpdateOAuthClient() {
		// fail("Not yet implemented");
	}

}
