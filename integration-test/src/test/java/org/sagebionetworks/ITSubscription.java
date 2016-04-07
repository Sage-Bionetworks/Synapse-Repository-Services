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
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.subscription.Etag;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class ITSubscription {

	private static SynapseClient synapse;
	private static SynapseAdminClient adminSynapse;
	private static Long userToDelete;
	private Project project;
	private String projectId;
	private Forum forum;

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
		projectId = project.getId();
		forum = synapse.getForumByProjectId(projectId);
	}

	@After
	public void cleanup() throws Exception {
		if (project != null) adminSynapse.deleteEntity(project, true);
		synapse.unsubscribeAll();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		if (userToDelete != null) adminSynapse.deleteUser(userToDelete);
	}
	@Test
	public void test() throws SynapseException {
		String forumId = forum.getId();
		Topic toSubscribe = new Topic();
		toSubscribe.setObjectId(forumId);
		toSubscribe.setObjectType(SubscriptionObjectType.FORUM);
		Subscription sub = synapse.subscribe(toSubscribe);
		assertNotNull(sub);
		assertEquals(forumId, sub.getObjectId());
		assertEquals(SubscriptionObjectType.FORUM, sub.getObjectType());
		assertEquals(userToDelete.toString(), sub.getSubscriberId());

		assertEquals(sub, synapse.getSubscription(sub.getSubscriptionId()));

		SubscriptionPagedResults results = synapse.getAllSubscriptions(SubscriptionObjectType.FORUM, 10L, 0L);
		assertNotNull(results);
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		assertEquals(sub, results.getResults().get(0));

		SubscriptionRequest request = new SubscriptionRequest();
		request.setObjectType(SubscriptionObjectType.FORUM);
		request.setIdList(Arrays.asList(forumId));
		results = synapse.listSubscriptions(request);
		assertNotNull(results);
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		assertEquals(sub, results.getResults().get(0));

		synapse.unsubscribe(Long.parseLong(sub.getSubscriptionId()));
		results = synapse.getAllSubscriptions(SubscriptionObjectType.FORUM, 10L, 0L);
		assertFalse(results.getResults().contains(sub));
	}

	@Test
	public void testGetEtag() throws SynapseException {
		Etag etag = synapse.getEtag(forum.getId(), ObjectType.FORUM);
		assertNotNull(etag);
		assertNotNull(etag.getEtag());
	}

}
