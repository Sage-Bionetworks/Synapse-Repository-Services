package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.Credential;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })

public class DBOAuthenticationDAOImplTest {
	
	@Autowired
	private AuthenticationDAO authDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private DBOBasicDao basicDAO;
		
	List<String> groupsToDelete;
	
	private static final String GROUP_NAME = "auth-test-group";
	private Credential credential;
	private DBOCredential secretRow;

	@Before
	public void setUp() throws Exception {
		groupsToDelete = new ArrayList<String>();
		
		// Initialize a UserGroup
		UserGroup ug = userGroupDAO.findGroup(GROUP_NAME, true);
		if (ug == null) {
			ug = new UserGroup();
			ug.setName(GROUP_NAME);
			ug.setIsIndividual(true);
			ug.setId(userGroupDAO.create(ug));
		}
		groupsToDelete.add(ug.getId());
		Long principalId = Long.parseLong(ug.getId());
		
		// Construct the username/password combo
		credential = new Credential();
		credential.setEmail(GROUP_NAME);
		credential.setPassHash("{PKCS5S2}1234567890abcdefghijklmnopqrstuvwxyz");

		// Make a row of Credentials but don't insert it yet
		secretRow = new DBOCredential();
		secretRow.setPrincipalId(principalId);
		secretRow.setValidatedOn(new Date());
		secretRow.setSessionToken("Hsssssss...");
		secretRow.setPassHash(credential.getPassHash());
		secretRow.setSecretKey("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
	}

	@After
	public void tearDown() throws Exception {
		for (String toDelete: groupsToDelete) {
			userGroupDAO.delete(toDelete);
		}
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testAuthenticateBadPassword() throws Exception {
		basicDAO.createNew(secretRow);
		
		credential.setPassHash("INVALID");
		authDAO.authenticate(credential);
		fail();
	}
	
	@Test
	public void testAuthenticateValidSession() throws Exception {
		// Test fast!  Only one second before expiration!
		Date almostExpired = secretRow.getValidatedOn();
		almostExpired.setTime(almostExpired.getTime() - DBOAuthenticationDAOImpl.SESSION_EXPIRATION_TIME + 1);
		basicDAO.createNew(secretRow);
		
		Session session = authDAO.authenticate(credential);
		assertEquals(secretRow.getSessionToken(), session.getSessionToken());
	}
	
	@Test
	public void testAuthenticateNoSession() throws Exception {
		secretRow.setSessionToken(null);
		basicDAO.createNew(secretRow);
		
		Session session = authDAO.authenticate(credential);
		assertNotNull(session.getSessionToken());
	}
	
	@Test
	public void testAuthenticateTerminatedSession() throws Exception {
		basicDAO.createNew(secretRow);
		authDAO.deleteSessionToken(secretRow.getSessionToken());
		
		Session session = authDAO.authenticate(credential);
		assertNotNull(session.getSessionToken());
	}
	
	@Test
	public void testAuthenticationAfterOneDay() throws Exception {
		// The token is still valid for 1 second!
		Date almostExpired = secretRow.getValidatedOn();
		almostExpired.setTime(almostExpired.getTime() - DBOAuthenticationDAOImpl.SESSION_EXPIRATION_TIME + 1);
		basicDAO.createNew(secretRow);
		
		Thread.sleep(1500);
		
		// Oh no, the token has gone bad!
		Session session = authDAO.authenticate(credential);
		assertFalse(secretRow.getSessionToken().equals(session.getSessionToken()));
	}
	
	@Test
	public void testGetPrincipal() throws Exception {
		basicDAO.createNew(secretRow);
		Long id = authDAO.getPrincipal(secretRow.getSessionToken());
		assertEquals(secretRow.getPrincipalId(), id);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testChangePassword() throws Exception {
		// The original credentials should authenticate correctly
		basicDAO.createNew(secretRow);
		authDAO.authenticate(credential);
		
		// Change the password and try to authenticate again
		// This time it should fail
		authDAO.changePassword(secretRow.getPrincipalId().toString(), "Bibbity Boppity BOO!");
		authDAO.authenticate(credential);
		fail();
	}
	
	@Test
	public void testSecretKey() throws Exception {
		String userId = secretRow.getPrincipalId().toString();
		basicDAO.createNew(secretRow);
		
		// Getter should work
		assertEquals(secretRow.getSecretKey(), authDAO.getSecretKey(userId));
		
		// Setter should work
		authDAO.changeSecretKey(userId);
		assertFalse(secretRow.getSecretKey().equals(authDAO.getSecretKey(userId)));
	}
}
