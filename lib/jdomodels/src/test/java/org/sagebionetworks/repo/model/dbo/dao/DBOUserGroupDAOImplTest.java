package org.sagebionetworks.repo.model.dbo.dao;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOUserGroupDAOImplTest {


	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private AccessControlListDAO aclDAO;

	@Autowired
	private PrincipalAliasDAO principalAliasDAO;

	@Autowired
	private NodeDAO nodeDao;

	private List<String> groupsToDelete;
	private String aclToDelete;
	private String projectToDelete;


	@Before
	public void setUp() throws Exception {
		groupsToDelete = new ArrayList<String>();
	}

	@After
	public void tearDown() throws Exception {
		// Order matters--user groups referenced by ACLs cannot be deleted
		if (aclToDelete != null) aclDAO.delete(aclToDelete, ObjectType.ENTITY);
		if (projectToDelete != null) nodeDao.delete(projectToDelete);
		for (String toDelete : groupsToDelete) {
			userGroupDAO.delete(toDelete);
		}
	}

	@Test
	public void testRoundTrip() throws Exception {
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		// Give it an ID
		String startingId = "123";
		group.setId("" + startingId);
		long initialCount = userGroupDAO.getCount();
		String groupId = userGroupDAO.create(group).toString();
		assertNotNull(groupId);
		groupsToDelete.add(groupId);
		assertFalse("A new ID should have been issued to the principal", groupId.equals(startingId));
		UserGroup clone = userGroupDAO.get(Long.parseLong(groupId));
		assertEquals(groupId, clone.getId());
		assertEquals(group.getIsIndividual(), clone.getIsIndividual());
		assertEquals(1 + initialCount, userGroupDAO.getCount());
	}

	@Test(expected = NotFoundException.class)
	public void testIsIndividualDoesNotExist() {
		userGroupDAO.isIndividual(-1L);
	}

	@Test
	public void testIsIndividualTrue() throws Exception {
		UserGroup group = new UserGroup();
		group.setIsIndividual(true);
		Long principalId = userGroupDAO.create(group);
		assertNotNull(principalId);
		groupsToDelete.add(principalId.toString());
		assertTrue(userGroupDAO.isIndividual(principalId));
	}

	@Test
	public void testIsIndividualFalse() throws Exception {
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		Long principalId = userGroupDAO.create(group);
		assertNotNull(principalId);
		groupsToDelete.add(principalId.toString());
		assertFalse(userGroupDAO.isIndividual(principalId));
	}


	@Test
	public void testBootstrapUsers() throws DatastoreException, NotFoundException {
		List<BootstrapPrincipal> boots = this.userGroupDAO.getBootstrapPrincipals();
		assertNotNull(boots);
		assertTrue(boots.size() > 0);
		// Each should exist
		for (BootstrapPrincipal bootUg : boots) {
			assertTrue(userGroupDAO.doesIdExist(bootUg.getId()));
			UserGroup ug = userGroupDAO.get(bootUg.getId());
			assertEquals(bootUg.getId().toString(), ug.getId());
		}
	}

	@Test
	public void testUndeletableUserGroupWithSharedProject() {
		Long groupId = userGroupDAO.create(UserGroupTestUtils.createGroup());
		groupsToDelete.add(groupId.toString()); // The call under test will fail, so we must delete the group afterwards

		Long ownerId = userGroupDAO.create(UserGroupTestUtils.createUser());
		groupsToDelete.add(ownerId.toString());

		String projectId = nodeDao.createNewNode(
				NodeTestUtils.createNew("project shared with a team", ownerId)).getId();
		projectToDelete = projectId;

		// Add an ACL at the project
		AccessControlList acl = AccessControlListUtil.createACL(projectId, new UserInfo(false, groupId),
				Collections.singleton(ACCESS_TYPE.DOWNLOAD), new Date());
		aclToDelete = aclDAO.create(acl, ObjectType.ENTITY);

		// Call under test
		try {
			userGroupDAO.delete(groupId.toString());
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// as expected
		}
	}

	@Test
	public void testCanDeleteUserGroupAfterUnsharingProject() {
		UserGroup group = UserGroupTestUtils.createGroup();
		Long groupId = userGroupDAO.create(group);
		// Don't add to groupsToDelete because the delete call should succeed

		// Need to create an owner for the project
		Long ownerId = userGroupDAO.create(UserGroupTestUtils.createUser());
		groupsToDelete.add(ownerId.toString());

		String projectId = nodeDao.createNewNode(
				NodeTestUtils.createNew("project shared with a team", ownerId)).getId();
		projectToDelete = projectId;

		// Add an ACL at the project
		AccessControlList acl = AccessControlListUtil.createACL(projectId, new UserInfo(false, groupId),
				Collections.singleton(ACCESS_TYPE.DOWNLOAD), new Date());
		String aclToDelete = aclDAO.create(acl, ObjectType.ENTITY);

		// Not testing to see if the team is currently undeletable, there is already a test for that

		// Delete the ACL; this should make the group deletable
		aclDAO.delete(aclToDelete, ObjectType.ENTITY);

		// Call under test
		userGroupDAO.delete(groupId.toString());
	}

}
