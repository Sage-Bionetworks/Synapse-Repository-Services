package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
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
	PermissionsManager permissionsManager;
		
	private Collection<Node> nodeList = new ArrayList<Node>();
	private Node node = null;
	private Node childNode = null;
	private UserInfo userInfo = null;
	
	private static final String TEST_USER = "test-user";
	
	private List<String> usersToDelete;
	
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
		return nodeManager.get(adminUser, nodeId);
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
		usersToDelete = new ArrayList<String>();
		usersToDelete.add(userInfo.getIndividualGroup().getId());
		usersToDelete.add(userInfo.getUser().getId());
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
	}
	
	@Test
	public void testGetACL() throws Exception {
		// retrieve parent acl
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		AccessControlList acl = permissionsManager.getACL(node.getId(), adminInfo);
		assertNotNull(acl);
		assertEquals(node.getId(), acl.getId());
		// retrieve child acl.  should get parent's
		try{
			acl = permissionsManager.getACL(childNode.getId(), adminInfo);
			fail("Get ACL on a node that inherits it permission should throw an exception");
		}catch (ACLInheritanceException e){
			// The exception should tell us the benefactor
			assertEquals(node.getId(), e.getBenefactorId());
			assertEquals(node.getNodeType(), e.getBenefactorType().name());
		}

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
		// Check the etag before
		String etagBefore = acl.getEtag();
		assertNotNull(etagBefore);
		acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ);
		acl = permissionsManager.updateACL(acl, adminInfo);
		// The etag should have changed
		assertNotNull(acl.getEtag());
		assertFalse(etagBefore.equals(acl.getEtag()));
		
		// now get it again and see that the permission is there
		acl = permissionsManager.getACL(node.getId(), userInfo);
		Set<ResourceAccess> ras = acl.getResourceAccess();
		assertEquals(2, ras.size());
		Iterator<ResourceAccess> it = ras.iterator();
		boolean foundIt = false;
		while(it.hasNext()){
			ResourceAccess ra = it.next();
			if(ra.getGroupName().equals(userInfo.getIndividualGroup().getName())){
				assertEquals(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.READ})),
						ra.getAccessType());
				foundIt = true;
				break;
			}
		}
		assertTrue(foundIt);
	}
	
	@Test (expected=ConflictingUpdateException.class)
	public void testConcurrentUpdate() throws DatastoreException, NotFoundException, InvalidModelException, UnauthorizedException, ConflictingUpdateException, ACLInheritanceException{
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		AccessControlList acl = permissionsManager.getACL(node.getId(), adminInfo);
		assertEquals(1, acl.getResourceAccess().size());
		// Change the etag so that it does not match the current value
		acl.setEtag(acl.getEtag()+"1");
		permissionsManager.updateACL(acl, adminInfo);
	}

	@Test
	// check that you get an exception if...
	public void testUpdateInvalidACL() throws Exception {
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		AccessControlList acl = permissionsManager.getACL(node.getId(), adminInfo);
		assertEquals(1, acl.getResourceAccess().size());
		try {
			acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ);
			acl.setCreatedBy(null);
			acl = permissionsManager.updateACL(acl, adminInfo);
			fail("exception expected");
		} catch (InvalidModelException e) {
			//as expected
		}
		acl.setId(node.getId());
		// ...group id is null...
		ResourceAccess ra = new ResourceAccess();
		ra.setGroupName(null);
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ra.setAccessType(ats);
		ra.getAccessType().add(ACCESS_TYPE.READ);
		acl.getResourceAccess().add(ra);
		acl.setCreatedBy(null);
		try {
			permissionsManager.updateACL(acl, adminInfo);
			fail("exception expected");
		} catch (InvalidModelException e) {
			//as expected
		}
	}

	@Test
	public void testOverrideInheritance() throws Exception {
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		AccessControlList acl = new AccessControlList();
		acl.setId(childNode.getId());
		String eTagBefore = childNode.getETag();
		assertNotNull(eTagBefore);
		AccessControlList results = permissionsManager.overrideInheritance(acl, adminInfo);
		assertNotNull(results);
		assertNotNull(results.getEtag());
		assertFalse("The Etag should have changed", eTagBefore.equals(results.getEtag()));
		assertEquals(childNode.getId(), results.getId());
		// The etag should match the node's tag
		childNode = nodeManager.get(adminInfo, childNode.getId());
		assertEquals(childNode.getETag(), results.getEtag());
		// call 'getACL':  the ACL should match the requested settings and specify the resource as the owner of the ACL
		AccessControlList acl2 = permissionsManager.getACL(childNode.getId(), adminInfo);
		assertEquals(childNode.getId(), acl2.getId());
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
		AccessControlList acl = null;
		try{
			// This will fail but we can use it to get the benefactor
			acl = permissionsManager.getACL(childNode.getId(), adminInfo);
			fail("Should not have been able to get the ACL for a node that inherits");
		}catch (ACLInheritanceException e){
			// Get the parent ACL
			acl = permissionsManager.getACL(e.getBenefactorId(), adminInfo);
		}
		// Set the ID to the child.
		acl.setId(childNode.getId());
		permissionsManager.overrideInheritance(acl, adminInfo);
		AccessControlList acl2 = permissionsManager.getACL(childNode.getId(), adminInfo);
		assertEquals(childNode.getId(), acl2.getId());
		assertEquals(acl, acl2);
		String eTagBefore = childNode.getETag();
		assertNotNull(eTagBefore);
		AccessControlList results = permissionsManager.restoreInheritance(childNode.getId(), adminInfo);
		assertNotNull(results);
		assertNotNull(results.getEtag());
		assertNotNull(node.getId(), results.getId());
		assertNotNull(node.getETag(), results.getEtag());
		childNode = nodeManager.get(adminInfo, childNode.getId());
		assertFalse("The etag of the child node should have changed", eTagBefore.equals(childNode.getETag()));
		// call 'getACL' on the resource.  The returned ACL should specify parent as the ACL owner
		try{
			AccessControlList acl3 = permissionsManager.getACL(childNode.getId(), adminInfo);
			fail("Should not have been able to get the ACL for a node that inherits");
		}catch (ACLInheritanceException e){
			// Get the parent ACL
			assertEquals(node.getId(), e.getBenefactorId());
		}
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
		AccessControlList acl = null;
		try{
			 permissionsManager.getACL(childNode.getId(), adminInfo);
			 fail();
		}catch(ACLInheritanceException e){
			acl = permissionsManager.getACL(e.getBenefactorId(), adminInfo);
		}

		acl.setId(childNode.getId());
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
