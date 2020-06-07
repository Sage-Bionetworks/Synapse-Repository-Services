package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.dataaccess.RequestManager;
import org.sagebionetworks.repo.manager.dataaccess.ResearchProjectManager;
import org.sagebionetworks.repo.manager.dataaccess.SubmissionManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroup;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRevokeRequest;
import org.sagebionetworks.repo.model.dataaccess.CreateSubmissionRequest;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AccessApprovalManagerImplAutoWiredTest {

	@Autowired
	private ResearchProjectDAO researchProjectDao;
	
	@Autowired
	private RequestDAO requestDao;
	
	@Autowired
	private SubmissionDAO submissionDao;

	@Autowired
	private UserManager userManager;

	@Autowired
	private NodeManager nodeManager;

	@Autowired
	private AccessRequirementManager accessRequirementManager;

	@Autowired
	private AccessApprovalManager accessApprovalManager;

	@Autowired
	private EntityPermissionsManager entityPermissionsManager;

	@Autowired
	private ResearchProjectManager researchProjectManager;

	@Autowired
	private RequestManager requestManager;

	@Autowired
	private SubmissionManager submissionManager;

	private UserInfo adminUserInfo;
	private UserInfo testUserInfo;

	private static final String TERMS_OF_USE = "my dog has fleas";

	private List<String> nodesToDelete;
	private List<String> approvalsToDelete;
	private List<String> requirementIdsToDelete;
	private List<String> researchProjectIdsToDelete;
	private List<String> requestIdsToDelete;
	private List<String> submissionIdsToDelete;

	private String nodeAId;
	private String nodeBId;

	private TermsOfUseAccessRequirement ar;
	private TermsOfUseAccessRequirement arB;
	private ManagedACTAccessRequirement managedActAr;
	
	
	@BeforeEach
	public void before() throws Exception {
		approvalsToDelete = new LinkedList<String>();
		requirementIdsToDelete = new LinkedList<String>();
		researchProjectIdsToDelete = new LinkedList<String>();
		requestIdsToDelete = new LinkedList<String>();
		submissionIdsToDelete = new LinkedList<String>();
		
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		DBOCredential cred = new DBOCredential();
		cred.setSecretKey("");
		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		
		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setAgreesToTermsOfUse(Boolean.TRUE);
		testUserInfo = userManager.createOrGetTestUser(adminUserInfo, nu, cred, tou);
		
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
		requirementIdsToDelete.add(ar.getId().toString());
		
		node = new Node();
		node.setName("B");
		node.setNodeType(EntityType.folder);
		node.setParentId(nodeAId);
		nodeBId = nodeManager.createNewNode(node, adminUserInfo);
		arB = newToUAccessRequirement(nodeBId);
		arB = accessRequirementManager.createAccessRequirement(adminUserInfo, arB);
		requirementIdsToDelete.add(arB.getId().toString());

		// now give 'testUserInfo' READ access to the entity hierarchy
		AccessControlList acl = entityPermissionsManager.getACL(rootId, adminUserInfo);
		Set<ResourceAccess> ras = acl.getResourceAccess();
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(testUserInfo.getId());
		ra.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.READ, ACCESS_TYPE.DOWNLOAD})));
		ras.add(ra);
		entityPermissionsManager.updateACL(acl, adminUserInfo);
	}
	
	@AfterEach
	public void after() throws Exception {
		for (String id: approvalsToDelete) {
			accessApprovalManager.deleteAccessApproval(adminUserInfo, id);
		}
		for (String id: submissionIdsToDelete) { // submissions must be deleted before requests
			submissionDao.delete(id);
		}
		for (String id: requestIdsToDelete) {
			requestDao.delete(id);
		}
		for (String id : researchProjectIdsToDelete) { // research projects must be deleted before access requirements
			researchProjectDao.delete(id);
		}
		for (String id: requirementIdsToDelete) {
			accessRequirementManager.deleteAccessRequirement(adminUserInfo, id);
		}
		ar=null;
		arB=null;
		if (nodeManager != null && nodesToDelete != null){
			for(String id: nodesToDelete){
				nodeManager.delete(adminUserInfo, id);
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
	
	/**
	 * Helper to create an AccessApproval
	 * @param user
	 * @param aa
	 * @return
	 */
	public AccessApproval createAccessApproval(UserInfo user, AccessApproval aa) {
		aa = accessApprovalManager.createAccessApproval(user, aa);
		this.approvalsToDelete.add(""+aa.getId());
		return aa;
	}

	@Test
	public void testApprovalRetrieval() throws Exception {
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(nodeAId);
		rod.setType(RestrictableObjectType.ENTITY);

		List<AccessApproval> aas = accessApprovalManager.getAccessApprovalsForSubject(adminUserInfo, rod, 10L, 0L);
		assertEquals(0, aas.size());
		AccessApproval aa = newAccessApproval(ar.getId(),ar.getVersionNumber(),  adminUserInfo.getId().toString());
		aa = createAccessApproval(adminUserInfo, aa);
		aas = accessApprovalManager.getAccessApprovalsForSubject(adminUserInfo, rod, 10L, 0L);
		assertEquals(1, aas.size());
		
		AccessApproval retrieved = accessApprovalManager.getAccessApproval(adminUserInfo, aa.getId().toString());
		assertEquals(aa, retrieved);
		
		// node B inherits the ARs and AAs from Node A
		rod.setId(nodeBId);
		aas = accessApprovalManager.getAccessApprovalsForSubject(adminUserInfo, rod, 10L, 0L);
		assertEquals(1, aas.size());
		
		AccessApproval aaB = newAccessApproval(arB.getId(), arB.getVersionNumber(), adminUserInfo.getId().toString());
		aaB = createAccessApproval(adminUserInfo, aaB);
		
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
		aa = createAccessApproval(adminUserInfo, aa);
		assertEquals(ar.getVersionNumber(), aa.getRequirementVersion());
	}

	/*
	 * PLFM-4484
	 */
	@Test
	public void testListExpiringAccessApprovals() {
		// AR v1 expires in 30 days
		managedActAr = newManagedACTAccessRequirement(nodeAId);
		managedActAr.setExpirationPeriod(30*24*60*60*1000L);
		managedActAr = accessRequirementManager.createAccessRequirement(adminUserInfo, managedActAr);
		requirementIdsToDelete.add(managedActAr.getId().toString());

		// create a research project
		ResearchProject rp = new ResearchProject();
		rp.setAccessRequirementId(managedActAr.getId().toString());
		rp.setInstitution("sage");
		rp.setIntendedDataUseStatement("for testing");
		rp.setProjectLead("Bruce");
		rp = researchProjectManager.create(testUserInfo, rp);
		researchProjectIdsToDelete.add(rp.getId());
		// create a request
		AccessorChange ac = new AccessorChange();
		ac.setUserId(testUserInfo.getId().toString());
		ac.setType(AccessType.GAIN_ACCESS);
		List<AccessorChange> accessorChanges = Arrays.asList(ac);
		RequestInterface request = new Request();
		request.setAccessRequirementId(managedActAr.getId().toString());
		request.setAccessorChanges(accessorChanges);
		request.setResearchProjectId(rp.getId());
		request = requestManager.create(testUserInfo, (Request) request);
		requestIdsToDelete.add(request.getId());

		// submit
		CreateSubmissionRequest csRequest = new CreateSubmissionRequest();
		csRequest.setRequestId(request.getId());
		csRequest.setRequestEtag(request.getEtag());
		csRequest.setSubjectId(nodeAId);
		csRequest.setSubjectType(RestrictableObjectType.ENTITY);
		SubmissionStatus status = submissionManager.create(testUserInfo, csRequest);
		submissionIdsToDelete.add(status.getSubmissionId());

		// approve
		SubmissionStateChangeRequest sscr = new SubmissionStateChangeRequest();
		sscr.setNewState(SubmissionState.APPROVED);
		sscr.setSubmissionId(status.getSubmissionId());
		submissionManager.updateStatus(adminUserInfo, sscr);

		// list expiring
		AccessorGroupRequest agr = new AccessorGroupRequest();
		agr.setAccessRequirementId(managedActAr.getId().toString());
		long twoMonthTs = System.currentTimeMillis() + 2*30*24*60*60*1000L;
		agr.setExpireBefore(new Date(twoMonthTs));
		AccessorGroupResponse agResponse = accessApprovalManager.listAccessorGroup(adminUserInfo, agr);
		assertNotNull(agResponse);
		assertEquals(1, agResponse.getResults().size());
		AccessorGroup ag = agResponse.getResults().get(0);
		assertNotNull(ag);
		assertEquals(ag.getSubmitterId(), testUserInfo.getId().toString());

		// AR v1 expires in 365 days
		managedActAr.setExpirationPeriod(365*24*60*60*1000L);
		managedActAr = accessRequirementManager.updateAccessRequirement(adminUserInfo, managedActAr.getId().toString(), managedActAr);

		// update request
		request = requestManager.getRequestForUpdate(testUserInfo, managedActAr.getId().toString());
		assertEquals(1, request.getAccessorChanges().size());
		ac = request.getAccessorChanges().get(0);
		assertEquals(AccessType.RENEW_ACCESS, ac.getType());
		request = requestManager.update(testUserInfo, request);
		// submit
		csRequest.setRequestEtag(request.getEtag());
		status = submissionManager.create(testUserInfo, csRequest);
		submissionIdsToDelete.add(status.getSubmissionId());		

		// approve
		sscr = new SubmissionStateChangeRequest();
		sscr.setNewState(SubmissionState.APPROVED);
		sscr.setSubmissionId(status.getSubmissionId());
		submissionManager.updateStatus(adminUserInfo, sscr);

		// list expiring
		agResponse = accessApprovalManager.listAccessorGroup(adminUserInfo, agr);
		assertNotNull(agResponse);
		assertEquals(0, agResponse.getResults().size());

		long fourHundredDaysTs = System.currentTimeMillis() + 400*24*60*60*1000L;
		agr.setExpireBefore(new Date(fourHundredDaysTs));
		agResponse = accessApprovalManager.listAccessorGroup(adminUserInfo, agr);
		assertNotNull(agResponse);
		assertEquals(1, agResponse.getResults().size());
		ag = agResponse.getResults().get(0);
		assertNotNull(ag);
		assertEquals(ag.getSubmitterId(), testUserInfo.getId().toString());
	}
	
	@Test
	public void testRevokeGroupStopsDownload() throws Exception {
		// approve the Terms-Of-Use access approval
		AccessApproval aa = newAccessApproval(ar.getId(),ar.getVersionNumber(), testUserInfo.getId().toString());
		aa = createAccessApproval(testUserInfo, aa);

		// create a managed access requirement
		managedActAr = newManagedACTAccessRequirement(nodeAId);
		managedActAr.setExpirationPeriod(30*24*60*60*1000L);
		managedActAr = accessRequirementManager.createAccessRequirement(adminUserInfo, managedActAr);
		requirementIdsToDelete.add(managedActAr.getId().toString());
		
		// show that entity can't be downloaded
		assertFalse(entityPermissionsManager.hasAccess(nodeAId, ACCESS_TYPE.DOWNLOAD, testUserInfo).isAuthorized());

		// create a research project
		ResearchProject rp = new ResearchProject();
		rp.setAccessRequirementId(managedActAr.getId().toString());
		rp.setInstitution("sage");
		rp.setIntendedDataUseStatement("for testing");
		rp.setProjectLead("Jay");
		rp = researchProjectManager.create(testUserInfo, rp);
		researchProjectIdsToDelete.add(rp.getId());
		// create a request
		AccessorChange ac = new AccessorChange();
		ac.setUserId(testUserInfo.getId().toString());
		ac.setType(AccessType.GAIN_ACCESS);
		List<AccessorChange> accessorChanges = Arrays.asList(ac);
		RequestInterface request = new Request();
		request.setAccessRequirementId(managedActAr.getId().toString());
		request.setAccessorChanges(accessorChanges);
		request.setResearchProjectId(rp.getId());
		request = requestManager.create(testUserInfo, (Request) request);
		requestIdsToDelete.add(request.getId());

		// submit
		CreateSubmissionRequest csRequest = new CreateSubmissionRequest();
		csRequest.setRequestId(request.getId());
		csRequest.setRequestEtag(request.getEtag());
		csRequest.setSubjectId(nodeAId);
		csRequest.setSubjectType(RestrictableObjectType.ENTITY);
		SubmissionStatus status = submissionManager.create(testUserInfo, csRequest);
		submissionIdsToDelete.add(status.getSubmissionId());

		// approve
		SubmissionStateChangeRequest sscr = new SubmissionStateChangeRequest();
		sscr.setNewState(SubmissionState.APPROVED);
		sscr.setSubmissionId(status.getSubmissionId());
		submissionManager.updateStatus(adminUserInfo, sscr);
		
		// show that entity can be downloaded
		assertTrue(entityPermissionsManager.hasAccess(nodeAId, ACCESS_TYPE.DOWNLOAD, testUserInfo).isAuthorized());
		
		// revoke access
		AccessorGroupRevokeRequest agrr = new AccessorGroupRevokeRequest();
		agrr.setAccessRequirementId(managedActAr.getId().toString());
		agrr.setSubmitterId(testUserInfo.getId().toString());
		accessApprovalManager.revokeGroup(adminUserInfo, agrr);
		
		// show that entity can't be downloaded (before PLFM-6209 this failed)
		assertFalse(entityPermissionsManager.hasAccess(nodeAId, ACCESS_TYPE.DOWNLOAD, testUserInfo).isAuthorized());
	}
	
	@Test
	public void testRevokeACTAccessRequirementStopsDownload() throws Exception {
		// approve the Terms-Of-Use access approval
		AccessApproval aa = newAccessApproval(ar.getId(),ar.getVersionNumber(), testUserInfo.getId().toString());
		aa = createAccessApproval(testUserInfo, aa);

		// Add ACT AR
		ACTAccessRequirement actAR = new ACTAccessRequirement();
		
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(nodeAId);
		rod.setType(RestrictableObjectType.ENTITY);
		actAR.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));

		actAR.setConcreteType(actAR.getClass().getName());
		actAR.setAccessType(ACCESS_TYPE.DOWNLOAD);
		actAR = accessRequirementManager.createAccessRequirement(adminUserInfo, actAR);
		requirementIdsToDelete.add(actAR.getId().toString());

		// approve
		aa = newAccessApproval(actAR.getId(),ar.getVersionNumber(), testUserInfo.getId().toString());
		aa.setAccessorId(testUserInfo.getId().toString());
		aa = createAccessApproval(adminUserInfo, aa);
		
		// show that entity can be downloaded
		assertTrue(entityPermissionsManager.hasAccess(nodeAId, ACCESS_TYPE.DOWNLOAD, testUserInfo).isAuthorized());
		
		// revoke access
		accessApprovalManager.revokeAccessApprovals(adminUserInfo, actAR.getId().toString(), testUserInfo.getId().toString());
		
		// show that entity can't be downloaded (before PLFM-6209 this failed)
		assertFalse(entityPermissionsManager.hasAccess(nodeAId, ACCESS_TYPE.DOWNLOAD, testUserInfo).isAuthorized());
	}
}
