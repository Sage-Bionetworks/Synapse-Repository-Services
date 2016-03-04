package org.sagebionetworks.repo.model.dbo.dao.subscription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.sagebionetworks.repo.model.subscription.Topic;
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
		subscriptionDao.getSubscriptionList(null, new ArrayList<Topic>(0));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetListWithNullTopicList() {
		subscriptionDao.getSubscriptionList(userId, null);
	}

	@Test
	public void testGetListWithEmptyTopicList() {
		Subscription dto = subscriptionDao.create(userId, objectId, objectType);
		SubscriptionPagedResults results = subscriptionDao.getSubscriptionList(userId, new ArrayList<Topic>(0));
		assertEquals((Long) 0L, results.getTotalNumberOfResults());
		assertEquals(new ArrayList<Subscription>(0), results.getResults());
		subscriptionIdToDelete.add(dto.getSubscriptionId());
	}

	@Test
	public void testGetList() {
		Subscription dto = subscriptionDao.create(userId, objectId, objectType);
		ArrayList<Topic> list = new ArrayList<Topic>();
		Topic topic = new Topic();
		topic.setObjectId(objectId);
		topic.setObjectType(objectType);
		list.add(topic);
		SubscriptionPagedResults results = subscriptionDao.getSubscriptionList(userId, list);
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		assertEquals(dto, results.getResults().get(0));

		Topic topic2 = new Topic();
		topic2.setObjectId("456");
		topic2.setObjectType(SubscriptionObjectType.FORUM);
		list.add(topic2);
		results = subscriptionDao.getSubscriptionList(userId, list);
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

	@Test
	public void testAddConditionWithNullObjectType() {
		assertEquals("query", DBOSubscriptionDAOImpl.addCondition("query", null));
	}

	@Test
	public void testAddCondition() {
		assertEquals("query AND OBJECT_TYPE = \"FORUM\"",
				DBOSubscriptionDAOImpl.addCondition("query", SubscriptionObjectType.FORUM));
	}

	@Test
	public void testBuildGetQuery() {
		assertEquals("SELECT * FROM SUBSCRIPTION WHERE SUBSCRIBER_ID = ? AND OBJECT_TYPE = \"FORUM\" limit 10 offset 0",
				DBOSubscriptionDAOImpl.buildGetQuery(10L, 0L, SubscriptionObjectType.FORUM));
	}

	@Test
	public void testBuildConditionWithOneElement() {
		Topic topic = new Topic();
		topic.setObjectId("123");
		topic.setObjectType(SubscriptionObjectType.FORUM);
		assertEquals(" AND (OBJECT_ID, OBJECT_TYPE) IN ((123, \"FORUM\"))",
				DBOSubscriptionDAOImpl.buildTopicCondition(Arrays.asList(topic)));
	}

	@Test
	public void testBuildConditionWithTwoElements() {
		Topic topic1 = new Topic();
		topic1.setObjectId("123");
		topic1.setObjectType(SubscriptionObjectType.FORUM);
		Topic topic2 = new Topic();
		topic2.setObjectId("456");
		topic2.setObjectType(SubscriptionObjectType.DISCUSSION_THREAD);
		assertEquals(" AND (OBJECT_ID, OBJECT_TYPE) IN ((123, \"FORUM\"), (456, \"DISCUSSION_THREAD\"))",
				DBOSubscriptionDAOImpl.buildTopicCondition(Arrays.asList(topic1, topic2)));
	}
}
