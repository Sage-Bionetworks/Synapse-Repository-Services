package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.manager.EvaluationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AuthorizationManagerImplTest {

	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private NodeManager nodeManager;
	@Autowired
	private UserManager userManager;	
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private ActivityManager activityManager;
	@Autowired
	private EvaluationManager evaluationManager;

	private Collection<Node> nodeList = new ArrayList<Node>();
	private Node node = null;
	private Node nodeCreatedByTestUser = null;
	private Node childNode = null;
	private UserInfo userInfo = null;
	private UserInfo adminUser;
	private Random rand = null;
	
	private List<String> usersToDelete;
	private List<String> activitiesToDelete;
	
	private Node createDTO(String name, Long createdBy, Long modifiedBy, String parentId, String activityId) {
		Node node = new Node();
		node.setName(name);
		node.setCreatedOn(new Date());
		node.setCreatedByPrincipalId(createdBy);
		node.setModifiedOn(new Date());
		node.setModifiedByPrincipalId(modifiedBy);
		node.setNodeType(EntityType.project.name());
		node.setActivityId(activityId);
		if (parentId!=null) node.setParentId(parentId);
		return node;
	}
	
	private Node createNode(String name, UserInfo creator, Long modifiedBy, String parentId, String activityId) throws Exception {
		Node node = createDTO(name, Long.parseLong(creator.getIndividualGroup().getId()), modifiedBy, parentId, activityId);
		String nodeId = nodeManager.createNewNode(node, creator);
		assertNotNull(nodeId);
		node = nodeManager.get(creator, nodeId);
		return node;
	}

	@Before
	public void setUp() throws Exception {
		// userInfo
		userManager.setUserDAO(new TestUserDAO()); // could use Mockito here
		userInfo = userManager.getUserInfo("AuthorizationManagerImplTest.testuser@foo.bar");
		usersToDelete = new ArrayList<String>();
		usersToDelete.add(userInfo.getIndividualGroup().getId());
		usersToDelete.add(userInfo.getUser().getId());
		
		adminUser = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		usersToDelete.add(adminUser.getIndividualGroup().getId());
		usersToDelete.add(adminUser.getUser().getId());
		rand = new Random();
		// create a resource
		node = createNode("foo_"+rand.nextLong(), adminUser, 2L, null, null);
		nodeList.add(node);
				
		childNode = createNode("foo2_"+rand.nextLong(), adminUser, 4L, node.getId(), null);

		Long testUserPrincipalId = Long.parseLong(userInfo.getIndividualGroup().getId());
		nodeCreatedByTestUser = createNode("bar_"+rand.nextLong(), userInfo, testUserPrincipalId, null, null);
		
		activitiesToDelete = new ArrayList<String>();
		
		nodeList.add(nodeCreatedByTestUser);
	}

	@After
	public void tearDown() throws Exception {
		UserInfo adminUser = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		for (Node n : nodeList) nodeManager.delete(adminUser, n.getId());
		this.node=null;
		
		if(userManager != null && usersToDelete != null){
			for(String idToDelete: usersToDelete){
				userManager.deleteUser(idToDelete);
			}
		}
		
		if(activitiesToDelete != null && activityManager != null) {
			for(String activityId : activitiesToDelete) {
				activityManager.deleteActivity(adminUser, activityId);
			}
		}
	}
	
	// test that removing a user from the ACL for their own node also removes their access
	@Test
	public void testOwnership() throws Exception {
		String pIdString = userInfo.getIndividualGroup().getId();
		Long pId = Long.parseLong(pIdString);
		assertTrue(authorizationManager.canAccess(userInfo, nodeCreatedByTestUser.getId(), ACCESS_TYPE.READ));
		// remove user from ACL
		AccessControlList acl = entityPermissionsManager.getACL(nodeCreatedByTestUser.getId(), userInfo);
		assertNotNull(acl);
		//acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ);
		Set<ResourceAccess> ras = acl.getResourceAccess();
		boolean foundit = false;
		for (ResourceAccess ra : ras) {
			Long raPId = ra.getPrincipalId();
			assertNotNull(raPId);
			if (raPId.equals(pId)) {
				foundit=true;
				ras.remove(ra);
				break;
			}
		}
		assertTrue(foundit);
		acl = entityPermissionsManager.updateACL(acl, adminUser);

		assertFalse(authorizationManager.canAccess(userInfo, nodeCreatedByTestUser.getId(), ACCESS_TYPE.READ));
	}
	
	
	@Test
	public void testCanAccessAsIndividual() throws Exception {
		// test that a user can access something they've been given access to individually
		// no access yet
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ));
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// now they should be able to access
		assertTrue(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ));
		// but they do not have a different kind of access
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.DELETE));
	}
	
	@Test 
	public void testCanAccessGroup() throws Exception {
		// test that a user can access something accessible to a group they belong to
		boolean b = authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ);
		// no access yet
		assertFalse(b);
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		UserGroup g = userManager.findGroup(TestUserDAO.TEST_GROUP_NAME, false);
		acl = AuthorizationHelper.addToACL(acl, g, ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// now they should be able to access
		b = authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ);
		assertTrue(b);
	}
	
	@Test 
	public void testCanAccessPublicGroup() throws Exception {
		// test that a user can access a Public resource
		boolean b = authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ);
		// no access yet
		assertFalse(b);
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		UserGroup pg = userManager.findGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false);
		acl = AuthorizationHelper.addToACL(acl, pg, ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// now they should be able to access
		b = authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ);
		assertTrue(b);
	}
	
	@Test 
	public void testAnonymousCanAccessPublicGroup() throws Exception {
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		UserGroup pg = userManager.findGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false);
		acl = AuthorizationHelper.addToACL(acl, pg, ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		UserInfo anonInfo = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		boolean b = authorizationManager.canAccess(anonInfo, node.getId(), ACCESS_TYPE.READ);
		assertTrue(b);
	}
	
	// test that even if someone tries to give create, write, etc. access to anonymous,
	// anonymous can only READ
	@Test
	public void testAnonymousCanOnlyReadPublicEntity() throws Exception {
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		UserGroup pg = userManager.findGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false);
		acl = AuthorizationHelper.addToACL(acl, pg, ACCESS_TYPE.READ);
		acl = AuthorizationHelper.addToACL(acl, pg, ACCESS_TYPE.CHANGE_PERMISSIONS);
		acl = AuthorizationHelper.addToACL(acl, pg, ACCESS_TYPE.CREATE);
		acl = AuthorizationHelper.addToACL(acl, pg, ACCESS_TYPE.DELETE);
		acl = AuthorizationHelper.addToACL(acl, pg, ACCESS_TYPE.DOWNLOAD);
		acl = AuthorizationHelper.addToACL(acl, pg, ACCESS_TYPE.UPDATE);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		UserInfo anonInfo = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		assertTrue(authorizationManager.canAccess(anonInfo, node.getId(), ACCESS_TYPE.READ));
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ACCESS_TYPE.CHANGE_PERMISSIONS));
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ACCESS_TYPE.CREATE));
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ACCESS_TYPE.DELETE));
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ACCESS_TYPE.DOWNLOAD));
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ACCESS_TYPE.UPDATE));
		
		Node childNode = new Node();
		childNode.setParentId(node.getId());
		assertFalse(authorizationManager.canCreate(anonInfo, childNode));
		
		UserEntityPermissions uep = entityPermissionsManager.getUserPermissionsForEntity(anonInfo, node.getId());
		assertTrue(uep.getCanView());
		assertTrue(uep.getCanPublicRead());
		assertFalse(uep.getCanAddChild());
		assertFalse(uep.getCanChangePermissions());
		assertFalse(uep.getCanDelete());
		assertFalse(uep.getCanDownload());
		assertFalse(uep.getCanEdit());
		assertFalse(uep.getCanEnableInheritance());
	}
	
	@Test
	public void testCanAccessAsAnonymous() throws Exception {
		UserInfo anonInfo = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		// give some other group access
		UserGroup g = userManager.findGroup(TestUserDAO.TEST_GROUP_NAME, false);
		acl = AuthorizationHelper.addToACL(acl, g, ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// anonymous does not have access
		boolean b = authorizationManager.canAccess(anonInfo, node.getId(), ACCESS_TYPE.READ);
		assertFalse(b);
	}
	
	@Test 
	public void testCanAccessAdmin() throws Exception {
		// test that an admin can access anything
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		// test that admin can access anything
		boolean b = authorizationManager.canAccess(adminInfo, node.getId(), ACCESS_TYPE.READ);
		assertTrue(b);
	}
	
	@Test 
	public void testCanPublicRead() throws Exception {
		// verify that anonymous user can't initially access
		UserInfo anonInfo = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		boolean b = authorizationManager.canAccess(anonInfo, node.getId(), ACCESS_TYPE.READ);
		assertFalse(b);
		
		//so public can't read, no matter who is requesting
		UserEntityPermissions uep = entityPermissionsManager.getUserPermissionsForEntity(adminInfo,  node.getId());
		assertFalse(uep.getCanPublicRead());
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertFalse(uep.getCanPublicRead());
		uep = entityPermissionsManager.getUserPermissionsForEntity(anonInfo,  node.getId());
		assertFalse(uep.getCanPublicRead());
		
		//update so that public group CAN read
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		UserGroup pg = userManager.findGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false);
		acl = AuthorizationHelper.addToACL(acl, pg, ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		
		//now verify that public can read is true (no matter who requests)
		uep = entityPermissionsManager.getUserPermissionsForEntity(adminInfo,  node.getId());
		assertTrue(uep.getCanPublicRead());
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertTrue(uep.getCanPublicRead());
		uep = entityPermissionsManager.getUserPermissionsForEntity(anonInfo,  node.getId());
		assertTrue(uep.getCanPublicRead());
	}
	
	@Test
	public void testCanAccessInherited() throws Exception {		
		// no access yet to parent
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ));

		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// now they should be able to access
		assertTrue(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ));
		// and the child as well
		assertTrue(authorizationManager.canAccess(userInfo, childNode.getId(), ACCESS_TYPE.READ));
		
		UserEntityPermissions uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(true, uep.getCanView());
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  childNode.getId());
		assertEquals(true, uep.getCanView());
		assertFalse(uep.getCanEnableInheritance());
		assertEquals(node.getCreatedByPrincipalId(), uep.getOwnerPrincipalId());
		
		acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.CHANGE_PERMISSIONS);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		assertFalse(uep.getCanEnableInheritance());
		
	}

	// test lack of access to something that doesn't inherit its permissions, whose parent you CAN access
	@Test
	public void testCantAccessNotInherited() throws Exception {		
		// no access yet to parent
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ));

		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl.setId(childNode.getId());
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		entityPermissionsManager.overrideInheritance(acl, adminInfo); // must do as admin!
		// permissions haven't changed (yet)
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ));
		assertFalse(authorizationManager.canAccess(userInfo, childNode.getId(), ACCESS_TYPE.READ));
		
		UserEntityPermissions uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanView());
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  childNode.getId());
		assertEquals(false, uep.getCanView());
		assertFalse(uep.getCanEnableInheritance());
		assertEquals(node.getCreatedByPrincipalId(), uep.getOwnerPrincipalId());
		
		// get a new copy of parent ACL
		acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// should be able to access parent but not child
		assertTrue(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ));
		assertFalse(authorizationManager.canAccess(userInfo, childNode.getId(), ACCESS_TYPE.READ));
		
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(true, uep.getCanView());
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  childNode.getId());
		assertEquals(false, uep.getCanView());
	}
	
	@Test
	public void testCreate() throws Exception {
		// make an object on which you have READ and WRITE permission
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		
		acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.UPDATE);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// now they should be able to access
		assertTrue(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ));				
		// but can't add a child 
		Node child = createDTO("child", 10L, 11L, node.getId(), null);
		assertFalse(authorizationManager.canCreate(userInfo, child));
		
		// but give them create access to the parent
		acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.CREATE);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// now it can
		assertTrue(authorizationManager.canCreate(userInfo, child));
		
	}

	@Test
	public void testCreateSpecialUsers() throws Exception {
		// admin always has access 
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		Node child = createDTO("child", 10L, 11L, node.getId(), null);
		assertTrue(authorizationManager.canCreate(adminInfo, child));

		// allow some access
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.CREATE);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// now they should be able to access
		assertTrue(authorizationManager.canCreate(userInfo, child));
		
		// but anonymous cannot
		UserInfo anonInfo = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		assertFalse(authorizationManager.canCreate(anonInfo, child));
	}

	@Test
	public void testCreateNoParent() throws Exception {

		// try to create node with no parent.  should fail
		Node orphan = createDTO("orphan", 10L, 11L, null, null);
		assertFalse(authorizationManager.canCreate(userInfo, orphan));

		// admin creates a node with no parent.  should work
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		assertTrue(authorizationManager.canCreate(adminInfo, orphan));
	}

	@Test
	public void testGetUserPermissionsForEntity() throws Exception{
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		assertTrue(adminInfo.isAdmin());
		// the admin user can do it all
		UserEntityPermissions uep = entityPermissionsManager.getUserPermissionsForEntity(adminInfo,  node.getId());
		assertNotNull(uep);
		assertEquals(true, uep.getCanAddChild());
		assertEquals(true, uep.getCanChangePermissions());
		assertEquals(true, uep.getCanDelete());
		assertEquals(true, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		assertEquals(true, uep.getCanDownload());
		
		// the user cannot do anything
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanAddChild());
		assertEquals(false, uep.getCanChangePermissions());
		assertEquals(false, uep.getCanDelete());
		assertEquals(false, uep.getCanEdit());
		assertEquals(false, uep.getCanView());
		assertEquals(true, uep.getCanDownload()); // can't read but CAN download, which is controlled separately
		assertEquals(false, uep.getCanEnableInheritance());
		assertEquals(node.getCreatedByPrincipalId(), uep.getOwnerPrincipalId());
		
		// Let the user read.
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanAddChild());
		assertEquals(false, uep.getCanChangePermissions());
		assertEquals(false, uep.getCanDelete());
		assertEquals(false, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		assertEquals(true, uep.getCanDownload()); // can't read but CAN download, which is controlled separately
		
		// Let the user update.
		acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.UPDATE);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanAddChild());
		assertEquals(false, uep.getCanChangePermissions());
		assertEquals(false, uep.getCanDelete());
		assertEquals(true, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		assertEquals(true, uep.getCanDownload()); // can't read but CAN download, which is controlled separately
		
		// Let the user delete.
		acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.DELETE);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanAddChild());
		assertEquals(false, uep.getCanChangePermissions());
		assertEquals(true, uep.getCanDelete());
		assertEquals(true, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		assertEquals(true, uep.getCanDownload()); // can't read but CAN download, which is controlled separately
		
		// Let the user change permissions.
		acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.CHANGE_PERMISSIONS);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanAddChild());
		assertEquals(true, uep.getCanChangePermissions());
		assertEquals(true, uep.getCanDelete());
		assertEquals(true, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		assertEquals(true, uep.getCanDownload()); // can't read but CAN download, which is controlled separately
		
		// Let the user change create.
		acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.CREATE);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(true, uep.getCanAddChild());
		assertEquals(true, uep.getCanChangePermissions());
		assertEquals(true, uep.getCanDelete());
		assertEquals(true, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		assertEquals(true, uep.getCanDownload()); // can't read but CAN download, which is controlled separately
	}
	
	@Test
	public void testOwnerAdminAccess() throws Exception {
		// the user can't do anything
		UserEntityPermissions uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanAddChild());
		assertEquals(false, uep.getCanChangePermissions());
		assertEquals(false, uep.getCanDelete());
		assertEquals(false, uep.getCanEdit());
		assertEquals(false, uep.getCanView());
		assertEquals(true, uep.getCanDownload());
		assertEquals(false, uep.getCanEnableInheritance());

		// now change the ownership so the user is the owner
		node.setCreatedByPrincipalId(Long.parseLong(userInfo.getIndividualGroup().getId()));
		nodeDao.updateNode(node);
		
		// the user still cannot do anything..
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanAddChild());
		assertEquals(false, uep.getCanChangePermissions());
		assertEquals(false, uep.getCanDelete());
		assertEquals(false, uep.getCanEdit());
		assertEquals(false, uep.getCanView());
		assertEquals(true, uep.getCanDownload());
		assertEquals(false, uep.getCanEnableInheritance());
		assertEquals(nodeCreatedByTestUser.getCreatedByPrincipalId(), uep.getOwnerPrincipalId());
	}
	
	@Test
	public void testCanAccessActivity() throws Exception {
		// create an activity 
		String activityId = activityManager.createActivity(userInfo, new Activity());
		assertNotNull(activityId);
		activitiesToDelete.add(activityId);
		nodeCreatedByTestUser.setActivityId(activityId);
		nodeManager.update(userInfo, nodeCreatedByTestUser);
		
		// test access
		boolean canAccess = authorizationManager.canAccessActivity(userInfo, activityId);		
		assertTrue(canAccess);
	}
	
	@Test
	public void testCanAccessActivityFail() throws Exception {
		// create an activity		
		String activityId = activityManager.createActivity(adminUser, new Activity());
		assertNotNull(activityId);
		activitiesToDelete.add(activityId);
		node.setActivityId(activityId);
		nodeManager.update(adminUser, node);
		
		// test access
		boolean canAccess = authorizationManager.canAccessActivity(userInfo, activityId);		
		assertFalse(canAccess);
	}
}
