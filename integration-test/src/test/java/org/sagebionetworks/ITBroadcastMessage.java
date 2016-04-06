package org.sagebionetworks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class ITBroadcastMessage {
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static SynapseClient synapseTwo;
	private static List<Long> userToDelete = new ArrayList<Long>();
	private Project project;
	private String projectId;
	private String forumId;
	private String bucketKeyOne;
	private String bucketKeyTwo;

	@BeforeClass
	public static void beforeClass() throws Exception {
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapseOne = new SynapseClientImpl();
		synapseTwo = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseOne);
		userToDelete.add(SynapseClientHelper.createUser(adminSynapse, synapseOne));
		userToDelete.add(SynapseClientHelper.createUser(adminSynapse, synapseTwo));
		synapseOne.updateMyProfile(synapseOne.getMyProfile());
		synapseOne.setUserName("test1");
		synapseTwo.updateMyProfile(synapseTwo.getMyProfile());
		synapseTwo.setUserName("test2");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		try {
			for (Long user : userToDelete) {
				adminSynapse.deleteUser(user);
			}
		} catch (SynapseException e) { }
	}

	@Before
	public void before() throws SynapseException {
		bucketKeyOne = EmailValidationUtil.getBucketKeyForEmail(synapseOne.getNotificationEmail());
		bucketKeyTwo = EmailValidationUtil.getBucketKeyForEmail(synapseTwo.getNotificationEmail());
		project = new Project();
		project = synapseOne.createEntity(project);
		assertNotNull(project);
		projectId = project.getId();
		forumId = synapseOne.getForumByProjectId(projectId).getId();

		grantREADPermission(synapseOne, projectId, synapseTwo);

		Topic toSubscribe = new Topic();
		toSubscribe.setObjectId(forumId);
		toSubscribe.setObjectType(SubscriptionObjectType.FORUM);
		synapseTwo.subscribe(toSubscribe );
	}

	private void grantREADPermission(SynapseClient synapseOne, String projectId, SynapseClient synpaseTwo) throws SynapseException {
		AccessControlList acl = synapseOne.getACL(projectId);
		Set<ResourceAccess> rs = acl.getResourceAccess();
		ResourceAccess synpaseTwoAccess = new ResourceAccess();
		synpaseTwoAccess.setPrincipalId(Long.parseLong(synapseTwo.getMyProfile().getOwnerId()));
		synpaseTwoAccess.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.READ)));
		rs.add(synpaseTwoAccess );
		acl.setResourceAccess(rs);
		synapseOne.updateACL(acl);
	}

	@After
	public void cleanup() throws SynapseException, JSONObjectAdapterException {
		if (project != null) adminSynapse.deleteEntity(project, true);
		synapseOne.unsubscribeAll();
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
		synapseOne.createThread(toCreate);
		assertFalse(EmailValidationUtil.doesFileExist(bucketKeyOne, 60000L));
		assertTrue(EmailValidationUtil.doesFileExist(bucketKeyTwo, 60000L));
		assertNotNull(EmailValidationUtil.readFile(bucketKeyTwo));
		EmailValidationUtil.deleteFile(bucketKeyTwo);
	}
}
