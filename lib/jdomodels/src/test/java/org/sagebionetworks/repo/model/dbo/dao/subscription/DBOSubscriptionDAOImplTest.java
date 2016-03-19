package org.sagebionetworks.repo.model.dbo.dao.subscription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOSubscriptionDAOImplTest {

	@Autowired
	private SubscriptionDAO subscriptionDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private IdGenerator idGenerator;

	private String userId = null;
	private String objectId;
	private SubscriptionObjectType objectType;
	private List<String> usersToDelete;
	private List<String> subscriptionIdToDelete;

	@Before
	public void before() {
		usersToDelete = new ArrayList<String>();
		subscriptionIdToDelete = new ArrayList<String>();

		// create a user to create a project
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		userId = userGroupDAO.create(user).toString();
		usersToDelete.add(userId);

		objectId = "123";
		objectType = SubscriptionObjectType.DISCUSSION_THREAD;
	}

	@After
	public void after() {
		for (String subscriptionId : subscriptionIdToDelete) {
			subscriptionDao.delete(Long.parseLong(subscriptionId));
		}
		for (String userId: usersToDelete) {
			if (userId != null) {
				userGroupDAO.delete(userId);
			}
		}
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullSubscriberId() {
		subscriptionDao.create(null, objectId, objectType);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullObjectId() {
		subscriptionDao.create(userId, null, objectType);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullObjectType() {
		subscriptionDao.create(userId, objectId, null);
	}

	@Test
	public void testCreate() {
		Subscription dto = subscriptionDao.create(userId, objectId, objectType);
		assertEquals(userId, dto.getSubscriberId());
		assertEquals(objectId, dto.getObjectId());
		assertEquals(objectType, dto.getObjectType());
		assertEquals(dto, subscriptionDao.get(Long.parseLong(dto.getSubscriptionId())));
		Subscription dto2 = subscriptionDao.create(userId, objectId, objectType);
		assertEquals(dto, dto2);
		subscriptionIdToDelete.add(dto.getSubscriptionId());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllWithNullSubscriberId(){
		subscriptionDao.getAll(null, 10L, 0L, objectType);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllWithNullLimit(){
		subscriptionDao.getAll(userId, null, 0L, objectType);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllWithNullOffset(){
		subscriptionDao.getAll(userId, 10L, null, objectType);
	}

	@Test
	public void testGetAllWithNullObjectType(){
		Subscription dto = subscriptionDao.create(userId, objectId, objectType);
		SubscriptionPagedResults results = subscriptionDao.getAll(userId, 10L, 0L, null);
		assertEquals(1L, results.getResults().size());
		assertEquals(dto, results.getResults().get(0));
		subscriptionIdToDelete.add(dto.getSubscriptionId());
	}

	@Test
	public void testGetAllWithObjectType() {
		Subscription threadSub = subscriptionDao.create(userId, objectId, SubscriptionObjectType.DISCUSSION_THREAD);
		Subscription forumSub = subscriptionDao.create(userId, objectId, SubscriptionObjectType.FORUM);
		SubscriptionPagedResults results = subscriptionDao.getAll(userId, 10L, 0L, null);
		assertEquals(2L, results.getResults().size());
		assertEquals((Long)2L, results.getTotalNumberOfResults());
		assertTrue(results.getResults().contains(threadSub));
		assertTrue(results.getResults().contains(forumSub));
		results = subscriptionDao.getAll(userId, 10L, 0L, SubscriptionObjectType.DISCUSSION_THREAD);
		assertEquals(1L, results.getResults().size());
		assertEquals(threadSub, results.getResults().get(0));
		results = subscriptionDao.getAll(userId, 10L, 0L, SubscriptionObjectType.FORUM);
		assertEquals(1L, results.getResults().size());
		assertEquals(forumSub, results.getResults().get(0));
		subscriptionIdToDelete.add(threadSub.getSubscriptionId());
		subscriptionIdToDelete.add(forumSub.getSubscriptionId());
	}

	@Test
	public void testGetAllWithOffset(){
		Subscription dto = subscriptionDao.create(userId, objectId, objectType);
		SubscriptionPagedResults results = subscriptionDao.getAll(userId, 10L, 1L, null);
		assertEquals(0L, results.getResults().size());
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		subscriptionIdToDelete.add(dto.getSubscriptionId());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetListWithNullSubscriberId() {
		subscriptionDao.getSubscriptionList(null, SubscriptionObjectType.FORUM, new ArrayList<String>(0));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetListWithNullObjectType() {
		subscriptionDao.getSubscriptionList(userId, null, new ArrayList<String>(0));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetListWithNullIdList() {
		subscriptionDao.getSubscriptionList(userId, SubscriptionObjectType.FORUM, null);
	}

	@Test
	public void testGetListWithEmptyTopicList() {
		Subscription dto = subscriptionDao.create(userId, objectId, objectType);
		SubscriptionPagedResults results = subscriptionDao.getSubscriptionList(userId, SubscriptionObjectType.FORUM, new ArrayList<String>(0));
		assertEquals((Long) 0L, results.getTotalNumberOfResults());
		assertEquals(new ArrayList<Subscription>(0), results.getResults());
		subscriptionIdToDelete.add(dto.getSubscriptionId());
	}

	@Test
	public void testGetList() {
		Subscription dto = subscriptionDao.create(userId, objectId, objectType);
		ArrayList<String> list = new ArrayList<String>();
		list.add(objectId);
		SubscriptionPagedResults results = subscriptionDao.getSubscriptionList(userId, objectType, list);
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		assertEquals(dto, results.getResults().get(0));

		String id2 = "456";
		list.add(id2);
		results = subscriptionDao.getSubscriptionList(userId, objectType, list);
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		assertEquals(dto, results.getResults().get(0));
		subscriptionIdToDelete.add(dto.getSubscriptionId());
	}

	@Test (expected=NotFoundException.class)
	public void testDelete() {
		Subscription dto = subscriptionDao.create(userId, objectId, objectType);
		long id = Long.parseLong(dto.getSubscriptionId());
		subscriptionDao.delete(id);
		subscriptionDao.get(id);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testSubscribeForumSubscriberToThreadWithNullForumId(){
		subscriptionDao.subscribeForumSubscriberToThread(null, objectId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testSubscribeForumSubscriberToThreadWithNullThreadId(){
		subscriptionDao.subscribeForumSubscriberToThread(objectId, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllSubscribersWithNullObjectId() {
		subscriptionDao.getAllSubscribers(null, SubscriptionObjectType.FORUM);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllSubscribersWithNullObjectType() {
		subscriptionDao.getAllSubscribers("123", null);
	}

	@Test
	public void testSubscribeForumSubscriberToThread(){
		String forumId = "123";
		String threadId = "456";
		subscriptionDao.subscribeForumSubscriberToThread(forumId, threadId);
		List<String> forumSubscribers = subscriptionDao.getAllSubscribers(forumId, SubscriptionObjectType.FORUM);
		List<String> threadSubscribers = subscriptionDao.getAllSubscribers(threadId, SubscriptionObjectType.DISCUSSION_THREAD);
		assertTrue(forumSubscribers.isEmpty());
		assertTrue(threadSubscribers.isEmpty());

		subscriptionDao.create(userId, forumId, SubscriptionObjectType.FORUM);
		subscriptionDao.subscribeForumSubscriberToThread(forumId, threadId);
		forumSubscribers = subscriptionDao.getAllSubscribers(forumId, SubscriptionObjectType.FORUM);
		threadSubscribers = subscriptionDao.getAllSubscribers(threadId, SubscriptionObjectType.DISCUSSION_THREAD);
		assertEquals(1L, forumSubscribers.size());
		assertEquals(1L, threadSubscribers.size());
		assertTrue(forumSubscribers.contains(userId));
		assertTrue(threadSubscribers.contains(userId));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testSubscribeAllWithNullUserId() {
		subscriptionDao.subscribeAll(null, new ArrayList<String>(0), SubscriptionObjectType.DISCUSSION_THREAD);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testSubscribeAllWithNullIdList() {
		subscriptionDao.subscribeAll(userId, null, SubscriptionObjectType.DISCUSSION_THREAD);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testSubscribeAllWithNullObjectType() {
		subscriptionDao.subscribeAll(userId, new ArrayList<String>(0), null);
	}

	@Test
	public void testSubscribeAllWithEmptyIdList() {
		subscriptionDao.subscribeAll(userId, new ArrayList<String>(0), SubscriptionObjectType.DISCUSSION_THREAD);
		assertTrue(subscriptionDao.getAll(userId, 10L, 0L, SubscriptionObjectType.DISCUSSION_THREAD).getResults().isEmpty());
	}

	@Test
	public void testSubscribeAll() {
		List<String> idList = new ArrayList<String>();
		idList.add(objectId);
		subscriptionDao.subscribeAll(userId, idList , SubscriptionObjectType.DISCUSSION_THREAD);
		List<Subscription> subs = subscriptionDao.getAll(userId, 10L, 0L, SubscriptionObjectType.DISCUSSION_THREAD).getResults();
		assertEquals(1L, subs.size());
		Subscription sub = subs.get(0);
		assertEquals(objectId, sub.getObjectId());
		assertEquals(SubscriptionObjectType.DISCUSSION_THREAD, sub.getObjectType());
		assertEquals(userId, sub.getSubscriberId());
	}
}
