package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.util.ModelConstants;

import com.google.common.collect.ImmutableSet;

@ExtendWith(ITTestExtension.class)
public class ITFolderTest {
	private static Long user2;
	private static SynapseClient synapse2;

	private Project project;
	
	private SynapseAdminClient adminSynapse;
	private SynapseClient synapse;
	
	public ITFolderTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}

	@BeforeAll
	public static void beforeClass(SynapseAdminClient adminSynapse) throws Exception {
		synapse2 = new SynapseClientImpl();
		user2 = SynapseClientHelper.createUser(adminSynapse, synapse2);
	}

	@BeforeEach
	public void before() throws Exception {
		adminSynapse.clearAllLocks();

		// Create a test project which we will need.
		project = new Project();
		project = synapse.createEntity(project);
	}

	@AfterEach
	public void after() throws Exception {
		if (project != null) {
			synapse.deleteEntity(project, true);
		}
	}

	@AfterAll
	public static void afterClass(SynapseAdminClient adminSynapse) {
		try {
			adminSynapse.deleteUser(user2);
		} catch (SynapseException e) {
			// Ignore possible exceptions
		}
	}

	@Test
	public void otherUserCanMoveFolders() throws Exception {
		// User 1 creates folder A and folder B and subfolder in folder A.
		Folder folderA = createFolder(project.getId());
		Folder folderB = createFolder(project.getId());
		Folder subfolder = createFolder(folderA.getId());

		// User 2 needs access to the project.
		grantUser2Access();

		// User 2 can move subfolder from folder A to folder B.
		subfolder.setParentId(folderB.getId());
		synapse2.putEntity(subfolder);

		// Getting the folder back shows the update.
		Folder result = synapse.getEntity(subfolder.getId(), Folder.class);
		assertEquals(folderB.getId(), result.getParentId());
	}

	private void grantUser2Access() throws Exception {
		ResourceAccess user1Access = new ResourceAccess();
		user1Access.setPrincipalId(Long.valueOf(synapse.getMyProfile().getOwnerId()));
		user1Access.setAccessType(ModelConstants.ENTITY_ADMIN_ACCESS_PERMISSIONS);

		ResourceAccess user2Access = new ResourceAccess();
		user2Access.setPrincipalId(user2);
		user2Access.setAccessType(EnumSet.of(ACCESS_TYPE.CREATE, ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE, ACCESS_TYPE.CHANGE_PERMISSIONS));

		AccessControlList acl = synapse.getACL(project.getId());
		acl.setResourceAccess(ImmutableSet.of(user1Access, user2Access));
		synapse.updateACL(acl);
	}

	private Folder createFolder(String parentId) throws Exception {
		Folder folder = new Folder();
		folder.setParentId(parentId);
		return synapse.createEntity(folder);
	}
}
