package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.NamedIdGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
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
	
	@Autowired
	private DBOBasicDao basicDAO;
	
	@Autowired
	private NamedIdGenerator idGenerator;
	
	private List<String> groupsToDelete;
	
	private UserGroup testGroup;
	private UserGroup testUserOne;
	private UserGroup testUserTwo;
	private UserGroup testUserThree;

	@Before
	public void setUp() throws Exception {
		groupsToDelete = new ArrayList<String>();
		
		testGroup = createTestGroup(false);
		testUserOne = createTestGroup(true);
		testUserTwo = createTestGroup(true);
		testUserThree = createTestGroup(true);
	}

	@After
	public void tearDown() throws Exception {
		for (String toDelete : groupsToDelete) {
			try {
				userGroupDAO.delete(toDelete);
			} catch (NotFoundException e) {
				// Good, not in DB
			}
		}
	}
	
	private UserGroup createTestGroup(boolean isIndividual) throws Exception {		
		UserGroup group = new UserGroup();
		group.setIsIndividual(isIndividual);
		String id = userGroupDAO.create(group).toString();
		assertNotNull(id);
		groupsToDelete.add(id);
		return userGroupDAO.get(Long.parseLong(id));
	}
	
	@Test
	public void testGetters() throws Exception {
		List<UserGroup> members = groupMembersDAO.getMembers(testGroup.getId());
		assertEquals("No members initially", 0, members.size());
		
		List<UserGroup> groups = groupMembersDAO.getUsersGroups(testUserOne.getId());
		assertEquals("No groups initially", 0, groups.size());
	}
	
	@Test
	public void testAddMembers() throws Exception {
		// Add users to the test group
		List<String> adder = new ArrayList<String>();
		
		// Empty list should work
		groupMembersDAO.addMembers(testGroup.getId(), adder);
		
		// Repeated entries should work
		adder.add(testUserOne.getId());
		adder.add(testUserTwo.getId());
		adder.add(testUserThree.getId());
		adder.add(testUserThree.getId());
		adder.add(testUserThree.getId());
		
		// Insertion is idempotent
		groupMembersDAO.addMembers(testGroup.getId(), adder);
		groupMembersDAO.addMembers(testGroup.getId(), adder); 
		
		// Validate the addition worked
		List<UserGroup> newMembers = groupMembersDAO.getMembers(testGroup.getId());
		assertEquals("Number of users should match", 3, newMembers.size());
		
		// Each user should be present, with unaltered etags
		assertTrue("User one should be in the retrieved member list", newMembers.contains(testUserOne));
		assertTrue("User two should be in the retrieved member list", newMembers.contains(testUserTwo));
		assertTrue("User three should be in the retrieved member list", newMembers.contains(testUserThree));
		
		// Verify that the parent group's etag has changed
		UserGroup updatedTestGroup = userGroupDAO.get(Long.parseLong(testGroup.getId()));
		assertTrue("Etag must have changed", !testGroup.getEtag().equals(updatedTestGroup.getEtag()));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testAddGroupToGroup() throws Exception {
		List<String> adder = new ArrayList<String>();
		adder.add(testUserOne.getId());
		adder.add(testUserTwo.getId());
		adder.add(testUserThree.getId());
		
		// Try to sneak this one into the addition
		adder.add(testGroup.getId());
		
		groupMembersDAO.addMembers(testGroup.getId(), adder); 
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testAddMemberToMember() throws Exception {
		List<String> adder = new ArrayList<String>();
		adder.add(testUserOne.getId());
		adder.add(testUserTwo.getId());
		adder.add(testUserThree.getId());
		
		// Can't add members to individuals
		groupMembersDAO.addMembers(testUserOne.getId(), adder); 
	}
	
	@Test
	public void testRemoveMembers() throws Exception {
		// Setup the group
		List<String> adder = new ArrayList<String>();
		adder.add(testUserOne.getId());
		adder.add(testUserTwo.getId());
		adder.add(testUserThree.getId());

		groupMembersDAO.addMembers(testGroup.getId(), adder);
		List<UserGroup> newMembers = groupMembersDAO.getMembers(testGroup.getId());
		assertEquals("Number of users should match", 3, newMembers.size());
		
		// Verify that the parent group's etag has changed
		UserGroup updatedTestGroup = userGroupDAO.get(Long.parseLong(testGroup.getId()));
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
		updatedTestGroup = userGroupDAO.get(Long.parseLong(testGroup.getId()));
		assertTrue("Etag must have changed", !testGroup.getEtag().equals(updatedTestGroup.getEtag()));
		
		// Remove the last guy from the group
		remover.clear();
		remover.add(antisocial);
		groupMembersDAO.removeMembers(testGroup.getId(), remover);
		List<UserGroup> emptyGroup = groupMembersDAO.getMembers(testGroup.getId());
		assertEquals("Number of users should match", 0, emptyGroup.size());
		
		// Verify that the parent group's etag has changed
		updatedTestGroup = userGroupDAO.get(Long.parseLong(testGroup.getId()));
		assertTrue("Etag must have changed", !testGroup.getEtag().equals(updatedTestGroup.getEtag()));
	}
	
	@Test
	public void testBootstrapGroups() throws Exception {
		String adminGroupId = BOOTSTRAP_PRINCIPAL.ADMINISTRATORS_GROUP.getPrincipalId().toString();
		List<UserGroup> admins = groupMembersDAO.getMembers(adminGroupId);
		Set<String> adminIds = new HashSet<String>();
		for (UserGroup ug : admins) {
			adminIds.add(ug.getId());
		}
		
		assertTrue(adminIds.contains(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString()));
	}
}
