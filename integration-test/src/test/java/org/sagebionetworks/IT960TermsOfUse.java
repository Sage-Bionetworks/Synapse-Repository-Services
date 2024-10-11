package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.auth.TermsOfServiceInfo;
import org.sagebionetworks.repo.model.auth.TermsOfServiceState;
import org.sagebionetworks.repo.model.auth.TermsOfServiceStatus;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;

@ExtendWith(ITTestExtension.class)
public class IT960TermsOfUse {
	private static SynapseClient rejectTOUsynapse;
	private static Long rejectTOUuserToDelete;
	
	private static Project project;
	private static FileEntity dataset;
	
	private SynapseClient synapse;
	
	public IT960TermsOfUse(SynapseClient synapse) {
		this.synapse = synapse;
	}
	
	@BeforeAll
	public static void beforeClass(SynapseAdminClient adminSynapse) throws Exception {
		rejectTOUsynapse = new SynapseClientImpl();
		rejectTOUuserToDelete = SynapseClientHelper.createUser(adminSynapse, rejectTOUsynapse, false, false);
		
		project = new Project();
		project.setName("foo");
		project = adminSynapse.createEntity(project);
		// make the project public readable
		String publicGroupPrincipalId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId().toString();
		AccessControlList acl = adminSynapse.getACL(project.getId());
		
		// Now add public-readable and push it back
		Set<ResourceAccess> resourceAccessSet = acl.getResourceAccess();
		Set<ACCESS_TYPE> accessTypes = new HashSet<ACCESS_TYPE>();
		accessTypes.add(ACCESS_TYPE.READ);
		accessTypes.add(ACCESS_TYPE.DOWNLOAD);
		
		ResourceAccess resourceAccess = new ResourceAccess();
		resourceAccess.setPrincipalId(Long.parseLong(publicGroupPrincipalId)); // add PUBLIC, READ access
		resourceAccess.setAccessType(accessTypes); // add PUBLIC, READ access
		resourceAccessSet.add(resourceAccess); // add it to the list
		adminSynapse.updateACL(acl); // push back to Synapse
		
		// a dataset added to the project will inherit its parent's permissions, i.e. will be public-readable
		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setExternalURL("http://foobar.com");
		efh.setFileName("foo.bar");
		efh = adminSynapse.createExternalFileHandle(efh);
		dataset = new FileEntity();
		dataset.setName("bar");
		dataset.setParentId(project.getId());
		dataset.setDataFileHandleId(efh.getId());
		dataset = adminSynapse.createEntity(dataset);
	}
	
	@AfterAll
	public static void afterClass(SynapseAdminClient adminSynapse) throws Exception {
		adminSynapse.deleteEntity(project);
		adminSynapse.deleteUser(rejectTOUuserToDelete);
		
	}
	
	@Test
	public void testRejectSynapseTermsOfUse() throws SynapseException, Exception {
		// I can download a data file because I have agreed to the Synapse terms of use
		assertNotNull(synapse.getFileEntityTemporaryUrlForCurrentVersion(dataset.getId()));
		
		// I cannot download the file because I have rejected the TOU
		assertThrows(SynapseForbiddenException.class, () -> rejectTOUsynapse.getFileEntityTemporaryUrlForCurrentVersion(dataset.getId()));
	}
	
	@Test
	public void testGetTermsOfServiceInfo() throws SynapseException {
		TermsOfServiceInfo tosInfo = synapse.getTermsOfServiceInfo();
		
		assertNotNull(tosInfo.getLatestTermsOfServiceVersion());
		assertEquals(String.format("https://raw.githubusercontent.com/Sage-Bionetworks/Sage-Governance-Documents/refs/tags/%s/Terms.md", tosInfo.getLatestTermsOfServiceVersion()), tosInfo.getTermsOfServiceUrl());
		
		assertNotNull(tosInfo.getCurrentRequirements().getMinimumTermsOfServiceVersion());
		assertNotNull(tosInfo.getCurrentRequirements().getRequirementDate());
	}
	
	@Test
	public void testGetUserTermsOfServiceStatus() throws SynapseException {

		assertEquals(TermsOfServiceState.UP_TO_DATE, synapse.getUserTermsOfServiceStatus().getUserCurrentTermsOfServiceState());

		assertEquals(TermsOfServiceState.MUST_AGREE_NOW, rejectTOUsynapse.getUserTermsOfServiceStatus().getUserCurrentTermsOfServiceState());
	}
 
}
