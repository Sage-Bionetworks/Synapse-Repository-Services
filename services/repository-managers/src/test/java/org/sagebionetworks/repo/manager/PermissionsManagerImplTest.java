package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
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
	private Node parentNode = null;
	private Node childNode = null;
	private Node grandchildNode0 = null;
	private Node grandchildNode1 = null;
	private UserInfo userInfo = null;
	
	private static final String TEST_USER = "test-user";
	
	private List<String> usersToDelete;
	
	private Node createDTO(String name, Long createdBy, Long modifiedBy, String parentId) {
		Node node = new Node();
		node.setName(name);
		node.setCreatedOn(new Date());
		node.setCreatedByPrincipalId(createdBy);
		node.setModifiedOn(new Date());
		node.setModifiedByPrincipalId(modifiedBy);
		node.setNodeType(EntityType.project.name());
		if (parentId!=null) node.setParentId(parentId);
		return node;
	}
	
	private Node createNode(String name, Long createdBy, Long modifiedBy, String parentId) throws Exception {
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
		parentNode = createNode("foo", 1L, 2L, null);
		nodeList.add(parentNode);
				
		childNode = createNode("foo2", 3L, 4L, parentNode.getId());
		grandchildNode0 = createNode("foo3", 5L, 6L, childNode.getId());
		grandchildNode1 = createNode("foo4", 7L, 8L, childNode.getId());
		
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
		this.parentNode=null;
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
		AccessControlList acl = permissionsManager.getACL(parentNode.getId(), adminInfo);
		assertNotNull(acl);
		assertEquals(parentNode.getId(), acl.getId());
		for (ResourceAccess ra : acl.getResourceAccess()) {
			// ra should have pId but not 'groupName' which is deprecated
			assertNull(ra.getGroupName());
			assertNotNull(ra.getPrincipalId());
		}
		// retrieve child acl.  should get parent's
		try{
			acl = permissionsManager.getACL(childNode.getId(), adminInfo);
			fail("Get ACL on a node that inherits it permission should throw an exception");
		}catch (ACLInheritanceException e){
			// The exception should tell us the benefactor
			assertEquals(parentNode.getId(), e.getBenefactorId());
		}

	}

	@Test
	public void testValidateContent()throws Exception {
//		fail("Not yet implemented");
	}

	@Test
	public void testUpdateACL() throws Exception {
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		AccessControlList acl = permissionsManager.getACL(parentNode.getId(), adminInfo);
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
		acl = permissionsManager.getACL(parentNode.getId(), userInfo);
		Set<ResourceAccess> ras = acl.getResourceAccess();
		assertEquals(2, ras.size());
		Iterator<ResourceAccess> it = ras.iterator();
		boolean foundIt = false;
		while(it.hasNext()){
			ResourceAccess ra = it.next();
			if(ra.getPrincipalId().toString().equals(userInfo.getIndividualGroup().getId())){
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
		AccessControlList acl = permissionsManager.getACL(parentNode.getId(), adminInfo);
		assertEquals(1, acl.getResourceAccess().size());
		// Change the etag so that it does not match the current value
		acl.setEtag(acl.getEtag()+"1");
		permissionsManager.updateACL(acl, adminInfo);
	}

	@Test
	// check that you get an exception if...
	public void testUpdateInvalidACL() throws Exception {
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		AccessControlList acl = permissionsManager.getACL(parentNode.getId(), adminInfo);
		assertEquals(1, acl.getResourceAccess().size());
		acl = AuthorizationHelper.addToACL(acl, userInfo.getIndividualGroup(), ACCESS_TYPE.READ);
		acl = permissionsManager.updateACL(acl, adminInfo);
		acl.setId(parentNode.getId());
		// ...group id is null...
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(null);
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ra.setAccessType(ats);
		ra.getAccessType().add(ACCESS_TYPE.READ);
		acl.getResourceAccess().add(ra);
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
		AccessControlList acl = permissionsManager.getACL(parentNode.getId(), adminInfo);
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
		assertNotNull(parentNode.getId(), results.getId());
		assertNotNull(parentNode.getETag(), results.getEtag());
		childNode = nodeManager.get(adminInfo, childNode.getId());
		assertFalse("The etag of the child node should have changed", eTagBefore.equals(childNode.getETag()));
		// call 'getACL' on the resource.  The returned ACL should specify parent as the ACL owner
		try{
			AccessControlList acl3 = permissionsManager.getACL(childNode.getId(), adminInfo);
			fail("Should not have been able to get the ACL for a node that inherits");
		}catch (ACLInheritanceException e){
			// Get the parent ACL
			assertEquals(parentNode.getId(), e.getBenefactorId());
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

	@Test
	public void testApplyInheritanceToChildren() throws Exception {
		// retrieve parent acl
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		AccessControlList parentAcl = permissionsManager.getACL(parentNode.getId(), adminInfo);
		assertNotNull(parentAcl);
		assertEquals(parentNode.getId(), parentAcl.getId());

		// retrieve child acl - should get parent's
		verifyInheritedAcl(childNode, parentNode.getId(), adminInfo);
		
		// assign new ACL to child
		AccessControlList childAcl = new AccessControlList();
		childAcl.setId(childNode.getId());
		String eTagBefore = childNode.getETag();
		assertNotNull(eTagBefore);
		permissionsManager.overrideInheritance(childAcl, adminInfo);
		
		// assign new ACL to grandchild0
		AccessControlList grandchild0Acl = new AccessControlList();
		grandchild0Acl.setId(grandchildNode0.getId());
		eTagBefore = grandchildNode0.getETag();
		assertNotNull(eTagBefore);
		permissionsManager.overrideInheritance(grandchild0Acl, adminInfo);
		
		// retrieve child acl - should get child's
		AccessControlList returnedAcl = permissionsManager.getACL(childNode.getId(), adminInfo);
		assertEquals("Child ACL not set properly", childAcl, returnedAcl);
		assertFalse("Child ACL should not match parent ACL", parentAcl.equals(returnedAcl));
		
		// retrieve grandchild0 acl - should get grandchild0's
		returnedAcl = permissionsManager.getACL(grandchildNode0.getId(), adminInfo);
		assertEquals("Grandchild ACL not set properly", grandchild0Acl, returnedAcl);
		assertFalse("Grandchild ACL should not match child ACL", childAcl.equals(returnedAcl));
		assertFalse("Grandchild ACL should not match parent ACL", parentAcl.equals(returnedAcl));
		
		// retrieve grandchild1 acl - should get child's
		verifyInheritedAcl(grandchildNode1, childNode.getId(), adminInfo);
		
		// apply inheritance to children
		permissionsManager.applyInheritanceToChildren(parentNode.getId(), adminInfo);
		
		// retrieve all descendant acls - should get parent's
		verifyInheritedAcl(childNode, parentNode.getId(), adminInfo);
		verifyInheritedAcl(grandchildNode0, parentNode.getId(), adminInfo);
		verifyInheritedAcl(grandchildNode1, parentNode.getId(), adminInfo);
	}
	
	@Test
	public void testApplyInheritanceToChildrenNotAuthorized() throws Exception {
		// retrieve parent acl
		UserInfo adminInfo = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
		AccessControlList parentAcl = permissionsManager.getACL(parentNode.getId(), adminInfo);
		assertNotNull(parentAcl);
		assertEquals(parentNode.getId(), parentAcl.getId());
		
		// assign new ACL to child
		AccessControlList childAcl = new AccessControlList();
		childAcl.setId(childNode.getId());
		String eTagBefore = childNode.getETag();
		assertNotNull(eTagBefore);
		permissionsManager.overrideInheritance(childAcl, adminInfo);
		
		// assign new ACL to grandchild0
		AccessControlList grandchild0Acl = new AccessControlList();
		grandchild0Acl.setId(grandchildNode0.getId());
		eTagBefore = grandchildNode0.getETag();
		assertNotNull(eTagBefore);
		permissionsManager.overrideInheritance(grandchild0Acl, adminInfo);
		
		// authorize test user to change permissions of parent and child nodes
		parentAcl = AuthorizationHelper.addToACL(parentAcl, userInfo.getIndividualGroup(), ACCESS_TYPE.CHANGE_PERMISSIONS);
		parentAcl = permissionsManager.updateACL(parentAcl, adminInfo);
		childAcl = AuthorizationHelper.addToACL(childAcl, userInfo.getIndividualGroup(), ACCESS_TYPE.CHANGE_PERMISSIONS);
		childAcl = permissionsManager.updateACL(childAcl, adminInfo);
		
		// apply inheritance to children as test user
		permissionsManager.applyInheritanceToChildren(parentNode.getId(), userInfo);
		
		// retrieve child and grandchild1 acls - should get parent's (authorized to change permissions)
		verifyInheritedAcl(childNode, parentNode.getId(), adminInfo);
		verifyInheritedAcl(grandchildNode1, parentNode.getId(), adminInfo);
		
		// retrieve grandchild0 acl - should get grandchild0's (not authorized to change permissions)
		try {
			AccessControlList returnedAcl = permissionsManager.getACL(grandchildNode0.getId(), adminInfo);
			assertEquals("Grandchild ACL not set properly", grandchild0Acl, returnedAcl);
		} catch (ACLInheritanceException e) {
			fail("Grandchild ACL was overwritten without authorization");
		}
		
	}
	
	private void verifyInheritedAcl(Node node, String expectedBenefactorId,  UserInfo userInfo) 
			throws NotFoundException, DatastoreException {
		try {
			permissionsManager.getACL(node.getId(), userInfo);
			fail("Node should be inheriting ACL, but it has its own local ACL.");
		} catch (ACLInheritanceException e){
			// The exception should tell us the benefactor
			assertEquals(expectedBenefactorId, e.getBenefactorId());
		}	
	}
}
