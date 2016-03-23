package org.sagebionetworks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class ITBroadcastMessage {
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	private static String email;
	private Project project;
	private String projectId;
	private String forumId;
	private String bucketKey;

	@BeforeClass
	public static void beforeClass() throws Exception {
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapse = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapse);
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
		synapse.updateMyProfile(synapse.getMyProfile());
		synapse.setUserName("test");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseException e) { }
	}

	@Before
	public void before() throws SynapseException {
		email = synapse.getNotificationEmail();
		bucketKey = EmailValidationUtil.getBucketKeyForEmail(email);
		project = new Project();
		project = synapse.createEntity(project);
		assertNotNull(project);
		projectId = project.getId();
		forumId = synapse.getForumByProjectId(projectId).getId();
	}

	@After
	public void cleanup() throws SynapseException, JSONObjectAdapterException {
		if (project != null) adminSynapse.deleteEntity(project, true);
		synapse.unsubscribeAll();
	}

	@Test
	public void test() throws Exception {
		assertFalse(EmailValidationUtil.doesFileExist(bucketKey, 1000L));
		CreateDiscussionThread toCreate = new CreateDiscussionThread();
		toCreate.setForumId(forumId);
		String title = "Question about file missing";
		toCreate.setTitle(title);
		String message = "I think my cats ate my files. Please help!";
		toCreate.setMessageMarkdown(message);
		synapse.createThread(toCreate);
		assertTrue(EmailValidationUtil.doesFileExist(bucketKey, 60000L));
		assertNotNull(EmailValidationUtil.readFile(bucketKey));
		EmailValidationUtil.deleteFile(bucketKey);
	}
}
