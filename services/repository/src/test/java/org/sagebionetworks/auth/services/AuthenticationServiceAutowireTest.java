package org.sagebionetworks.auth.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.authutil.OpenIDInfo;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.principal.UserProfileUtillity;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:authentication-services-spb.xml" })
public class AuthenticationServiceAutowireTest {
	
	private static final String OPEN_ID_TEST_EMAIL = "openId@test.org";
	private static final String OPEN_ID_TEST_ID = "https://www.google.com/accounts/o8/id?id=SOMEID";

	@Autowired
	UserManager userManger;
	
	@Autowired
	PrincipalAliasDAO principalAlaisDAO;
	
	@Autowired
	AuthenticationService authenticationService;
	
	@Autowired
	UserProfileManager userProfileManger;
	
	Long principalId;
	Long principalId2;
	
	UserInfo amdin;
	
	@Before
	public void before() throws NotFoundException{
		amdin = userManger.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		NewUser nu = new NewUser();
		nu.setEmail(OPEN_ID_TEST_EMAIL);
		nu.setUserName("openIdTestUser");
		principalId = userManger.createUser(nu);
	}
	

	@After
	public void after(){
		if(principalId != null){
			try {
				userManger.deletePrincipal(amdin, principalId);
			} catch (NotFoundException e) {}
		}
		if(principalId2 != null){
			try {
				userManger.deletePrincipal(amdin, principalId2);
			} catch (NotFoundException e) {}
		}
	}
	/**
	 * This test should be updated when PLFM-2437 is addressed.
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * 
	 */
	@Test
	public void testPLFM_2498() throws DatastoreException, UnauthorizedException, NotFoundException{
		// Before we start the user should not have an openId, but they should have an Email
		UserProfile profile = userProfileManger.getUserProfile(amdin, principalId.toString());
		assertNotNull(profile);
		assertEquals(null, profile.getOpenIds());
		// Now the user should be able to login with open ID because we look them up by email.
		// After doing so their openID should be bound to the user.
		OpenIDInfo openIdInfo = new OpenIDInfo();
		openIdInfo.setEmail(OPEN_ID_TEST_EMAIL);
		openIdInfo.setIdentifier(OPEN_ID_TEST_ID);
		// We should be able to login
		Session session = authenticationService.processOpenIDInfo(openIdInfo, DomainType.SYNAPSE);
		assertNotNull(session);
		// The open ID should now be bound to the user's profile
		profile = userProfileManger.getUserProfile(amdin, principalId.toString());
		assertNotNull(profile);
		assertNotNull(profile.getOpenIds());
		assertEquals(1, profile.getOpenIds().size());
		assertEquals(OPEN_ID_TEST_ID, profile.getOpenIds().get(0));
		
		// now if we log in again, it should not change.
		session = authenticationService.processOpenIDInfo(openIdInfo, DomainType.SYNAPSE);
		assertNotNull(session);
		// The open ID should now be bound to the user's profile
		profile = userProfileManger.getUserProfile(amdin, principalId.toString());
		assertNotNull(profile);
		assertNotNull(profile.getOpenIds());
		assertEquals(1, profile.getOpenIds().size());
		assertEquals(OPEN_ID_TEST_ID, profile.getOpenIds().get(0));
	}
	
	@Test
	public void testPLFM_2511() throws NotFoundException{
		NewUser nu = new NewUser();
		nu.setEmail("user123@test.org");
		// Create a user with temporary Username
		nu.setUserName(UserProfileUtillity.createTempoaryUserName(123));
		principalId2 = userManger.createUser(nu);
		// Now try to loging with open ID
		OpenIDInfo openIdInfo = new OpenIDInfo();
		openIdInfo.setEmail(nu.getEmail());
		openIdInfo.setIdentifier("https://www.google.com/accounts/o8/id?id=S123");
		// We should be able to login
		Session session = authenticationService.processOpenIDInfo(openIdInfo, DomainType.SYNAPSE);
		assertNotNull(session);
	}
}
