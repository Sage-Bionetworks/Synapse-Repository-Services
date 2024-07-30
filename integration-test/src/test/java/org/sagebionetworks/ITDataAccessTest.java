package org.sagebionetworks;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
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
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionFulfillment;
import org.sagebionetworks.repo.model.RestrictionInformationBatchRequest;
import org.sagebionetworks.repo.model.RestrictionInformationBatchResponse;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserBundle;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalNotificationResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalSearchRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalSearchResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalSearchSort;
import org.sagebionetworks.repo.model.dataaccess.AccessApprovalSortField;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementConversionRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementSearchRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementSearchResponse;
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
import org.sagebionetworks.repo.model.dataaccess.SubmissionSearchRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionSearchResponse;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.warehouse.WarehouseTestHelper;

@ExtendWith(ITTestExtension.class)
public class ITDataAccessTest {

	private Project project;
	private ACTAccessRequirement actAR;
	private ManagedACTAccessRequirement managedAR;
	
	private SynapseAdminClient adminSynapse;
	private SynapseClient synapse;
	private WarehouseTestHelper warehouseHelper;
	private Long userTwoId;
	private String submissionId;
	
	public ITDataAccessTest(SynapseAdminClient adminSynapse, SynapseClient synapse, WarehouseTestHelper warehouseHelper) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
		this.warehouseHelper = warehouseHelper;
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
		if (submissionId != null) {
			adminSynapse.deleteDataAccessSubmission(submissionId);
		}
	}

	@Test
	public void test() throws Exception {
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
		
		Instant now = Instant.now();
		
		String query = String.format(
				"select count(*) from accessrequirementsnapshots where"
						+ " snapshot_date %s and"
						+ " change_timestamp %s and"
						+ " id = %s and"
						+ " version_number = %s and"
						+ " change_type = 'UPDATE' and"
						+ " is_idu_public = true and"
						+ " concrete_type = '%s'",
				warehouseHelper.toDateStringBetweenPlusAndMinusThirtySeconds(now),
				warehouseHelper.toIsoTimestampStringBetweenPlusAndMinusThirtySeconds(now),
				managedAR.getId(),
				managedAR.getVersionNumber(),
				managedAR.getConcreteType());
		
		warehouseHelper.assertWarehouseQuery(query);

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
		
		submissionId = status.getSubmissionId();

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
		
		RestrictionInformationBatchResponse expectedRestrictionBatch = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setObjectId(KeyFactory.stringToKey(project.getId()))
				.setHasUnmetAccessRequirement(false)
				.setIsUserDataContributor(true)
				.setUserHasDownloadPermission(true)
				.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
				.setRestrictionDetails(List.of(
					new RestrictionFulfillment()
						.setAccessRequirementId(managedAR.getId())
						.setIsApproved(true)
						.setIsExempt(false)
						.setIsMet(true)
				))
		));
		
		RestrictionInformationBatchResponse restrictionInfoBatch = synapse.getRestrictionInformationBatch(new RestrictionInformationBatchRequest()
				.setRestrictableObjectType(RestrictableObjectType.ENTITY)
				.setObjectIds(List.of(project.getId()))
		);
		
		assertEquals(expectedRestrictionBatch, restrictionInfoBatch);

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
		
		// Now create a folder in the project, the entity should inherit the AR from the project and the data should appear in the DW
		
		Folder folder = new Folder()
			.setParentId(project.getId())
			.setName(UUID.randomUUID().toString());
		
		folder = synapse.createEntity(folder);
		
		expectedRestrictionBatch = new RestrictionInformationBatchResponse().setRestrictionInformation(List.of(
			new RestrictionInformationResponse()
				.setObjectId(KeyFactory.stringToKey(project.getId()))
				.setHasUnmetAccessRequirement(true)
				.setIsUserDataContributor(true)
				.setUserHasDownloadPermission(true)
				.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
				.setRestrictionDetails(List.of(
					new RestrictionFulfillment()
						.setAccessRequirementId(managedAR.getId())
						.setIsApproved(false)
						.setIsExempt(false)
						.setIsMet(false)
				)),
			new RestrictionInformationResponse()
				.setObjectId(KeyFactory.stringToKey(folder.getId()))
				.setHasUnmetAccessRequirement(true)
				.setIsUserDataContributor(true)
				.setUserHasDownloadPermission(true)
				.setRestrictionLevel(RestrictionLevel.CONTROLLED_BY_ACT)
				.setRestrictionDetails(List.of(
					new RestrictionFulfillment()
						.setAccessRequirementId(managedAR.getId())
						.setIsApproved(false)
						.setIsExempt(false)
						.setIsMet(false)
				))
		));
		
		restrictionInfoBatch = synapse.getRestrictionInformationBatch(new RestrictionInformationBatchRequest()
				.setRestrictableObjectType(RestrictableObjectType.ENTITY)
				.setObjectIds(List.of(project.getId(), folder.getId()))
		);
		
		assertEquals(expectedRestrictionBatch, restrictionInfoBatch);
		
		now = Instant.now();
		
		query = String.format(
				"select count(*) from nodesnapshots where"
						+ " snapshot_date %s and"
						+ " change_timestamp %s and"
						+ " id = %s and"
						+ " contains(effective_ars, %s)",
				warehouseHelper.toDateStringBetweenPlusAndMinusThirtySeconds(now),
				warehouseHelper.toIsoTimestampStringBetweenPlusAndMinusThirtySeconds(now),
				KeyFactory.stringToKey(folder.getId()),
				managedAR.getId());
		
		warehouseHelper.assertWarehouseQuery(query);
		
		// Sleeping gives the snapshot worker a chance to take the snapshots before the test suite deletes the project.
		Thread.sleep(10_000);
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

	/**
	 * Test for PLFM-8248
	 * A user is considered exempt only if they have been granted both EDIT and DELETE permission on a file,
	 * and they have been granted the EXEMPTION_ELIGIBLE permission on the AR.
	 */
	@Test
	public void testExemptionEligibleForDataContributor() throws SynapseException, IOException, JSONObjectAdapterException {
		Project project = synapse.createEntity(new Project());
		URL url = IT054FileEntityTest.class.getClassLoader().getResource("images/LittleImage.png");
		File imageFile = new File(url.getFile().replaceAll("%20", " "));
		assertNotNull(imageFile);
		assertTrue(imageFile.exists());

		CloudProviderFileHandleInterface fileHandle = synapse.multipartUpload(imageFile, null, true, true);
		FileEntity file = new FileEntity();
		file.setName("someFile");
		file.setParentId(project.getId());
		file.setDataFileHandleId(fileHandle.getId());
		file = this.synapse.createEntity(file);
		String fileId = IdAndVersion.parse(file.getId()).getId().toString();
		SynapseClient synapseContributor = new SynapseClientImpl();
		Long contributorId = SynapseClientHelper.createUser(adminSynapse, synapseContributor, true, false);
		AccessControlList acl = new AccessControlList()
				.setId(file.getId())
				.setResourceAccess(Collections.singleton(
						new ResourceAccess().setPrincipalId(contributorId)
								.setAccessType(Set.of(ACCESS_TYPE.UPDATE, ACCESS_TYPE.DELETE, ACCESS_TYPE.DOWNLOAD))
				));

		adminSynapse.createACL(acl);
		managedAR = new ManagedACTAccessRequirement()
				.setAccessType(ACCESS_TYPE.DOWNLOAD)
				.setSubjectIds(Collections.singletonList(new RestrictableObjectDescriptor().setId(file.getId()).setType(RestrictableObjectType.ENTITY)));

		managedAR = adminSynapse.createAccessRequirement(managedAR);
		// download file
		FileHandleAssociation fileHandleAssociation = new FileHandleAssociation().setFileHandleId(fileHandle.getId())
				.setAssociateObjectId(fileId).setAssociateObjectType(FileHandleAssociateType.FileEntity);

		String message = assertThrows(SynapseForbiddenException.class, ()->{
			// call under test
			synapseContributor.getFileURL(fileHandleAssociation);
		}).getMessage();
		assertEquals("There are unmet access requirements that must be met to read content in the requested container.", message);

		AccessControlList aclOnAR = new AccessControlList()
				.setId(managedAR.getId().toString())
				.setResourceAccess(Set.of(
						new ResourceAccess().setPrincipalId(Long.valueOf(contributorId))
								.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE))
				));

		adminSynapse.createAccessRequirementAcl(aclOnAR);
		// call under test
		synapseContributor.getFileURL(fileHandleAssociation);
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
		
		submissionId = submissionStatus.getSubmissionId();
		
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
	
	@Test
	public void testSearchAccessApprovals() throws SynapseException, JSONObjectAdapterException {
		
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
		
		submissionId = submissionStatus.getSubmissionId();
		
		Submission submission = adminSynapse.updateSubmissionState(submissionStatus.getSubmissionId(), SubmissionState.APPROVED, "Approving the request");
		
		assertEquals(SubmissionState.APPROVED, submission.getState());
		
		String accessorId = synapse.getMyProfile().getOwnerId();
		
		AccessApprovalSearchRequest searchRequest = new AccessApprovalSearchRequest()
			.setAccessorId(accessorId)
			.setSort(List.of(new AccessApprovalSearchSort().setField(AccessApprovalSortField.MODIFIED_ON)));
		
		// Only ACT can search through approvals
		assertThrows(SynapseForbiddenException.class, () -> {
			synapse.searchAccessApprovals(searchRequest);
		});

		AccessApprovalSearchResponse response = adminSynapse.searchAccessApprovals(searchRequest);
		
		assertFalse(response.getResults().isEmpty());
		response.getResults().forEach( r -> {
			assertEquals(accessorId, r.getSubmitterId());
		});
		
		searchRequest.setAccessRequirementId("-1");
		
		assertTrue(adminSynapse.searchAccessApprovals(searchRequest).getResults().isEmpty());
		
	}
	
	@Test
	public void testSearchSubmissions() throws SynapseException, JSONObjectAdapterException {
		
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
		
		submissionId = submissionStatus.getSubmissionId();
		
		Submission submission = adminSynapse.updateSubmissionState(submissionStatus.getSubmissionId(), SubmissionState.APPROVED, "Approving the request");
		
		assertEquals(SubmissionState.APPROVED, submission.getState());

		SubmissionSearchRequest searchRequest = new SubmissionSearchRequest()
			.setAccessRequirementId(managedAR.getId().toString())
			.setSubmissionState(SubmissionState.APPROVED);
		
		SubmissionSearchResponse result = synapse.searchDataAccessSubmissions(searchRequest);
		
		// The user cannot review any submission yet
		assertTrue(result.getResults().isEmpty());
		
		result = adminSynapse.searchDataAccessSubmissions(searchRequest);
		
		// The ACT user can see everything
		assertEquals(1, result.getResults().size());
		
		result.getResults().forEach( r -> {
			assertEquals(submission.getId(), r.getId());
		});
		
		AccessControlList acl = new AccessControlList()
			.setId(managedAR.getId().toString())
			.setResourceAccess(Set.of(
				new ResourceAccess().setPrincipalId(Long.valueOf(synapse.getMyProfile().getOwnerId())).setAccessType(Collections.singleton(ACCESS_TYPE.REVIEW_SUBMISSIONS))
			));
		
		// Add the user directly to the ACL
		acl = adminSynapse.createAccessRequirementAcl(acl);
		
		result = synapse.searchDataAccessSubmissions(searchRequest);
		
		// Now the user can see the submission
		assertEquals(1, result.getResults().size());
		
		result.getResults().forEach( r -> {
			assertEquals(submission.getId(), r.getId());
		});
		
		// Now updated the ACL so that only the team of the user is a reviewer
		Team team = synapse.createTeam(new Team().setName(UUID.randomUUID().toString()));
		
		acl.setResourceAccess(Set.of(
			new ResourceAccess().setPrincipalId(Long.valueOf(team.getId())).setAccessType(Collections.singleton(ACCESS_TYPE.REVIEW_SUBMISSIONS))
		));
		
		adminSynapse.updateAccessRequiremenetAcl(acl);
		
		result = synapse.searchDataAccessSubmissions(searchRequest);
		
		// The user can see the submission through the team
		assertEquals(1, result.getResults().size());
		
		result.getResults().forEach( r -> {
			assertEquals(submission.getId(), r.getId());
		});
		
	}
	
	@Test
	public void testSearchAccessRequirements() throws SynapseException, JSONObjectAdapterException {
		
		String name = "Test AR " + UUID.randomUUID().toString();
		
		managedAR = new ManagedACTAccessRequirement()
			.setName(name)
			.setAccessType(ACCESS_TYPE.DOWNLOAD)
			.setSubjectIds(Collections.singletonList(new RestrictableObjectDescriptor().setId(project.getId()).setType(RestrictableObjectType.ENTITY)));
		
		managedAR = adminSynapse.createAccessRequirement(managedAR);
		
		AccessRequirementSearchRequest searchRequest = new AccessRequirementSearchRequest()
			.setNameContains(name.substring(name.length() - 5, name.length() - 1))
			.setAccessType(ACCESS_TYPE.DOWNLOAD);
		
		AccessRequirementSearchResponse result = synapse.searchAccessRequirements(searchRequest);
		
		assertFalse(result.getResults().isEmpty());
		assertEquals(managedAR.getId().toString(), result.getResults().get(0).getId());
		
		// Add filter by the reviewer
		searchRequest.setReviewerId(synapse.getMyProfile().getOwnerId());
		
		result = synapse.searchAccessRequirements(searchRequest);
		
		// No reviewers yet, so empty list
		assertTrue(result.getResults().isEmpty());
		
		// Add a reviewer to the AR
		AccessControlList acl = new AccessControlList()
			.setId(managedAR.getId().toString())
			.setResourceAccess(Set.of(
				new ResourceAccess().setPrincipalId(Long.valueOf(synapse.getMyProfile().getOwnerId())).setAccessType(Collections.singleton(ACCESS_TYPE.REVIEW_SUBMISSIONS))
			));
		
		// Add the user directly to the ACL
		acl = adminSynapse.createAccessRequirementAcl(acl);
		
		result = synapse.searchAccessRequirements(searchRequest);
		
		assertFalse(result.getResults().isEmpty());
		assertEquals(managedAR.getId().toString(), result.getResults().get(0).getId());
	}
	
	@Test
	public void testGetSubmission() throws SynapseException, JSONObjectAdapterException {
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
		
		submissionId = submissionStatus.getSubmissionId();
		
		// ACT is able to fetch the submission
		Submission submission = adminSynapse.getDataAccessSubmission(submissionStatus.getSubmissionId());
		
		assertEquals(submissionStatus.getSubmissionId(), submission.getId());
		
		// A normal non-reviewer user cannot access the submission
		String message = assertThrows(SynapseForbiddenException.class, () -> {
			synapse.getDataAccessSubmission(submissionStatus.getSubmissionId());	
		}).getMessage();
		
		assertEquals("The user must be validated in order to review data access submissions.", message);
		
		// A validated user cannot access the submission unless they are reviewers
		message = assertThrows(SynapseForbiddenException.class, () -> {
			synapseTwo.getDataAccessSubmission(submissionStatus.getSubmissionId());	
		}).getMessage();
		
		assertEquals("The user does not have permissions to review data access submissions for access requirement " + managedAR.getId() + ".", message);
		
		// Add both user as reviewers
		AccessControlList acl = new AccessControlList()
			.setId(managedAR.getId().toString())
			.setResourceAccess(Set.of(
				new ResourceAccess().setPrincipalId(Long.valueOf(synapse.getMyProfile().getOwnerId())).setAccessType(Collections.singleton(ACCESS_TYPE.REVIEW_SUBMISSIONS)),
				new ResourceAccess().setPrincipalId(Long.valueOf(synapseTwo.getMyProfile().getOwnerId())).setAccessType(Collections.singleton(ACCESS_TYPE.REVIEW_SUBMISSIONS))
					));
				
				// Add the user directly to the ACL
				acl = adminSynapse.createAccessRequirementAcl(acl);
				
				// Now the validated user can fetch the submission
				submission = synapseTwo.getDataAccessSubmission(submissionStatus.getSubmissionId());
				
				assertEquals(submissionStatus.getSubmissionId(), submission.getId());
				
				// The first user is still not validated
				message = assertThrows(SynapseForbiddenException.class, () -> {
					synapse.getDataAccessSubmission(submissionStatus.getSubmissionId());	
				}).getMessage();
				
				assertEquals("The user must be validated in order to review data access submissions.", message);
	}
	
	@Test
	public void testGetUserBundleIsArReviewer() throws SynapseException, JSONObjectAdapterException {
		// Creates a new fresh user
		SynapseClient synapseTwo = new SynapseClientImpl();
		userTwoId = SynapseClientHelper.createUser(adminSynapse, synapseTwo, true, true);
		
		UserBundle userBundle = synapseTwo.getMyOwnUserBundle(0xFF);
		
		assertFalse(userBundle.getIsARReviewer());
		
		managedAR = new ManagedACTAccessRequirement()
			.setAccessType(ACCESS_TYPE.DOWNLOAD)
			.setSubjectIds(Collections.singletonList(new RestrictableObjectDescriptor().setId(project.getId()).setType(RestrictableObjectType.ENTITY)));
		
		managedAR = adminSynapse.createAccessRequirement(managedAR);
		
		// Add the user as a reviewer to the AR
		AccessControlList acl = new AccessControlList()
			.setId(managedAR.getId().toString())
			.setResourceAccess(Set.of(
				new ResourceAccess().setPrincipalId(Long.valueOf(synapseTwo.getMyProfile().getOwnerId())).setAccessType(Collections.singleton(ACCESS_TYPE.REVIEW_SUBMISSIONS))
			));
		
		// Add the user directly to the ACL
		acl = adminSynapse.createAccessRequirementAcl(acl);
		
		userBundle = synapseTwo.getMyOwnUserBundle(0xFF);
		
		assertTrue(userBundle.getIsARReviewer());
		
		// Deleting the AR should remove its ACL (See https://sagebionetworks.jira.com/browse/PLFM-7317)
		
		adminSynapse.deleteAccessRequirement(managedAR.getId());
		
		userBundle = synapseTwo.getMyOwnUserBundle(0xFF);
		
		assertFalse(userBundle.getIsARReviewer());
	}
	
	// Test for https://sagebionetworks.jira.com/browse/PLFM-8322
	@Test
	public void testResearchProjectIduWithSpecialChars() throws SynapseException {
		
		managedAR = new ManagedACTAccessRequirement()
				.setIsIDUPublic(true)
				.setAccessType(ACCESS_TYPE.DOWNLOAD)
				.setSubjectIds(Collections.singletonList(new RestrictableObjectDescriptor().setId(project.getId()).setType(RestrictableObjectType.ENTITY)));
			
		managedAR = adminSynapse.createAccessRequirement(managedAR);
		
		String idu = "Straight quotes are the two generic vertical quotation marks located near the return key: "
				+ "the straight single quote (') and the straight double quote (\").\n\n"
				+ "Curly quotes are the quotation marks used in good typography. "
				+ "There are four curly quote characters: "
				+ "the opening single quote (‘), the closing single quote (’), the opening double quote (“), and the closing double quote (”).";
		
		ResearchProject rp = synapse.getResearchProjectForUpdate(managedAR.getId().toString());
		
		rp.setInstitution("Sage");
		rp.setProjectLead("Lead");
		rp.setIntendedDataUseStatement(idu);
		rp.setAccessRequirementId(managedAR.getId().toString());
		
		rp = synapse.createOrUpdateResearchProject(rp);
		
		// Is the reserch project IDU stored as intended?
		assertEquals(idu, rp.getIntendedDataUseStatement());
		
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
		
		Submission submission = adminSynapse.getDataAccessSubmission(submissionStatus.getSubmissionId());
		
		// The submission stores a "snapshot" or copy of the research project object in a serialized field, is that stored correctly?
		assertEquals(idu, submission.getResearchProjectSnapshot().getIntendedDataUseStatement());
		
		submission = adminSynapse.updateSubmissionState(submissionStatus.getSubmissionId(), SubmissionState.APPROVED, "Approving the request");
		
		assertEquals(idu, submission.getResearchProjectSnapshot().getIntendedDataUseStatement());
		
		List<SubmissionInfo> submissions = adminSynapse.listApprovedSubmissionInfo(managedAR.getId().toString(), null).getResults();
		
		assertEquals(idu, submissions.get(0).getIntendedDataUseStatement());
		
	}
	

}
