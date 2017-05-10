package org.sagebionetworks;

import static org.junit.Assert.*;

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
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmission;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.ACTAccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.BatchAccessApprovalRequest;
import org.sagebionetworks.repo.model.dataaccess.BatchAccessApprovalResult;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;

public class ITDataAccessTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static Long userToDelete;
	private Project project;
	private ACTAccessRequirement accessRequirement;

	@BeforeClass
	public static void beforeClass() throws Exception {
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapseOne = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseOne);
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapseOne);
	}

	@Before
	public void before() throws SynapseException {
		project = synapseOne.createEntity(new Project());
		// add an access requirement
		accessRequirement = new ACTAccessRequirement();
		
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(project.getId());
		rod.setType(RestrictableObjectType.ENTITY);
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod}));
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setAcceptRequest(true);
		accessRequirement = adminSynapse.createAccessRequirement(accessRequirement);
	}
	
	@After
	public void after() throws Exception {
		try {
			adminSynapse.deleteAndPurgeEntityById(project.getId());
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

		ResearchProject rp = synapseOne.getResearchProjectForUpdate(accessRequirement.getId().toString());
		assertNotNull(rp);
		// create
		rp.setInstitution("Sage");
		rp.setProjectLead("Bruce");
		rp.setIntendedDataUseStatement("intendedDataUseStatement");
		rp.setAccessRequirementId(accessRequirement.getId().toString());
		ResearchProject created = synapseOne.createOrUpdateResearchProject(rp);

		assertEquals(created, synapseOne.getResearchProjectForUpdate(accessRequirement.getId().toString()));

		created.setIntendedDataUseStatement("new intendedDataUseStatement");
		ResearchProject updated = synapseOne.createOrUpdateResearchProject(created);

		assertEquals(updated, synapseOne.getResearchProjectForUpdate(accessRequirement.getId().toString()));

		Request request = (Request) synapseOne.getRequestForUpdate(accessRequirement.getId().toString());
		assertNotNull(request);

		request.setAccessRequirementId(accessRequirement.getId().toString());
		request.setResearchProjectId(updated.getId());
		Request createdRequest = (Request) synapseOne.createOrUpdateRequest(request);

		assertEquals(createdRequest, synapseOne.getRequestForUpdate(accessRequirement.getId().toString()));

		String adminId = adminSynapse.getMyOwnUserBundle(1).getUserProfile().getOwnerId();
		String userId = synapseOne.getMyOwnUserBundle(1).getUserProfile().getOwnerId();
		createdRequest.setAccessors(Arrays.asList(adminId, userId));
		Request updatedRequest = (Request) synapseOne.createOrUpdateRequest(createdRequest);

		SubmissionStatus status = synapseOne.submitRequest(updatedRequest.getId(), updatedRequest.getEtag());
		assertNotNull(status);

		AccessRequirementStatus arStatus = synapseOne.getAccessRequirementStatus(accessRequirement.getId().toString());
		assertNotNull(arStatus);
		assertTrue(arStatus instanceof ACTAccessRequirementStatus);
		assertEquals(status, ((ACTAccessRequirementStatus)arStatus).getCurrentSubmissionStatus());

		OpenSubmissionPage openSubmissions = adminSynapse.getOpenSubmissions(null);
		assertNotNull(openSubmissions);
		assertEquals(1, openSubmissions.getOpenSubmissionList().size());
		OpenSubmission os = openSubmissions.getOpenSubmissionList().get(0);
		assertEquals(accessRequirement.getId().toString(), os.getAccessRequirementId());
		assertEquals((Long)1L, os.getNumberOfSubmittedSubmission());

		Submission submission = adminSynapse.updateSubmissionState(status.getSubmissionId(), SubmissionState.APPROVED, null);
		assertNotNull(submission);

		try {
			synapseOne.cancelSubmission(status.getSubmissionId());
			fail("should not be able to cancel an approved submission");
		} catch (SynapseBadRequestException e) {
			// as expected
		}

		assertEquals(updatedRequest, synapseOne.getRequestForUpdate(accessRequirement.getId().toString()));

		SubmissionPage submissions = adminSynapse.listSubmissions(accessRequirement.getId().toString(), null, null, null, null);
		assertNotNull(submissions);
		assertEquals(1, submissions.getResults().size());
		assertEquals(submission, submissions.getResults().get(0));

		BatchAccessApprovalRequest batchRequest = new BatchAccessApprovalRequest();
		batchRequest.setUserIds(Arrays.asList(userId));
		batchRequest.setAccessRequirementId(accessRequirement.getId().toString());
		BatchAccessApprovalResult batchResult = adminSynapse.getAccessApprovalInfo(batchRequest);
		assertNotNull(batchResult);
		assertNotNull(batchResult.getResults());
		assertEquals(1, batchResult.getResults().size());
	}

}
