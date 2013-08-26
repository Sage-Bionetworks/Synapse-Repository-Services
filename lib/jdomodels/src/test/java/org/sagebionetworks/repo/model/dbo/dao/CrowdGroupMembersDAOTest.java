package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.User;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.tools.javac.util.Pair;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })

public class CrowdGroupMembersDAOTest {
	
	@Autowired
	private GroupMembersDAO crowdGroupMembersDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;

	private static final Log log = LogFactory.getLog(CrowdGroupMembersDAOTest.class);
	private static final String TEST_GROUP_NAME = "testGroupOfDOOOOM";
	private static final Integer NUM_USERS = 3; // Need at least 2 for testgetUserGroups()
	
	UserGroup testGroup;
	List<UserGroup> testUsers;
	
	// Cleanup
	List<Pair<String, String> > groupsToDelete; // Group name and ID, respectively
	List<String> usersToDeleteFromCrowd;

	@Before
	public void setUp() throws Exception {
		groupsToDelete = new ArrayList<Pair<String, String> >();
		usersToDeleteFromCrowd = new ArrayList<String>();
		
		testGroup = createTestGroup(TEST_GROUP_NAME);
		
		// Make some users
		testUsers = new ArrayList<UserGroup>();
		for (int i = 0; i < NUM_USERS; i++) {
			String username = "Bogus@email.com"+i;
			// Add some users to the DB
			UserGroup user = new UserGroup();
			user.setName(username);
			user.setIsIndividual(true);
			try {
				user.setId(userGroupDAO.create(user)); 
			} catch (DatastoreException e) {
				user.setId(userGroupDAO.findGroup(username, true).getId());
			}
			testUsers.add(user);
			groupsToDelete.add(new Pair<String, String>(user.getName(), user.getId()));
			
			// Add those users to Crowd
			User crowdUser = new User();
			crowdUser.setFirstName("bogus");
			crowdUser.setLastName("bogus");
			crowdUser.setDisplayName(username);
			crowdUser.setEmail(username);
			crowdUser.setPassword("super secure password");
			try {
				CrowdAuthUtil.createUser(crowdUser);
			} catch (AuthenticationException e) {
				if (e.getRespStatus() == 400) {
					// User already present
				} else {
					throw e;
				}
			}
			usersToDeleteFromCrowd.add(crowdUser.getEmail());
		}
	}

	@After
	public void tearDown() throws Exception {
		if(groupsToDelete != null) {
			for(Pair<String, String> todelete: groupsToDelete){
				deleteTestGroup(todelete.fst, todelete.snd);
			}
		}
		if(usersToDeleteFromCrowd != null){
			for(String todelete: usersToDeleteFromCrowd) {
				CrowdAuthUtil.deleteUser(todelete);
			}
		}
	}
	
	public UserGroup createTestGroup(String name) throws Exception {
		// Create the test-group in Crowd and local DB 
		try {
			CrowdAuthUtil.createGroup(name);
		} catch (AuthenticationException e) {
			if (e.getRespStatus() == 400) {
				// Good, the group already exists
			} else {
				logException(e);
				throw e;
			}
		}
		
		UserGroup group = new UserGroup();
		group.setName(name);
		group.setIsIndividual(false);
		String id = null;
		try {
			id = userGroupDAO.create(group);
		} catch (DatastoreException e) {
			// Already exists
			id = userGroupDAO.findGroup(name, false).getId();
		}
		assertNotNull(id);
		groupsToDelete.add(new Pair<String, String>(name, id));
		return userGroupDAO.get(id);
	}
	
	private void deleteTestGroup(String name, String id) throws Exception {
		try {
			CrowdAuthUtil.deleteGroup(name);
		} catch (AuthenticationException e) {
			if (e.getRespStatus() == 404) {
				// Good, the group doesn't exist
			} else {
				logException(e);
				throw e;
			}
		}
		
		try {
			userGroupDAO.delete(id);
		} catch (NotFoundException e) {
			// Good, not in DB
		}
	}
	
	private void logException(Throwable e) {
		log.debug(e.getMessage());
		log.debug(e.getCause().getMessage());
		for (Throwable foo : e.getSuppressed()) {
			log.debug(foo.getMessage());
		}
	}
	
	@Test
	public void testGetMembers() throws Exception {
		List<UserGroup> members = crowdGroupMembersDAO.getMembers(testGroup.getId());
		assertEquals("No members initially", 0, members.size());
		
		members = crowdGroupMembersDAO.getMembers(testGroup.getId(), true);
		assertEquals("No members initially", 0, members.size());
	}
	
	@Test
	public void testAddMembers() throws Exception {
		// Put users into Crowd then retrieve them
		List<String> adder = new ArrayList<String>();
		adder.add(testUsers.get(0).getId());
		crowdGroupMembersDAO.addMembers(testGroup.getId(), adder);
		adder.set(0, testUsers.get(1).getId());
		crowdGroupMembersDAO.addMembers(testGroup.getId(), adder);
		adder.set(0, testUsers.get(2).getId());
		crowdGroupMembersDAO.addMembers(testGroup.getId(), adder);
		List<UserGroup> newMembers = crowdGroupMembersDAO.getMembers(testGroup.getId());
		assertEquals("Number of users should match", NUM_USERS.longValue(), newMembers.size());
		
		// A few fields are not filled in when creating the users, so an array comparison must be done with caution
		// Also, the ordering is not guaranteed
		for (int i = 0; i < NUM_USERS; i++) {
			newMembers.get(i).setCreationDate(null);
			newMembers.get(i).setUri(null);
			newMembers.get(i).setEtag(null);
			assertTrue("All fetched members should be in the original set", testUsers.contains(newMembers.get(i)));
		}
	}
	
	@Test
	public void testRemoveMembers() throws Exception {
		// Put users into Crowd then retrieve them
		List<String> adder = new ArrayList<String>();
		adder.add(testUsers.get(0).getId());
		crowdGroupMembersDAO.addMembers(testGroup.getId(), adder);
		adder.set(0, testUsers.get(1).getId());
		crowdGroupMembersDAO.addMembers(testGroup.getId(), adder);
		adder.set(0, testUsers.get(2).getId());
		crowdGroupMembersDAO.addMembers(testGroup.getId(), adder);
		List<UserGroup> newMembers = crowdGroupMembersDAO.getMembers(testGroup.getId());
		assertEquals("Number of users should match", NUM_USERS.longValue(), newMembers.size());
		
		// Remove all but one of the users from the group
		List<String> remover = new ArrayList<String>();
		remover.add(testUsers.get(0).getId());
		crowdGroupMembersDAO.removeMembers(testGroup.getId(), remover);
		remover.set(0, testUsers.get(1).getId());
		crowdGroupMembersDAO.removeMembers(testGroup.getId(), remover);
		List<UserGroup> fewerMembers = crowdGroupMembersDAO.getMembers(testGroup.getId());
		assertEquals("Number of users should match", 1, fewerMembers.size());
		fewerMembers.get(0).setCreationDate(null);
		fewerMembers.get(0).setUri(null);
		fewerMembers.get(0).setEtag(null);
		assertEquals("Last member should match the one removed from the DTO", testUsers.get(2), fewerMembers.get(0));
		
		// Remove the last guy from the group
		remover.set(0, testUsers.get(2).getId());
		crowdGroupMembersDAO.removeMembers(testGroup.getId(), remover);
		List<UserGroup> emptyGroup = crowdGroupMembersDAO.getMembers(testGroup.getId());
		assertEquals("Number of users should match", 0, emptyGroup.size());
	}
	
	@Test
	public void testGetUserGroups() throws Exception {
		// Setup two groups
		String childGroupName = TEST_GROUP_NAME + "Two";
		UserGroup testGroup = createTestGroup(TEST_GROUP_NAME);
		UserGroup otherGroup = createTestGroup(childGroupName);
		List<String> adder = new ArrayList<String>();
		
		// Get some users
		UserGroup oneMember = testUsers.get(0);
		UserGroup bothMember = testUsers.get(1);
		
		// Add one member to one group
		adder.add(oneMember.getId());
		crowdGroupMembersDAO.addMembers(testGroup.getId(), adder);
		
		// Add one member to both groups
		adder.clear();
		adder.add(bothMember.getId());
		crowdGroupMembersDAO.addMembers(testGroup.getId(), adder);
		crowdGroupMembersDAO.addMembers(otherGroup.getId(), adder);
		
		// The first user should now ONLY belong in the parent group
		// PUBLIC and AUTH_USERS are managed the level above the DAO
		List<UserGroup> parentGroup = crowdGroupMembersDAO.getUsersGroups(oneMember.getId());
		assertEquals("Number of groups should be 1", 1, parentGroup.size());
		assertEquals("Group should have the correct name", TEST_GROUP_NAME, parentGroup.get(0).getName());
		
		// The second user should belong to two groups
		List<UserGroup> twoGroup = crowdGroupMembersDAO.getUsersGroups(bothMember.getId());
		assertEquals("Number of groups should be 2", 2, twoGroup.size());
		assertTrue("List should contain both groups", 
				(twoGroup.get(0).getName().equals(TEST_GROUP_NAME) && twoGroup.get(1).getName().equals(childGroupName))
				^ (twoGroup.get(0).getName().equals(childGroupName) && twoGroup.get(1).getName().equals(TEST_GROUP_NAME)));
	}

}
