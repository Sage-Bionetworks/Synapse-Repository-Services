package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })

public class DBOGroupMembersDAOImplTest {
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;

	private static final String TEST_GROUP_NAME = "testGroupOfDOOOOM";
	private static final Integer NUM_USERS = 3; // Need at least 2 for testgetUserGroups()
	
	UserGroup testGroup;
	List<UserGroup> testUsers;
	
	// Cleanup
	List<String> groupsToDelete;

	@Before
	public void setUp() throws Exception {
		groupsToDelete = new ArrayList<String>();
		
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
			groupsToDelete.add(user.getId());
		}
	}

	@After
	public void tearDown() throws Exception {
		if(groupsToDelete != null) {
			for(String todelete: groupsToDelete){
				try {
					userGroupDAO.delete(todelete);
				} catch (NotFoundException e) {
					// Good, not in DB
				}
			}
		}
	}
	
	private UserGroup createTestGroup(String name) throws Exception {		
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
		groupsToDelete.add(id);
		return userGroupDAO.get(id);
	}
	
	@Test
	public void testGetMembers() throws Exception {
		List<UserGroup> members = groupMembersDAO.getMembers(testGroup.getId());
		assertEquals("No members initially", 0, members.size());
		
		members = groupMembersDAO.getMembers(testGroup.getId(), true);
		assertEquals("No nested members initially", 0, members.size());
	}
	
	@Test
	public void testAddMembers() throws Exception {
		// Put users in then retrieve them
		List<String> adder = new ArrayList<String>();
		for (UserGroup user : testUsers) {
			adder.add(user.getId());
		}
		groupMembersDAO.addMembers(testGroup.getId(), adder);
		groupMembersDAO.addMembers(testGroup.getId(), adder); // This should be a no-op
		List<UserGroup> newMembers = groupMembersDAO.getMembers(testGroup.getId());
		assertEquals("Number of users should match", NUM_USERS.longValue(), newMembers.size());
		
		// A few fields are not filled in when creating the users, so an array comparison must be done with caution
		// Also, the ordering is not guaranteed
		for (int i = 0; i < NUM_USERS; i++) {
			newMembers.get(i).setCreationDate(null);
			newMembers.get(i).setUri(null);
			newMembers.get(i).setEtag(null);
			assertTrue("All fetched members should be in the original set", testUsers.contains(newMembers.get(i)));
		}
		
		// Verify that the parent group's etag has changed
		UserGroup updatedTestGroup = userGroupDAO.get(testGroup.getId());
		assertTrue("Etag must have changed", !testGroup.getEtag().equals(updatedTestGroup.getEtag()));
	}
	
	@Test
	public void testCircularInsertion() throws Exception {
		// Setup three groups
		String paradoxChildGroupName = TEST_GROUP_NAME + "Two";
		String paradoxGrandchildGroupName = TEST_GROUP_NAME + "Three";
		// UserGroup 'testGroup' is created in setup
		UserGroup childGroup = createTestGroup(paradoxChildGroupName);
		UserGroup grandchildGroup = createTestGroup(paradoxGrandchildGroupName);
		
		List<String> adder = new ArrayList<String>();
		
		// Add the parent group as a child to itself, which should fail
		adder.add(testGroup.getId());
		try {
			groupMembersDAO.addMembers(testGroup.getId(), adder);
			assertTrue(false);
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("already a child"));
		}
		
		// Add the child group to the parent group
		adder.set(0, childGroup.getId());
		groupMembersDAO.addMembers(testGroup.getId(), adder);
		
		// Add the parent group as the child of the child group, which should fail
		adder.set(0, testGroup.getId());
		try {
			groupMembersDAO.addMembers(childGroup.getId(), adder);
			assertTrue(false);
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("already a child"));
		}
		
		// Add the grandchild to the child group
		adder.set(0, grandchildGroup.getId());
		groupMembersDAO.addMembers(childGroup.getId(), adder);
		
		// Add the parent group as the child of the grandchild group, which should fail
		adder.set(0, testGroup.getId());
		try {
			groupMembersDAO.addMembers(grandchildGroup.getId(), adder);
			assertTrue(false);
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("already a child"));
		}
	}
	
	@Test
	public void testRemoveMembers() throws Exception {
		// Put users in then retrieve them
		List<String> adder = new ArrayList<String>();
		for (UserGroup user : testUsers) {
			adder.add(user.getId());
		}
		groupMembersDAO.addMembers(testGroup.getId(), adder);
		List<UserGroup> newMembers = groupMembersDAO.getMembers(testGroup.getId());
		assertEquals("Number of users should match", NUM_USERS.longValue(), newMembers.size());
		
		// Verify that the parent group's etag has changed
		UserGroup updatedTestGroup = userGroupDAO.get(testGroup.getId());
		assertTrue("Etag must have changed", !testGroup.getEtag().equals(updatedTestGroup.getEtag()));
		
		// Remove all but one of the users from the group
		List<String> remover = new ArrayList<String>(adder);
		String antisocial = remover.remove(0);
		groupMembersDAO.removeMembers(testGroup.getId(), remover);
		List<UserGroup> fewerMembers = groupMembersDAO.getMembers(testGroup.getId());
		assertEquals("Number of users should match", 1, fewerMembers.size());
		fewerMembers.get(0).setCreationDate(null);
		fewerMembers.get(0).setUri(null);
		fewerMembers.get(0).setEtag(null);
		assertEquals("Last member should match the one removed from the DTO", antisocial, fewerMembers.get(0).getId());
		
		// Verify that the parent group's etag has changed
		updatedTestGroup = userGroupDAO.get(testGroup.getId());
		assertTrue("Etag must have changed", !testGroup.getEtag().equals(updatedTestGroup.getEtag()));
		
		// Remove the last guy from the group
		remover.clear();
		remover.add(antisocial);
		groupMembersDAO.removeMembers(testGroup.getId(), remover);
		List<UserGroup> emptyGroup = groupMembersDAO.getMembers(testGroup.getId());
		assertEquals("Number of users should match", 0, emptyGroup.size());
		
		// Verify that the parent group's etag has changed
		updatedTestGroup = userGroupDAO.get(testGroup.getId());
		assertTrue("Etag must have changed", !testGroup.getEtag().equals(updatedTestGroup.getEtag()));
	}
	
	@Test
	public void testGetUserGroups() throws Exception {
		// Setup two groups
		String childGroupName = TEST_GROUP_NAME + "Two";
		// UserGroup 'testGroup' is created in setup
		UserGroup otherGroup = createTestGroup(childGroupName);
		
		List<String> adder = new ArrayList<String>();
		
		// Add one group as the child to the other
		adder.add(otherGroup.getId());
		groupMembersDAO.addMembers(testGroup.getId(), adder);
		
		// Get some users
		UserGroup oneMember   = testUsers.get(0);
		UserGroup bothMember  = testUsers.get(1);
		UserGroup childMember = testUsers.get(2);
		
		// Add one member to one group
		adder.set(0, oneMember.getId());
		groupMembersDAO.addMembers(testGroup.getId(), adder);
		
		// Add one member to both groups
		adder.set(0, bothMember.getId());
		groupMembersDAO.addMembers(testGroup.getId(), adder);
		groupMembersDAO.addMembers(otherGroup.getId(), adder);
		
		// Add one member to just the child group
		adder.set(0, childMember.getId());
		groupMembersDAO.addMembers(otherGroup.getId(), adder);
		
		// The first user should now ONLY belong in the parent group
		// PUBLIC and AUTH_USERS are managed the level above the DAO
		List<UserGroup> parentGroup = groupMembersDAO.getUsersGroups(oneMember.getId());
		assertEquals("Number of groups should be 1", 1, parentGroup.size());
		assertEquals("Group should have the correct name", TEST_GROUP_NAME, parentGroup.get(0).getName());
		
		// The second user should belong to two groups
		List<UserGroup> twoGroup = groupMembersDAO.getUsersGroups(bothMember.getId());
		assertEquals("Number of groups should be 2", 2, twoGroup.size());
		assertTrue("List should contain both groups", 
				(twoGroup.get(0).getName().equals(TEST_GROUP_NAME) && twoGroup.get(1).getName().equals(childGroupName))
				^ (twoGroup.get(0).getName().equals(childGroupName) && twoGroup.get(1).getName().equals(TEST_GROUP_NAME)));
		
		// The third user should belong to two groups
		List<UserGroup> childGroup = groupMembersDAO.getUsersGroups(childMember.getId());
		assertEquals("Number of groups should be 2", 2, childGroup.size());
		assertTrue("List should contain both groups", 
				(childGroup.get(0).getName().equals(TEST_GROUP_NAME) && childGroup.get(1).getName().equals(childGroupName))
				^ (childGroup.get(0).getName().equals(childGroupName) && childGroup.get(1).getName().equals(TEST_GROUP_NAME)));
	}

}
