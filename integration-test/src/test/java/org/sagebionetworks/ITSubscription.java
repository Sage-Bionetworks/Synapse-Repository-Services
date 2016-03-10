package org.sagebionetworks;

import static org.junit.Assert.*;

import java.util.Arrays;

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
	public void cleanup() throws SynapseException, JSONObjectAdapterException {
		if (project != null) adminSynapse.deleteEntity(project, true);
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

		assertEquals(sub, synapse.get(sub.getSubscriptionId()));

		SubscriptionPagedResults results = synapse.getAllSubscriptions(null, 10L, 0L);
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
		results = synapse.getAllSubscriptions(null, 10L, 0L);
		assertFalse(results.getResults().contains(sub));
	}

}
