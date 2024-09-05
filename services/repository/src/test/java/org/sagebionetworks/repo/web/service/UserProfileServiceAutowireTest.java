package org.sagebionetworks.repo.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER;
import static org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP;
import static org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP;
import static org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO;
import org.sagebionetworks.repo.service.UserProfileService;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.controller.AbstractAutowiredControllerTestBase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
public class UserProfileServiceAutowireTest extends AbstractAutowiredControllerTestBase {
	
	@Autowired
	private UserManager userManger;
	@Autowired
	private UserProfileService userProfileService;
	@Autowired
	private PrincipalPrefixDAO principalPrefixDAO;
	
	private List<Long> principalsToDelete;

	Long principalOne;
	Long principalTwo;
	Long principalThree;
	UserInfo admin;

	@BeforeEach
	public void before() throws NotFoundException{
		principalPrefixDAO.truncateTable();
		principalsToDelete = new LinkedList<Long>();
		// Get the admin info
		admin = userManger.getUserInfo(THE_ADMIN_USER.getPrincipalId());
		// Create two users
		
		// Create a user Profile
		NewUser nu = new NewUser();
		nu.setFirstName("James");
		nu.setLastName("Bond");
		nu.setUserName("007");
		nu.setEmail("superSpy@Spies.org");
		principalOne = userManger.createUser(nu);
		// In the wild a worker will add these alais.
		principalsToDelete.add(principalOne);
		principalPrefixDAO.addPrincipalAlias(nu.getUserName(), principalOne);
		principalPrefixDAO.addPrincipalName(nu.getFirstName(), nu.getLastName(), principalOne);
		
		// Create another profile
		nu = new NewUser();
		nu.setFirstName("Jack");
		nu.setLastName("Black");
		nu.setUserName("random");
		nu.setEmail("super@duper.org");
		principalTwo = userManger.createUser(nu);
		// In the wild a worker will add these alais.
		principalsToDelete.add(principalTwo);
		principalPrefixDAO.addPrincipalAlias(nu.getUserName(), principalTwo);
		principalPrefixDAO.addPrincipalName(nu.getFirstName(), nu.getLastName(), principalTwo);
		
		// Create another profile
		nu = new NewUser();
		nu.setFirstName("Cate");
		nu.setLastName("Archer");
		nu.setUserName("cate001");
		nu.setEmail("cate@Spies.org");
		principalThree = userManger.createUser(nu);
		// In the wild a worker will add these alais.
		principalsToDelete.add(principalThree);
		principalPrefixDAO.addPrincipalAlias(nu.getUserName(), principalThree);
		principalPrefixDAO.addPrincipalName(nu.getFirstName(), nu.getLastName(), principalThree);
		
		// Add some groups
		principalPrefixDAO.addPrincipalAlias(AUTHENTICATED_USERS_GROUP.name(), AUTHENTICATED_USERS_GROUP.getPrincipalId());
		principalPrefixDAO.addPrincipalAlias(PUBLIC_GROUP.name(), PUBLIC_GROUP.getPrincipalId());
	}
	
	@AfterEach
	public void after(){
		if(principalsToDelete != null){
			for(Long id: principalsToDelete){
				try {
					userManger.deletePrincipal(admin, id);
				} catch (Exception e) {}
			}
		}
	}
	
	@Test
	public void testPrivateEmailAndOpenId() throws NumberFormatException, DatastoreException, UnauthorizedException, NotFoundException{
		// Each user can see the others profile
		UserProfile profile =userProfileService.getUserProfileByOwnerId(principalOne, principalTwo.toString());
		assertNotNull(profile);
		assertEquals(principalTwo.toString(), profile.getOwnerId());
		assertEquals(null, profile.getEmail());
		assertEquals(null, profile.getEmails());
		assertEquals(null, profile.getOpenIds());
		assertEquals("random", profile.getUserName());
		// We should be able see our own data
		profile =userProfileService.getUserProfileByOwnerId(principalTwo, principalTwo.toString());
		assertNotNull(profile);
		assertEquals(principalTwo.toString(), profile.getOwnerId());
		List<String> expected = new LinkedList<String>();
		expected.add("super@duper.org");
		assertEquals(expected, profile.getEmails(), "A user must be able to see their own email's");
	}
	
	@Test
	public void testHeaders() throws DatastoreException, NotFoundException{
		UserGroupHeaderResponsePage ughrp = userProfileService.getUserGroupHeadersByPrefix("j", null, 0, Integer.MAX_VALUE);
		assertNotNull(ughrp);
		assertNotNull(ughrp.getChildren());
		assertTrue(ughrp.getChildren().size() >= 2);
		// Get the ID of all results
		Set<Long> resultSet = new HashSet<Long>();
		for(UserGroupHeader ugh: ughrp.getChildren()){
			Long ownerId = Long.parseLong(ugh.getOwnerId());
			resultSet.add(ownerId);
			assertEquals(null, ugh.getEmail());
			assertEquals(null, ugh.getEmail());
			if(principalOne.equals(ownerId)){
				assertEquals("James", ugh.getFirstName());
				assertEquals("Bond", ugh.getLastName());
				assertEquals("007", ugh.getUserName());
			}
		}
		
		assertTrue(resultSet.contains(principalOne));
		assertTrue(resultSet.contains(principalTwo));
	}

	@Test
	public void testGetUserGroupHeadersNoFilter() throws ServletException, IOException, DatastoreException, NotFoundException {
		UserGroupHeaderResponsePage ughrp = userProfileService.getUserGroupHeadersByPrefix("", null, 0, Integer.MAX_VALUE);
		assertNotNull(ughrp);
		List<UserGroupHeader> children = ughrp.getChildren();
		assertNotNull(children);
		
		assertTrue(ughrp.getChildren().size() >= 2);

		Set<Long> resultSet = new HashSet<Long>();	
		for (UserGroupHeader ugh : children) {
			Long ownerId = Long.parseLong(ugh.getOwnerId());
			resultSet.add(ownerId);
		}
		// spot check: should find first 15 alphabetical names
		assertTrue(resultSet.contains(principalOne));
		assertTrue(resultSet.contains(principalTwo));
		assertTrue(resultSet.contains(principalThree));
		assertTrue(resultSet.contains(AUTHENTICATED_USERS_GROUP.getPrincipalId()));
		assertTrue(resultSet.contains(PUBLIC_GROUP.getPrincipalId()));
	}
	

	@Test
	public void testGetUserGroupHeadersWithFilterUsername() throws ServletException, IOException, DatastoreException, NotFoundException {
		UserGroupHeaderResponsePage ughrp = userProfileService.getUserGroupHeadersByPrefix("cate001",null, 0, Integer.MAX_VALUE);
		assertNotNull(ughrp);
		assertNotNull(ughrp.getChildren());		
		assertTrue(ughrp.getChildren().size() == 1);
		assertEquals(principalThree.toString(), ughrp.getChildren().get(0).getOwnerId());
	}

	
	@Test
	public void testGetUserGroupHeadersWithFilterByLastName() throws ServletException, IOException, DatastoreException, NotFoundException {
		UserGroupHeaderResponsePage ughrp = userProfileService.getUserGroupHeadersByPrefix("B",null, 0, Integer.MAX_VALUE);
		assertNotNull(ughrp);
		List<UserGroupHeader> children = ughrp.getChildren();
		assertNotNull(children);
		
		assertTrue(ughrp.getChildren().size() >= 2);
		// Get the ID of all results
		Set<Long> resultSet = new HashSet<Long>();
		for(UserGroupHeader ugh: ughrp.getChildren()){
			Long ownerId = Long.parseLong(ugh.getOwnerId());
			resultSet.add(ownerId);
		}
		
		assertTrue(resultSet.contains(principalOne));
		assertTrue(resultSet.contains(principalTwo));
	}
	
	@Test
	public void testGetUserGroupHeadersByIds() throws DatastoreException, NotFoundException{
		// Request both users and groups.
		List<Long> request = new LinkedList<Long>();
		request.add(THE_ADMIN_USER.getPrincipalId());
		request.add(PUBLIC_GROUP.getPrincipalId());
		request.add(AUTHENTICATED_USERS_GROUP.getPrincipalId());
		request.add(ANONYMOUS_USER.getPrincipalId());
		request.add(principalThree);
		// request
		UserGroupHeaderResponsePage ughrp = userProfileService.getUserGroupHeadersByIds(request);
		assertNotNull(ughrp);
		List<UserGroupHeader> children = ughrp.getChildren();
		System.out.println(children.toString());
		assertNotNull(children);
		assertEquals(5, children.size());
		// They should be in the same order as requested
		// admin user
		UserGroupHeader header = children.get(0);
		assertEquals(true, header.getIsIndividual());
		assertEquals(THE_ADMIN_USER.getPrincipalId().toString(), header.getOwnerId());
		assertNotNull(header.getUserName());
		// We no longer give out emails.
		assertEquals(null, header.getEmail());
		// public group
		header = children.get(1);
		assertEquals(false, header.getIsIndividual());
		assertEquals(PUBLIC_GROUP.getPrincipalId().toString(), header.getOwnerId());
		assertNotNull(header.getUserName());
		assertEquals(null, header.getFirstName());
		assertEquals(null, header.getLastName());
		// We no longer give out emails.
		assertEquals(null, header.getEmail());
		// Authenticated users group
		header = children.get(2);
		assertEquals(false, header.getIsIndividual());
		assertEquals(AUTHENTICATED_USERS_GROUP.getPrincipalId().toString(), header.getOwnerId());
		assertNotNull(header.getUserName());
		assertEquals(null, header.getFirstName());
		assertEquals(null, header.getLastName());
		// We no longer give out emails.
		assertEquals(null, header.getEmail());
		// ANONYMOUS_USERs
		header = children.get(3);
		assertEquals(true, header.getIsIndividual());
		assertEquals(ANONYMOUS_USER.getPrincipalId().toString(), header.getOwnerId());
		assertNotNull(header.getUserName());
		// We no longer give out emails.
		assertEquals(null, header.getEmail());
		// Cate Archer
		header = children.get(4);
		assertEquals(true, header.getIsIndividual());
		assertEquals(principalThree.toString(), header.getOwnerId());
		assertEquals("cate001", header.getUserName());
		assertEquals("Cate", header.getFirstName());
		assertEquals("Archer", header.getLastName());
		// We no longer give out emails.
		assertEquals(null, header.getEmail());
	}
}
