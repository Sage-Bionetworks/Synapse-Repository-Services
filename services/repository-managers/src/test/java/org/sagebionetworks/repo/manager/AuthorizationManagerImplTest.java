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
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.manager.EvaluationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AuthorizationManagerImplTest {

	@Autowired
	private AuthorizationManager authorizationManager;
	
	@Autowired
	private AuthenticationManager authenticationManager;
	
	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private UserProfileManager userProfileManager;
	
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	
	@Autowired
	private NodeDAO nodeDao;
	
	@Autowired
	private ActivityManager activityManager;
	
	@Autowired
	private EvaluationManager evaluationManager;
	
	@Autowired
	private DBOBasicDao basicDao;
	
	private Collection<Node> nodeList = new ArrayList<Node>();
	private Node node = null;
	private Node nodeCreatedByTestUser = null;
	private Node childNode = null;
	
	private UserInfo userInfo;
	private UserInfo adminUser;
	private UserInfo anonInfo;
	private UserGroup testGroup;
	private UserGroup publicGroup;
	
	private Random rand = null;
	
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
		Node node = createDTO(name, creator.getId(), modifiedBy, parentId, activityId);
		String nodeId = nodeManager.createNewNode(node, creator);
		assertNotNull(nodeId);
		node = nodeManager.get(creator, nodeId);
		return node;
	}

	@Before
	public void setUp() throws Exception {
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setDomain(DomainType.SYNAPSE);
		tou.setAgreesToTermsOfUse(Boolean.TRUE);
		
		// Create a new user
		DBOCredential cred = new DBOCredential();
		cred.setAgreesToTermsOfUse(true);
		cred.setSecretKey("");
		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		userInfo = userManager.createUser(adminUser, nu, cred, tou);
		
		// Create a new group
		testGroup = new UserGroup();
		testGroup.setIsIndividual(false);
		testGroup.setId(userGroupDAO.create(testGroup).toString());
		
		// Add new user to new group (in the user's info)
		userInfo.getGroups().add(Long.parseLong(testGroup.getId()));
		
		// Find some existing principals
		anonInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		publicGroup = userGroupDAO.get(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		
		rand = new Random();
		// create a resource
		node = createNode("foo_"+rand.nextLong(), adminUser, 2L, null, null);
		nodeList.add(node);
				
		childNode = createNode("foo2_"+rand.nextLong(), adminUser, 4L, node.getId(), null);

		Long testUserPrincipalId = userInfo.getId();
		nodeCreatedByTestUser = createNode("bar_"+rand.nextLong(), userInfo, testUserPrincipalId, null, null);
		
		activitiesToDelete = new ArrayList<String>();
		
		nodeList.add(nodeCreatedByTestUser);
	}

	@After
	public void tearDown() throws Exception {
		for (Node n : nodeList) nodeManager.delete(adminUser, n.getId());
		this.node=null;
		
		if(activitiesToDelete != null && activityManager != null) {
			for(String activityId : activitiesToDelete) {
				activityManager.deleteActivity(adminUser, activityId);
			}
		}
		
		userManager.deletePrincipal(adminUser, userInfo.getId());
		userGroupDAO.delete(testGroup.getId());
	}
	
	// test that removing a user from the ACL for their own node also removes their access
	@Test
	public void testOwnership() throws Exception {
		String pIdString = userInfo.getId().toString();
		Long pId = Long.parseLong(pIdString);
		assertTrue(authorizationManager.canAccess(userInfo, nodeCreatedByTestUser.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		// remove user from ACL
		AccessControlList acl = entityPermissionsManager.getACL(nodeCreatedByTestUser.getId(), userInfo);
		assertNotNull(acl);
		//acl = AuthorizationTestHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ);
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

		assertFalse(authorizationManager.canAccess(userInfo, nodeCreatedByTestUser.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
	}
	
	
	@Test
	public void testCanAccessAsIndividual() throws Exception {
		// test that a user can access something they've been given access to individually
		// no access yet
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// now they should be able to access
		assertTrue(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		// but they do not have a different kind of access
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.DELETE));
	}
	
	@Test 
	public void testCanAccessGroup() throws Exception {
		// test that a user can access something accessible to a group they belong to
		boolean b = authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ);
		// no access yet
		assertFalse(b);
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, testGroup, ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// now they should be able to access
		b = authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ);
		assertTrue(b);
	}
	
	@Test 
	public void testCanAccessPublicGroup() throws Exception {
		// test that a user can access a Public resource
		boolean b = authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ);
		// no access yet
		assertFalse(b);
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, publicGroup, ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// now they should be able to access
		b = authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ);
		assertTrue(b);
	}
	
	@Test 
	public void testAnonymousCanAccessPublicGroup() throws Exception {
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, publicGroup, ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		boolean b = authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ);
		assertTrue(b);
	}
	
	// test that even if someone tries to give create, write, etc. access to anonymous,
	// anonymous can only READ
	@Test
	public void testAnonymousCanOnlyReadPublicEntity() throws Exception {
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, publicGroup, ACCESS_TYPE.READ);
		acl = AuthorizationTestHelper.addToACL(acl, publicGroup, ACCESS_TYPE.CHANGE_PERMISSIONS);
		acl = AuthorizationTestHelper.addToACL(acl, publicGroup, ACCESS_TYPE.CREATE);
		acl = AuthorizationTestHelper.addToACL(acl, publicGroup, ACCESS_TYPE.DELETE);
		acl = AuthorizationTestHelper.addToACL(acl, publicGroup, ACCESS_TYPE.DOWNLOAD);
		acl = AuthorizationTestHelper.addToACL(acl, publicGroup, ACCESS_TYPE.UPDATE);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		assertTrue(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.CHANGE_PERMISSIONS));
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.CREATE));
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.DELETE));
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD));
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE));
		
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
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		// give some other group access
		acl = AuthorizationTestHelper.addToACL(acl, testGroup, ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// anonymous does not have access
		boolean b = authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ);
		assertFalse(b);
	}
	
	@Test 
	public void testCanAccessAdmin() throws Exception {
		// test that an admin can access anything
		boolean b = authorizationManager.canAccess(adminUser, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ);
		assertTrue(b);
	}
	
	@Test 
	public void testCanPublicRead() throws Exception {
		// verify that anonymous user can't initially access
		boolean b = authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ);
		assertFalse(b);
		
		//so public can't read, no matter who is requesting
		UserEntityPermissions uep = entityPermissionsManager.getUserPermissionsForEntity(adminUser,  node.getId());
		assertFalse(uep.getCanPublicRead());
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertFalse(uep.getCanPublicRead());
		uep = entityPermissionsManager.getUserPermissionsForEntity(anonInfo,  node.getId());
		assertFalse(uep.getCanPublicRead());
		
		//update so that public group CAN read
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, publicGroup, ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		
		//now verify that public can read is true (no matter who requests)
		uep = entityPermissionsManager.getUserPermissionsForEntity(adminUser,  node.getId());
		assertTrue(uep.getCanPublicRead());
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertTrue(uep.getCanPublicRead());
		uep = entityPermissionsManager.getUserPermissionsForEntity(anonInfo,  node.getId());
		assertTrue(uep.getCanPublicRead());
	}
	
	@Test
	public void testCanAccessInherited() throws Exception {		
		// no access yet to parent
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));

		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// now they should be able to access
		assertTrue(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		// and the child as well
		assertTrue(authorizationManager.canAccess(userInfo, childNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		
		UserEntityPermissions uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(true, uep.getCanView());
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  childNode.getId());
		assertEquals(true, uep.getCanView());
		assertFalse(uep.getCanEnableInheritance());
		assertEquals(node.getCreatedByPrincipalId(), uep.getOwnerPrincipalId());
		
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.CHANGE_PERMISSIONS);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		assertFalse(uep.getCanEnableInheritance());
		
	}

	// test lack of access to something that doesn't inherit its permissions, whose parent you CAN access
	@Test
	public void testCantAccessNotInherited() throws Exception {		
		// no access yet to parent
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));

		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl.setId(childNode.getId());
		entityPermissionsManager.overrideInheritance(acl, adminUser); // must do as admin!
		// permissions haven't changed (yet)
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		assertFalse(authorizationManager.canAccess(userInfo, childNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		
		UserEntityPermissions uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanView());
		uep = entityPermissionsManager.getUserPermissionsForEntity(userInfo,  childNode.getId());
		assertEquals(false, uep.getCanView());
		assertFalse(uep.getCanEnableInheritance());
		assertEquals(node.getCreatedByPrincipalId(), uep.getOwnerPrincipalId());
		
		// get a new copy of parent ACL
		acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// should be able to access parent but not child
		assertTrue(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		assertFalse(authorizationManager.canAccess(userInfo, childNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		
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
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		
		acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.UPDATE);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// now they should be able to access
		assertTrue(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ));				
		// but can't add a child 
		Node child = createDTO("child", 10L, 11L, node.getId(), null);
		assertFalse(authorizationManager.canCreate(userInfo, child));
		
		// but give them create access to the parent
		acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.CREATE);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// now it can
		assertTrue(authorizationManager.canCreate(userInfo, child));
		
	}

	@Test
	public void testCreateSpecialUsers() throws Exception {
		// admin always has access 
		Node child = createDTO("child", 10L, 11L, node.getId(), null);
		assertTrue(authorizationManager.canCreate(adminUser, child));

		// allow some access
		AccessControlList acl = entityPermissionsManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.CREATE);
		acl = entityPermissionsManager.updateACL(acl, adminUser);
		// now they should be able to access
		assertTrue(authorizationManager.canCreate(userInfo, child));
		
		// but anonymous cannot
		assertFalse(authorizationManager.canCreate(anonInfo, child));
	}

	@Test
	public void testCreateNoParent() throws Exception {

		// try to create node with no parent.  should fail
		Node orphan = createDTO("orphan", 10L, 11L, null, null);
		assertFalse(authorizationManager.canCreate(userInfo, orphan));

		// admin creates a node with no parent.  should work
		assertTrue(authorizationManager.canCreate(adminUser, orphan));
	}

	@Test
	public void testGetUserPermissionsForEntity() throws Exception{
		assertTrue(adminUser.isAdmin());
		assertTrue(authenticationManager.hasUserAcceptedTermsOfUse(adminUser.getId(), DomainType.SYNAPSE));
		// the admin user can do it all
		UserEntityPermissions uep = entityPermissionsManager.getUserPermissionsForEntity(adminUser, node.getId());
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
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.READ);
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
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.UPDATE);
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
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.DELETE);
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
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.CHANGE_PERMISSIONS);
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
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.CREATE);
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
		node.setCreatedByPrincipalId(userInfo.getId());
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
	
	@Test
	public void testIsAnonymousUser() throws DatastoreException, NotFoundException{
		assertNotNull(anonInfo);
		assertTrue(authorizationManager.isAnonymousUser(anonInfo));
		assertFalse(authorizationManager.isAnonymousUser(userInfo));
	}
}
