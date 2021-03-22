package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoRequest;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementConversionRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupResponse;
import org.sagebionetworks.repo.model.dataaccess.CreateSubmissionRequest;
import org.sagebionetworks.repo.model.dataaccess.ManagedACTAccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmission;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfo;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfoPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;

public class ITDataAccessTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static Long userToDelete;
	private Project project;
	private ACTAccessRequirement actAR;
	private ManagedACTAccessRequirement managedAR;

	@BeforeClass
	public static void beforeClass() throws Exception {
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapseOne = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseOne);
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapseOne);
	}

	@Before
	public void before() throws SynapseException {
		project = synapseOne.createEntity(new Project());
		// add an access requirement
		actAR = new ACTAccessRequirement();
		
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(project.getId());
		rod.setType(RestrictableObjectType.ENTITY);
		actAR.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		actAR.setAccessType(ACCESS_TYPE.DOWNLOAD);
		actAR = adminSynapse.createAccessRequirement(actAR);
	}
	
	@After
	public void after() throws Exception {
		try {
			adminSynapse.deleteEntity(project);
		} catch (SynapseNotFoundException e) {}
	}

	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseException e) { }
	}


	@Test
	public void test() throws SynapseException {
		RestrictionInformationRequest restrictionInformationRequest = new RestrictionInformationRequest();
		restrictionInformationRequest.setObjectId(project.getId());
		restrictionInformationRequest.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse restrictionInfo = synapseOne.getRestrictionInformation(restrictionInformationRequest);
		assertNotNull(restrictionInfo);
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, restrictionInfo.getRestrictionLevel());
		assertTrue(restrictionInfo.getHasUnmetAccessRequirement());

		AccessRequirementConversionRequest conversionRequest = new AccessRequirementConversionRequest();
		conversionRequest.setAccessRequirementId(actAR.getId().toString());
		conversionRequest.setCurrentVersion(actAR.getVersionNumber());
		conversionRequest.setEtag(actAR.getEtag());
		managedAR = (ManagedACTAccessRequirement) adminSynapse.convertAccessRequirement(conversionRequest);
		
		managedAR.setIsIDUPublic(true);
		managedAR = (ManagedACTAccessRequirement) adminSynapse.updateAccessRequirement(managedAR);

		assertNotNull(synapseOne.getSubjects(managedAR.getId().toString(), null));

		ResearchProject rp = synapseOne.getResearchProjectForUpdate(managedAR.getId().toString());
		assertNotNull(rp);
		// create
		rp.setInstitution("Sage");
		rp.setProjectLead("Bruce");
		rp.setIntendedDataUseStatement("intendedDataUseStatement");
		rp.setAccessRequirementId(managedAR.getId().toString());
		ResearchProject created = synapseOne.createOrUpdateResearchProject(rp);

		assertEquals(created, synapseOne.getResearchProjectForUpdate(managedAR.getId().toString()));

		created.setIntendedDataUseStatement("new intendedDataUseStatement");
		ResearchProject updated = synapseOne.createOrUpdateResearchProject(created);

		assertEquals(updated, synapseOne.getResearchProjectForUpdate(managedAR.getId().toString()));

		Request request = (Request) synapseOne.getRequestForUpdate(managedAR.getId().toString());
		assertNotNull(request);

		request.setAccessRequirementId(managedAR.getId().toString());
		request.setResearchProjectId(updated.getId());
		Request createdRequest = (Request) synapseOne.createOrUpdateRequest(request);

		assertEquals(createdRequest, synapseOne.getRequestForUpdate(managedAR.getId().toString()));

		String adminId = adminSynapse.getMyOwnUserBundle(1).getUserProfile().getOwnerId();
		String userId = synapseOne.getMyOwnUserBundle(1).getUserProfile().getOwnerId();

		AccessorChange adminChange = new AccessorChange();
		adminChange.setUserId(adminId);
		adminChange.setType(AccessType.GAIN_ACCESS);

		AccessorChange userChange = new AccessorChange();
		userChange.setUserId(userId);
		userChange.setType(AccessType.GAIN_ACCESS);

		createdRequest.setAccessorChanges(Arrays.asList(adminChange, userChange));
		Request updatedRequest = (Request) synapseOne.createOrUpdateRequest(createdRequest);

		CreateSubmissionRequest csRequest = new CreateSubmissionRequest();
		csRequest.setRequestId(updatedRequest.getId());
		csRequest.setRequestEtag(updatedRequest.getEtag());
		csRequest.setSubjectId(project.getId());
		csRequest.setSubjectType(RestrictableObjectType.ENTITY);
		SubmissionStatus status = synapseOne.submitRequest(csRequest);
		assertNotNull(status);

		AccessRequirementStatus arStatus = synapseOne.getAccessRequirementStatus(managedAR.getId().toString());
		assertNotNull(arStatus);
		assertTrue(arStatus instanceof ManagedACTAccessRequirementStatus);
		assertEquals(status, ((ManagedACTAccessRequirementStatus)arStatus).getCurrentSubmissionStatus());

		OpenSubmissionPage openSubmissions = adminSynapse.getOpenSubmissions(null);
		assertNotNull(openSubmissions);
		assertEquals(1, openSubmissions.getOpenSubmissionList().size());
		OpenSubmission os = openSubmissions.getOpenSubmissionList().get(0);
		assertEquals(managedAR.getId().toString(), os.getAccessRequirementId());
		assertEquals((Long)1L, os.getNumberOfSubmittedSubmission());

		Submission submission = adminSynapse.updateSubmissionState(status.getSubmissionId(), SubmissionState.APPROVED, null);
		assertNotNull(submission);

		try {
			synapseOne.cancelSubmission(status.getSubmissionId());
			fail("should not be able to cancel an approved submission");
		} catch (SynapseBadRequestException e) {
			// as expected
		}

		RequestInterface renewal = synapseOne.getRequestForUpdate(managedAR.getId().toString());
		assertNotNull(renewal);
		assertTrue(renewal instanceof Renewal);

		SubmissionPage submissions = adminSynapse.listSubmissions(managedAR.getId().toString(), null, null, null, null);
		assertNotNull(submissions);
		assertEquals(1, submissions.getResults().size());
		assertEquals(submission, submissions.getResults().get(0));
		
		SubmissionInfoPage researchProjectPage = adminSynapse.listApprovedSubmissionInfo(managedAR.getId().toString(), null);
		assertNotNull(researchProjectPage.getResults());
		assertEquals(1, researchProjectPage.getResults().size());
		SubmissionInfo submissionInfo = researchProjectPage.getResults().get(0);
		assertEquals(submission.getResearchProjectSnapshot().getIntendedDataUseStatement(), 
				submissionInfo.getIntendedDataUseStatement());
		assertEquals(submission.getModifiedOn().getTime(), submissionInfo.getModifiedOn().getTime());
		
		AccessorGroupRequest accessorGroupRequest = new AccessorGroupRequest();
		AccessorGroupResponse response = adminSynapse.listAccessorGroup(accessorGroupRequest);
		assertNotNull(response);

		BatchAccessApprovalInfoRequest approvalInfoRequest = new BatchAccessApprovalInfoRequest();
		approvalInfoRequest.setUserId(userId);
		approvalInfoRequest.setAccessRequirementIds(Arrays.asList(managedAR.getId().toString()));
		assertNotNull(synapseOne.getBatchAccessApprovalInfo(approvalInfoRequest ));

		adminSynapse.revokeGroup(managedAR.getId().toString(), userId);
		
		adminSynapse.deleteDataAccessSubmission(status.getSubmissionId());
	}
	
	// This test is used solely to verify the controller integration
	@Test
	public void testGetAccessAprovalNotifications() throws SynapseException {
		
		AccessApprovalNotificationRequest request = new AccessApprovalNotificationRequest();
		
		request.setRequirementId(actAR.getId());
		request.setRecipientIds(Arrays.asList(userToDelete));
		
		AccessApprovalNotificationResponse result = adminSynapse.getAccessApprovalNotifications(request);
		
		assertNotNull(result);
		assertEquals(actAR.getId(), result.getRequirementId());
		assertTrue(result.getResults().isEmpty());
	}

}
