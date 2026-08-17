package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementConversionRequest;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;

/**
 * Verifies the wiring of the eDUC update-in-flight-envelope endpoints. The precheck path that does
 * not require a routed DocuSign envelope is exercised unconditionally; the full route/update happy
 * path requires a configured DocuSign integration and is left to manager unit tests.
 */
@ExtendWith(ITTestExtension.class)
public class ITEDucSignatureTest {

	private final SynapseAdminClient adminSynapse;
	private final SynapseClient synapse;

	private Project project;
	private ACTAccessRequirement actAR;
	private ManagedACTAccessRequirement managedAR;

	public ITEDucSignatureTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}

	@BeforeEach
	public void before() throws SynapseException {
		project = synapse.createEntity(new Project());

		actAR = new ACTAccessRequirement();
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(project.getId());
		rod.setType(RestrictableObjectType.ENTITY);
		actAR.setSubjectIds(Arrays.asList(rod));
		actAR.setAccessType(ACCESS_TYPE.DOWNLOAD);
		actAR = adminSynapse.createAccessRequirement(actAR);

		AccessRequirementConversionRequest conversionRequest = new AccessRequirementConversionRequest();
		conversionRequest.setAccessRequirementId(actAR.getId().toString());
		conversionRequest.setCurrentVersion(actAR.getVersionNumber());
		conversionRequest.setEtag(actAR.getEtag());
		managedAR = (ManagedACTAccessRequirement) adminSynapse.convertAccessRequirement(conversionRequest);
	}

	@AfterEach
	public void after() throws Exception {
		try {
			adminSynapse.deleteEntity(project);
		} catch (SynapseNotFoundException e) {
			// already gone
		}
	}

	private Request createRequest() throws SynapseException {
		ResearchProject rp = synapse.getResearchProjectForUpdate(managedAR.getId().toString());
		rp.setInstitution("MIT");
		rp.setProjectLead("Dr. Jones");
		rp.setIntendedDataUseStatement("For research.");
		ResearchProject researchProject = synapse.createOrUpdateResearchProject(rp);

		Request request = (Request) synapse.getRequestForUpdate(managedAR.getId().toString());
		request.setAccessRequirementId(managedAR.getId().toString());
		request.setResearchProjectId(researchProject.getId());
		return (Request) synapse.createOrUpdateRequest(request);
	}

	@Test
	public void testCanUpdateRoutedSignatureWithNoEnvelope() throws SynapseException {
		Request request = createRequest();

		// call under test — a request that has never been routed cannot be updated
		boolean canUpdate = synapse.canUpdateRoutedEDucSignature(request.getId());

		assertFalse(canUpdate);
	}

	@Test
	public void testUpdateRoutedSignatureWithNoEnvelope() throws SynapseException {
		Request request = createRequest();
		assertNotNull(request.getId());

		// call under test — updating a request that was never routed is a 400 (bad request)
		assertThrows(SynapseException.class,
				() -> synapse.updateRoutedEDucSignature(request.getId()));
	}
}
