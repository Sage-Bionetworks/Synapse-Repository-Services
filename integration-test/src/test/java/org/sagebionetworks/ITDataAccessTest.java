package org.sagebionetworks;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoRequest;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
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
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

@ExtendWith(ITTestExtension.class)
public class ITDataAccessTest {

	private Project project;
	private ACTAccessRequirement actAR;
	private ManagedACTAccessRequirement managedAR;
	
	private SynapseAdminClient adminSynapse;
	private SynapseClient synapse;
	private Long userTwoId;
	
	public ITDataAccessTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}
	
	@BeforeEach
	public void before() throws SynapseException {
		project = synapse.createEntity(new Project());
		// add an access requirement
		actAR = new ACTAccessRequirement();
		
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(project.getId());
		rod.setType(RestrictableObjectType.ENTITY);
		actAR.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		actAR.setAccessType(ACCESS_TYPE.DOWNLOAD);
		actAR = adminSynapse.createAccessRequirement(actAR);
	}
	
	@AfterEach
	public void after() throws Exception {
		try {
			adminSynapse.deleteEntity(project);
		} catch (SynapseNotFoundException e) {}
		
		if (userTwoId != null) {
			try {
				adminSynapse.deleteUser(userTwoId);
			} catch (SynapseException e) {}
		}
	}

	@Test
	public void test() throws SynapseException {
		RestrictionInformationRequest restrictionInformationRequest = new RestrictionInformationRequest();
		restrictionInformationRequest.setObjectId(project.getId());
		restrictionInformationRequest.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse restrictionInfo = synapse.getRestrictionInformation(restrictionInformationRequest);
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

		assertNotNull(synapse.getSubjects(managedAR.getId().toString(), null));

		ResearchProject rp = synapse.getResearchProjectForUpdate(managedAR.getId().toString());
		assertNotNull(rp);
		// create
		rp.setInstitution("Sage");
		rp.setProjectLead("Bruce");
		rp.setIntendedDataUseStatement("intendedDataUseStatement");
		rp.setAccessRequirementId(managedAR.getId().toString());
		ResearchProject created = synapse.createOrUpdateResearchProject(rp);

		assertEquals(created, synapse.getResearchProjectForUpdate(managedAR.getId().toString()));

		created.setIntendedDataUseStatement("new intendedDataUseStatement");
		ResearchProject updated = synapse.createOrUpdateResearchProject(created);

		assertEquals(updated, synapse.getResearchProjectForUpdate(managedAR.getId().toString()));

		Request request = (Request) synapse.getRequestForUpdate(managedAR.getId().toString());
		assertNotNull(request);

		request.setAccessRequirementId(managedAR.getId().toString());
		request.setResearchProjectId(updated.getId());
		Request createdRequest = (Request) synapse.createOrUpdateRequest(request);

		assertEquals(createdRequest, synapse.getRequestForUpdate(managedAR.getId().toString()));

		String adminId = adminSynapse.getMyOwnUserBundle(1).getUserProfile().getOwnerId();
		String userId = synapse.getMyOwnUserBundle(1).getUserProfile().getOwnerId();

		AccessorChange adminChange = new AccessorChange();
		adminChange.setUserId(adminId);
		adminChange.setType(AccessType.GAIN_ACCESS);

		AccessorChange userChange = new AccessorChange();
		userChange.setUserId(userId);
		userChange.setType(AccessType.GAIN_ACCESS);

		createdRequest.setAccessorChanges(Arrays.asList(adminChange, userChange));
		Request updatedRequest = (Request) synapse.createOrUpdateRequest(createdRequest);

		CreateSubmissionRequest csRequest = new CreateSubmissionRequest();
		csRequest.setRequestId(updatedRequest.getId());
		csRequest.setRequestEtag(updatedRequest.getEtag());
		csRequest.setSubjectId(project.getId());
		csRequest.setSubjectType(RestrictableObjectType.ENTITY);
		SubmissionStatus status = synapse.submitRequest(csRequest);
		assertNotNull(status);

		AccessRequirementStatus arStatus = synapse.getAccessRequirementStatus(managedAR.getId().toString());
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
			synapse.cancelSubmission(status.getSubmissionId());
			fail("should not be able to cancel an approved submission");
		} catch (SynapseBadRequestException e) {
			// as expected
		}

		RequestInterface renewal = synapse.getRequestForUpdate(managedAR.getId().toString());
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
		assertEquals(submission.getSubmittedBy(), submissionInfo.getSubmittedBy());
		assertEquals(submission.getAccessorChanges(), submissionInfo.getAccessorChanges());
		
		AccessorGroupRequest accessorGroupRequest = new AccessorGroupRequest();
		AccessorGroupResponse response = adminSynapse.listAccessorGroup(accessorGroupRequest);
		assertNotNull(response);

		BatchAccessApprovalInfoRequest approvalInfoRequest = new BatchAccessApprovalInfoRequest();
		approvalInfoRequest.setUserId(userId);
		approvalInfoRequest.setAccessRequirementIds(Arrays.asList(managedAR.getId().toString()));
		assertNotNull(synapse.getBatchAccessApprovalInfo(approvalInfoRequest ));

		adminSynapse.revokeGroup(managedAR.getId().toString(), userId);
		
		adminSynapse.deleteDataAccessSubmission(status.getSubmissionId());
	}
	
	// This test is used solely to verify the controller integration
	@Test
	public void testGetAccessAprovalNotifications() throws SynapseException {
		
		AccessApprovalNotificationRequest request = new AccessApprovalNotificationRequest();
		
		request.setRequirementId(actAR.getId());
		request.setRecipientIds(Arrays.asList(Long.valueOf(synapse.getMyProfile().getOwnerId())));
		
		AccessApprovalNotificationResponse result = adminSynapse.getAccessApprovalNotifications(request);
		
		assertNotNull(result);
		assertEquals(actAR.getId(), result.getRequirementId());
		assertTrue(result.getResults().isEmpty());
	}
	
	@Test
	public void testAccessRequirementAcl() throws SynapseException {
		AccessControlList acl = new AccessControlList()
			.setId(actAR.getId().toString())
			.setResourceAccess(Collections.singleton(
				new ResourceAccess().setPrincipalId(Long.valueOf(synapse.getMyProfile().getOwnerId())).setAccessType(Collections.singleton(ACCESS_TYPE.REVIEW_SUBMISSIONS))
			));
		
		assertThrows(SynapseForbiddenException.class, () -> {
			synapse.createAccessRequirementAcl(acl);
		});
		
		adminSynapse.createAccessRequirementAcl(acl);
		
		AccessControlList fetchedAcl = adminSynapse.getAccessRequirementAcl(actAR.getId().toString());
		
		String currentEtag = fetchedAcl.getEtag();
		
		AccessControlList updatedAcl = new AccessControlList()
			.setId(actAR.getId().toString())
			.setEtag(fetchedAcl.getEtag())
			.setCreationDate(fetchedAcl.getCreationDate())
			.setResourceAccess(Collections.singleton(
				new ResourceAccess().setPrincipalId(Long.valueOf(adminSynapse.getMyProfile().getOwnerId())).setAccessType(Collections.singleton(ACCESS_TYPE.REVIEW_SUBMISSIONS))
			));
		
		assertThrows(SynapseForbiddenException.class, () -> {
			synapse.updateAccessRequiremenetAcl(updatedAcl);
		});
		
		fetchedAcl = adminSynapse.updateAccessRequiremenetAcl(updatedAcl);
		
		assertNotEquals(currentEtag, fetchedAcl.getEtag());
		
		assertThrows(SynapseForbiddenException.class, () -> {
			synapse.deleteAccessRequirementAcl(actAR.getId().toString());
		});
		
		adminSynapse.deleteAccessRequirementAcl(actAR.getId().toString());
		
		assertThrows(SynapseNotFoundException.class, () -> {
			adminSynapse.getAccessRequirementAcl(actAR.getId().toString());
		});
		
	}
	
	@Test
	public void testAccessRequirementSubmissionReviewer() throws SynapseException, JSONObjectAdapterException {
		
		// A validated user
		SynapseClient synapseTwo = new SynapseClientImpl();
		userTwoId = SynapseClientHelper.createUser(adminSynapse, synapseTwo, true, true);
		
		managedAR = new ManagedACTAccessRequirement()
			.setAccessType(ACCESS_TYPE.DOWNLOAD)
			.setSubjectIds(Collections.singletonList(new RestrictableObjectDescriptor().setId(project.getId()).setType(RestrictableObjectType.ENTITY)));
		
		managedAR = adminSynapse.createAccessRequirement(managedAR);
		
		ResearchProject rp = synapse.getResearchProjectForUpdate(managedAR.getId().toString());
		
		rp.setInstitution("Sage");
		rp.setProjectLead("Lead");
		rp.setIntendedDataUseStatement("intendedDataUseStatement");
		rp.setAccessRequirementId(managedAR.getId().toString());
		
		rp = synapse.createOrUpdateResearchProject(rp);
		
		RequestInterface request = synapse.createOrUpdateRequest(new Request()
			.setResearchProjectId(rp.getId())
			.setAccessRequirementId(managedAR.getId().toString())
			.setAccessorChanges(Arrays.asList(
				new AccessorChange().setType(AccessType.GAIN_ACCESS).setUserId(adminSynapse.getMyProfile().getOwnerId()),
				new AccessorChange().setType(AccessType.GAIN_ACCESS).setUserId(synapse.getMyProfile().getOwnerId())
			)));
		
		SubmissionStatus submissionStatus = synapse.submitRequest(new CreateSubmissionRequest()
				.setRequestId(request.getId())
				.setRequestEtag(request.getEtag())
				.setSubjectId(project.getId())
				.setSubjectType(RestrictableObjectType.ENTITY));
		
		// The first user is not validated
		String errorMessage = assertThrows(SynapseForbiddenException.class, () -> {			
			synapse.updateSubmissionState(submissionStatus.getSubmissionId(), SubmissionState.APPROVED, "Approving the request");
		}).getMessage();
		
		assertEquals("The user must be validated in order to review data access submissions.", errorMessage);
			
		// The second user is validated, but does not have permissions yet
		errorMessage = assertThrows(SynapseForbiddenException.class, () -> {		
			synapseTwo.updateSubmissionState(submissionStatus.getSubmissionId(), SubmissionState.APPROVED, "Approving the request");
		}).getMessage();
		
		assertEquals("The user does not have permissions to review data access submissions for access requirement " + managedAR.getId() + ".", errorMessage);
		
		// Adds both users as reviewers
		AccessControlList acl = new AccessControlList()
			.setId(managedAR.getId().toString())
			.setResourceAccess(Set.of(
				new ResourceAccess().setPrincipalId(Long.valueOf(synapse.getMyProfile().getOwnerId())).setAccessType(Collections.singleton(ACCESS_TYPE.REVIEW_SUBMISSIONS)),
				new ResourceAccess().setPrincipalId(userTwoId).setAccessType(Collections.singleton(ACCESS_TYPE.REVIEW_SUBMISSIONS))
			));
		
		adminSynapse.createAccessRequirementAcl(acl);
		
		// The first user is still not validated
		errorMessage = assertThrows(SynapseForbiddenException.class, () -> {		
			synapse.updateSubmissionState(submissionStatus.getSubmissionId(), SubmissionState.APPROVED, "Approving the request");
		}).getMessage();
		
		assertEquals("The user must be validated in order to review data access submissions.", errorMessage);
		
		// Second user is validated and has permissions now
		Submission submission = synapseTwo.updateSubmissionState(submissionStatus.getSubmissionId(), SubmissionState.APPROVED, "Approving the request");
		
		assertEquals(SubmissionState.APPROVED, submission.getState());
		
		// Try to update the request
		request = synapse.getRequestForUpdate(managedAR.getId().toString());
		
		request.setAccessorChanges(Arrays.asList(
			new AccessorChange().setType(AccessType.REVOKE_ACCESS).setUserId(adminSynapse.getMyProfile().getOwnerId())
		));
		
		SubmissionStatus newSubmission = synapse.submitRequest(new CreateSubmissionRequest()
				.setRequestId(request.getId())
				.setRequestEtag(request.getEtag())
				.setSubjectId(project.getId())
				.setSubjectType(RestrictableObjectType.ENTITY));
		
		// The first user is still not validated
		errorMessage = assertThrows(SynapseForbiddenException.class, () -> {		
			synapse.updateSubmissionState(newSubmission.getSubmissionId(), SubmissionState.REJECTED, "Rejecting the request");
		}).getMessage();
		
		assertEquals("The user must be validated in order to review data access submissions.", errorMessage);
		
		// Second user is validated and has permissions now
		submission = synapseTwo.updateSubmissionState(newSubmission.getSubmissionId(), SubmissionState.REJECTED, "Rejecting the request");
		
		assertEquals(SubmissionState.REJECTED, submission.getState());
	}

}
