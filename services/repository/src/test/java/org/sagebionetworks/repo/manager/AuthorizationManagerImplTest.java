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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:manager-test-context.xml" })
public class AuthorizationManagerImplTest {
	
	@Autowired
	AuthorizationManager authorizationManager;
	@Autowired
	private NodeDAO nodeDAO;
	@Autowired
	private UserManager userManager;
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	AccessControlListDAO accessControlListDAO;
		
	private Collection<Node> nodeList = new ArrayList<Node>();
	private Node node = null;
	private UserInfo userInfo = null;
	
	private static final String TEST_USER = "test-user";
	
	private AccessControlList acl;

	@Before
	public void setUp() throws Exception {
		// create a resource
		node = new Node();
		node.setName("foo");
		node.setCreatedOn(new Date());
		node.setCreatedBy("me");
		node.setModifiedOn(new Date());
		node.setModifiedBy("metoo");
		node.setNodeType(ObjectType.project.name());
		String nodeId = nodeDAO.createNew(node);
		assertNotNull(nodeId);
		node.setId(nodeId);
		nodeList.add(node);
		
		
//		acl = new AccessControlList();
//		acl.setCreationDate(new Date());
//		acl.setCreatedBy("me");
//		acl.setModifiedOn(new Date());
//		acl.setModifiedBy("you");
//		acl.setResourceId(node.getId());
//		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
////		ResourceAccess ra = new ResourceAccess();
////		ra.setUserGroupId(group.getId());
////		ra.setAccessType(new HashSet<AuthorizationConstants.ACCESS_TYPE>(
////				Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[]{
////						AuthorizationConstants.ACCESS_TYPE.READ
////				})));
////		ras.add(ra);
//		acl.setResourceAccess(ras);
//		String id = accessControlListDAO.create(acl);
//		acl.setId(id);
////		aclList.add(acl);
		
		// userInfo
		userManager.setUserDAO(new TestUserDAO()); // could use Mockito here
		userInfo = userManager.getUserInfo(TEST_USER);
	}

	@After
	public void tearDown() throws Exception {
		for (Node n : nodeList) nodeDAO.delete(n.getId());
		this.node=null;
		
//		for (UserGroup g : userGroupDAO.getAll(true)) 
//			userGroupDAO.delete(g.getId());
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
	
	private void addToACL(AccessControlList acl, UserGroup ug, ACCESS_TYPE at) throws Exception {
		Set<ResourceAccess> ras = null;
		if (acl.getResourceAccess()==null) {
			ras = new HashSet<ResourceAccess>();
		} else {
			ras = new HashSet<ResourceAccess>(acl.getResourceAccess());
		}
		ResourceAccess ra = new ResourceAccess();
		ra.setUserGroupId(ug.getId());
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ats.add(ACCESS_TYPE.READ);
		ra.setAccessType(ats);
		ras.add(ra);
		acl.setResourceAccess(ras);
		accessControlListDAO.update(acl);
	}
	
	@Test
	public void testCanAccessAsIndividual() throws Exception {
		// test that a user can access something they've been given access to individually
		boolean b = authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ);
		// no access yet
		assertFalse(b);
		AccessControlList acl = accessControlListDAO.getForResource(node.getId());
		assertNotNull(acl);
		addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ);
		// now they should be able to access
		b = authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ);
		assertTrue(b);
		// but they do not have a different kind of access
		b = authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.DELETE);
		assertFalse(b);
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
		addToACL(acl, g, ACCESS_TYPE.READ);
		// now they should be able to access
		b = authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ);
		assertTrue(b);
	}
	
	@Test 
	public void testCanAccessPublicGroup() throws Exception {
		// test that a user can access something accessible to a group they belong to
		boolean b = authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ);
		// no access yet
		assertFalse(b);
		AccessControlList acl = accessControlListDAO.getForResource(node.getId());
		assertNotNull(acl);
		UserGroup pg = userGroupDAO.findGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false);
		addToACL(acl, pg, ACCESS_TYPE.READ);
		// now they should be able to access
		b = authorizationManager.canAccess(userInfo, node.getId(), ACCESS_TYPE.READ);
		assertTrue(b);
	}
	
	@Test
	public void testCanAccessAsAnonymous() throws Exception {
		UserInfo anonInfo = userManager.getUserInfo(AuthorizationConstants.ANONYMOUS_USER_ID);
		AccessControlList acl = accessControlListDAO.getForResource(node.getId());
		assertNotNull(acl);
		// give some other group access
		UserGroup g = userGroupDAO.findGroup(TestUserDAO.TEST_GROUP_NAME, false);
		addToACL(acl, g, ACCESS_TYPE.READ);

		// anonymous does not have access
		boolean b = authorizationManager.canAccess(anonInfo, node.getId(), ACCESS_TYPE.READ);
		assertFalse(b);
	}
	
	@Test
	public void testCanAccessx() {
		// test that anonymous can't access something a group can access
		// test that an admin can access anything
		// test that a user can access a Public resource
		// test that anonymous can access a Public resource
		
		// test access to something that inherits its permissions
		// test lack of access to something that doesn't inherit its permissions, whose parent you CAN access
//		fail("Not yet implemented");
	}

	@Test
	public void testCanCreate() {
		// make an object on which you have READ and WRITE permission
		// try to add a child
		// should fail
		// add CREATE permission on the parent
		// try to add the child
		// should be successful
		
		// admin should be able to add child, without any explicit permissions
		// anonymous should not be able to add child
		
		// try to create node with no parent.  should fail
		// admin creates a node with no parent.  should work
//		fail("Not yet implemented");
	}

}
