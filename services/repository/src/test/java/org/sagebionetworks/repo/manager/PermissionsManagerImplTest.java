package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:manager-test-context.xml" })
public class PermissionsManagerImplTest {

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
	
	@Autowired
	PermissionsManager permissionsManager;
		
	private Collection<Node> nodeList = new ArrayList<Node>();
	private Node node = null;
	private Node childNode = null;
//	private UserInfo userInfo = null;
	
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
		Node node = createDTO(name, createdBy, modifiedBy, parentId);
		String nodeId = nodeDAO.createNew(node);
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
//		userInfo = userManager.getUserInfo(TEST_USER);
	}

	@After
	public void tearDown() throws Exception {
		for (Node n : nodeList) nodeDAO.delete(n.getId());
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
	@Ignore
	public void testGetACL() throws Exception {
		// retrieve parent acl
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		AccessControlList acl = permissionsManager.getACL(node.getId(), adminInfo);
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
		// check that you get an exception if
		// group id is null
		// resource id is null
		// no access type is specified
//		fail("Not yet implemented");
	}

	@Test
	public void testOverrideInheritance() {
		// should get exception if object already has an acl
		// should get exception if you don't have authority to change permissions
		// call 'getACL':  the ACL should match the requested settings and specify the resource as the owner of the ACL
//		fail("Not yet implemented");
	}

	@Test
	public void testRestoreInheritance() {
		// should get exception if resource already inherits
		// should get exception if don't have authority to change permissions
		// should get exception if resource doen't have a parent
		// call 'getACL' on the resource.  The returned ACL should specify someone else as the ACL owner
//		fail("Not yet implemented");
	}

}
