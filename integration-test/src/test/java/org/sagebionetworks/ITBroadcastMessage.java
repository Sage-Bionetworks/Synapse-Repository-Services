package org.sagebionetworks;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.jupiter.api.AfterAll;
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
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

@ExtendWith(ITTestExtension.class)
public class ITBroadcastMessage {
	private static SynapseClient synapseTwo;
	private static List<Long> userToDelete = new ArrayList<Long>();
	private Project project;
	private String projectId;
	private String forumId;
	private String bucketKeyOne;
	private String bucketKeyTwo;
	
	private SynapseAdminClient adminSynapse;
	private SynapseClient synapse;
	
	public ITBroadcastMessage(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}

	@BeforeAll
	public static void beforeClass(SynapseAdminClient adminSynapse) throws Exception {
		synapseTwo = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseTwo);
		userToDelete.add(SynapseClientHelper.createUser(adminSynapse, synapseTwo));
		synapseTwo.updateMyProfile(synapseTwo.getMyProfile());
		synapseTwo.setUsername("test2");
	}

	@AfterAll
	public static void afterClass(SynapseAdminClient adminSynapse) throws Exception {
		try {
			for (Long user : userToDelete) {
				adminSynapse.deleteUser(user);
			}
		} catch (SynapseException e) { }
	}

	@BeforeEach
	public void before() throws SynapseException {
		bucketKeyOne = EmailValidationUtil.getBucketKeyForEmail(synapse.getNotificationEmail().getEmail());
		bucketKeyTwo = EmailValidationUtil.getBucketKeyForEmail(synapseTwo.getNotificationEmail().getEmail());
		project = new Project();
		project = synapse.createEntity(project);
		assertNotNull(project);
		projectId = project.getId();
		forumId = synapse.getForumByProjectId(projectId).getId();

		grantREADPermission(synapse, projectId, synapseTwo);

		Topic toSubscribe = new Topic();
		toSubscribe.setObjectId(forumId);
		toSubscribe.setObjectType(SubscriptionObjectType.FORUM);
		synapseTwo.subscribe(toSubscribe);
	}

	private void grantREADPermission(SynapseClient synapse, String projectId, SynapseClient synapseTwo) throws SynapseException {
		AccessControlList acl = synapse.getACL(projectId);
		Set<ResourceAccess> rs = acl.getResourceAccess();
		ResourceAccess synapseTwoAccess = new ResourceAccess();
		synapseTwoAccess.setPrincipalId(Long.parseLong(ITBroadcastMessage.synapseTwo.getMyProfile().getOwnerId()));
		synapseTwoAccess.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.READ)));
		rs.add(synapseTwoAccess );
		acl.setResourceAccess(rs);
		synapse.updateACL(acl);
	}

	@After
	public void cleanup() throws SynapseException, JSONObjectAdapterException {
		if (project != null) adminSynapse.deleteEntity(project, true);
		synapse.unsubscribeAll();
	}

	@Test
	public void test() throws Exception {
		assertFalse(EmailValidationUtil.doesFileExist(bucketKeyOne, 1000L));
		assertFalse(EmailValidationUtil.doesFileExist(bucketKeyTwo, 1000L));
		CreateDiscussionThread toCreate = new CreateDiscussionThread();
		toCreate.setForumId(forumId);
		String title = "Question about file missing";
		toCreate.setTitle(title);
		String message = "I think my cats ate my files. Please help!";
		toCreate.setMessageMarkdown(message);
		synapse.createThread(toCreate);
		assertFalse(EmailValidationUtil.doesFileExist(bucketKeyOne, 60000L));
		assertTrue(EmailValidationUtil.doesFileExist(bucketKeyTwo, 60000L));
		assertNotNull(EmailValidationUtil.readFile(bucketKeyTwo));
		EmailValidationUtil.deleteFile(bucketKeyTwo);
	}
}
