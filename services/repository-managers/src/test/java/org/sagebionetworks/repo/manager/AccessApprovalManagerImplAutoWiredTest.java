package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessApproval;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.ACTApprovalStatus;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AccessApprovalManagerImplAutoWiredTest {
	@Autowired
	public NodeManager nodeManager;
	@Autowired
	public UserProvider testUserProvider;
	@Autowired
	public AccessRequirementManager accessRequirementManager;
	@Autowired
	public AccessApprovalManager accessApprovalManager;
	@Autowired
	public AuthorizationManager authorizationManager;
	@Autowired
	public EntityPermissionsManager entityPermissionsManager;
	
	private UserInfo adminUserInfo;
	
	private static final String TERMS_OF_USE = "my dog has fleas";

	List<String> nodesToDelete;
	
	String entityId;
	
	TermsOfUseAccessRequirement ar;
	ACTAccessRequirement actAr;
	
	@Before
	public void before() throws Exception{
		adminUserInfo = testUserProvider.getTestAdminUserInfo();
		assertNotNull(nodeManager);
		nodesToDelete = new ArrayList<String>();
		
		Node rootProject = new Node();
		rootProject.setName("root "+System.currentTimeMillis());
		rootProject.setNodeType(EntityType.project.name());
		String rootId = nodeManager.createNewNode(rootProject, adminUserInfo);
		nodesToDelete.add(rootId); // the deletion of 'rootId' will cascade to its children
		Node node = new Node();
		node.setName("A");
		node.setNodeType(EntityType.layer.name());
		node.setParentId(rootId);
		entityId = nodeManager.createNewNode(node, adminUserInfo);

		ar = newToUAccessRequirement(entityId);
		ar = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		
		// now give 'testUserInfo' READ access to the entity
		AccessControlList acl = entityPermissionsManager.getACL(rootId, adminUserInfo);
		Set<ResourceAccess> ras = acl.getResourceAccess();
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(Long.parseLong(testUserProvider.getTestUserInfo().getIndividualGroup().getId()));
		ra.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.READ})));
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
			if (actAr!=null && actAr.getId()!=null) {
				accessRequirementManager.deleteAccessRequirement(adminUserInfo, actAr.getId().toString());
				actAr=null;
			}
		}
	}
	
	private static TermsOfUseAccessRequirement newToUAccessRequirement(String entityId) {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));

		ar.setEntityType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse(TERMS_OF_USE);
		return ar;
	}
	
	private static TermsOfUseAccessApproval newToUAccessApproval(Long requirementId, String accessorId) {
		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		aa.setAccessorId(accessorId);
		aa.setEntityType(TermsOfUseAccessApproval.class.getName());
		aa.setRequirementId(requirementId);
		return aa;
	}
	
	private static ACTAccessRequirement newACTAccessRequirement(String entityId) {
		ACTAccessRequirement ar = new ACTAccessRequirement();
		
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));

		ar.setEntityType(ar.getClass().getName());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setActContactInfo("send a message in a bottle");
		return ar;
	}
	
	private static ACTAccessApproval newACTAccessApproval(Long requirementId, String accessorId) {
		ACTAccessApproval aa = new ACTAccessApproval();
		aa.setAccessorId(accessorId);
		aa.setEntityType(aa.getClass().getName());
		aa.setRequirementId(requirementId);
		aa.setApprovalStatus(ACTApprovalStatus.APPROVED);
		return aa;
	}
	
	@Test
	public void testCreateAccessApproval() throws Exception {
		TermsOfUseAccessApproval aa = newToUAccessApproval(ar.getId(), adminUserInfo.getIndividualGroup().getId());
		aa = accessApprovalManager.createAccessApproval(adminUserInfo, aa);
		assertNotNull(aa.getCreatedBy());
		assertNotNull(aa.getCreatedOn());
		assertNotNull(aa.getId());
		assertNotNull(aa.getModifiedBy());
		assertNotNull(aa.getModifiedOn());
		assertEquals(adminUserInfo.getIndividualGroup().getId(), aa.getAccessorId());
		assertEquals(ar.getId(), aa.getRequirementId());
		assertEquals(TermsOfUseAccessApproval.class.getName(), aa.getEntityType());
	}
	
	// since the user is not an admin they can't delete
	@Test(expected=UnauthorizedException.class)
	public void testCreateAccessApprovalForbidden() throws Exception {
		TermsOfUseAccessApproval aa = newToUAccessApproval(ar.getId(), null);
		aa = accessApprovalManager.createAccessApproval(testUserProvider.getTestUserInfo(), aa);
		accessApprovalManager.deleteAccessApproval(testUserProvider.getTestUserInfo(), aa.getId().toString());
	}
	
	// check that signing ToU actually gives download access
	@Test
	public void testHappyPath() throws Exception {
		UserInfo other = testUserProvider.getTestUserInfo();
		// can't download at first
		assertFalse(authorizationManager.canAccess(other, entityId, ACCESS_TYPE.DOWNLOAD));
		// then he signs the terms of use for the data
		TermsOfUseAccessApproval aa = newToUAccessApproval(ar.getId(), other.getIndividualGroup().getId());
		aa = accessApprovalManager.createAccessApproval(other, aa);
		// now he *can* download the data
		assertTrue(authorizationManager.canAccess(other, entityId, ACCESS_TYPE.DOWNLOAD));
	}
		
	public void testCreateAccessApprovalAndFillInUser() throws Exception {
		TermsOfUseAccessApproval aa = newToUAccessApproval(ar.getId(), null);
		aa = accessApprovalManager.createAccessApproval(adminUserInfo, aa);
		assertEquals(adminUserInfo.getIndividualGroup().getId(), aa.getAccessorId());
	}
	
	@Test(expected=InvalidModelException.class)
	public void testCreateAccessApprovalBadParam2() throws Exception {
		TermsOfUseAccessApproval aa = newToUAccessApproval(null, adminUserInfo.getIndividualGroup().getId());
		aa = accessApprovalManager.createAccessApproval(adminUserInfo, aa);
	}
	
	@Test(expected=InvalidModelException.class)
	public void testCreateAccessApprovalBadParam3() throws Exception {
		TermsOfUseAccessApproval aa = newToUAccessApproval(ar.getId(), adminUserInfo.getIndividualGroup().getId());
		aa.setEntityType(ACTAccessApproval.class.getName());
		aa = accessApprovalManager.createAccessApproval(adminUserInfo, aa);
	}
	
	// can't apply an ACTAccessApproval to a TermsOfUse requirement
	@Test(expected=InvalidModelException.class)
	public void testCreateAccessApprovalBadParam4() throws Exception {
		ACTAccessApproval aa = newACTAccessApproval(ar.getId(), adminUserInfo.getIndividualGroup().getId());
		aa = accessApprovalManager.createAccessApproval(adminUserInfo, aa);
	}
	
	// can't apply a TermsOfUseApproval to an ACT requirement
	@Test(expected=InvalidModelException.class)
	public void testCreateAccessApprovalBadParam5() throws Exception {
		actAr = newACTAccessRequirement(entityId);
		actAr = accessRequirementManager.createAccessRequirement(adminUserInfo, actAr);
		TermsOfUseAccessApproval aa = newToUAccessApproval(actAr.getId(), adminUserInfo.getIndividualGroup().getId());
		aa = accessApprovalManager.createAccessApproval(adminUserInfo, aa);
	}
	
	// not OK for someone to sign TermsOfUse for someone else
	// the service fills in the accessor ID appropriately
	@Test
	public void testCreateAccessApprovalBadAccessorId() throws Exception {
		TermsOfUseAccessApproval aa = newToUAccessApproval(ar.getId(), adminUserInfo.getIndividualGroup().getId());
		aa = accessApprovalManager.createAccessApproval(testUserProvider.getTestUserInfo(), aa);
		assertEquals(testUserProvider.getTestUserInfo().getIndividualGroup().getId(), aa.getAccessorId());
	}
	
	// it's OK for an administrator of the resource to give ACT approval
	@Test
	public void testGiveACTApproval() throws Exception {
		actAr = newACTAccessRequirement(entityId);
		actAr = accessRequirementManager.createAccessRequirement(adminUserInfo, actAr);
		ACTAccessApproval actAa = newACTAccessApproval(actAr.getId(), testUserProvider.getTestUserInfo().getIndividualGroup().getId());
		actAa = accessApprovalManager.createAccessApproval(adminUserInfo, actAa);
		assertNotNull(actAa.getId());
	}
	
	// it's not ok for a non-admin to give ACT approval (in this case for themselves)
	@Test(expected=UnauthorizedException.class)
	public void testGiveACTApprovalForbidden() throws Exception {
		actAr = newACTAccessRequirement(entityId);
		actAr = accessRequirementManager.createAccessRequirement(adminUserInfo, actAr);
		ACTAccessApproval actAa = newACTAccessApproval(actAr.getId(), testUserProvider.getTestUserInfo().getIndividualGroup().getId());
		actAa = accessApprovalManager.createAccessApproval(testUserProvider.getTestUserInfo(), actAa);
		assertNotNull(actAa.getId());
	}
	
	@Test
	public void testApprovalRetrieval() throws Exception {
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);

		QueryResults<AccessApproval> aas = accessApprovalManager.getAccessApprovalsForSubject(adminUserInfo, rod);
		assertEquals(0L, aas.getTotalNumberOfResults());
		assertEquals(0, aas.getResults().size());
		TermsOfUseAccessApproval aa = newToUAccessApproval(ar.getId(), adminUserInfo.getIndividualGroup().getId());
		aa = accessApprovalManager.createAccessApproval(adminUserInfo, aa);
		aas = accessApprovalManager.getAccessApprovalsForSubject(adminUserInfo, rod);
		assertEquals(1L, aas.getTotalNumberOfResults());
		assertEquals(1, aas.getResults().size());
	}
	
	@Test
	public void testUpdateAccessApproval() throws Exception {
		TermsOfUseAccessApproval aa = newToUAccessApproval(ar.getId(), adminUserInfo.getIndividualGroup().getId());
		aa = accessApprovalManager.createAccessApproval(adminUserInfo, aa);

		// ensure that the 'modifiedOn' date is later
		Thread.sleep(100L);
		long aaModifiedOn = aa.getModifiedOn().getTime();
		TermsOfUseAccessApproval aa2 = accessApprovalManager.updateAccessApproval(adminUserInfo, aa);
		assertTrue(aa.getModifiedOn().getTime()-aaModifiedOn>0);
	}
	
	@Test
	public void testDeleteAccessApproval() throws Exception {
		TermsOfUseAccessApproval aa = newToUAccessApproval(ar.getId(), adminUserInfo.getIndividualGroup().getId());
		aa = accessApprovalManager.createAccessApproval(adminUserInfo, aa);
		accessApprovalManager.deleteAccessApproval(adminUserInfo, aa.getId().toString());
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(entityId);
		rod.setType(RestrictableObjectType.ENTITY);
		QueryResults<AccessApproval> aas = accessApprovalManager.getAccessApprovalsForSubject(adminUserInfo, rod);
		assertEquals(0L, aas.getTotalNumberOfResults());
		assertEquals(0, aas.getResults().size());
	}
	
	
}
