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
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequest;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionState;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionStatus;
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
		ResearchProject rp = synapseOne.getResearchProjectForUpdate(accessRequirement.getId().toString());
		assertNotNull(rp);
		// create
		rp.setInstitution("Sage");
		rp.setProjectLead("Bruce");
		rp.setIntendedDataUseStatement("intendedDataUseStatement");
		rp.setAccessRequirementId(accessRequirement.getId().toString());
		ResearchProject created = synapseOne.createOrUpdate(rp);

		assertEquals(created, synapseOne.getResearchProjectForUpdate(accessRequirement.getId().toString()));

		created.setIntendedDataUseStatement("new intendedDataUseStatement");
		ResearchProject updated = synapseOne.createOrUpdate(created);

		assertEquals(updated, synapseOne.getResearchProjectForUpdate(accessRequirement.getId().toString()));

		DataAccessRequest request = (DataAccessRequest) synapseOne.getRequestForUpdate(accessRequirement.getId().toString());
		assertNotNull(request);

		request.setAccessRequirementId(accessRequirement.getId().toString());
		request.setResearchProjectId(updated.getId());
		DataAccessRequest createdRequest = (DataAccessRequest) synapseOne.createOrUpdate(request);

		assertEquals(createdRequest, synapseOne.getRequestForUpdate(accessRequirement.getId().toString()));

		String adminId = adminSynapse.getMyOwnUserBundle(1).getUserProfile().getOwnerId();
		createdRequest.setAccessors(Arrays.asList(adminId));
		DataAccessRequest updatedRequest = (DataAccessRequest) synapseOne.createOrUpdate(createdRequest);

		DataAccessSubmissionStatus status = synapseOne.submit(updatedRequest.getId(), updatedRequest.getEtag());
		assertNotNull(status);

		assertEquals(status, synapseOne.getDataAccessSubmissionStatus(accessRequirement.getId().toString()));

		DataAccessSubmission submission = adminSynapse.updateState(status.getSubmissionId(), DataAccessSubmissionState.APPROVED, null);
		assertNotNull(submission);

		try {
			synapseOne.cancel(status.getSubmissionId());
			fail("should not be able to cancel an approved submission");
		} catch (SynapseBadRequestException e) {
			// as expected
		}

		assertEquals(updatedRequest, synapseOne.getRequestForUpdate(accessRequirement.getId().toString()));

		DataAccessSubmissionPage submissions = adminSynapse.listSubmission(accessRequirement.getId().toString(), null, null, null, null);
		assertNotNull(submissions);
		assertEquals(1, submissions.getResults().size());
		assertEquals(submission, submissions.getResults().get(0));
	}

}
