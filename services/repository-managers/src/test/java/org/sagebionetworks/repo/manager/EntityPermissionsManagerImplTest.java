package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityPermissionsManagerImplTest {
	
	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	
	@Autowired
	private AccessRequirementManager accessRequirementManager;
	
	@Autowired
	private DBOBasicDao basicDao;

	private Collection<Node> nodeList = new ArrayList<Node>();
	private Node project = null;
	private Node childNode = null;
	private Node grandchildNode0 = null;
	private Node grandchildNode1 = null;
	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private UserInfo otherUserInfo;
	private static Long ownerId;
	private String arId;
	
	private Node createDTO(String name, Long createdBy, Long modifiedBy, String parentId) {
		Node node = new Node();
		node.setName(name);
		node.setCreatedOn(new Date());
		node.setCreatedByPrincipalId(createdBy);
		node.setModifiedOn(new Date());
		node.setModifiedByPrincipalId(modifiedBy);
		node.setNodeType(EntityType.project);
		if (parentId!=null) node.setParentId(parentId);
		return node;
	}
	
	private Node createNode(String name, Long createdBy, Long modifiedBy, String parentId) throws Exception {
		Node node = createDTO(name, createdBy, modifiedBy, parentId);
		String nodeId = nodeManager.createNewNode(node, adminUserInfo);
		assertNotNull(nodeId);
		node.setId(nodeId);
		return nodeManager.get(adminUserInfo, nodeId);
	}

	@Before
	public void setUp() throws Exception {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setAgreesToTermsOfUse(Boolean.TRUE);
		
		DBOCredential cred = new DBOCredential();
		cred.setSecretKey("");
		
		// Need two users for this test
		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		userInfo = userManager.createUser(adminUserInfo, nu, cred, tou);
		
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		otherUserInfo = userManager.createUser(adminUserInfo, nu, cred, tou);
		
		tou.setPrincipalId(otherUserInfo.getId());
		basicDao.createOrUpdate(tou);
		
		ownerId = userInfo.getId();
		
		// create a resource
		project = createNode("foo", 1L, 2L, null);
		nodeList.add(project);
				
		childNode = createNode("foo2", 3L, 4L, project.getId());
		grandchildNode0 = createNode("foo3", 5L, 6L, childNode.getId());
		grandchildNode1 = createNode("foo4", 7L, 8L, childNode.getId());
	}

	@After
	public void tearDown() throws Exception {
		for (Node n : nodeList) {
			try {
				nodeManager.delete(adminUserInfo, n.getId());
			} catch (NotFoundException e) {}
		}
		
		userManager.deletePrincipal(adminUserInfo, userInfo.getId());
		userManager.deletePrincipal(adminUserInfo, otherUserInfo.getId());
		
		if (arId!=null) {
			accessRequirementManager.deleteAccessRequirement(adminUserInfo, arId);
		}
	}
	
	@Test
	public void testGetACL() throws Exception {
		// retrieve parent acl
		AccessControlList acl = entityPermissionsManager.getACL(project.getId(), adminUserInfo);
		assertNotNull(acl);
		assertEquals(project.getId(), acl.getId());
		assertEquals(1, acl.getResourceAccess().size());
		for (ResourceAccess ra : acl.getResourceAccess()) {
			assertNotNull(ra.getPrincipalId());
		}
		// retrieve child acl.  should get parent's
		try{
			acl = entityPermissionsManager.getACL(childNode.getId(), adminUserInfo);
			fail("Get ACL on a node that inherits it permission should throw an exception");
		}catch (ACLInheritanceException e){
			// The exception should tell us the benefactor
			assertEquals(project.getId(), e.getBenefactorId());
		}

	}

	@Test
	public void testValidateACLContent() throws Exception {
		ResourceAccess userRA = new ResourceAccess();
		userRA.setPrincipalId(userInfo.getId());
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ats.add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		userRA.setAccessType(ats);
		
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ras.add(userRA);
		
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ras);	
		
		// Should not throw any exceptions
		PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
	}
	
	@Test(expected = InvalidModelException.class)
	public void testValidateACLContent_UserMissing()throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		
		// Should fail, since user is not included with proper permissions in ACL
		PermissionsManagerUtils.validateACLContent(acl, otherUserInfo, ownerId);
	}

	
	@Test
	public void testValidateACLContent_AdminMissing()throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		
		// Should not throw any exceptions
		PermissionsManagerUtils.validateACLContent(acl, adminUserInfo, ownerId);
	}
	
	@Test
	public void testValidateACLContent_OwnerMissing()throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		
		// Should not throw any exceptions
		PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
	}
	
	@Test(expected = InvalidModelException.class)
	public void testValidateACLContent_UserInsufficientPermissions() throws Exception {
		ResourceAccess userRA = new ResourceAccess();
		userRA.setPrincipalId(userInfo.getId());
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ats.add(ACCESS_TYPE.READ);
		userRA.setAccessType(ats);
		
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ras.add(userRA);
		
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ras);	
		
		// Should fail since user does not have permission editing rights in ACL
		PermissionsManagerUtils.validateACLContent(acl, otherUserInfo, ownerId);
	}
	
	@Test
	public void testValidateACLContent_indirectMembership() throws Exception {
		ResourceAccess userRA = new ResourceAccess();
		// 'other user' should be a member of 'authenticated users'
		Long groupId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId();
		assertTrue(otherUserInfo.getGroups().contains(groupId));
		// giving 'authenticated users' change_permissions access should fulfill the requirement
		// that the editor of the ACL does not remove their own access
		userRA.setPrincipalId(groupId);
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ats.add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		userRA.setAccessType(ats);
		
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ras.add(userRA);
		
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ras);	
		
		PermissionsManagerUtils.validateACLContent(acl, otherUserInfo, ownerId);
		
	}
	
	@Test(expected = InvalidModelException.class)
	public void testValidateACLContentAnonDownload() throws Exception {
		ResourceAccess userRA = new ResourceAccess();
		userRA.setPrincipalId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ats.add(ACCESS_TYPE.DOWNLOAD);
		userRA.setAccessType(ats);
		
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ras.add(userRA);
		
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ras);	

		PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
	}

	/*
	 * PLFM-3632s
	 */
	@Test(expected = UnauthorizedException.class)
	public void testValidateACLContentNonCertifiedUserMakeACLPublic() throws Exception {
		ResourceAccess userRA = new ResourceAccess();
		userRA.setPrincipalId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ats.add(ACCESS_TYPE.DOWNLOAD);
		userRA.setAccessType(ats);
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ras.add(userRA);
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ras);	
		// userInfo is not certified
		PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
	}

	@Test
	public void testValidateACLContentCertifiedUserMakeACLPublic() throws Exception {
		ResourceAccess userRA = new ResourceAccess();
		userRA.setPrincipalId(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ats.add(ACCESS_TYPE.DOWNLOAD);
		userRA.setAccessType(ats);
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		ras.add(userRA);
		AccessControlList acl = new AccessControlList();
		acl.setId("resource id");
		acl.setResourceAccess(ras);
		// certify userInfo
		userInfo.getGroups().add(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		PermissionsManagerUtils.validateACLContent(acl, userInfo, ownerId);
	}

	@Test
	public void testUpdateACL() throws Exception {
		AccessControlList acl = entityPermissionsManager.getACL(project.getId(), adminUserInfo);
		assertEquals(1, acl.getResourceAccess().size());
		// Check the etag before
		String etagBefore = acl.getEtag();
		assertNotNull(etagBefore);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUserInfo);
		// The etag should have changed
		assertNotNull(acl.getEtag());
		assertFalse(etagBefore.equals(acl.getEtag()));
		
		// now get it again and see that the permission is there
		acl = entityPermissionsManager.getACL(project.getId(), userInfo);
		Set<ResourceAccess> ras = acl.getResourceAccess();
		assertEquals(2, ras.size());
		Iterator<ResourceAccess> it = ras.iterator();
		boolean foundIt = false;
		while(it.hasNext()){
			ResourceAccess ra = it.next();
			if(ra.getPrincipalId().toString().equals(userInfo.getId().toString())){
				assertEquals(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.READ})),
						ra.getAccessType());
				foundIt = true;
				break;
			}
		}
		assertTrue(foundIt);
	}
	
	@Test (expected=ConflictingUpdateException.class)
	public void testConcurrentUpdate() throws Exception {
		AccessControlList acl = entityPermissionsManager.getACL(project.getId(), adminUserInfo);
		assertEquals(1, acl.getResourceAccess().size());
		// Change the etag so that it does not match the current value
		acl.setEtag(acl.getEtag()+"1");
		entityPermissionsManager.updateACL(acl, adminUserInfo);
	}

	@Test
	// check that you get an exception if...
	public void testUpdateInvalidACL() throws Exception {
		AccessControlList acl = entityPermissionsManager.getACL(project.getId(), adminUserInfo);
		assertEquals(1, acl.getResourceAccess().size());
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.READ);
		acl = entityPermissionsManager.updateACL(acl, adminUserInfo);
		acl.setId(project.getId());
		// ...group id is null...
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(null);
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ra.setAccessType(ats);
		ra.getAccessType().add(ACCESS_TYPE.READ);
		acl.getResourceAccess().add(ra);
		try {
			entityPermissionsManager.updateACL(acl, adminUserInfo);
			fail("exception expected");
		} catch (InvalidModelException e) {
			//as expected
		}
	}

	@Test
	public void testOverrideInheritance() throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId(childNode.getId());
		String eTagBefore = childNode.getETag();
		assertNotNull(eTagBefore);
		AccessControlList results = entityPermissionsManager.overrideInheritance(acl, adminUserInfo);
		assertNotNull(results);
		assertNotNull(results.getEtag());
		assertFalse("The Etag should have changed", eTagBefore.equals(results.getEtag()));
		assertEquals(childNode.getId(), results.getId());
		childNode = nodeManager.get(adminUserInfo, childNode.getId());
		// call 'getACL':  the ACL should match the requested settings and specify the resource as the owner of the ACL
		AccessControlList acl2 = entityPermissionsManager.getACL(childNode.getId(), adminUserInfo);
		assertEquals(childNode.getId(), acl2.getId());
		assertEquals(acl, acl2);
	}

	@Test
	public void testOverrideInheritanceEdgeCases() throws Exception {
		AccessControlList acl = entityPermissionsManager.getACL(project.getId(), adminUserInfo);
		// should get exception if object already has an acl
		try {
			entityPermissionsManager.overrideInheritance(acl, adminUserInfo);
			fail("exception expected");
		} catch (UnauthorizedException ue) {
			// as expected
		}
		// should get exception if you don't have authority to change permissions
		try {
			entityPermissionsManager.overrideInheritance(acl, userInfo);
			fail("exception expected");
		} catch (UnauthorizedException ue) {
			// as expected
		}
	}

	@Test
	public void testRestoreInheritance() throws Exception {
		AccessControlList acl = null;
		try{
			// This will fail but we can use it to get the benefactor
			acl = entityPermissionsManager.getACL(childNode.getId(), adminUserInfo);
			fail("Should not have been able to get the ACL for a node that inherits");
		}catch (ACLInheritanceException e){
			// Get the parent ACL
			acl = entityPermissionsManager.getACL(e.getBenefactorId(), adminUserInfo);
		}
		// Set the ID to the child.
		acl.setId(childNode.getId());
		entityPermissionsManager.overrideInheritance(acl, adminUserInfo);
		AccessControlList acl2 = entityPermissionsManager.getACL(childNode.getId(), adminUserInfo);
		assertEquals(childNode.getId(), acl2.getId());
		assertEquals(acl, acl2);
		String eTagBefore = childNode.getETag();
		assertNotNull(eTagBefore);
		AccessControlList results = entityPermissionsManager.restoreInheritance(childNode.getId(), adminUserInfo);
		assertNotNull(results);
		assertNotNull(results.getEtag());
		assertEquals(project.getId(), results.getId());
		childNode = nodeManager.get(adminUserInfo, childNode.getId());
		assertFalse("The etag of the child node should have changed", eTagBefore.equals(childNode.getETag()));
		// call 'getACL' on the resource.  The returned ACL should specify parent as the ACL owner
		try{
			entityPermissionsManager.getACL(childNode.getId(), adminUserInfo);
			fail("Should not have been able to get the ACL for a node that inherits");
		}catch (ACLInheritanceException e){
			// Get the parent ACL
			assertEquals(project.getId(), e.getBenefactorId());
		}
	}

	@Test
	public void testRestoreInheritanceEdgeCases() throws Exception {
		// should get exception if resource already inherits
		try {
			entityPermissionsManager.restoreInheritance(childNode.getId(), adminUserInfo);
			fail("exception expected");
		} catch (UnauthorizedException ue) {
			// as expected
		}
		AccessControlList acl = null;
		try{
			 entityPermissionsManager.getACL(childNode.getId(), adminUserInfo);
			 fail();
		}catch(ACLInheritanceException e){
			acl = entityPermissionsManager.getACL(e.getBenefactorId(), adminUserInfo);
		}

		acl.setId(childNode.getId());
		entityPermissionsManager.overrideInheritance(acl, adminUserInfo);
		
		// should get exception if don't have authority to change permissions
		try {
			entityPermissionsManager.restoreInheritance(childNode.getId(), userInfo);
			fail("exception expected");
		} catch (UnauthorizedException ue) {
			// as expected
		}
		
	}

	@Test
	public void testApplyInheritanceToChildren() throws Exception {
		// retrieve parent acl
		AccessControlList parentAcl = entityPermissionsManager.getACL(project.getId(), adminUserInfo);
		assertNotNull(parentAcl);
		assertEquals(project.getId(), parentAcl.getId());

		// retrieve child acl - should get parent's
		verifyInheritedAcl(childNode, project.getId(), adminUserInfo);
		
		// assign new ACL to child
		AccessControlList childAcl = new AccessControlList();
		childAcl.setId(childNode.getId());
		String eTagBefore = childNode.getETag();
		assertNotNull(eTagBefore);
		entityPermissionsManager.overrideInheritance(childAcl, adminUserInfo);
		
		// assign new ACL to grandchild0
		AccessControlList grandchild0Acl = new AccessControlList();
		grandchild0Acl.setId(grandchildNode0.getId());
		eTagBefore = grandchildNode0.getETag();
		assertNotNull(eTagBefore);
		entityPermissionsManager.overrideInheritance(grandchild0Acl, adminUserInfo);
		
		// retrieve child acl - should get child's
		AccessControlList returnedAcl = entityPermissionsManager.getACL(childNode.getId(), adminUserInfo);
		assertEquals("Child ACL not set properly", childAcl, returnedAcl);
		assertFalse("Child ACL should not match parent ACL", parentAcl.equals(returnedAcl));
		
		// retrieve grandchild0 acl - should get grandchild0's
		returnedAcl = entityPermissionsManager.getACL(grandchildNode0.getId(), adminUserInfo);
		assertEquals("Grandchild ACL not set properly", grandchild0Acl, returnedAcl);
		assertFalse("Grandchild ACL should not match child ACL", childAcl.equals(returnedAcl));
		assertFalse("Grandchild ACL should not match parent ACL", parentAcl.equals(returnedAcl));
		
		// retrieve grandchild1 acl - should get child's
		verifyInheritedAcl(grandchildNode1, childNode.getId(), adminUserInfo);
		
		// apply inheritance to children
		entityPermissionsManager.applyInheritanceToChildren(project.getId(), adminUserInfo);
		
		// retrieve all descendant acls - should get parent's
		verifyInheritedAcl(childNode, project.getId(), adminUserInfo);
		verifyInheritedAcl(grandchildNode0, project.getId(), adminUserInfo);
		verifyInheritedAcl(grandchildNode1, project.getId(), adminUserInfo);
	}
	
	@Test
	public void testApplyInheritanceToChildrenNotAuthorized() throws Exception {
		// retrieve parent acl
		AccessControlList parentAcl = entityPermissionsManager.getACL(project.getId(), adminUserInfo);
		assertNotNull(parentAcl);
		assertEquals(project.getId(), parentAcl.getId());
		
		// assign new ACL to child
		AccessControlList childAcl = new AccessControlList();
		childAcl.setId(childNode.getId());
		String eTagBefore = childNode.getETag();
		assertNotNull(eTagBefore);
		entityPermissionsManager.overrideInheritance(childAcl, adminUserInfo);
		
		// assign new ACL to grandchild0
		AccessControlList grandchild0Acl = new AccessControlList();
		grandchild0Acl.setId(grandchildNode0.getId());
		eTagBefore = grandchildNode0.getETag();
		assertNotNull(eTagBefore);
		entityPermissionsManager.overrideInheritance(grandchild0Acl, adminUserInfo);
		
		// authorize test user to change permissions of parent and child nodes
		parentAcl = AuthorizationTestHelper.addToACL(parentAcl, userInfo.getId(), ACCESS_TYPE.CHANGE_PERMISSIONS);
		parentAcl = entityPermissionsManager.updateACL(parentAcl, adminUserInfo);
		childAcl = AuthorizationTestHelper.addToACL(childAcl, userInfo.getId(), ACCESS_TYPE.CHANGE_PERMISSIONS);
		childAcl = entityPermissionsManager.updateACL(childAcl, adminUserInfo);
		
		// apply inheritance to children as test user
		entityPermissionsManager.applyInheritanceToChildren(project.getId(), userInfo);
		
		// retrieve child and grandchild1 acls - should get parent's (authorized to change permissions)
		verifyInheritedAcl(childNode, project.getId(), adminUserInfo);
		verifyInheritedAcl(grandchildNode1, project.getId(), adminUserInfo);
		
		// retrieve grandchild0 acl - should get grandchild0's (not authorized to change permissions)
		try {
			AccessControlList returnedAcl = entityPermissionsManager.getACL(grandchildNode0.getId(), adminUserInfo);
			assertEquals("Grandchild ACL not set properly", grandchild0Acl, returnedAcl);
		} catch (ACLInheritanceException e) {
			fail("Grandchild ACL was overwritten without authorization");
		}
		
	}
	
	/**
	 * Regression test for PLFM-1865
	 * @throws Exception
	 */
	@Test 
	public void testProperACLCreationInheritancePLFM1865() throws Exception {
		// create a project as admin user
		project = createNode("Project", 1L, 2L, null);
		nodeList.add(project);
		
		// grant CRU access to collaborator		
		Set<ACCESS_TYPE> collaboratorAccess = new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[] { ACCESS_TYPE.CREATE, ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE })); 
		AccessControlList acl = entityPermissionsManager.getACL(project.getId(), adminUserInfo);
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(collaboratorAccess);
		ra.setPrincipalId(userInfo.getId());	
		acl.getResourceAccess().add(ra);
		entityPermissionsManager.updateACL(acl, adminUserInfo);
		
		// create folder in project as collaborator
		Node folder = createDTO("collaborators folder", userInfo.getId(), userInfo.getId(), project.getId());
		String nodeId = nodeManager.createNewNode(folder, userInfo);
		assertNotNull(nodeId);
		folder.setId(nodeId);
		folder = nodeManager.get(userInfo, nodeId);
		nodeList.add(folder);
		
		// create local ACL for folder
		AccessControlList folderAcl = new AccessControlList();
		folderAcl.setId(folder.getId());
		folderAcl.setResourceAccess(acl.getResourceAccess());		
		entityPermissionsManager.overrideInheritance(folderAcl, adminUserInfo);
		
		// get ACL as collaborator and assure proper ACL 
		AccessControlList retAcl = entityPermissionsManager.getACL(folder.getId(), userInfo);
		assertEquals(acl.getResourceAccess().size(), retAcl.getResourceAccess().size());
		for(ResourceAccess raExpect : acl.getResourceAccess()) {
			for(ResourceAccess raActual : retAcl.getResourceAccess()) {
				if(raExpect.getPrincipalId().equals(raActual.getPrincipalId())) {
					assertEquals(raExpect.getAccessType(), raActual.getAccessType());					
				}
			}
		}
	}
	
	private void verifyInheritedAcl(Node node, String expectedBenefactorId,  UserInfo userInfo) 
			throws NotFoundException, DatastoreException {
		try {
			entityPermissionsManager.getACL(node.getId(), userInfo);
			fail("Node should be inheriting ACL, but it has its own local ACL.");
		} catch (ACLInheritanceException e){
			// The exception should tell us the benefactor
			assertEquals(expectedBenefactorId, e.getBenefactorId());
		}	
	}
	
	@Test
	public void testCanDownload() throws Exception {
		AccessControlList acl = entityPermissionsManager.getACL(project.getId(), adminUserInfo);
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.READ, ACCESS_TYPE.DOWNLOAD})));
		ra.setPrincipalId(otherUserInfo.getId());	
		acl.getResourceAccess().add(ra);
		entityPermissionsManager.updateACL(acl, adminUserInfo);
		// baseline:  there is no restriction against downloading this entity
		assertTrue(entityPermissionsManager.hasAccess(childNode.getId(), ACCESS_TYPE.DOWNLOAD, otherUserInfo).getAuthorized());
		// now create an access requirement on project and child
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(project.getId());
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		ar.setConcreteType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse("foo");
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		arId = ""+ar.getId();
		// now we can't download
		assertFalse(entityPermissionsManager.hasAccess(childNode.getId(), ACCESS_TYPE.DOWNLOAD, otherUserInfo).getAuthorized());
		accessRequirementManager.deleteAccessRequirement(adminUserInfo, arId);
		arId=null;
		// back to the baseline
		assertTrue(entityPermissionsManager.hasAccess(childNode.getId(), ACCESS_TYPE.DOWNLOAD, otherUserInfo).getAuthorized());
		// now add the AR to the child node itself
		ar.setId(null);
		ar.setEtag(null);
		rod.setId(childNode.getId());
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		arId = ""+ar.getId();
		// again, we can't download
		assertFalse(entityPermissionsManager.hasAccess(childNode.getId(), ACCESS_TYPE.DOWNLOAD, otherUserInfo).getAuthorized());
	}
}
