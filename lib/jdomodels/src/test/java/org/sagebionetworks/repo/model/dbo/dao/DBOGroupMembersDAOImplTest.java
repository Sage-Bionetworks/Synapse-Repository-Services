package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
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
	DBOChangeDAO changeDAO;
	
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
	public void testFilterUserGroups() {
		List<String> groupList = new ArrayList<String>();
		groupList.add(testGroup.getId());
		
		// userId is required
		try {
			groupMembersDAO.filterUserGroups(null, groupList);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		
		// a null group list is filtered to an empty list
		List<String> groupIds = groupMembersDAO.filterUserGroups(testUserOne.getId(), null);
		assertTrue(groupIds.isEmpty());
		
		// an empty group list is filtered to an empty list
		groupIds = groupMembersDAO.filterUserGroups(testUserOne.getId(), Collections.EMPTY_LIST);
		assertTrue(groupIds.isEmpty());
		
		// if the user isn't in the group, an empty list is returned
		groupIds = groupMembersDAO.filterUserGroups(testUserOne.getId(), groupList);
		assertTrue(groupIds.isEmpty());

		// if the user isn't in the group, an empty list is returned
		groupIds = groupMembersDAO.filterUserGroups(testUserOne.getId(), groupList);
		
		// same for multiple groups, none of which the user is in
		groupList.add("99999");
		groupIds = groupMembersDAO.filterUserGroups(testUserOne.getId(), groupList);
		assertTrue(groupIds.isEmpty());
		
		// now we add a member
		groupMembersDAO.addMembers(testGroup.getId(), Collections.singletonList(testUserOne.getId()));

		// the group should now show up in the results
		groupIds = groupMembersDAO.filterUserGroups(testUserOne.getId(), groupList);
		assertEquals(Collections.singletonList(testGroup.getId()), groupIds);
		
		// if we add another group ID to the query, only the original group should be returned
		groupList.add("99999");
		groupIds = groupMembersDAO.filterUserGroups(testUserOne.getId(), groupList);
		assertEquals(Collections.singletonList(testGroup.getId()), groupIds);
		
	}
	
	@Test
	public void testAddMembers() throws Exception {
		changeDAO.deleteAllChanges();
		long startChangeNumber = changeDAO.getCurrentChangeNumber();

		// Add users to the test group
		List<String> adder = new ArrayList<String>();
	
		// Empty list should work
		groupMembersDAO.addMembers(testGroup.getId(), adder);
		// No messages should be sent
		List<ChangeMessage> changes = changeDAO.listChanges(changeDAO.getCurrentChangeNumber(), ObjectType.PRINCIPAL, Long.MAX_VALUE);
		assertNotNull(changes);
		assertTrue(changes.isEmpty());

		// Repeated entries should work
		adder.add(testUserOne.getId());
		adder.add(testUserTwo.getId());
		adder.add(testUserThree.getId());
		adder.add(testUserThree.getId());
		adder.add(testUserThree.getId());

		groupMembersDAO.addMembers(testGroup.getId(), adder);

		changes = changeDAO.listChanges(startChangeNumber, ObjectType.PRINCIPAL, Long.MAX_VALUE);
		assertNotNull(changes);
		assertEquals(1, changes.size());
		ChangeMessage message = changes.get(0);
		assertNotNull(message);
		assertEquals(ChangeType.UPDATE, message.getChangeType());
		assertEquals(ObjectType.PRINCIPAL, message.getObjectType());
		assertEquals(testGroup.getId(), message.getObjectId());

		// Insertion is idempotent
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
		assertFalse(groupMembersDAO.areMemberOf(testGroup.getId(), null));
		assertFalse(groupMembersDAO.areMemberOf(testGroup.getId(), new HashSet<String>()));

		// Setup the group
		List<String> adder = new ArrayList<String>();
		adder.add(testUserOne.getId());
		adder.add(testUserTwo.getId());
		adder.add(testUserThree.getId());
		assertFalse(groupMembersDAO.areMemberOf(testGroup.getId(), new HashSet<String>(adder)));

		groupMembersDAO.addMembers(testGroup.getId(), adder);
		List<UserGroup> newMembers = groupMembersDAO.getMembers(testGroup.getId());
		assertEquals("Number of users should match", 3, newMembers.size());

		assertTrue(groupMembersDAO.areMemberOf(testGroup.getId(), new HashSet<String>(adder)));

		// Verify that the parent group's etag has changed
		UserGroup updatedTestGroup = userGroupDAO.get(Long.parseLong(testGroup.getId()));
		assertTrue("Etag must have changed", !testGroup.getEtag().equals(updatedTestGroup.getEtag()));
		
		// Remove all but one of the users from the group
		List<String> remover = new ArrayList<String>(adder);
		String antisocial = remover.remove(0);
		assertTrue(groupMembersDAO.areMemberOf(testGroup.getId(), new HashSet<String>(remover)));
		groupMembersDAO.removeMembers(testGroup.getId(), remover);

		assertFalse(groupMembersDAO.areMemberOf(testGroup.getId(), new HashSet<String>(adder)));

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
	public void testGetMemberIdsDoesNotExist(){
		Long doesNotExist = -1L;
		Set<Long> membersIds = groupMembersDAO.getMemberIds(doesNotExist);
		assertNotNull(membersIds);
		assertTrue(membersIds.isEmpty());
	}
	
	@Test
	public void testGetMemberIds(){
		// Add users to the test group
		List<String> idsToAdd = new ArrayList<String>();
		idsToAdd.add(testUserOne.getId());
		idsToAdd.add(testUserTwo.getId());
		
		groupMembersDAO.addMembers(testGroup.getId(), idsToAdd);
		// call under test
		Set<Long> membersIds = groupMembersDAO.getMemberIds(Long.parseLong(testGroup.getId()));
		assertNotNull(membersIds);
		assertEquals(2, membersIds.size());
		assertTrue(membersIds.contains(Long.parseLong(testUserOne.getId())));
		assertTrue(membersIds.contains(Long.parseLong(testUserTwo.getId())));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllIndividualWithNullPrincipalIds(){
		groupMembersDAO.getIndividuals(null, 10L, 0L);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllIndividualWithNullLimit(){
		Set<String> principalIds = new HashSet<String>();
		principalIds.addAll(Arrays.asList(testGroup.getId()));
		groupMembersDAO.getIndividuals(principalIds, null, 0L);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllIndividualWithNullOffset(){
		Set<String> principalIds = new HashSet<String>();
		principalIds.addAll(Arrays.asList(testGroup.getId()));
		groupMembersDAO.getIndividuals(principalIds, 10L, null);
	}

	@Test
	public void testGetAllIndividualWithEmptySet(){
		assertEquals(new HashSet<String>(), groupMembersDAO.getIndividuals(new HashSet<String>(), 10L, 0L));
	}

	@Test
	public void testGetAllIndividual(){
		groupMembersDAO.addMembers(testGroup.getId(), Arrays.asList(testUserOne.getId(), testUserThree.getId()));
		Set<String> principalIds = new HashSet<String>();
		principalIds.addAll(Arrays.asList(testGroup.getId(), testUserTwo.getId()));
		Set<String> actual = groupMembersDAO.getIndividuals(principalIds, 10L, 0L);
		assertTrue(actual.contains(testUserOne.getId()));
		assertTrue(actual.contains(testUserTwo.getId()));
		assertTrue(actual.contains(testUserThree.getId()));
		assertFalse(actual.contains(testGroup.getId()));
	}

	@Test
	public void testGetAllIndividualWithRepeatedMembers(){
		groupMembersDAO.addMembers(testGroup.getId(), Arrays.asList(testUserOne.getId(), testUserThree.getId()));
		Set<String> principalIds = new HashSet<String>();
		principalIds.addAll(Arrays.asList(testGroup.getId(), testUserTwo.getId(), testUserOne.getId()));
		Set<String> actual = groupMembersDAO.getIndividuals(principalIds, 10L, 0L);
		assertTrue(actual.contains(testUserOne.getId()));
		assertTrue(actual.contains(testUserTwo.getId()));
		assertTrue(actual.contains(testUserThree.getId()));
		assertFalse(actual.contains(testGroup.getId()));
	}

	@Test
	public void testGetAllIndividualLimitOffsetAndOrder(){
		groupMembersDAO.addMembers(testGroup.getId(), Arrays.asList(testUserOne.getId(), testUserThree.getId()));
		Set<String> principalIds = new HashSet<String>();
		principalIds.addAll(Arrays.asList(testGroup.getId(), testUserTwo.getId()));

		Set<String> actual = groupMembersDAO.getIndividuals(principalIds, 1L, 0L);
		assertTrue(actual.contains(testUserOne.getId()));
		assertFalse(actual.contains(testUserTwo.getId()));
		assertFalse(actual.contains(testUserThree.getId()));

		actual = groupMembersDAO.getIndividuals(principalIds, 1L, 1L);
		assertFalse(actual.contains(testUserOne.getId()));
		assertTrue(actual.contains(testUserTwo.getId()));
		assertFalse(actual.contains(testUserThree.getId()));

		actual = groupMembersDAO.getIndividuals(principalIds, 1L, 2L);
		assertFalse(actual.contains(testUserOne.getId()));
		assertFalse(actual.contains(testUserTwo.getId()));
		assertTrue(actual.contains(testUserThree.getId()));

		assertEquals(new HashSet<String>(), groupMembersDAO.getIndividuals(principalIds, 1L, 3L));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetIndividualCountWithNullSet() {
		groupMembersDAO.getIndividualCount(null);
	}

	@Test
	public void testGetIndividualCountWithNullEmptySet() {
		assertEquals((Long)0L, groupMembersDAO.getIndividualCount(new HashSet<String>()));
	}

	@Test
	public void testGetIndividualCount() {
		groupMembersDAO.addMembers(testGroup.getId(), Arrays.asList(testUserOne.getId(), testUserThree.getId()));
		Set<String> principalIds = new HashSet<String>();
		principalIds.addAll(Arrays.asList(testGroup.getId(), testUserTwo.getId()));
		assertEquals((Long)3L, groupMembersDAO.getIndividualCount(principalIds));
	}

	@Test
	public void testGetIndividualCountWithRepeatedMembers() {
		groupMembersDAO.addMembers(testGroup.getId(), Arrays.asList(testUserOne.getId(), testUserThree.getId()));
		Set<String> principalIds = new HashSet<String>();
		principalIds.addAll(Arrays.asList(testGroup.getId(), testUserTwo.getId(), testUserOne.getId()));
		assertEquals((Long)3L, groupMembersDAO.getIndividualCount(principalIds));
	}
}
