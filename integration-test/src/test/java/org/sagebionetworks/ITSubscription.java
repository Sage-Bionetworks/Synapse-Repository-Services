package org.sagebionetworks;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.subscription.SortByType;
import org.sagebionetworks.repo.model.subscription.SortDirection;
import org.sagebionetworks.repo.model.subscription.SubscriberCount;
import org.sagebionetworks.repo.model.subscription.SubscriberPagedResults;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;

public class ITSubscription extends BaseITTest {

	private Project project;
	private String projectId;
	private Forum forum;

	@BeforeEach
	public void before() throws SynapseException {
		project = new Project();
		project = synapse.createEntity(project);
		assertNotNull(project);
		projectId = project.getId();
		forum = synapse.getForumByProjectId(projectId);
	}

	@AfterEach
	public void cleanup() throws Exception {
		if (project != null) adminSynapse.deleteEntity(project, true);
		synapse.unsubscribeAll();
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
		SortByType sortType = SortByType.CREATED_ON;
		SortDirection sortDirection = SortDirection.DESC;

		SubscriptionPagedResults results = synapse.getAllSubscriptions(SubscriptionObjectType.FORUM, 10L, 0L, sortType, sortDirection);
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

		SubscriberPagedResults subscribers = synapse.getSubscribers(toSubscribe, null);
		assertNotNull(subscribers);
		assertEquals(Arrays.asList(sub.getSubscriberId()), subscribers.getSubscribers());
		assertNull(subscribers.getNextPageToken());

		SubscriberCount subscriberCount = synapse.getSubscriberCount(toSubscribe);
		assertNotNull(subscriberCount);
		assertEquals((Long) 1L, subscriberCount.getCount());
		

		synapse.unsubscribe(Long.parseLong(sub.getSubscriptionId()));
		sortType = null;
		sortDirection = null;
		results = synapse.getAllSubscriptions(SubscriptionObjectType.FORUM, 10L, 0L, sortType, sortDirection);
		assertFalse(results.getResults().contains(sub));

		assertNotNull(adminSynapse.subscribeAll(SubscriptionObjectType.DATA_ACCESS_SUBMISSION));
	}

}
