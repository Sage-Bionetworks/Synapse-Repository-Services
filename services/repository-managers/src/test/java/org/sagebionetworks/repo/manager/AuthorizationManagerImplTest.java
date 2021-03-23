package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.manager.AuthorizationManagerImpl.ANONYMOUS_ACCESS_DENIED_REASON;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InviteeVerificationSignedToken;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSignedToken;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
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
	private EntityAclManager entityAclManager;
	
	@Autowired
	private EntityAuthorizationManager entityAuthorizationManager;
	
	@Autowired
	private ActivityManager activityManager;

	@Autowired
	private TeamManager teamManager;
	
	@Autowired
	private TokenGenerator tokenGenerator;
	
	@Autowired
	private ObjectTypeManager objectTypeManager;

	private Collection<Node> nodeList = new ArrayList<Node>();
	private Node node = null;
	private Node nodeCreatedByTestUser = null;
	private Node childNode = null;
	
	private UserInfo userInfo;
	private UserInfo adminUser;
	private UserInfo anonInfo;
	private UserInfo teamAdmin;
	private Team team;
	private UserGroup testGroup;
	private UserGroup publicGroup;
	
	private Random rand = null;

	private String forumId;
	
	private List<String> activitiesToDelete;
	
	private Node createDTO(String name, Long createdBy, Long modifiedBy, String parentId, String activityId) {
		Node node = new Node();
		node.setName(name);
		node.setCreatedOn(new Date());
		node.setCreatedByPrincipalId(createdBy);
		node.setModifiedOn(new Date());
		node.setModifiedByPrincipalId(modifiedBy);
		node.setNodeType(EntityType.project);
		node.setActivityId(activityId);
		if (parentId!=null) node.setParentId(parentId);
		return node;
	}
	
	private Node createNode(String name, UserInfo creator, Long modifiedBy, String parentId, String activityId) throws Exception {
		Node node = createDTO(name, creator.getId(), modifiedBy, parentId, activityId);
		String nodeId = nodeManager.createNewNode(node, creator);
		assertNotNull(nodeId);
		node = nodeManager.getNode(creator, nodeId);
		return node;
	}

	@BeforeEach
	public void setUp() throws Exception {
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setAgreesToTermsOfUse(Boolean.TRUE);
		
		// Create a new user
		DBOCredential cred = new DBOCredential();
		cred.setSecretKey("");
		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		userInfo = userManager.createOrGetTestUser(adminUser, nu, cred, tou);

		// Create a new group
		testGroup = new UserGroup();
		testGroup.setIsIndividual(false);
		testGroup.setId(userGroupDAO.create(testGroup).toString());
		
		// Add new user to new group (in the user's info)
		userInfo.getGroups().add(Long.parseLong(testGroup.getId()));
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		// Create team with teamAdmin as the admin
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		teamAdmin = userManager.createOrGetTestUser(adminUser, nu, cred, tou);
		team = new Team();
		team.setName("teamName");
		team = teamManager.create(teamAdmin, team);

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

		forumId = "1";
	}

	@AfterEach
	public void tearDown() throws Exception {
		for (Node n : nodeList) nodeManager.delete(adminUser, n.getId());
		this.node=null;
		
		if(activitiesToDelete != null && activityManager != null) {
			for(String activityId : activitiesToDelete) {
				activityManager.deleteActivity(adminUser, activityId);
			}
		}

		teamManager.delete(teamAdmin, team.getId());
		userManager.deletePrincipal(adminUser, teamAdmin.getId());
		userManager.deletePrincipal(adminUser, userInfo.getId());
		userGroupDAO.delete(testGroup.getId());
	}
	
	// test that removing a user from the ACL for their own node also removes their access
	@Test
	public void testOwnership() throws Exception {
		String pIdString = userInfo.getId().toString();
		Long pId = Long.parseLong(pIdString);
		assertTrue(authorizationManager.canAccess(userInfo, nodeCreatedByTestUser.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());
		// remove user from ACL
		AccessControlList acl = entityAclManager.getACL(nodeCreatedByTestUser.getId(), userInfo);
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
		acl = entityAclManager.updateACL(acl, adminUser);

		assertFalse(authorizationManager.canAccess(userInfo, nodeCreatedByTestUser.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());
	}
	
	
	@Test
	public void testCanAccessAsIndividual() throws Exception {
		// test that a user can access something they've been given access to individually
		// no access yet
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());
		AccessControlList acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.READ);
		acl = entityAclManager.updateACL(acl, adminUser);
		// now they should be able to access
		assertTrue(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());
		// but they do not have a different kind of access
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.DELETE).isAuthorized());
	}
	
	@Test 
	public void testCanAccessGroup() throws Exception {
		// test that a user can access something accessible to a group they belong to
		boolean b = authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized();
		// no access yet
		assertFalse(b);
		AccessControlList acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, testGroup, ACCESS_TYPE.READ);
		acl = entityAclManager.updateACL(acl, adminUser);
		// now they should be able to access
		b = authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized();
		assertTrue(b);
	}
	
	@Test 
	public void testCanAccessPublicGroup() throws Exception {
		// test that a user can access a Public resource
		boolean b = authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized();
		// no access yet
		assertFalse(b);
		AccessControlList acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, publicGroup, ACCESS_TYPE.READ);
		acl = entityAclManager.updateACL(acl, adminUser);
		// now they should be able to access
		b = authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized();
		assertTrue(b);
	}
	
	@Test 
	public void testAnonymousCanAccessPublicGroup() throws Exception {
		AccessControlList acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, publicGroup, ACCESS_TYPE.READ);
		acl = entityAclManager.updateACL(acl, adminUser);
		boolean b = authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized();
		assertTrue(b);
	}
	
	@Test
	public void testAnonymousCanOnlyReadPublicEntity() throws Exception {
		
		AccessControlList acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, publicGroup, ACCESS_TYPE.READ);
		acl = entityAclManager.updateACL(acl, adminUser);
		
		assertTrue(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.CHANGE_PERMISSIONS).isAuthorized());
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.CHANGE_SETTINGS).isAuthorized());
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.CREATE).isAuthorized());
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.DELETE).isAuthorized());
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD).isAuthorized());
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE).isAuthorized());
		
		assertFalse(authorizationManager.canCreate(anonInfo, node.getId(), EntityType.file).isAuthorized());
		
		UserEntityPermissions uep = entityAuthorizationManager.getUserPermissionsForEntity(anonInfo, node.getId());
		assertTrue(uep.getCanView());
		assertTrue(uep.getCanPublicRead());
		assertFalse(uep.getCanAddChild());
		assertFalse(uep.getCanChangePermissions());
		assertFalse(uep.getCanChangeSettings());
		assertFalse(uep.getCanDelete());
		assertFalse(uep.getCanDownload());
		assertFalse(uep.getCanUpload());
		assertFalse(uep.getCanEdit());
		assertFalse(uep.getCanEnableInheritance());
	}
	
	@Test
	public void testCanDownloadAsAnonymousWhenOpenAccessAndReadAccess() throws Exception {
		AccessControlList acl = entityAclManager.getACL(node.getId(), anonInfo);
		acl = AuthorizationTestHelper.addToACL(acl, publicGroup, ACCESS_TYPE.READ);
		
		acl = entityAclManager.updateACL(acl, adminUser);
		
		// Can read now
		assertTrue(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());
		// Cannot download initially
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD).isAuthorized());
		
		// Set the entity as open access
		objectTypeManager.changeObjectsDataType(adminUser, node.getId(), ObjectType.ENTITY, DataType.OPEN_DATA);
		
		// Call under test
		assertTrue(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD).isAuthorized());
	}
	
	@Test
	public void testCanDownloadAsAnonymousWhenOpenAccessWithoutReadAccess() throws Exception {		
		// Cannot read now
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());
		// Cannot download initially
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD).isAuthorized());
		
		// Set the entity as open access
		objectTypeManager.changeObjectsDataType(adminUser, node.getId(), ObjectType.ENTITY, DataType.OPEN_DATA);
		
		// Call under test
		assertFalse(authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD).isAuthorized());
	}
	
	@Test
	public void testCanAccessAsAnonymous() throws Exception {
		AccessControlList acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		// give some other group access
		acl = AuthorizationTestHelper.addToACL(acl, testGroup, ACCESS_TYPE.READ);
		acl = entityAclManager.updateACL(acl, adminUser);
		// anonymous does not have access
		boolean b = authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized();
		assertFalse(b);
	}
	
	@Test 
	public void testCanAccessAdmin() throws Exception {
		// test that an admin can access anything
		boolean b = authorizationManager.canAccess(adminUser, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized();
		assertTrue(b);
	}
	
	@Test 
	public void testCanPublicRead() throws Exception {
		// verify that anonymous user can't initially access
		boolean b = authorizationManager.canAccess(anonInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized();
		assertFalse(b);
		
		//so public can't read, no matter who is requesting
		UserEntityPermissions uep = entityAuthorizationManager.getUserPermissionsForEntity(adminUser,  node.getId());
		assertFalse(uep.getCanPublicRead());
		uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertFalse(uep.getCanPublicRead());
		uep = entityAuthorizationManager.getUserPermissionsForEntity(anonInfo,  node.getId());
		assertFalse(uep.getCanPublicRead());
		
		//update so that public group CAN read
		AccessControlList acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, publicGroup, ACCESS_TYPE.READ);
		acl = entityAclManager.updateACL(acl, adminUser);
		
		//now verify that public can read is true (no matter who requests)
		uep = entityAuthorizationManager.getUserPermissionsForEntity(adminUser,  node.getId());
		assertTrue(uep.getCanPublicRead());
		uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertTrue(uep.getCanPublicRead());
		uep = entityAuthorizationManager.getUserPermissionsForEntity(anonInfo,  node.getId());
		assertTrue(uep.getCanPublicRead());
	}
	
	@Test
	public void testCanAccessInherited() throws Exception {		
		// no access yet to parent
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());

		AccessControlList acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.READ);
		acl = entityAclManager.updateACL(acl, adminUser);
		// now they should be able to access
		assertTrue(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());
		// and the child as well
		assertTrue(authorizationManager.canAccess(userInfo, childNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());
		
		UserEntityPermissions uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(true, uep.getCanView());
		uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  childNode.getId());
		assertEquals(true, uep.getCanView());
		assertFalse(uep.getCanEnableInheritance());
		assertEquals(node.getCreatedByPrincipalId(), uep.getOwnerPrincipalId());
		
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.CHANGE_PERMISSIONS);
		acl = entityAclManager.updateACL(acl, adminUser);
		assertFalse(uep.getCanEnableInheritance());
		
	}

	// test lack of access to something that doesn't inherit its permissions, whose parent you CAN access
	@Test
	public void testCantAccessNotInherited() throws Exception {		
		// no access yet to parent
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());

		AccessControlList acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl.setId(childNode.getId());
		entityAclManager.overrideInheritance(acl, adminUser); // must do as admin!
		// permissions haven't changed (yet)
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());
		assertFalse(authorizationManager.canAccess(userInfo, childNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());
		
		UserEntityPermissions uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanView());
		uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  childNode.getId());
		assertEquals(false, uep.getCanView());
		assertFalse(uep.getCanEnableInheritance());
		assertEquals(node.getCreatedByPrincipalId(), uep.getOwnerPrincipalId());
		
		// get a new copy of parent ACL
		acl = entityAclManager.getACL(node.getId(), userInfo);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.READ);
		acl = entityAclManager.updateACL(acl, adminUser);
		// should be able to access parent but not child
		assertTrue(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());
		assertFalse(authorizationManager.canAccess(userInfo, childNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());
		
		uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(true, uep.getCanView());
		uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  childNode.getId());
		assertEquals(false, uep.getCanView());
	}
	
	@Test
	public void testCreate() throws Exception {
		// make an object on which you have READ and WRITE permission
		AccessControlList acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.READ);
		acl = entityAclManager.updateACL(acl, adminUser);
		
		acl = entityAclManager.getACL(node.getId(), userInfo);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.UPDATE);
		acl = entityAclManager.updateACL(acl, adminUser);
		// now they should be able to access
		assertTrue(authorizationManager.canAccess(userInfo, node.getId(), ObjectType.ENTITY, ACCESS_TYPE.READ).isAuthorized());
		// but can't add a child 
		assertFalse(authorizationManager.canCreate(userInfo, node.getId(), EntityType.project).isAuthorized());
		
		// but give them create access to the parent
		acl = entityAclManager.getACL(node.getId(), userInfo);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.CREATE);
		acl = entityAclManager.updateACL(acl, adminUser);
		// now it can
		assertTrue(authorizationManager.canCreate(userInfo, node.getId(), EntityType.project).isAuthorized());
		
	}

	@Test
	public void testCreateSpecialUsers() throws Exception {
		// admin always has access 
		assertTrue(authorizationManager.canCreate(adminUser, node.getId(), EntityType.project).isAuthorized());

		// allow some access
		AccessControlList acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.CREATE);
		acl = entityAclManager.updateACL(acl, adminUser);
		// now they should be able to access
		assertTrue(authorizationManager.canCreate(userInfo, node.getId(), EntityType.project).isAuthorized());
		
		// but anonymous cannot
		assertFalse(authorizationManager.canCreate(anonInfo, node.getId(), EntityType.project).isAuthorized());
	}
	
	@Test
	public void testGetUserPermissionForOpenAccessEntityForAnonymous() throws Exception {
		AccessControlList acl = entityAclManager.getACL(node.getId(), anonInfo);
		acl = AuthorizationTestHelper.addToACL(acl, publicGroup, ACCESS_TYPE.READ);
		
		acl = entityAclManager.updateACL(acl, adminUser);
		
		// Set the entity as open access
		objectTypeManager.changeObjectsDataType(adminUser, node.getId(), ObjectType.ENTITY, DataType.OPEN_DATA);
		
		UserEntityPermissions uep = entityAuthorizationManager.getUserPermissionsForEntity(anonInfo, node.getId());
		
		assertEquals(false, uep.getCanAddChild());
		assertEquals(false, uep.getCanChangePermissions());
		assertEquals(false, uep.getCanChangeSettings());
		assertEquals(false, uep.getCanDelete());
		assertEquals(false, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		assertEquals(true, uep.getCanDownload());
		assertEquals(false, uep.getCanUpload());
	}

	@Test
	public void testGetUserPermissionsForEntity() throws Exception{
		assertTrue(adminUser.isAdmin());
		assertTrue(authenticationManager.hasUserAcceptedTermsOfUse(adminUser.getId()));
		// the admin user can do it all
		UserEntityPermissions uep = entityAuthorizationManager.getUserPermissionsForEntity(adminUser, node.getId());
		assertNotNull(uep);
		assertEquals(true, uep.getCanAddChild());
		assertEquals(true, uep.getCanChangePermissions());
		assertEquals(true, uep.getCanChangeSettings());
		assertEquals(true, uep.getCanDelete());
		assertEquals(true, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		assertEquals(true, uep.getCanDownload());
		assertEquals(true, uep.getCanUpload());
		
		// the user cannot do anything
		uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanAddChild());
		assertEquals(false, uep.getCanChangePermissions());
		assertEquals(false, uep.getCanChangeSettings());
		assertEquals(false, uep.getCanDelete());
		assertEquals(false, uep.getCanEdit());
		assertEquals(false, uep.getCanView());
		assertEquals(true, uep.getCanUpload());
		assertEquals(false, uep.getCanDownload());
		assertEquals(false, uep.getCanEnableInheritance());
		assertEquals(node.getCreatedByPrincipalId(), uep.getOwnerPrincipalId());
		
		// Let the user read and download
		AccessControlList acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.READ);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.DOWNLOAD);
		acl = entityAclManager.updateACL(acl, adminUser);
		
		uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanAddChild());
		assertEquals(false, uep.getCanChangePermissions());
		assertEquals(false, uep.getCanChangeSettings());
		assertEquals(false, uep.getCanDelete());
		assertEquals(false, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		assertEquals(true, uep.getCanDownload()); // CAN read and now CAN download
		assertEquals(true, uep.getCanUpload()); // can read and CAN upload, which is controlled separately
		
		// Let the user update.
		acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.UPDATE);
		acl = entityAclManager.updateACL(acl, adminUser);
		
		uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanAddChild());
		assertEquals(false, uep.getCanChangePermissions());
		assertEquals(false, uep.getCanChangeSettings());
		assertEquals(false, uep.getCanDelete());
		assertEquals(true, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		assertEquals(true, uep.getCanDownload()); // can't read but CAN download, which is controlled separately
		assertEquals(true, uep.getCanUpload()); // can't read but CAN upload, which is controlled separately
		
		// Let the user delete.
		acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.DELETE);
		acl = entityAclManager.updateACL(acl, adminUser);
		
		uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanAddChild());
		assertEquals(false, uep.getCanChangePermissions());
		assertEquals(false, uep.getCanChangeSettings());
		assertEquals(true, uep.getCanDelete());
		assertEquals(true, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		assertEquals(true, uep.getCanDownload()); // can't read but CAN download, which is controlled separately
		assertEquals(true, uep.getCanUpload()); // can't read but CAN upload, which is controlled separately
		
		// Let the user change permissions.
		acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.CHANGE_PERMISSIONS);
		acl = entityAclManager.updateACL(acl, adminUser);
		
		uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanAddChild());
		assertEquals(true, uep.getCanChangePermissions());
		assertEquals(false, uep.getCanChangeSettings());
		assertEquals(true, uep.getCanDelete());
		assertEquals(true, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		assertEquals(true, uep.getCanDownload()); // can't read but CAN download, which is controlled separately
		assertEquals(true, uep.getCanUpload()); // can't read but CAN upload, which is controlled separately
		
		// Let the user change create.
		acl = entityAclManager.getACL(node.getId(), userInfo);
		assertNotNull(acl);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.CREATE);
		acl = entityAclManager.updateACL(acl, adminUser);
		
		uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(true, uep.getCanAddChild());
		assertEquals(true, uep.getCanChangePermissions());
		assertEquals(false, uep.getCanChangeSettings());
		assertEquals(true, uep.getCanDelete());
		assertEquals(true, uep.getCanEdit());
		assertEquals(true, uep.getCanView());
		assertEquals(true, uep.getCanDownload()); // can't read but CAN download, which is controlled separately
		assertEquals(true, uep.getCanUpload()); // can't read but CAN upload, which is controlled separately
	}
	
	@Test
	public void testOwnerAdminAccess() throws Exception {
		// the user can't do anything
		UserEntityPermissions uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  node.getId());
		uep = entityAuthorizationManager.getUserPermissionsForEntity(userInfo,  node.getId());
		assertEquals(false, uep.getCanAddChild());
		assertEquals(false, uep.getCanChangePermissions());
		assertEquals(false, uep.getCanChangeSettings());
		assertEquals(false, uep.getCanDelete());
		assertEquals(false, uep.getCanEdit());
		assertEquals(false, uep.getCanView());
		assertEquals(false, uep.getCanDownload());
		assertEquals(true, uep.getCanUpload()); // can't read but CAN upload, which is controlled separately
		assertEquals(false, uep.getCanEnableInheritance());
	}
	
	@Test
	public void testCanAccessActivity() throws Exception {
		// create an activity 
		String activityId = activityManager.createActivity(userInfo, new Activity());
		assertNotNull(activityId);
		activitiesToDelete.add(activityId);
		nodeCreatedByTestUser.setActivityId(activityId);
		nodeManager.update(userInfo, nodeCreatedByTestUser, null, false);
		
		// test access
		boolean canAccess = authorizationManager.canAccessActivity(userInfo, activityId).isAuthorized();
		assertTrue(canAccess);
	}
	
	@Test
	public void testCanAccessActivityFail() throws Exception {
		// create an activity		
		String activityId = activityManager.createActivity(adminUser, new Activity());
		assertNotNull(activityId);
		activitiesToDelete.add(activityId);
		node.setActivityId(activityId);
		nodeManager.update(adminUser, node, null, false);
		
		// test access
		boolean canAccess = authorizationManager.canAccessActivity(userInfo, activityId).isAuthorized();
		assertFalse(canAccess);
	}
	
	@Test
	public void testIsAnonymousUser() throws DatastoreException, NotFoundException{
		assertNotNull(anonInfo);
		assertTrue(authorizationManager.isAnonymousUser(anonInfo));
		assertFalse(authorizationManager.isAnonymousUser(userInfo));
	}

	@Test
	public void testCanSubscribeAnonymous() {
		assertEquals(AuthorizationStatus.accessDenied(ANONYMOUS_ACCESS_DENIED_REASON),
				authorizationManager.canSubscribe(anonInfo, forumId, SubscriptionObjectType.FORUM));
	}

	@Test
	public void testCanAccessMembershipInvitation() {
		// Create invitation from team to userInfo
		MembershipInvitation mis = new MembershipInvitation();
		String misId = "1";
		mis.setId(misId);
		mis.setTeamId(team.getId());
		mis.setInviteeId(userInfo.getId().toString());
		mis.setMessage("Please join our team.");

		// Test all access types
		for (ACCESS_TYPE accessType : ACCESS_TYPE.values()) {
			// Invitee can only read or delete the invitation
			AuthorizationStatus inviteeAuthorization = authorizationManager.canAccessMembershipInvitation(userInfo, mis, accessType);
			if (accessType == ACCESS_TYPE.READ || accessType == ACCESS_TYPE.DELETE) {
				assertTrue(inviteeAuthorization.isAuthorized(), inviteeAuthorization.getMessage());
			} else {
				assertFalse(inviteeAuthorization.isAuthorized(), inviteeAuthorization.getMessage());
			}
			// Team admin can only create, read or delete the invitation
			AuthorizationStatus teamAdminAuthorization = authorizationManager.canAccessMembershipInvitation(teamAdmin, mis, accessType);
			if (accessType == ACCESS_TYPE.READ || accessType == ACCESS_TYPE.DELETE || accessType == ACCESS_TYPE.CREATE) {
				assertTrue(teamAdminAuthorization.isAuthorized(), teamAdminAuthorization.getMessage());
			} else {
				assertFalse(teamAdminAuthorization.isAuthorized(), teamAdminAuthorization.getMessage());
			}
			// Synapse admin has access of any type
			AuthorizationStatus adminAuthorization = authorizationManager.canAccessMembershipInvitation(adminUser, mis, accessType);
			assertTrue(adminAuthorization.isAuthorized(), adminAuthorization.getMessage());
		}
	}

	@Test
	public void testCanAccessMembershipInvitationWithMembershipInvtnSignedToken() {
		MembershipInvtnSignedToken token = new MembershipInvtnSignedToken();
		token.setMembershipInvitationId("validId");
		tokenGenerator.signToken(token);

		for (ACCESS_TYPE accessType : ACCESS_TYPE.values()) {
			AuthorizationStatus status = authorizationManager.canAccessMembershipInvitation(token, accessType);
			// Only reading is allowed
			if (accessType != ACCESS_TYPE.READ) {
				assertFalse(status.isAuthorized());
			} else {
				assertTrue(status.isAuthorized());
			}
		}

		token.setMembershipInvitationId("corruptedId");
		// Non valid signed token should be denied
		assertFalse(authorizationManager.canAccessMembershipInvitation(token, ACCESS_TYPE.READ).isAuthorized());
	}

	@Test
	public void testCanAccessMembershipInvitationWithInviteeVerificationSignedToken() {
		Long userId = 1L;
		InviteeVerificationSignedToken token = new InviteeVerificationSignedToken();
		token.setInviteeId(userId.toString());
		token.setMembershipInvitationId("validId");
		tokenGenerator.signToken(token);

		for (ACCESS_TYPE accessType : ACCESS_TYPE.values()) {
			// Only updating is allowed
			AuthorizationStatus status = authorizationManager.canAccessMembershipInvitation(userId, token, accessType);
			if (accessType != ACCESS_TYPE.UPDATE) {
				assertFalse(status.isAuthorized());
			} else {
				assertTrue(status.isAuthorized());
			}
		}

		// Incorrect user id should be denied
		Long incorrectUserId = 2L;
		assertFalse(authorizationManager.canAccessMembershipInvitation(incorrectUserId, token, ACCESS_TYPE.UPDATE).isAuthorized());
		// Invalid token should be denied
		token.setMembershipInvitationId("corruptedId");
		assertFalse(authorizationManager.canAccessMembershipInvitation(userId, token, ACCESS_TYPE.UPDATE).isAuthorized());
	}

	@Test
	public void testCanAccessMembershipRequest() {
		// Create request from userInfo to team
		MembershipRequest mr = new MembershipRequest();
		String mrId = "1";
		mr.setId(mrId);
		mr.setTeamId(team.getId());
		mr.setUserId(userInfo.getId().toString());
		mr.setMessage("Please let me join your team.");

		// Test all access types
		for (ACCESS_TYPE accessType : ACCESS_TYPE.values()) {
			// Invitee can only read or delete the invitation
			AuthorizationStatus inviteeAuthorization = authorizationManager.canAccessMembershipRequest(userInfo, mr, accessType);
			if (accessType == ACCESS_TYPE.READ || accessType == ACCESS_TYPE.DELETE) {
				assertTrue(inviteeAuthorization.isAuthorized(), inviteeAuthorization.getMessage());
			} else {
				assertFalse(inviteeAuthorization.isAuthorized(), inviteeAuthorization.getMessage());
			}
			// Team admin can only read or delete the request
			AuthorizationStatus teamAdminAuthorization = authorizationManager.canAccessMembershipRequest(teamAdmin, mr, accessType);
			if (accessType == ACCESS_TYPE.READ || accessType == ACCESS_TYPE.DELETE) {
				assertTrue(teamAdminAuthorization.isAuthorized(), teamAdminAuthorization.getMessage());
			} else {
				assertFalse(teamAdminAuthorization.isAuthorized(), teamAdminAuthorization.getMessage());
			}
			// Synapse admin has access of any type
			AuthorizationStatus adminAuthorization = authorizationManager.canAccessMembershipRequest(adminUser, mr, accessType);
			assertTrue(adminAuthorization.isAuthorized(), adminAuthorization.getMessage());
		}
	}
}
