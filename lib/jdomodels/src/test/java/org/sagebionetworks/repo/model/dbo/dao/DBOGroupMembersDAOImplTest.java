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
import org.sagebionetworks.repo.model.GroupMembers;
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
	
	List<String> groupsToDelete;

	@Before
	public void setUp() throws Exception {
		groupsToDelete = new ArrayList<String>();
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
	
	private GroupMembers initGroupMembers() throws Exception {
		List<UserGroup> users = new ArrayList<UserGroup>();
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
			users.add(user);
			groupsToDelete.add(user.getId());
		}
		GroupMembers members = new GroupMembers();
		members.setMembers(users);
		return members;
	}
	
	@Test
	public void testgetMembers() throws Exception {
		UserGroup testGroup = createTestGroup(TEST_GROUP_NAME);
		
		GroupMembers members = groupMembersDAO.getMembers(testGroup.getId());
		assertEquals("ID must match the ID passed in", testGroup.getId(), members.getId());
		assertEquals("No members initially", 0, members.getMembers().size());
	}
	
	@Test
	public void testaddMembers() throws Exception {
		GroupMembers members = initGroupMembers();
		UserGroup testGroup = createTestGroup(TEST_GROUP_NAME);
		members.setId(testGroup.getId());
		
		// Put users in then retrieve them
		groupMembersDAO.addMembers(members);
		groupMembersDAO.addMembers(members); // This should be a no-op
		GroupMembers newMembers = groupMembersDAO.getMembers(testGroup.getId());
		assertEquals("ID must match the ID passed in", testGroup.getId(), newMembers.getId());
		assertEquals("Number of users should match", NUM_USERS.longValue(), newMembers.getMembers().size());
		
		// A few fields are not filled in when creating the users, so an array comparison must be done with caution
		// Also, the ordering is not guaranteed
		for (int i = 0; i < NUM_USERS; i++) {
			newMembers.getMembers().get(i).setCreationDate(null);
			newMembers.getMembers().get(i).setUri(null);
			newMembers.getMembers().get(i).setEtag(null);
			assertTrue("All fetched members should be in the original set", members.getMembers().contains(newMembers.getMembers().get(i)));
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
		UserGroup testGroup = createTestGroup(TEST_GROUP_NAME);
		UserGroup childGroup = createTestGroup(paradoxChildGroupName);
		UserGroup grandchildGroup = createTestGroup(paradoxGrandchildGroupName);
		GroupMembers nesting = new GroupMembers();
		nesting.setMembers(new ArrayList<UserGroup>());
		nesting.getMembers().add(new UserGroup());
		
		// Add the parent group as a child to itself, which should fail
		nesting.setId(testGroup.getId());
		nesting.getMembers().get(0).setId(testGroup.getId());
		try {
			groupMembersDAO.addMembers(nesting);
			assertTrue(false);
		} catch (DatastoreException e) {
			assertTrue(e.getMessage().contains("member of itself"));
		}
		
		// Add the child group to the parent group
		nesting.setId(testGroup.getId());
		nesting.getMembers().get(0).setId(childGroup.getId());
		groupMembersDAO.addMembers(nesting);
		
		// Add the parent group as the child of the child group, which should fail
		nesting.setId(childGroup.getId());
		nesting.getMembers().get(0).setId(testGroup.getId());
		try {
			groupMembersDAO.addMembers(nesting);
			assertTrue(false);
		} catch (DatastoreException e) {
			assertTrue(e.getMessage().contains("already a parent"));
		}
		
		// Add the grandchild to the child group
		nesting.setId(childGroup.getId());
		nesting.getMembers().get(0).setId(grandchildGroup.getId());
		groupMembersDAO.addMembers(nesting);
		
		// Verify that the parent group's etag has changed
		UserGroup updatedTestGroup = userGroupDAO.get(testGroup.getId());
		assertTrue("Etag must have changed", !testGroup.getEtag().equals(updatedTestGroup.getEtag()));
		
		// Add the parent group as the child of the grandchild group, which should fail
		nesting.setId(grandchildGroup.getId());
		nesting.getMembers().get(0).setId(testGroup.getId());
		try {
			groupMembersDAO.addMembers(nesting);
			assertTrue(false);
		} catch (DatastoreException e) {
			assertTrue(e.getMessage().contains("already a parent"));
		}
	}
	
	@Test
	public void testremoveMembers() throws Exception {
		GroupMembers members = initGroupMembers();
		UserGroup testGroup = createTestGroup(TEST_GROUP_NAME);
		members.setId(testGroup.getId());
		
		// Put users in then retrieve them
		groupMembersDAO.addMembers(members);
		GroupMembers newMembers = groupMembersDAO.getMembers(testGroup.getId());
		assertEquals("ID must match the ID passed in", testGroup.getId(), newMembers.getId());
		assertEquals("Number of users should match", NUM_USERS.longValue(), newMembers.getMembers().size());
		
		// Verify that the parent group's etag has changed
		UserGroup updatedTestGroup = userGroupDAO.get(testGroup.getId());
		assertTrue("Etag must have changed", !testGroup.getEtag().equals(updatedTestGroup.getEtag()));
		
		// Remove all but one of the users from the group
		UserGroup antisocial = members.getMembers().remove(NUM_USERS -1);
		groupMembersDAO.removeMembers(members);
		GroupMembers fewerMembers = groupMembersDAO.getMembers(testGroup.getId());
		assertEquals("ID must match the ID passed in", testGroup.getId(), fewerMembers.getId());
		assertEquals("Number of users should match", 1, fewerMembers.getMembers().size());
		fewerMembers.getMembers().get(0).setCreationDate(null);
		fewerMembers.getMembers().get(0).setUri(null);
		fewerMembers.getMembers().get(0).setEtag(null);
		assertEquals("Last member should match the one removed from the DTO", antisocial, fewerMembers.getMembers().get(0));
		
		// Verify that the parent group's etag has changed
		updatedTestGroup = userGroupDAO.get(testGroup.getId());
		assertTrue("Etag must have changed", !testGroup.getEtag().equals(updatedTestGroup.getEtag()));
		
		// Remove the last guy from the group
		members.getMembers().clear();
		members.getMembers().add(antisocial);
		groupMembersDAO.removeMembers(members);
		GroupMembers emptyGroup = groupMembersDAO.getMembers(testGroup.getId());
		assertEquals("ID must match the ID passed in", testGroup.getId(), emptyGroup.getId());
		assertEquals("Number of users should match", 0, emptyGroup.getMembers().size());
		
		// Verify that the parent group's etag has changed
		updatedTestGroup = userGroupDAO.get(testGroup.getId());
		assertTrue("Etag must have changed", !testGroup.getEtag().equals(updatedTestGroup.getEtag()));
	}
	
	@Test
	public void testgetUserGroups() throws Exception {
		// Setup two groups
		String childGroupName = TEST_GROUP_NAME + "Two";
		UserGroup testGroup = createTestGroup(TEST_GROUP_NAME);
		UserGroup otherGroup = createTestGroup(childGroupName);
		
		// Add one group as the child to the other
		GroupMembers nesting = new GroupMembers();
		nesting.setId(testGroup.getId());
		nesting.setMembers(new ArrayList<UserGroup>());
		nesting.getMembers().add(new UserGroup());
		nesting.getMembers().get(0).setId(otherGroup.getId());
		groupMembersDAO.addMembers(nesting);
		
		// Get some users
		GroupMembers members = initGroupMembers();
		UserGroup oneMember = members.getMembers().get(0);
		UserGroup bothMember = members.getMembers().get(1);
		UserGroup childMember = members.getMembers().get(2);
		
		// Add one member to one group
		GroupMembers adder = new GroupMembers();
		adder.setMembers(new ArrayList<UserGroup>());
		adder.setId(testGroup.getId());
		adder.getMembers().add(oneMember);
		groupMembersDAO.addMembers(adder);
		
		// Add one member to both groups
		adder.setId(testGroup.getId());
		adder.getMembers().clear();
		adder.getMembers().add(bothMember);
		groupMembersDAO.addMembers(adder);
		adder.setId(otherGroup.getId());
		groupMembersDAO.addMembers(adder);
		
		// Add one member to just the child group
		adder.setId(otherGroup.getId());
		adder.getMembers().clear();
		adder.getMembers().add(childMember);
		groupMembersDAO.addMembers(adder);
		
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
