package org.sagebionetworks;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class ITDiscussion {

	private static SynapseClient synapse;
	private static SynapseAdminClient adminSynapse;
	private static Long userToDelete;
	private Project project;

	@BeforeClass
	public static void beforeClass() throws SynapseException, JSONObjectAdapterException {
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}

	@Before
	public void before() throws SynapseException {
		project = new Project();
		project = synapse.createEntity(project);
		assertNotNull(project);
	}

	@After
	public void cleanup() throws SynapseException, JSONObjectAdapterException {
		if (project != null) adminSynapse.deleteEntity(project, true);
		if (userToDelete != null) adminSynapse.deleteUser(userToDelete);
	}

	@Test
	public void test() throws SynapseException {
		Forum dto = synapse.getForumMetadata(project.getId());
		assertNotNull(dto);
		assertNotNull(dto.getId());
		assertEquals(dto.getProjectId(), project.getId());
	}

}
