package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;

public class SubscriptionControllerAutowiredTest extends AbstractAutowiredControllerTestBase{

	private Entity project;
	private Long adminUserId;
	private Forum forum;
	private Topic toSubscribe;

	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = servletTestHelper.createEntity(dispatchServlet, project, adminUserId);

		CreateDiscussionThread createThread = new CreateDiscussionThread();
		createThread.setTitle("title");
		createThread.setMessageMarkdown("messageMarkdown");

		forum = servletTestHelper.getForumByProjectId(dispatchServlet, project.getId(), adminUserId);

		toSubscribe = new Topic();
		toSubscribe.setObjectType(SubscriptionObjectType.FORUM);
		toSubscribe.setObjectId(forum.getId());

		servletTestHelper.unsubscribeAll(dispatchServlet, adminUserId);
	}

	@After
	public void cleanup() throws Exception {
		servletTestHelper.unsubscribeAll(dispatchServlet, adminUserId);
		try {
			servletTestHelper.deleteEntity(dispatchServlet, null, project.getId(), adminUserId,
					Collections.singletonMap("skipTrashCan", "false"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testCreate() throws Exception {
		Subscription subscription = servletTestHelper.subscribe(dispatchServlet, adminUserId, toSubscribe);
		assertEquals(toSubscribe.getObjectId(), subscription.getObjectId());
		assertEquals(toSubscribe.getObjectType(), subscription.getObjectType());
		assertEquals(adminUserId.toString(), subscription.getSubscriberId());
	}

	@Test
	public void testGet() throws Exception {
		Subscription subscription = servletTestHelper.subscribe(dispatchServlet, adminUserId, toSubscribe);
		assertEquals(subscription, servletTestHelper.get(dispatchServlet, adminUserId, subscription.getSubscriptionId()));
	}

	@Test
	public void testGetAll() throws Exception {
		Subscription subscription = servletTestHelper.subscribe(dispatchServlet, adminUserId, toSubscribe);
		Long limit = 10L;
		Long offset = 0L;
		SubscriptionPagedResults results = servletTestHelper.getAllSubscriptions(dispatchServlet, adminUserId, limit, offset, toSubscribe.getObjectType());
		assertNotNull(results);
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		List<Subscription> subscriptions = results.getResults();
		assertEquals(1L, subscriptions.size());
		assertEquals(subscription, subscriptions.get(0));
	}

	@Test
	public void testDelete() throws Exception {
		Subscription subscription = servletTestHelper.subscribe(dispatchServlet, adminUserId, toSubscribe);
		String subscriptionId = subscription.getSubscriptionId();
		Long limit = 10L;
		Long offset = 0L;
		SubscriptionPagedResults results = servletTestHelper.getAllSubscriptions(dispatchServlet, adminUserId, limit, offset, toSubscribe.getObjectType());
		assertTrue(results.getResults().contains(subscription));
		servletTestHelper.unsubscribe(dispatchServlet, adminUserId, subscriptionId);
		subscription = null;
		results = servletTestHelper.getAllSubscriptions(dispatchServlet, adminUserId, limit, offset, toSubscribe.getObjectType());
		assertFalse(results.getResults().contains(subscription));
		servletTestHelper.unsubscribe(dispatchServlet, adminUserId, subscriptionId);
	}

	@Test
	public void testGetList() throws Exception {
		Subscription subscription = servletTestHelper.subscribe(dispatchServlet, adminUserId, toSubscribe);
		SubscriptionRequest request = new SubscriptionRequest();
		request.setObjectType(toSubscribe.getObjectType());
		request.setIdList(Arrays.asList(toSubscribe.getObjectId()));
		SubscriptionPagedResults results = servletTestHelper.getSubscriptionList(dispatchServlet, adminUserId, request);
		assertNotNull(results);
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		List<Subscription> subscriptions = results.getResults();
		assertEquals(1L, subscriptions.size());
		assertEquals(subscription, subscriptions.get(0));
	}

}
