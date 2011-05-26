package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AuthorizationManagerImplTest {
	
	@Autowired
	AuthorizationManager authorizationManager;
	@Autowired
	NodeManager nodeManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	AccessControlListDAO accessControlListDAO;
	@Autowired
	PermissionsManager permissionsManager;
	
		
	private Collection<Node> nodeList = new ArrayList<Node>();
	private Node node = null;
	private Node childNode = null;
	private UserInfo userInfo = null;
	
	private static final String TEST_USER = "test-user";
	
	private Node createDTO(String name, String createdBy, String modifiedBy, String parentId) {
		Node node = new Node();
		node.setName(name);
		node.setCreatedOn(new Date());
		node.setCreatedBy(createdBy);
		node.setModifiedOn(new Date());
		node.setModifiedBy(modifiedBy);
		node.setNodeType(ObjectType.project.name());
		if (parentId!=null) node.setParentId(parentId);
		return node;
	}
	
	private Node createNode(String name, String createdBy, String modifiedBy, String parentId) throws Exception {
		UserInfo adminUser = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		Node node = createDTO(name, createdBy, modifiedBy, parentId);
		String nodeId = nodeManager.createNewNode(node, adminUser);
		assertNotNull(nodeId);
		node.setId(nodeId);
		return node;
	}

	@Before
	public void setUp() throws Exception {
		// create a resource
		node = createNode("foo", "me", "metoo", null);
		nodeList.add(node);
				
		childNode = createNode("foo2", "me2", "metoo2", node.getId());
		
		// userInfo
		userManager.setUserDAO(new TestUserDAO()); // could use Mockito here
		userInfo = userManager.getUserInfo(TEST_USER);
	}

	@After
	public void tearDown() throws Exception {
		UserInfo adminUser = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		for (Node n : nodeList) nodeManager.delete(adminUser, n.getId());
		this.node=null;
		
		for (UserGroup g: userGroupDAO.getAll(true)) {
			if (g.getName().equals(AuthorizationConstants.ADMIN_GROUP_NAME)) {
				// leave it
			} else if (g.getName().equals(AuthorizationConstants.PUBLIC_GROUP_NAME)) {
				// leave it
			} else if (g.getName().equals(AuthorizationConstants.ANONYMOUS_USER_ID)) {
				// leave it
			} else {
				userGroupDAO.delete(g.getId());
			}
		}
		
	}
	
	@Test
	public void testCanAccessAsIndividual() throws Exception {
		// test that a user can access something they've been given access to individually
		// no access yet
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ));
		AccessControlList acl = accessControlListDAO.getForResource(node.getId());
		assertNotNull(acl);
		AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ, accessControlListDAO);
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
		AccessControlList acl = accessControlListDAO.getForResource(node.getId());
		assertNotNull(acl);
		UserGroup g = userGroupDAO.findGroup(TestUserDAO.TEST_GROUP_NAME, false);
		AuthorizationHelper.addToACL(acl, g, ACCESS_TYPE.READ, accessControlListDAO);
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
		AccessControlList acl = accessControlListDAO.getForResource(node.getId());
		assertNotNull(acl);
		UserGroup pg = userGroupDAO.findGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false);
		AuthorizationHelper.addToACL(acl, pg, ACCESS_TYPE.READ, accessControlListDAO);
		// now they should be able to access
		b = authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ);
		assertTrue(b);
	}
	
	@Test 
	public void testAnonymousCantAccessPublicGroup() throws Exception {
		AccessControlList acl = accessControlListDAO.getForResource(node.getId());
		assertNotNull(acl);
		UserGroup pg = userGroupDAO.findGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false);
		AuthorizationHelper.addToACL(acl, pg, ACCESS_TYPE.READ, accessControlListDAO);
		
		UserInfo anonInfo = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		boolean b = authorizationManager.canAccess(anonInfo, node.getId(), ACCESS_TYPE.READ);
		assertFalse(b);
	}
	
	@Test
	public void testCanAccessAsAnonymous() throws Exception {
		UserInfo anonInfo = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		AccessControlList acl = accessControlListDAO.getForResource(node.getId());
		assertNotNull(acl);
		// give some other group access
		UserGroup g = userGroupDAO.findGroup(TestUserDAO.TEST_GROUP_NAME, false);
		AuthorizationHelper.addToACL(acl, g, ACCESS_TYPE.READ, accessControlListDAO);

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
	public void testCanAccessInherited() throws Exception {		
		// no access yet to parent
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ));

		AccessControlList acl = accessControlListDAO.getForResource(node.getId());
		assertNotNull(acl);
		AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ, accessControlListDAO);
		// now they should be able to access
		assertTrue(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ));
		// and the child as well
		assertTrue(authorizationManager.canAccess(userInfo, childNode.getId(), ACCESS_TYPE.READ));
	}

	// test lack of access to something that doesn't inherit its permissions, whose parent you CAN access
	@Test
	public void testCantAccessNotInherited() throws Exception {		
		// no access yet to parent
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ));

		AccessControlList acl = accessControlListDAO.getForResource(node.getId());
		assertNotNull(acl);
		acl.setResourceId(childNode.getId());
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		permissionsManager.overrideInheritance(acl, adminInfo); // must do as admin!
		// permissions haven't changed (yet)
		assertFalse(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ));
		assertFalse(authorizationManager.canAccess(userInfo, childNode.getId(), ACCESS_TYPE.READ));
		
		// get a new copy of parent ACL
		acl = accessControlListDAO.getForResource(node.getId());
		AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ, accessControlListDAO);
		// should be able to access parent but not child
		assertTrue(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ));
		assertFalse(authorizationManager.canAccess(userInfo, childNode.getId(), ACCESS_TYPE.READ));
	}
	
	@Test
	public void testCreate() throws Exception {
		// make an object on which you have READ and WRITE permission
		AccessControlList acl = accessControlListDAO.getForResource(node.getId());
		assertNotNull(acl);
		AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ, accessControlListDAO);
		AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.UPDATE, accessControlListDAO);
		// now they should be able to access
		assertTrue(authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ));				
		// but can't add a child 
		Node child = createDTO("child", "me4", "you4", node.getId());
		assertFalse(authorizationManager.canCreate(userInfo, child));
		
		// but give them create access to the parent
		AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.CREATE, accessControlListDAO);
		// now it can
		assertTrue(authorizationManager.canCreate(userInfo, child));
	}

	@Test
	public void testCreateSpecialUsers() throws Exception {
		// admin always has access 
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		Node child = createDTO("child", "me4", "you4", node.getId());
		assertTrue(authorizationManager.canCreate(adminInfo, child));

		// allow some access
		AccessControlList acl = accessControlListDAO.getForResource(node.getId());
		assertNotNull(acl);
		AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.CREATE, accessControlListDAO);
		// now they should be able to access
		assertTrue(authorizationManager.canCreate(userInfo, child));
		
		// but anonymous cannot
		UserInfo anonInfo = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		assertFalse(authorizationManager.canCreate(anonInfo, child));
	}

	@Test
	public void testCreateNoParent() throws Exception {
	
		// try to create node with no parent.  should fail
		Node orphan = createDTO("orphan", "me4", "you4", null);
		assertFalse(authorizationManager.canCreate(userInfo, orphan));
		
		// admin creates a node with no parent.  should work
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		assertTrue(authorizationManager.canCreate(adminInfo, orphan));

	}

}
