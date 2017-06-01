package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AccessApprovalManagerImplAutoWiredTest {
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private AccessRequirementManager accessRequirementManager;
	
	@Autowired
	private AccessApprovalManager accessApprovalManager;
	
	@Autowired
	private AuthorizationManager authorizationManager;
	
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	
	private UserInfo adminUserInfo;
	private UserInfo testUserInfo;
	
	private static final String TERMS_OF_USE = "my dog has fleas";

	private List<String> nodesToDelete;
	
	private String nodeAId;
	private String nodeBId;
	
	private TermsOfUseAccessRequirement ar;
	private ManagedACTAccessRequirement actAr;
	
	private TermsOfUseAccessRequirement arB;

	@Before
	public void before() throws Exception {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		DBOCredential cred = new DBOCredential();
		cred.setSecretKey("");
		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		
		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setAgreesToTermsOfUse(Boolean.TRUE);
		testUserInfo = userManager.createUser(adminUserInfo, nu, cred, tou);
		
		assertNotNull(nodeManager);
		nodesToDelete = new ArrayList<String>();
		
		Node rootProject = new Node();
		rootProject.setName("root "+System.currentTimeMillis());
		rootProject.setNodeType(EntityType.project);
		String rootId = nodeManager.createNewNode(rootProject, adminUserInfo);
		nodesToDelete.add(rootId); // the deletion of 'rootId' will cascade to its children
		Node node = new Node();
		node.setName("A");
		node.setNodeType(EntityType.folder);
		node.setParentId(rootId);
		nodeAId = nodeManager.createNewNode(node, adminUserInfo);
		ar = newToUAccessRequirement(nodeAId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		
		node = new Node();
		node.setName("B");
		node.setNodeType(EntityType.folder);
		node.setParentId(nodeAId);
		nodeBId = nodeManager.createNewNode(node, adminUserInfo);
		arB = newToUAccessRequirement(nodeBId);
		arB = accessRequirementManager.createAccessRequirement(adminUserInfo, arB);

		// now give 'testUserInfo' READ access to the entity hierarchy
		AccessControlList acl = entityPermissionsManager.getACL(rootId, adminUserInfo);
		Set<ResourceAccess> ras = acl.getResourceAccess();
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(testUserInfo.getId());
		ra.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.READ, ACCESS_TYPE.DOWNLOAD})));
		ras.add(ra);
		entityPermissionsManager.updateACL(acl, adminUserInfo);
	}
	
	@After
	public void after() throws Exception {
		if(nodeManager != null && nodesToDelete != null){
			for(String id: nodesToDelete){
				try {
					nodeManager.delete(adminUserInfo, id);
				} catch (Exception e) {
					e.printStackTrace();
				} 				
			}
		}
		if (accessRequirementManager!=null) {
			if (ar!=null && ar.getId()!=null) {
				accessRequirementManager.deleteAccessRequirement(adminUserInfo, ar.getId().toString());
				ar=null;
			}
			if (arB!=null && arB.getId()!=null) {
				accessRequirementManager.deleteAccessRequirement(adminUserInfo, arB.getId().toString());
				arB=null;
			}
			if (actAr!=null && actAr.getId()!=null) {
				accessRequirementManager.deleteAccessRequirement(adminUserInfo, actAr.getId().toString());
				actAr=null;
			}
		}
		userManager.deletePrincipal(adminUserInfo, testUserInfo.getId());
	}
	
	private static TermsOfUseAccessRequirement newToUAccessRequirement(String entityId) {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));

		ar.setConcreteType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse(TERMS_OF_USE);
		return ar;
	}

	private static AccessApproval newAccessApproval(Long requirementId, Long requirementVersion, String accessorId) {
		AccessApproval aa = new AccessApproval();
		aa.setAccessorId(accessorId);
		aa.setRequirementId(requirementId);
		aa.setRequirementVersion(requirementVersion);
		return aa;
	}
	
	private static ManagedACTAccessRequirement newManagedACTAccessRequirement(String entityId) {
		ManagedACTAccessRequirement ar = new ManagedACTAccessRequirement();
		
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));

		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		return ar;
	}

	@Test
	public void testCreateAccessApproval() throws Exception {
		AccessApproval aa = newAccessApproval(ar.getId(), ar.getVersionNumber(), adminUserInfo.getId().toString());
		aa = accessApprovalManager.createAccessApproval(adminUserInfo, aa);
		assertNotNull(aa.getCreatedBy());
		assertNotNull(aa.getCreatedOn());
		assertNotNull(aa.getId());
		assertNotNull(aa.getModifiedBy());
		assertNotNull(aa.getModifiedOn());
		assertEquals(adminUserInfo.getId().toString(), aa.getAccessorId());
		assertEquals(ar.getId(), aa.getRequirementId());
	}
	
	// since the user is not an admin they can't delete
	@Test(expected=UnauthorizedException.class)
	public void testCreateAccessApprovalForbidden() throws Exception {
		AccessApproval aa = newAccessApproval(ar.getId(), ar.getVersionNumber(), null);
		aa = accessApprovalManager.createAccessApproval(testUserInfo, aa);
		accessApprovalManager.deleteAccessApproval(testUserInfo, aa.getId().toString());
	}
	
	// check that signing ToU actually gives download access
	@Test
	public void testHappyPath() throws Exception {
		// can't download at first
		assertFalse(authorizationManager.canAccess(testUserInfo, nodeAId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD).getAuthorized());
		// then he signs the terms of use for the data
		AccessApproval aa = newAccessApproval(ar.getId(), ar.getVersionNumber(), testUserInfo.getId().toString());
		aa = accessApprovalManager.createAccessApproval(testUserInfo, aa);
		// now he *can* download the data
		assertTrue(authorizationManager.canAccess(testUserInfo, nodeAId,  ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD).getAuthorized());

		// nodeB inherits both access requirements
		// can't download at first
		assertFalse(authorizationManager.canAccess(testUserInfo, nodeBId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD).getAuthorized());
		// then he signs the terms of use for the data
		AccessApproval aa2 = newAccessApproval(arB.getId(), arB.getVersionNumber(), testUserInfo.getId().toString());
		aa2 = accessApprovalManager.createAccessApproval(testUserInfo, aa2);
		// now he *can* download the data
		assertTrue(authorizationManager.canAccess(testUserInfo, nodeBId,  ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD).getAuthorized());
	
	}
		
	public void testCreateAccessApprovalAndFillInUser() throws Exception {
		AccessApproval aa = newAccessApproval(ar.getId(), ar.getVersionNumber(), null);
		aa = accessApprovalManager.createAccessApproval(adminUserInfo, aa);
		assertEquals(adminUserInfo.getId().toString(), aa.getAccessorId());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateAccessApprovalBadParam2() throws Exception {
		AccessApproval aa = newAccessApproval(null, ar.getVersionNumber(), adminUserInfo.getId().toString());
		aa = accessApprovalManager.createAccessApproval(adminUserInfo, aa);
	}

	// not OK for someone to sign TermsOfUse for someone else
	// the service fills in the accessor ID appropriately
	@Test
	public void testCreateAccessApprovalBadAccessorId() throws Exception {
		AccessApproval aa = newAccessApproval(ar.getId(), ar.getVersionNumber(), adminUserInfo.getId().toString());
		aa = accessApprovalManager.createAccessApproval(testUserInfo, aa);
		assertEquals(testUserInfo.getId().toString(), aa.getAccessorId());
	}
	
	// it's OK for an administrator of the resource to give ACT approval
	@Test
	public void testGiveACTApproval() throws Exception {
		actAr = newManagedACTAccessRequirement(nodeAId);
		actAr = accessRequirementManager.createAccessRequirement(adminUserInfo, actAr);
		AccessApproval actAa = newAccessApproval(actAr.getId(), actAr.getVersionNumber(), testUserInfo.getId().toString());
		actAa = accessApprovalManager.createAccessApproval(adminUserInfo, actAa);
		assertNotNull(actAa.getId());
	}
	
	// it's not ok for a non-admin to give ACT approval (in this case for themselves)
	@Test(expected=UnauthorizedException.class)
	public void testGiveACTApprovalForbidden() throws Exception {
		actAr = newManagedACTAccessRequirement(nodeAId);
		actAr = accessRequirementManager.createAccessRequirement(adminUserInfo, actAr);
		AccessApproval actAa = newAccessApproval(actAr.getId(), actAr.getVersionNumber(), testUserInfo.getId().toString());
		actAa = accessApprovalManager.createAccessApproval(testUserInfo, actAa);
		assertNotNull(actAa.getId());
	}
	
	@Test
	public void testApprovalRetrieval() throws Exception {
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(nodeAId);
		rod.setType(RestrictableObjectType.ENTITY);

		List<AccessApproval> aas = accessApprovalManager.getAccessApprovalsForSubject(adminUserInfo, rod, 10L, 0L);
		assertEquals(0, aas.size());
		AccessApproval aa = newAccessApproval(ar.getId(),ar.getVersionNumber(),  adminUserInfo.getId().toString());
		aa = accessApprovalManager.createAccessApproval(adminUserInfo, aa);
		aas = accessApprovalManager.getAccessApprovalsForSubject(adminUserInfo, rod, 10L, 0L);
		assertEquals(1, aas.size());
		
		AccessApproval retrieved = accessApprovalManager.getAccessApproval(adminUserInfo, aa.getId().toString());
		assertEquals(aa, retrieved);
		
		// node B inherits the ARs and AAs from Node A
		rod.setId(nodeBId);
		aas = accessApprovalManager.getAccessApprovalsForSubject(adminUserInfo, rod, 10L, 0L);
		assertEquals(1, aas.size());
		
		AccessApproval aaB = newAccessApproval(arB.getId(), arB.getVersionNumber(), adminUserInfo.getId().toString());
		aaB = accessApprovalManager.createAccessApproval(adminUserInfo, aaB);
		
		aas = accessApprovalManager.getAccessApprovalsForSubject(adminUserInfo, rod, 10L, 0L);
		assertEquals(2, aas.size());
	}
	
	@Test
	public void testDeleteAccessApproval() throws Exception {
		AccessApproval aa = newAccessApproval(ar.getId(), ar.getVersionNumber(), adminUserInfo.getId().toString());
		aa = accessApprovalManager.createAccessApproval(adminUserInfo, aa);
		accessApprovalManager.deleteAccessApproval(adminUserInfo, aa.getId().toString());
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(nodeAId);
		rod.setType(RestrictableObjectType.ENTITY);
		List<AccessApproval> aas = accessApprovalManager.getAccessApprovalsForSubject(adminUserInfo, rod, 10L, 0L);
		assertEquals(0, aas.size());
	}

	@Test
	public void testApprovalForSecondRequirementVersion() throws Exception {
		ar = accessRequirementManager.updateAccessRequirement(adminUserInfo, ar.getId().toString(), ar);
		assertEquals((Long)1L, ar.getVersionNumber());
		AccessApproval aa = newAccessApproval(ar.getId(), ar.getVersionNumber(), adminUserInfo.getId().toString());
		aa = accessApprovalManager.createAccessApproval(adminUserInfo, aa);
		assertEquals(ar.getVersionNumber(), aa.getRequirementVersion());
	}
}
