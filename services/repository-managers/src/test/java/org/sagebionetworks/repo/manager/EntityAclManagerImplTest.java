package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityAclManagerImplTest {
	
	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private EntityAclManager entityAclManager;
	
	@Autowired
	private EntityAuthorizationManager entityAuthorizationManager;
	
	@Autowired
	private AccessRequirementManager accessRequirementManager;
	
	@Autowired
	private DBOBasicDao basicDao;

	private Collection<Node> nodeList = new ArrayList<Node>();
	private Node project = null;
	private Node childNode = null;
	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private UserInfo otherUserInfo;
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
		return nodeManager.createNode(node, adminUserInfo);
	}

	@BeforeEach
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
		userInfo = userManager.createOrGetTestUser(adminUserInfo, nu, cred, tou);
		
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		otherUserInfo = userManager.createOrGetTestUser(adminUserInfo, nu, cred, tou);
		
		tou.setPrincipalId(otherUserInfo.getId());
		basicDao.createOrUpdate(tou);
		
		// create a resource
		project = createNode("foo", 1L, 2L, null);
		
		nodeList.add(project);
				
		childNode = createNode("foo2", 3L, 4L, project.getId());
	}

	@AfterEach
	public void tearDown() throws Exception {
		// delete in reverse order.
		Collections.reverse((List<?>) nodeList);
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
		AccessControlList acl = entityAclManager.getACL(project.getId(), adminUserInfo);
		assertNotNull(acl);
		assertEquals(project.getId(), acl.getId());
		assertEquals(1, acl.getResourceAccess().size());
		for (ResourceAccess ra : acl.getResourceAccess()) {
			assertNotNull(ra.getPrincipalId());
		}
		
		// retrieve child acl.  should get parent's
		ACLInheritanceException e = assertThrows(ACLInheritanceException.class, () -> {			
			entityAclManager.getACL(childNode.getId(), adminUserInfo);
		});
		
		// The exception should tell us the benefactor
		assertEquals(project.getId(), e.getBenefactorId());
	}

	@Test
	public void testGetACLNoSYN() throws Exception {
		// retrieve parent acl
		AccessControlList acl = entityAclManager.getACL(project.getId().substring("syn".length()), adminUserInfo);
		assertNotNull(acl);
	}

	@Test
	public void testUpdateACL() throws Exception {
		AccessControlList acl = entityAclManager.getACL(project.getId(), adminUserInfo);
		assertEquals(1, acl.getResourceAccess().size());
		// Check the etag before
		String etagBefore = acl.getEtag();
		assertNotNull(etagBefore);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.READ);
		acl = entityAclManager.updateACL(acl, adminUserInfo);
		// The etag should have changed
		assertNotNull(acl.getEtag());
		assertFalse(etagBefore.equals(acl.getEtag()));
		
		// now get it again and see that the permission is there
		acl = entityAclManager.getACL(project.getId(), userInfo);
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
	
	@Test
	public void testConcurrentUpdate() throws Exception {
		AccessControlList acl = entityAclManager.getACL(project.getId(), adminUserInfo);
		assertEquals(1, acl.getResourceAccess().size());
		// Change the etag so that it does not match the current value
		acl.setEtag(acl.getEtag()+"1");
		assertThrows(ConflictingUpdateException.class, () -> {
			entityAclManager.updateACL(acl, adminUserInfo);
		});
	}

	@Test
	// check that you get an exception if...
	public void testUpdateInvalidACL() throws Exception {
		AccessControlList acl = entityAclManager.getACL(project.getId(), adminUserInfo);
		assertEquals(1, acl.getResourceAccess().size());
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.READ);
		acl = entityAclManager.updateACL(acl, adminUserInfo);
		acl.setId(project.getId());
		// ...group id is null...
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(null);
		Set<ACCESS_TYPE> ats = new HashSet<ACCESS_TYPE>();
		ra.setAccessType(ats);
		ra.getAccessType().add(ACCESS_TYPE.READ);
		acl.getResourceAccess().add(ra);
		
		AccessControlList updateAcl = acl;
		
		assertThrows(InvalidModelException.class, () -> {
			entityAclManager.updateACL(updateAcl, adminUserInfo);
		});
	}

	@Test
	public void testOverrideInheritance() throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId(childNode.getId());
		String eTagBefore = childNode.getETag();
		assertNotNull(eTagBefore);
		AccessControlList results = entityAclManager.overrideInheritance(acl, adminUserInfo);
		assertNotNull(results);
		assertNotNull(results.getEtag());
		assertFalse(eTagBefore.equals(results.getEtag()), "The Etag should have changed");
		assertEquals(childNode.getId(), results.getId());
		childNode = nodeManager.getNode(adminUserInfo, childNode.getId());
		// call 'getACL':  the ACL should match the requested settings and specify the resource as the owner of the ACL
		AccessControlList acl2 = entityAclManager.getACL(childNode.getId(), adminUserInfo);
		assertEquals(childNode.getId(), acl2.getId());
		assertEquals(acl, acl2);
	}

	@Test
	public void testOverrideInheritanceEdgeCases() throws Exception {
		AccessControlList acl = entityAclManager.getACL(project.getId(), adminUserInfo);
		// should get exception if object already has an acl
		assertThrows(UnauthorizedException.class, () -> {			
			entityAclManager.overrideInheritance(acl, adminUserInfo);
		});
		
		// should get exception if you don't have authority to change permissions
		assertThrows(UnauthorizedException.class, () -> {		
			entityAclManager.overrideInheritance(acl, userInfo);
		});
	}

	@Test
	public void testRestoreInheritance() throws Exception {
		ACLInheritanceException e = assertThrows(ACLInheritanceException.class, () -> {		
			// This will fail but we can use it to get the benefactor
			entityAclManager.getACL(childNode.getId(), adminUserInfo);
		});
		
		// Get the parent ACL
		AccessControlList acl = entityAclManager.getACL(e.getBenefactorId(), adminUserInfo);
		
		// Set the ID to the child.
		acl.setId(childNode.getId());
		entityAclManager.overrideInheritance(acl, adminUserInfo);
		AccessControlList acl2 = entityAclManager.getACL(childNode.getId(), adminUserInfo);
		assertEquals(childNode.getId(), acl2.getId());
		assertEquals(acl, acl2);
		String eTagBefore = childNode.getETag();
		assertNotNull(eTagBefore);
		AccessControlList results = entityAclManager.restoreInheritance(childNode.getId(), adminUserInfo);
		assertNotNull(results);
		assertNotNull(results.getEtag());
		assertEquals(project.getId(), results.getId());
		childNode = nodeManager.getNode(adminUserInfo, childNode.getId());
		assertFalse(eTagBefore.equals(childNode.getETag()), "The etag of the child node should have changed");
		// call 'getACL' on the resource.  The returned ACL should specify parent as the ACL owner
		e =	assertThrows(ACLInheritanceException.class, () -> {		
			entityAclManager.getACL(childNode.getId(), adminUserInfo);
		});
		// Get the parent ACL
		assertEquals(project.getId(), e.getBenefactorId());
		
	}

	@Test
	public void testRestoreInheritanceEdgeCases() throws Exception {
		// should get exception if resource already inherits
		assertThrows(UnauthorizedException.class, () -> {		
			entityAclManager.restoreInheritance(childNode.getId(), adminUserInfo);
		});
		
		ACLInheritanceException e = assertThrows(ACLInheritanceException.class, () -> {	
			 entityAclManager.getACL(childNode.getId(), adminUserInfo);
		});
		
		AccessControlList acl = entityAclManager.getACL(e.getBenefactorId(), adminUserInfo);

		acl.setId(childNode.getId());
		entityAclManager.overrideInheritance(acl, adminUserInfo);
		
		// should get exception if don't have authority to change permissions
		assertThrows(UnauthorizedException.class, () -> {
			entityAclManager.restoreInheritance(childNode.getId(), userInfo);
		});
		
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
		AccessControlList acl = entityAclManager.getACL(project.getId(), adminUserInfo);
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(collaboratorAccess);
		ra.setPrincipalId(userInfo.getId());	
		acl.getResourceAccess().add(ra);
		entityAclManager.updateACL(acl, adminUserInfo);
		
		// create folder in project as collaborator
		Node folder = createDTO("collaborators folder", userInfo.getId(), userInfo.getId(), project.getId());
		folder = nodeManager.createNode(folder, userInfo);
		nodeList.add(folder);
		
		// create local ACL for folder
		AccessControlList folderAcl = new AccessControlList();
		folderAcl.setId(folder.getId());
		folderAcl.setResourceAccess(acl.getResourceAccess());		
		entityAclManager.overrideInheritance(folderAcl, adminUserInfo);
		
		// get ACL as collaborator and assure proper ACL 
		AccessControlList retAcl = entityAclManager.getACL(folder.getId(), userInfo);
		assertEquals(acl.getResourceAccess().size(), retAcl.getResourceAccess().size());
		for(ResourceAccess raExpect : acl.getResourceAccess()) {
			for(ResourceAccess raActual : retAcl.getResourceAccess()) {
				if(raExpect.getPrincipalId().equals(raActual.getPrincipalId())) {
					assertEquals(raExpect.getAccessType(), raActual.getAccessType());					
				}
			}
		}
	}
	
	@Test
	public void testCanDownload() throws Exception {
		AccessControlList acl = entityAclManager.getACL(project.getId(), adminUserInfo);
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.READ, ACCESS_TYPE.DOWNLOAD})));
		ra.setPrincipalId(otherUserInfo.getId());	
		acl.getResourceAccess().add(ra);
		entityAclManager.updateACL(acl, adminUserInfo);
		// baseline:  there is no restriction against downloading this entity
		assertTrue(entityAuthorizationManager.hasAccess(otherUserInfo, childNode.getId(), ACCESS_TYPE.DOWNLOAD).isAuthorized());
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
		assertFalse(entityAuthorizationManager.hasAccess(otherUserInfo, childNode.getId(), ACCESS_TYPE.DOWNLOAD).isAuthorized());
		accessRequirementManager.deleteAccessRequirement(adminUserInfo, arId);
		arId=null;
		// back to the baseline
		assertTrue(entityAuthorizationManager.hasAccess(otherUserInfo, childNode.getId(), ACCESS_TYPE.DOWNLOAD).isAuthorized());
		// now add the AR to the child node itself
		ar.setId(null);
		ar.setEtag(null);
		rod.setId(childNode.getId());
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		arId = ""+ar.getId();
		// again, we can't download
		assertFalse(entityAuthorizationManager.hasAccess(otherUserInfo, childNode.getId(), ACCESS_TYPE.DOWNLOAD).isAuthorized());
	}
	
	@Test
	public void testCreateNodeWithNonCertifiedUser() throws Exception {
		// Add permissions to create in the project for the (uncertified) user
		AccessControlList acl = entityAclManager.getACL(project.getId(), adminUserInfo);
		acl = AuthorizationTestHelper.addToACL(acl, userInfo.getId(), ACCESS_TYPE.CREATE);
		acl = entityAclManager.updateACL(acl, adminUserInfo);
		
		// Tries to create a folder
		Node folder = createDTO("Test Folder ", userInfo.getId(), userInfo.getId(), project.getId());
		folder.setNodeType(EntityType.folder);
		
		UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {
			// Method under test
			nodeManager.createNode(folder, userInfo);
		});
		
		assertEquals("Only certified users may create or update content in Synapse.", ex.getMessage());	
	}
	
}
