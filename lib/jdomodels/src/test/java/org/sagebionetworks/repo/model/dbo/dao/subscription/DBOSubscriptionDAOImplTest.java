package org.sagebionetworks.repo.model.dbo.dao.subscription;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.subscription.Subscriber;
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
	private UserProfileDAO userProfileDao;
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	@Autowired
	private NotificationEmailDAO notificationEmailDAO;
	@Autowired
	private ForumDAO forumDAO;
	@Autowired
	private DiscussionThreadDAO threadDAO;
	@Autowired
	private NodeDAO nodeDAO;

	private String userId = null;
	private String threadId;
	private List<String> usersToDelete;
	private List<String> subscriptionIdToDelete;
	String projectId;
	String forumId;
	Subscriber subscriber;
	Set<Long> projectIds;

	@Before
	public void before() {
		usersToDelete = new ArrayList<String>();
		subscriptionIdToDelete = new ArrayList<String>();

		// create a user to create a project
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		userId = userGroupDAO.create(user).toString();
		usersToDelete.add(userId);
		
		subscriber = new Subscriber();
		subscriber.setFirstName("foo");
		subscriber.setLastName("bar");
		subscriber.setNotificationEmail("foo.bar@domain.org");
		subscriber.setUsername("someUsername");
		
		UserProfile profile = new UserProfile();
		profile.setFirstName(subscriber.getFirstName());
		profile.setLastName(subscriber.getLastName());
		profile.setOwnerId(userId);
		Settings settings = new Settings();
		settings.setSendEmailNotifications(true);
		profile.setNotificationSettings(settings);
		userProfileDao.create(profile);
		
		// username
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(subscriber.getUsername());
		alias.setPrincipalId(Long.parseLong(userId));
		alias.setType(AliasType.USER_NAME);
		principalAliasDAO.bindAliasToPrincipal(alias);
		// email
		alias = new PrincipalAlias();
		alias.setAlias(subscriber.getNotificationEmail());
		alias.setPrincipalId(Long.parseLong(userId));
		alias.setType(AliasType.USER_EMAIL);
		PrincipalAlias pa = principalAliasDAO.bindAliasToPrincipal(alias);
		notificationEmailDAO.create(pa);

		projectId = "321";
		Node node = new Node();
		node.setId(projectId);
		node.setModifiedByPrincipalId(1L);
		node.setModifiedOn(new Date());
		node.setNodeType(EntityType.project);
		node.setName("project");
		node.setCreatedByPrincipalId(1L);
		node.setCreatedOn(new Date());
		nodeDAO.createNew(node);
		forumId = forumDAO.createForum(projectId).getId();
		threadId = "123";
		threadDAO.createThread(forumId, threadId, "title", "messageKey", Long.parseLong(userId));

		projectIds = new HashSet<Long>();
		projectIds.add(Long.parseLong(projectId));
	}

	@After
	public void after() {
		for (String subscriptionId : subscriptionIdToDelete) {
			subscriptionDao.delete(Long.parseLong(subscriptionId));
		}
		try {
			nodeDAO.delete(projectId);
		} catch (Exception e) {}
		for (String userId: usersToDelete) {
			if (userId != null) {
				userGroupDAO.delete(userId);
			}
		}
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullSubscriberId() {
		subscriptionDao.create(null, threadId, SubscriptionObjectType.THREAD);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullObjectId() {
		subscriptionDao.create(userId, null, SubscriptionObjectType.THREAD);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateWithNullObjectType() {
		subscriptionDao.create(userId, threadId, null);
	}

	@Test
	public void testCreate() {
		Subscription dto = subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		assertEquals(userId, dto.getSubscriberId());
		assertEquals(threadId, dto.getObjectId());
		assertEquals(SubscriptionObjectType.THREAD, dto.getObjectType());
		assertEquals(dto, subscriptionDao.get(Long.parseLong(dto.getSubscriptionId())));
		Subscription dto2 = subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		assertEquals(dto, dto2);
		subscriptionIdToDelete.add(dto.getSubscriptionId());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllWithNullSubscriberId(){
		subscriptionDao.getAll(null, 10L, 0L, SubscriptionObjectType.THREAD, new HashSet<Long>());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllWithNullLimit(){
		subscriptionDao.getAll(userId, null, 0L, SubscriptionObjectType.THREAD, new HashSet<Long>());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllWithNullOffset(){
		subscriptionDao.getAll(userId, 10L, null, SubscriptionObjectType.THREAD, new HashSet<Long>());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllWithNullObjectType(){
		subscriptionDao.getAll(userId, 10L, 0L, null, new HashSet<Long>());
	}

	@Test
	public void testGetAllWithEmptyProjectIds(){
		subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		SubscriptionPagedResults threadSubs = subscriptionDao.getAll(userId, 10L, 0L, SubscriptionObjectType.THREAD, new HashSet<Long>());
		assertNotNull(threadSubs);
		assertEquals((Long)0L, threadSubs.getTotalNumberOfResults());
		assertTrue(threadSubs.getResults().isEmpty());
	}

	@Test
	public void testGetAllThreadSubs() {
		Subscription threadSub = subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		SubscriptionPagedResults results = subscriptionDao.getAll(userId, 10L, 0L, SubscriptionObjectType.THREAD, projectIds);
		assertEquals(1L, results.getResults().size());
		assertEquals(threadSub, results.getResults().get(0));

		threadDAO.markThreadAsDeleted(Long.parseLong(threadId));
		results = subscriptionDao.getAll(userId, 10L, 0L, SubscriptionObjectType.THREAD, projectIds);
		assertEquals(0L, results.getResults().size());
		nodeDAO.delete(projectId);
		results = subscriptionDao.getAll(userId, 10L, 0L, SubscriptionObjectType.THREAD, projectIds);
		assertEquals(0L, results.getResults().size());
		subscriptionIdToDelete.add(threadSub.getSubscriptionId());
	}

	@Test
	public void testGetAllForumSubs(){
		Subscription forumSub = subscriptionDao.create(userId, forumId, SubscriptionObjectType.FORUM);
		SubscriptionPagedResults results = subscriptionDao.getAll(userId, 10L, 0L, SubscriptionObjectType.FORUM, projectIds);
		assertEquals(1L, results.getResults().size());
		assertEquals(forumSub, results.getResults().get(0));
		nodeDAO.delete(projectId);
		results = subscriptionDao.getAll(userId, 10L, 0L, SubscriptionObjectType.FORUM, projectIds);
		assertEquals(0L, results.getResults().size());
		subscriptionIdToDelete.add(forumSub.getSubscriptionId());
	}

	@Test
	public void testGetAllWithOffset(){
		Subscription dto = subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		SubscriptionPagedResults results = subscriptionDao.getAll(userId, 10L, 1L, SubscriptionObjectType.THREAD, projectIds);
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
		Subscription dto = subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		SubscriptionPagedResults results = subscriptionDao.getSubscriptionList(userId, SubscriptionObjectType.FORUM, new ArrayList<String>(0));
		assertEquals((Long) 0L, results.getTotalNumberOfResults());
		assertEquals(new ArrayList<Subscription>(0), results.getResults());
		subscriptionIdToDelete.add(dto.getSubscriptionId());
	}

	@Test
	public void testGetList() {
		Subscription dto = subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		ArrayList<String> list = new ArrayList<String>();
		list.add(threadId);
		SubscriptionPagedResults results = subscriptionDao.getSubscriptionList(userId, SubscriptionObjectType.THREAD, list);
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		assertEquals(dto, results.getResults().get(0));

		String id2 = "456";
		list.add(id2);
		results = subscriptionDao.getSubscriptionList(userId, SubscriptionObjectType.THREAD, list);
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		assertEquals(dto, results.getResults().get(0));
		subscriptionIdToDelete.add(dto.getSubscriptionId());

		threadDAO.markThreadAsDeleted(Long.parseLong(threadId));
		results = subscriptionDao.getSubscriptionList(userId, SubscriptionObjectType.THREAD, list);
		assertEquals((Long) 0L, results.getTotalNumberOfResults());

		nodeDAO.delete(projectId);
		results = subscriptionDao.getSubscriptionList(userId, SubscriptionObjectType.THREAD, list);
		assertEquals((Long) 0L, results.getTotalNumberOfResults());
	}

	@Test (expected=NotFoundException.class)
	public void testDelete() {
		Subscription dto = subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		long id = Long.parseLong(dto.getSubscriptionId());
		subscriptionDao.delete(id);
		subscriptionDao.get(id);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllSubscribersWithNullObjectId() {
		subscriptionDao.getAllSubscribers(null, SubscriptionObjectType.FORUM);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllSubscribersWithNullObjectType() {
		subscriptionDao.getAllSubscribers("123", null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testSubscribeAllUsersWithNullSubscribers(){
		subscriptionDao.subscribeAllUsers(null, threadId, SubscriptionObjectType.THREAD);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testSubscribeAllUsersWithNullObjectId(){
		subscriptionDao.subscribeAllUsers(new HashSet<String>(0), null, SubscriptionObjectType.THREAD);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testSubscribeAllUsersWithNullObjectType(){
		subscriptionDao.subscribeAllUsers(new HashSet<String>(0), threadId, null);
	}

	@Test
	public void testSubscribeAllUsersWithEmptySubscribers(){
		List<String> currentSubscribers = subscriptionDao.getAllSubscribers(threadId, SubscriptionObjectType.THREAD);
		subscriptionDao.subscribeAllUsers(new HashSet<String>(0), threadId, SubscriptionObjectType.THREAD);
		assertEquals(currentSubscribers, subscriptionDao.getAllSubscribers(threadId, SubscriptionObjectType.THREAD));
	}

	@Test
	public void testSubscribeAllUsers(){
		List<String> before = subscriptionDao.getAllSubscribers(threadId, SubscriptionObjectType.THREAD);
		Set<String> subscribers = new HashSet<String>();
		subscribers.add(userId);
		subscriptionDao.subscribeAllUsers(subscribers , threadId, SubscriptionObjectType.THREAD);
		List<String> after = subscriptionDao.getAllSubscribers(threadId, SubscriptionObjectType.THREAD);
		assertFalse(before.equals(after));
		assertEquals(1, after.size() - before.size());
	}

	@Test
	public void testGetAllEmailSubscribers(){
		List<Subscriber> forumSubscribers = subscriptionDao.getAllEmailSubscribers(threadId, SubscriptionObjectType.THREAD);
		assertTrue(forumSubscribers.isEmpty());

		subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		forumSubscribers = subscriptionDao.getAllEmailSubscribers(threadId, SubscriptionObjectType.THREAD);
		assertEquals(1L, forumSubscribers.size());
		Subscriber sub = forumSubscribers.get(0);

		assertEquals(userId, sub.getSubscriberId());
		assertNotNull(sub.getSubscriberId());
		assertEquals(subscriber.getFirstName(), sub.getFirstName());
		assertEquals(subscriber.getLastName(), sub.getLastName());
		assertEquals(subscriber.getNotificationEmail(), sub.getNotificationEmail());
		assertEquals(subscriber.getUsername(), sub.getUsername());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testSubscribeAllWithNullUserId() {
		subscriptionDao.subscribeAllTopic(null, new ArrayList<String>(0), SubscriptionObjectType.THREAD);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testSubscribeAllWithNullIdList() {
		subscriptionDao.subscribeAllTopic(userId, null, SubscriptionObjectType.THREAD);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testSubscribeAllWithNullObjectType() {
		subscriptionDao.subscribeAllTopic(userId, new ArrayList<String>(0), null);
	}

	@Test
	public void testSubscribeAllWithEmptyIdList() {
		subscriptionDao.subscribeAllTopic(userId, new ArrayList<String>(0), SubscriptionObjectType.THREAD);
		assertTrue(subscriptionDao.getAll(userId, 10L, 0L, SubscriptionObjectType.THREAD, projectIds).getResults().isEmpty());
	}

	@Test
	public void testSubscribeAll() {
		List<String> idList = new ArrayList<String>();
		idList.add(threadId);
		subscriptionDao.subscribeAllTopic(userId, idList , SubscriptionObjectType.THREAD);
		List<Subscription> subs = subscriptionDao.getAll(userId, 10L, 0L, SubscriptionObjectType.THREAD, projectIds).getResults();
		assertEquals(1L, subs.size());
		Subscription sub = subs.get(0);
		assertEquals(threadId, sub.getObjectId());
		assertEquals(SubscriptionObjectType.THREAD, sub.getObjectType());
		assertEquals(userId, sub.getSubscriberId());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDeleteListWithNullUserId() {
		subscriptionDao.deleteList(null, new ArrayList<String>(0), SubscriptionObjectType.THREAD);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDeleteListWithNullIdList() {
		subscriptionDao.deleteList(userId, null, SubscriptionObjectType.THREAD);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDeleteListWithNullObjectType() {
		subscriptionDao.deleteList(userId, new ArrayList<String>(0), null);
	}

	@Test
	public void testDeleteListWithEmptyIdList() {
		List<String> idList = new ArrayList<String>();
		idList.add(threadId);
		subscriptionDao.subscribeAllTopic(userId, idList , SubscriptionObjectType.THREAD);
		subscriptionDao.deleteList(userId, new ArrayList<String>(0), SubscriptionObjectType.THREAD);
		List<Subscription> subs = subscriptionDao.getAll(userId, 10L, 0L, SubscriptionObjectType.THREAD, projectIds).getResults();
		assertEquals(1L, subs.size());
		Subscription sub = subs.get(0);
		assertEquals(threadId, sub.getObjectId());
		assertEquals(SubscriptionObjectType.THREAD, sub.getObjectType());
		assertEquals(userId, sub.getSubscriberId());
	}

	@Test
	public void testDeleteList() {
		List<String> idList = new ArrayList<String>();
		idList.add(threadId);
		subscriptionDao.subscribeAllTopic(userId, idList , SubscriptionObjectType.THREAD);
		subscriptionDao.deleteList(userId, idList , SubscriptionObjectType.THREAD);
		assertTrue(subscriptionDao.getAll(userId, 10L, 0L, SubscriptionObjectType.THREAD, projectIds).getResults().isEmpty());
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetAllProjectsWithNullUserId() {
		subscriptionDao.getAllProjects(null, SubscriptionObjectType.THREAD);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetAllProjectsWithNullObjectType() {
		subscriptionDao.getAllProjects(userId, null);
	}

	@Test
	public void testGetAllProjectsForThreadSubs() {
		Set<Long> projects = subscriptionDao.getAllProjects(userId, SubscriptionObjectType.THREAD);
		assertNotNull(projects);
		assertTrue(projects.isEmpty());
		subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		projects = subscriptionDao.getAllProjects(userId, SubscriptionObjectType.THREAD);
		assertNotNull(projects);
		assertEquals(1L, projects.size());
		assertTrue(projects.contains(Long.parseLong(projectId)));
	}

	@Test
	public void testGetAllProjectsForForumSubs() {
		Set<Long> projects = subscriptionDao.getAllProjects(userId, SubscriptionObjectType.FORUM);
		assertNotNull(projects);
		assertTrue(projects.isEmpty());
		subscriptionDao.create(userId, forumId, SubscriptionObjectType.FORUM);
		projects = subscriptionDao.getAllProjects(userId, SubscriptionObjectType.FORUM);
		assertNotNull(projects);
		assertEquals(1L, projects.size());
		assertTrue(projects.contains(Long.parseLong(projectId)));
	}
}
