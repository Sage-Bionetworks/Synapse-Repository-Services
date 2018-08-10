package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
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
		for (String todelete: groupsToDelete) {
			userGroupDAO.delete(todelete);
		}
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		// Give it an ID
		String startingId = "123";
		group.setId(""+startingId);
		long initialCount = userGroupDAO.getCount();
		String groupId = userGroupDAO.create(group).toString();
		assertNotNull(groupId);
		groupsToDelete.add(groupId);
		assertFalse("A new ID should have been issued to the principal",groupId.equals(startingId));
		UserGroup clone = userGroupDAO.get(Long.parseLong(groupId));
		assertEquals(groupId, clone.getId());
		assertEquals(group.getIsIndividual(), clone.getIsIndividual());
		assertEquals(1+initialCount, userGroupDAO.getCount());
	}
	
	@Test (expected=NotFoundException.class)
	public void testIsIndividualDoesNotExist(){
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
	public void testBootstrapUsers() throws DatastoreException, NotFoundException{
		List<BootstrapPrincipal> boots = this.userGroupDAO.getBootstrapPrincipals();
		assertNotNull(boots);
		assertTrue(boots.size() >0);
		// Each should exist
		for(BootstrapPrincipal bootUg: boots){
			assertTrue(userGroupDAO.doesIdExist(bootUg.getId()));
			UserGroup ug = userGroupDAO.get(bootUg.getId());
			assertEquals(bootUg.getId().toString(), ug.getId());
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUndeletableUserGroupWithSharedProject() {
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		String startingId = "123";
		group.setId(""+startingId);
		String groupId = userGroupDAO.create(group).toString();
		groupsToDelete.add(groupId); // The call under test will fail, so we must delete the group afterwards

		String ownerId = createUserIdForProject();
		String projectId = createProjectIdForAcl(ownerId);

		// Add an ACL at the project
		AccessControlList acl = createNodeAclForUserGroup(projectId, ownerId, Long.valueOf(groupId), new Date());
		aclToDelete = aclDAO.create(acl, ObjectType.ENTITY);

		// Call under test
		userGroupDAO.delete(groupId);
	}

	@Test
	public void testCanDeleteUserGroupAfterUnsharingProject() {
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		String startingId = "123";
		group.setId(""+startingId);
		String groupId = userGroupDAO.create(group).toString();
		// Don't add to groupsToDelete because the delete call should succeed

		String ownerId = createUserIdForProject();
		String projectId = createProjectIdForAcl(ownerId);

		// Add an ACL at the project
		AccessControlList acl = createNodeAclForUserGroup(projectId, ownerId, Long.valueOf(groupId), new Date());
		String localAclToDelete = aclDAO.create(acl, ObjectType.ENTITY);
		aclDAO.delete(localAclToDelete, ObjectType.ENTITY);

		// Call under test
		userGroupDAO.delete(groupId);
	}

	private String createProjectIdForAcl(String ownerId) {
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("user-name");
		alias.setPrincipalId(Long.parseLong(ownerId));
		alias.setType(AliasType.USER_NAME);
		principalAliasDAO.bindAliasToPrincipal(alias);

		Node project = NodeTestUtils.createNew("project", alias.getPrincipalId());
		project.setNodeType(EntityType.project);
		project = nodeDao.createNewNode(project);
		projectToDelete = project.getId();
		return project.getId();
	}

	private String createUserIdForProject() {
		// need an arbitrary user to own the project
		UserGroup owner = new UserGroup();
		owner.setIsIndividual(true);
		owner.setId(userGroupDAO.create(owner).toString());
		groupsToDelete.add(owner.getId());
		return owner.getId();
	}

	private static AccessControlList createNodeAclForUserGroup(
			final String nodeId,
			final String pid,
			final Long ugId,
			final Date creationDate) {
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.DOWNLOAD}));
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(accessSet);
		ra.setPrincipalId(ugId);
		raSet.add(ra);
		AccessControlList acl = new AccessControlList();
		acl.setId(nodeId);
		acl.setCreatedBy(pid);
		acl.setCreationDate(creationDate);
		acl.setModifiedBy(pid);
		acl.setModifiedOn(creationDate);
		acl.setResourceAccess(raSet);
		return acl;
	}
}
