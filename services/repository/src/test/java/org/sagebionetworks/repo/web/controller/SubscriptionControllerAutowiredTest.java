package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
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

		forum = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);

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
		servletTestHelper.unsubscribe(dispatchServlet, adminUserId, subscription.getSubscriptionId());
		subscription = null;
		Long limit = 10L;
		Long offset = 0L;
		SubscriptionPagedResults results = servletTestHelper.getAllSubscriptions(dispatchServlet, adminUserId, limit, offset, toSubscribe.getObjectType());
		assertNotNull(results);
		assertEquals((Long) 0L, results.getTotalNumberOfResults());
		List<Subscription> subscriptions = results.getResults();
		assertEquals(0L, subscriptions.size());
	}

	@Test
	public void testGetList() throws Exception {
		Subscription subscription = servletTestHelper.subscribe(dispatchServlet, adminUserId, toSubscribe);
		IdList idList = new IdList();
		idList.setList(Arrays.asList(Long.parseLong(toSubscribe.getObjectId())));
		SubscriptionPagedResults results = servletTestHelper.getSubscriptionList(dispatchServlet, adminUserId, toSubscribe.getObjectType(), idList);
		assertNotNull(results);
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		List<Subscription> subscriptions = results.getResults();
		assertEquals(1L, subscriptions.size());
		assertEquals(subscription, subscriptions.get(0));
	}
}
