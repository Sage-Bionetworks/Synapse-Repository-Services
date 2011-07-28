package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class PermissionsManagerImplTest {

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
//				System.out.println("Deleting: "+g);
				userGroupDAO.delete(g.getId());
			}
		}
		
	}
	
	@Test
	public void testGetACL() throws Exception {
		// retrieve parent acl
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		AccessControlList acl = permissionsManager.getACL(node.getId(), adminInfo);
		assertNotNull(acl);
		assertEquals(node.getId(), acl.getResourceId());
		// retrieve child acl.  should get parent's
		acl = permissionsManager.getACL(childNode.getId(), adminInfo);
		assertEquals(node.getId(), acl.getResourceId());
	}

	@Test
	public void testValidateContent()throws Exception {
//		fail("Not yet implemented");
	}

	@Test
	public void testUpdateACL() throws Exception {
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		AccessControlList acl = permissionsManager.getACL(node.getId(), adminInfo);
		assertEquals(1, acl.getResourceAccess().size());
		AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ, accessControlListDAO);
		// now get it again and see that the permission is there
		acl = permissionsManager.getACL(node.getId(), userInfo);
		Set<ResourceAccess> ras = acl.getResourceAccess();
		assertEquals(2, ras.size());
		Iterator<ResourceAccess> it = ras.iterator();
		boolean foundIt = false;
		while(it.hasNext()){
			ResourceAccess ra = it.next();
			if(ra.getUserGroupId().equals(userInfo.getIndividualGroup().getId())){
				assertEquals(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.READ})),
						ra.getAccessType());
				foundIt = true;
				break;
			}
		}
		assertTrue(foundIt);
	}

	@Test
	// check that you get an exception if...
	public void testUpdateInvalidACL() throws Exception {
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		AccessControlList acl = permissionsManager.getACL(node.getId(), adminInfo);
		assertEquals(1, acl.getResourceAccess().size());
		// ...resource id is null...
		acl.setResourceId(null);
		try {
			AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ, accessControlListDAO);
			fail("exception expected");
		} catch (InvalidModelException e) {
			//as expected
		}
		acl.setResourceId(node.getId());
		// ...group id is null...
		ResourceAccess ra = new ResourceAccess();
		ra.setUserGroupId(null);
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ra.setAccessType(ats);
		ra.getAccessType().add(ACCESS_TYPE.READ);
		acl.getResourceAccess().add(ra);
		try {
			accessControlListDAO.update(acl);
			fail("exception expected");
		} catch (InvalidModelException e) {
			//as expected
		}
//		// ... no access type is specified
//		acl.setResourceAccess(new HashSet<ResourceAccess>());
//		ra = new ResourceAccess();
//		ra.setUserGroupId(userInfo.getIndividualGroup().getId());
//		ats = new HashSet<ACCESS_TYPE>();
//		ra.setAccessType(ats);
//		acl.getResourceAccess().add(ra);
//		try {
//			accessControlListDAO.update(acl);
//			fail("exception expected");
//		} catch (InvalidModelException e) {
//			//as expected
//		}
	}

	@Test
	public void testOverrideInheritance() throws Exception {
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		AccessControlList acl = permissionsManager.getACL(node.getId(), adminInfo);
		acl.setResourceId(childNode.getId());
		permissionsManager.overrideInheritance(acl, adminInfo);
		// call 'getACL':  the ACL should match the requested settings and specify the resource as the owner of the ACL
		AccessControlList acl2 = permissionsManager.getACL(childNode.getId(), adminInfo);
		assertEquals(childNode.getId(), acl2.getResourceId());
		assertEquals(acl, acl2);
	}

	@Test
	public void testOverrideInheritanceEdgeCases() throws Exception {
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		AccessControlList acl = permissionsManager.getACL(node.getId(), adminInfo);
		// should get exception if object already has an acl
		try {
			permissionsManager.overrideInheritance(acl, adminInfo);
			fail("exception expected");
		} catch (UnauthorizedException ue) {
			// as expected
		}
		// should get exception if you don't have authority to change permissions
		try {
			permissionsManager.overrideInheritance(acl, userInfo);
			fail("exception expected");
		} catch (UnauthorizedException ue) {
			// as expected
		}
	}

	@Test
	public void testRestoreInheritance() throws Exception {
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		AccessControlList acl = permissionsManager.getACL(childNode.getId(), adminInfo);
		acl.setResourceId(childNode.getId());
		permissionsManager.overrideInheritance(acl, adminInfo);
		AccessControlList acl2 = permissionsManager.getACL(childNode.getId(), adminInfo);
		assertEquals(childNode.getId(), acl2.getResourceId());
		assertEquals(acl, acl2);
		
		permissionsManager.restoreInheritance(childNode.getId(), adminInfo);
		// call 'getACL' on the resource.  The returned ACL should specify parent as the ACL owner
		AccessControlList acl3 = permissionsManager.getACL(childNode.getId(), adminInfo);
		assertEquals(node.getId(), acl3.getResourceId());

		
	}

	@Test
	public void testRestoreInheritanceEdgeCases() throws Exception {
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		// should get exception if resource already inherits
		try {
			permissionsManager.restoreInheritance(childNode.getId(), adminInfo);
			fail("exception expected");
		} catch (UnauthorizedException ue) {
			// as expected
		}
		
		AccessControlList acl = permissionsManager.getACL(childNode.getId(), adminInfo);
		acl.setResourceId(childNode.getId());
		permissionsManager.overrideInheritance(acl, adminInfo);
		
		// should get exception if don't have authority to change permissions
		try {
			permissionsManager.restoreInheritance(childNode.getId(), userInfo);
			fail("exception expected");
		} catch (UnauthorizedException ue) {
			// as expected
		}
		
	}

}
