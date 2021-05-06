package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
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
import org.sagebionetworks.repo.model.file.ExternalFileHandle;

public class IT960TermsOfUse {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	
	private static Project project;
	private static FileEntity dataset;
	
	@BeforeAll
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
		
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
	
	@BeforeEach
	public void before() throws Exception {
		synapse.signTermsOfUse(synapse.getCurrentSessionToken(), true);
	}
	
	@AfterAll
	public static void afterClass() throws Exception {
		adminSynapse.deleteEntity(project);
		adminSynapse.deleteUser(userToDelete);
	}
	
	@Test
	public void testRepoSvcWithTermsOfUse() throws Exception {
		// should be able to see locations (i.e. the location is 'tier 1' data
		FileEntity ds = synapse.getEntity(dataset.getId(), FileEntity.class);
		assertNotNull(synapse.getFileEntityTemporaryUrlForCurrentVersion(dataset.getId()));
	}

	@Test
	public void testRepoSvcNoTermsOfUse() throws Exception {
		FileEntity ds = synapse.getEntity(dataset.getId(), FileEntity.class);
		assertNotNull(synapse.getFileEntityTemporaryUrlForCurrentVersion(dataset.getId()));
		
		FileEntity idHolder = new FileEntity();
		idHolder.setId(ds.getId());
		// an admin should be able to retrieve the entity and download the content
		ds = adminSynapse.getEntity(idHolder.getId(), FileEntity.class);
		assertNotNull(synapse.getFileEntityTemporaryUrlForCurrentVersion(idHolder.getId()));

	}
	
	@Test
	public void testRejectSynapseTermsOfUse() throws SynapseException, Exception {
		// I can download a data file because I have agreed to the Synapse terms of use
		assertNotNull(synapse.getFileEntityTemporaryUrlForCurrentVersion(dataset.getId()));
		
		// method under test
		synapse.signTermsOfUse(synapse.getCurrentSessionToken(), false);
		
		// I cannot download the file because I have rejected the TOU
		assertThrows(SynapseForbiddenException.class, () -> synapse.getFileEntityTemporaryUrlForCurrentVersion(dataset.getId()));
	}
	

}
