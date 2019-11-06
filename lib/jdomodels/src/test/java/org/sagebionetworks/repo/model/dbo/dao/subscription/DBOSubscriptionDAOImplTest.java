package org.sagebionetworks.repo.model.dbo.dao.subscription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.sagebionetworks.repo.model.dbo.dao.subscription.DBOSubscriptionDAOImpl.LIMIT;
import static org.sagebionetworks.repo.model.dbo.dao.subscription.DBOSubscriptionDAOImpl.OBJECT_IDS;
import static org.sagebionetworks.repo.model.dbo.dao.subscription.DBOSubscriptionDAOImpl.OBJECT_TYPE;
import static org.sagebionetworks.repo.model.dbo.dao.subscription.DBOSubscriptionDAOImpl.OFFSET;
import static org.sagebionetworks.repo.model.dbo.dao.subscription.DBOSubscriptionDAOImpl.PROJECT_IDS;
import static org.sagebionetworks.repo.model.dbo.dao.subscription.DBOSubscriptionDAOImpl.SUBSCRIBER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBSCRIPTION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBSCRIPTION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBSCRIPTION_OBJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBSCRIPTION_OBJECT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SUBSCRIPTION_SUBSCRIBER_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
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
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionListRequest;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.subscription.SortByType;
import org.sagebionetworks.repo.model.subscription.SortDirection;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOSubscriptionDAOImplTest {

	private static final String ALL_OBJECT_IDS = "0";
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
	private Long userIdLong;
	private String threadId;
	private String threadId2;
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
		userIdLong = userGroupDAO.create(user);
		userId = userIdLong.toString();
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

		Node node = new Node();
		node.setModifiedByPrincipalId(1L);
		node.setModifiedOn(new Date()); 
		node.setNodeType(EntityType.project);
		node.setName("project");
		node.setCreatedByPrincipalId(1L);
		node.setCreatedOn(new Date());
		node = nodeDAO.createNewNode(node);
		projectId = KeyFactory.stringToKey(node.getId()).toString();
		
		forumId = forumDAO.createForum(projectId).getId();
		threadId = "123";
		threadDAO.createThread(forumId, threadId, "title", "messageKey", Long.parseLong(userId));
		threadId2 = "456";
		threadDAO.createThread(forumId, threadId2, "title", "messageKeyTwo", Long.parseLong(userId));

		projectIds = new HashSet<Long>();
		projectIds.add(Long.parseLong(projectId));
	}

	@After
	public void after() {
		try {
			principalAliasDAO.removeAllAliasFromPrincipal(userIdLong);
		}  catch (Exception e1) {}
		try {
			subscriptionDao.deleteAll(userIdLong);
		} catch (Exception e1) {}
		try {
			nodeDAO.delete(projectId);
		} catch (Exception e) {}
		for (String userId: usersToDelete) {
			if (userId != null) {
				userGroupDAO.delete(userId);
			}
		}
	}
	
	
	public SubscriptionPagedResults getAllSubscriptions(String subscriberId, Long limit,
			Long offset, SubscriptionObjectType objectType) {
		SubscriptionListRequest request = new SubscriptionListRequest().withSubscriberId(subscriberId)
				.withObjectType(objectType).withLimit(limit).withOffset(offset);
		List<Subscription> subscriptions = subscriptionDao.listSubscriptions(request);
		long count= subscriptionDao.listSubscriptionsCount(request);
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		results.setResults(subscriptions);
		results.setTotalNumberOfResults(count);
		return results;
	}

	public SubscriptionPagedResults getAllThreadSubscriptions(String subscriberId, Long limit,
			Long offset, Set<Long> projectIds) {
		SubscriptionListRequest request = new SubscriptionListRequest().withSubscriberId(subscriberId)
				.withObjectType(SubscriptionObjectType.THREAD).withProjectIds(projectIds).withLimit(limit).withOffset(offset);
		List<Subscription> subscriptions = subscriptionDao.listSubscriptions(request);
		long count= subscriptionDao.listSubscriptionsCount(request);
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		results.setResults(subscriptions);
		results.setTotalNumberOfResults(count);
		return results;
	}

	public SubscriptionPagedResults getAllForumSubscriptions(String subscriberId, Long limit,
			Long offset, Set<Long> projectIds) {
		SubscriptionListRequest request = new SubscriptionListRequest().withSubscriberId(subscriberId)
				.withObjectType(SubscriptionObjectType.FORUM).withProjectIds(projectIds).withLimit(limit).withOffset(offset);
		List<Subscription> subscriptions = subscriptionDao.listSubscriptions(request);
		long count= subscriptionDao.listSubscriptionsCount(request);
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		results.setResults(subscriptions);
		results.setTotalNumberOfResults(count);
		return results;
	}
	
	public SubscriptionPagedResults listSubscriptions(String subscriberId,
			SubscriptionObjectType objectType, List<String> ids) {
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		SubscriptionListRequest request = new SubscriptionListRequest().withSubscriberId(subscriberId)
				.withObjectType(objectType).withObjectIds(ids);
		List<Subscription> subscriptions = subscriptionDao.listSubscriptions(request);
		results.setResults(subscriptions);
		results.setTotalNumberOfResults((long)subscriptions.size());
		return results;
	}

	public SubscriptionPagedResults listSubscriptionForThread(String subscriberId, List<String> ids) {
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		SubscriptionListRequest request = new SubscriptionListRequest().withSubscriberId(subscriberId)
				.withObjectType(SubscriptionObjectType.THREAD).withObjectIds(ids);
		List<Subscription> subscriptions = subscriptionDao.listSubscriptions(request);
		results.setResults(subscriptions);
		results.setTotalNumberOfResults((long)subscriptions.size());
		return results;
	}

	public SubscriptionPagedResults listSubscriptionForForum(String subscriberId, List<String> ids) {
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		SubscriptionListRequest request = new SubscriptionListRequest().withSubscriberId(subscriberId)
				.withObjectType(SubscriptionObjectType.FORUM).withObjectIds(ids);
		List<Subscription> subscriptions = subscriptionDao.listSubscriptions(request);
		results.setResults(subscriptions);
		results.setTotalNumberOfResults((long)subscriptions.size());
		return results;
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
	public void testGetAllSubscriptionsWithNullSubscriberId(){
		getAllSubscriptions(null, 10L, 0L, SubscriptionObjectType.DATA_ACCESS_SUBMISSION);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllSubscriptionsWithNullObjectType(){
		getAllSubscriptions(userId, 10L, 0L, null);
	}

	@Test
	public void testGetAllSubscriptionsWithOffset(){
		Subscription dto = subscriptionDao.create(userId, ALL_OBJECT_IDS, SubscriptionObjectType.DATA_ACCESS_SUBMISSION);
		SubscriptionPagedResults results = getAllSubscriptions(userId, 10L, 1L, SubscriptionObjectType.DATA_ACCESS_SUBMISSION);
		assertEquals(0L, results.getResults().size());
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		subscriptionIdToDelete.add(dto.getSubscriptionId());
	}

	@Test
	public void testGetAllSubmissions(){
		Subscription sub = subscriptionDao.create(userId, ALL_OBJECT_IDS, SubscriptionObjectType.DATA_ACCESS_SUBMISSION);
		SubscriptionPagedResults results = getAllSubscriptions(userId, 10L, 0L, SubscriptionObjectType.DATA_ACCESS_SUBMISSION);
		assertEquals(1L, results.getResults().size());
		assertEquals(sub, results.getResults().get(0));
		subscriptionIdToDelete.add(sub.getSubscriptionId());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllThreadSubscriptionsWithNullSubscriberId(){
		getAllThreadSubscriptions(null, 10L, 0L, projectIds);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllThreadSubscriptionsWithNullProjectIds(){
		getAllSubscriptions(userId, 10L, 0L, null);
	}

	@Test
	public void testGetAllThreadSubs() {
		Subscription threadSub = subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		SubscriptionPagedResults results = getAllThreadSubscriptions(userId, 10L, 0L, projectIds);
		assertEquals(1L, results.getResults().size());
		assertEquals(threadSub, results.getResults().get(0));
	
		threadDAO.markThreadAsDeleted(Long.parseLong(threadId));
		results = getAllThreadSubscriptions(userId, 10L, 0L, projectIds);
		assertEquals(0L, results.getResults().size());
		nodeDAO.delete(projectId);
		results = getAllThreadSubscriptions(userId, 10L, 0L, projectIds);
		assertEquals(0L, results.getResults().size());
		subscriptionIdToDelete.add(threadSub.getSubscriptionId());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllForumSubscriptionsWithNullSubscriberId(){
		getAllForumSubscriptions(null, 10L, 0L, projectIds);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetAllForumSubscriptionsWithNullProjectIds(){
		getAllSubscriptions(userId, 10L, 0L, null);
	}

	@Test
	public void testGetAllForumSubs(){
		Subscription forumSub = subscriptionDao.create(userId, forumId, SubscriptionObjectType.FORUM);
		SubscriptionPagedResults results = getAllForumSubscriptions(userId, 10L, 0L, projectIds);
		assertEquals(1L, results.getResults().size());
		assertEquals(forumSub, results.getResults().get(0));
		nodeDAO.delete(projectId);
		results = getAllForumSubscriptions(userId, 10L, 0L, projectIds);
		assertEquals(0L, results.getResults().size());
		subscriptionIdToDelete.add(forumSub.getSubscriptionId());
	}

	@Test
	public void testGetListWithEmptyTopicList() {
		Subscription dto = subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		SubscriptionPagedResults results = listSubscriptions(userId, SubscriptionObjectType.FORUM, new ArrayList<String>(0));
		assertEquals((Long) 0L, results.getTotalNumberOfResults());
		assertEquals(new ArrayList<Subscription>(0), results.getResults());
		subscriptionIdToDelete.add(dto.getSubscriptionId());
	}

	@Test
	public void testListSubscriptionForThread() {
		Subscription dto = subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		ArrayList<String> list = new ArrayList<String>();
		list.add(threadId);
		SubscriptionPagedResults results = listSubscriptionForThread(userId, list);
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		assertEquals(dto, results.getResults().get(0));

		String id2 = "456";
		list.add(id2);
		results = listSubscriptionForThread(userId, list);
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		assertEquals(dto, results.getResults().get(0));
		subscriptionIdToDelete.add(dto.getSubscriptionId());

		threadDAO.markThreadAsDeleted(Long.parseLong(threadId));
		results = listSubscriptionForThread(userId, list);
		assertEquals((Long) 0L, results.getTotalNumberOfResults());

		nodeDAO.delete(projectId);
		results = listSubscriptionForThread(userId, list);
		assertEquals((Long) 0L, results.getTotalNumberOfResults());
	}

	@Test
	public void testListSubscriptionForForum() {
		Subscription dto = subscriptionDao.create(userId, forumId, SubscriptionObjectType.FORUM);
		ArrayList<String> list = new ArrayList<String>();
		list.add(forumId);
		SubscriptionPagedResults results = listSubscriptionForForum(userId, list);
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		assertEquals(dto, results.getResults().get(0));

		String id2 = "456";
		list.add(id2);
		results = listSubscriptionForForum(userId, list);
		assertEquals((Long) 1L, results.getTotalNumberOfResults());
		assertEquals(dto, results.getResults().get(0));
		subscriptionIdToDelete.add(dto.getSubscriptionId());

		nodeDAO.delete(projectId);
		results = listSubscriptionForForum(userId, list);
		assertEquals((Long) 0L, results.getTotalNumberOfResults());
	}

	@Test
	public void testListSubscriptions(){
		String submissionId = "1";
		ArrayList<String> list = new ArrayList<String>();
		list.add(submissionId);

		SubscriptionPagedResults results = listSubscriptions(userId, SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS, list);
		assertEquals(0L, results.getResults().size());

		Subscription sub = subscriptionDao.create(userId, submissionId, SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS);
		results = listSubscriptions(userId, SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS, list);
		assertEquals(1L, results.getResults().size());
		assertEquals(sub, results.getResults().get(0));
		subscriptionIdToDelete.add(sub.getSubscriptionId());
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

	@Test (expected=IllegalArgumentException.class)
	public void testGetAllProjectsUserHasThreadSubsWithNullUserId() {
		subscriptionDao.getAllProjectsUserHasThreadSubs(null);
	}

	@Test
	public void testGetAllProjectsUserHasThreadSubs() {
		Set<Long> projects = subscriptionDao.getAllProjectsUserHasThreadSubs(userId);
		assertNotNull(projects);
		assertTrue(projects.isEmpty());
		subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		projects = subscriptionDao.getAllProjectsUserHasThreadSubs(userId);
		assertNotNull(projects);
		assertEquals(1L, projects.size());
		assertTrue(projects.contains(Long.parseLong(projectId)));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetAllProjectsUserHasForumSubsWithNullUserId() {
		subscriptionDao.getAllProjectsUserHasForumSubs(null);
	}

	@Test
	public void testGetAllProjectsUserHasForumSubs() {
		Set<Long> projects = subscriptionDao.getAllProjectsUserHasForumSubs(userId);
		assertNotNull(projects);
		assertTrue(projects.isEmpty());
		subscriptionDao.create(userId, forumId, SubscriptionObjectType.FORUM);
		projects = subscriptionDao.getAllProjectsUserHasForumSubs(userId);
		assertNotNull(projects);
		assertEquals(1L, projects.size());
		assertTrue(projects.contains(Long.parseLong(projectId)));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetSubscribersWithNullObjectId() {
		subscriptionDao.getSubscribers(null, SubscriptionObjectType.FORUM, 0, 0);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetSubscribersWithNullObjectType() {
		subscriptionDao.getSubscribers("123", null, 0, 0);
	}

	@Test
	public void testGetSubscribersEmpty() {
		assertNotNull(subscriptionDao.getSubscribers("123", SubscriptionObjectType.FORUM, 0, 0));
	}

	@Test
	public void testGetSubscribers() {
		subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		assertEquals(Arrays.asList(userId),
				subscriptionDao.getSubscribers(threadId, SubscriptionObjectType.THREAD, 10, 0));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetSubscriberCountWithNullObjectId() {
		subscriptionDao.getSubscriberCount(null, SubscriptionObjectType.THREAD);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetSubscriberCountWithNullObjectType() {
		subscriptionDao.getSubscriberCount("1", null);
	}

	@Test
	public void testGetSubscriberCountWithNonExistTopic() {
		assertEquals(0, subscriptionDao.getSubscriberCount("1", SubscriptionObjectType.THREAD));
	}

	@Test
	public void testGetSubscriberCountWithExistTopic() {
		subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		assertEquals(1, subscriptionDao.getSubscriberCount(threadId, SubscriptionObjectType.THREAD));
	}
	
	
	@Test
	public void testGetSortDirection() {
		// call under test
		assertEquals("ASC", DBOSubscriptionDAOImpl.getSortDirection(SortDirection.ASC));
		assertEquals("DESC", DBOSubscriptionDAOImpl.getSortDirection(SortDirection.DESC));
	}
	
	@Test
	public void testGetSortDirectionAllTypes() {
		// each type must be supported
		for(SortDirection direction: SortDirection.values()) {
			assertNotNull(DBOSubscriptionDAOImpl.getSortDirection(direction));
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetSortDirectionNull() {
		// call under test
		DBOSubscriptionDAOImpl.getSortDirection(null);
	}
	
	@Test
	public void testGetColunNameForSortType() {
		assertEquals(COL_SUBSCRIPTION_ID, DBOSubscriptionDAOImpl.getColunNameForSortType(SortByType.SUBSCRIPTION_ID));
		assertEquals(COL_SUBSCRIPTION_SUBSCRIBER_ID, DBOSubscriptionDAOImpl.getColunNameForSortType(SortByType.SUBSCRIBER_ID));
		assertEquals(COL_SUBSCRIPTION_CREATED_ON, DBOSubscriptionDAOImpl.getColunNameForSortType(SortByType.CREATED_ON));
		assertEquals(COL_SUBSCRIPTION_OBJECT_ID, DBOSubscriptionDAOImpl.getColunNameForSortType(SortByType.OBJECT_ID));
		assertEquals(COL_SUBSCRIPTION_OBJECT_TYPE, DBOSubscriptionDAOImpl.getColunNameForSortType(SortByType.OBJECT_TYPE));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetColunNameForSortTypeNull() {
		DBOSubscriptionDAOImpl.getColunNameForSortType(null);
	}
	
	@Test
	public void testGetColumnNameForSortTypeAllTypes() {
		// all types must be supported
		for(SortByType type: SortByType.values()) {
			assertNotNull(DBOSubscriptionDAOImpl.getColunNameForSortType(type));
		}
	}
	
	@Test
	public void testAddTypeSpecificSqlForumNoProjects() {
		SubscriptionObjectType objectType = SubscriptionObjectType.FORUM;
		Set<Long> projectIds = null;
		StringBuilder builder = new StringBuilder();
		// call under test
		DBOSubscriptionDAOImpl.addTypeSpecificSql(builder, objectType, projectIds);
		// join with forum to filter out deleted forums
		assertEquals(" JOIN FORUM F ON (S.OBJECT_ID = F.ID)", builder.toString());
	}
	
	@Test
	public void testAddTypeSpecificSqlForumWithProjects() {
		SubscriptionObjectType objectType = SubscriptionObjectType.FORUM;
		Set<Long> projectIds = new HashSet<>(1);
		projectIds.add(123L);
		StringBuilder builder = new StringBuilder();
		// call under test
		DBOSubscriptionDAOImpl.addTypeSpecificSql(builder, objectType, projectIds);
		assertEquals(" JOIN FORUM F ON (S.OBJECT_ID = F.ID AND F.PROJECT_ID IN (:projectIds))", builder.toString());
	}
	
	@Test
	public void testAddTypeSpecificSqlThreadNoProjects() {
		SubscriptionObjectType objectType = SubscriptionObjectType.THREAD;
		Set<Long> projectIds = null;
		StringBuilder builder = new StringBuilder();
		// call under test
		DBOSubscriptionDAOImpl.addTypeSpecificSql(builder, objectType, projectIds);
		// filter deleted threads
		assertEquals(" JOIN DISCUSSION_THREAD T ON (S.OBJECT_ID = T.ID AND T.IS_DELETED = FALSE)", builder.toString());
	}
	
	@Test
	public void testAddTypeSpecificSqlThreadWithProjects() {
		SubscriptionObjectType objectType = SubscriptionObjectType.THREAD;
		Set<Long> projectIds = new HashSet<>(1);
		projectIds.add(123L);
		StringBuilder builder = new StringBuilder();
		// call under test
		DBOSubscriptionDAOImpl.addTypeSpecificSql(builder, objectType, projectIds);
		// filter deleted threads and projectIds.
		assertEquals(" JOIN DISCUSSION_THREAD T ON (S.OBJECT_ID = T.ID AND T.IS_DELETED = FALSE)"
					+ " JOIN FORUM F ON (T.FORUM_ID = F.ID AND F.PROJECT_ID IN (:projectIds))", builder.toString());
	}
	
	@Test
	public void testAddTypeSpecificSqlOther() {
		SubscriptionObjectType objectType = SubscriptionObjectType.DATA_ACCESS_SUBMISSION;
		Set<Long> projectIds = new HashSet<>(1);
		projectIds.add(123L);
		StringBuilder builder = new StringBuilder();
		// call under test
		DBOSubscriptionDAOImpl.addTypeSpecificSql(builder, objectType, projectIds);
		assertEquals("", builder.toString());
	}
	
	@Test
	public void testCreateQueryCoreBasic() {
		StringBuilder builder = new StringBuilder();
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.DATA_ACCESS_SUBMISSION).withSubscriberId("123");
		// call under test
		DBOSubscriptionDAOImpl.createQueryCore(builder, request);
		assertEquals(" FROM SUBSCRIPTION S WHERE S.OBJECT_TYPE = :objectType AND S.SUBSCRIBER_ID = :subscriberId", builder.toString());
	}
	
	@Test
	public void testCreateQueryCoreWithObjectIds() {
		StringBuilder builder = new StringBuilder();
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.DATA_ACCESS_SUBMISSION).withSubscriberId("123")
				.withObjectIds(Lists.newArrayList("123"));
		// call under test
		DBOSubscriptionDAOImpl.createQueryCore(builder, request);
		assertEquals(" FROM SUBSCRIPTION S WHERE S.OBJECT_TYPE = :objectType"
				+ " AND S.SUBSCRIBER_ID = :subscriberId AND S.OBJECT_ID IN (:objectIds)", builder.toString());
	}
	
	@Test
	public void testCreateQueryCoreThread() {
		StringBuilder builder = new StringBuilder();
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.THREAD).withSubscriberId("123");
		// call under test
		DBOSubscriptionDAOImpl.createQueryCore(builder, request);
		assertEquals(" FROM SUBSCRIPTION S"
				+ " JOIN DISCUSSION_THREAD T ON (S.OBJECT_ID = T.ID AND T.IS_DELETED = FALSE)"
				+ " WHERE S.OBJECT_TYPE = :objectType AND S.SUBSCRIBER_ID = :subscriberId", builder.toString());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateQueryCoreNullObjectType() {
		StringBuilder builder = new StringBuilder();
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(null).withSubscriberId("123");
		// call under test
		DBOSubscriptionDAOImpl.createQueryCore(builder, request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateQueryCoreNullSubscriberId() {
		StringBuilder builder = new StringBuilder();
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.DATA_ACCESS_SUBMISSION).withSubscriberId(null);
		// call under test
		DBOSubscriptionDAOImpl.createQueryCore(builder, request);
	}
	
	@Test
	public void testcreateQueryNullSortTypeNullDirectionNullLimitNullOffset() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.DATA_ACCESS_SUBMISSION)
				.withSubscriberId("123")
				.withSortByType(null)
				.withSortDirection(null)
				.withLimit(null)
				.withOffset(null);
		// call under test
		String sql = DBOSubscriptionDAOImpl.createQuery(request);
		assertEquals("SELECT * FROM SUBSCRIPTION S "
				+ "WHERE S.OBJECT_TYPE = :objectType AND S.SUBSCRIBER_ID = :subscriberId", sql);
	}
	
	@Test
	public void testcreateQueryNullDirectionNullLimitNullOffset() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.DATA_ACCESS_SUBMISSION)
				.withSubscriberId("123")
				.withSortByType(SortByType.CREATED_ON)
				.withSortDirection(null)
				.withLimit(null)
				.withOffset(null);
		// call under test
		String sql = DBOSubscriptionDAOImpl.createQuery(request);
		assertEquals("SELECT * FROM SUBSCRIPTION S"
				+ " WHERE S.OBJECT_TYPE = :objectType AND S.SUBSCRIBER_ID = :subscriberId"
				+ " ORDER BY S.CREATED_ON", sql);
	}
	
	@Test
	public void testcreateQueryNullLimitNullOffset() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.DATA_ACCESS_SUBMISSION)
				.withSubscriberId("123")
				.withSortByType(SortByType.CREATED_ON)
				.withSortDirection(SortDirection.DESC)
				.withLimit(null)
				.withOffset(null);
		// call under test
		String sql = DBOSubscriptionDAOImpl.createQuery(request);
		assertEquals("SELECT * FROM SUBSCRIPTION S"
				+ " WHERE S.OBJECT_TYPE = :objectType AND S.SUBSCRIBER_ID = :subscriberId"
				+ " ORDER BY S.CREATED_ON DESC", sql);
	}
	
	@Test
	public void testcreateQueryNullSortTypeNullDirectionNullOffset() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.DATA_ACCESS_SUBMISSION)
				.withSubscriberId("123")
				.withSortByType(null)
				.withSortDirection(null)
				.withLimit(101L)
				.withOffset(null);
		// call under test
		String sql = DBOSubscriptionDAOImpl.createQuery(request);
		assertEquals("SELECT * FROM SUBSCRIPTION S"
				+ " WHERE S.OBJECT_TYPE = :objectType AND S.SUBSCRIBER_ID = :subscriberId LIMIT :limit", sql);
	}
	
	@Test
	public void testcreateQueryNullSortTypeNullDirectionNullLimit() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.DATA_ACCESS_SUBMISSION)
				.withSubscriberId("123")
				.withSortByType(null)
				.withSortDirection(null)
				.withLimit(null)
				.withOffset(10L);
		// call under test
		String sql = DBOSubscriptionDAOImpl.createQuery(request);
		assertEquals("SELECT * FROM SUBSCRIPTION S"
				+ " WHERE S.OBJECT_TYPE = :objectType AND S.SUBSCRIBER_ID = :subscriberId", sql);
	}
	
	@Test
	public void testcreateQueryNullSortTypeNullDirection() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.DATA_ACCESS_SUBMISSION)
				.withSubscriberId("123")
				.withSortByType(null)
				.withSortDirection(null)
				.withLimit(101L)
				.withOffset(10L);
		// call under test
		String sql = DBOSubscriptionDAOImpl.createQuery(request);
		assertEquals("SELECT * FROM SUBSCRIPTION S"
				+ " WHERE S.OBJECT_TYPE = :objectType"
				+ " AND S.SUBSCRIBER_ID = :subscriberId LIMIT :limit OFFSET :offset", sql);
	}
	
	@Test
	public void testcreateQuery() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.DATA_ACCESS_SUBMISSION)
				.withSubscriberId("123")
				.withSortByType(SortByType.OBJECT_ID)
				.withSortDirection(SortDirection.ASC)
				.withLimit(101L)
				.withOffset(10L);
		// call under test
		String sql = DBOSubscriptionDAOImpl.createQuery(request);
		assertEquals("SELECT * FROM SUBSCRIPTION S"
				+ " WHERE S.OBJECT_TYPE = :objectType AND S.SUBSCRIBER_ID = :subscriberId"
				+ " ORDER BY S.OBJECT_ID ASC LIMIT :limit OFFSET :offset", sql);
	}
	
	@Test
	public void testCreateCountQuery() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.DATA_ACCESS_SUBMISSION)
				.withSubscriberId("123")
				.withSortByType(SortByType.OBJECT_ID)
				.withSortDirection(SortDirection.ASC)
				.withLimit(101L)
				.withOffset(10L);
		// call under test
		String sql = DBOSubscriptionDAOImpl.createCountQuery(request);
		assertEquals("SELECT COUNT(*) FROM SUBSCRIPTION S"
				+ " WHERE S.OBJECT_TYPE = :objectType AND S.SUBSCRIBER_ID = :subscriberId", sql);
	}
	
	@Test
	public void testCreateParametersAll() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.FORUM)
				.withSubscriberId("123")
				.withObjectIds(Lists.newArrayList("111"))
				.withProjectIds(Sets.newHashSet(222L))
				.withSortByType(SortByType.OBJECT_ID)
				.withSortDirection(SortDirection.ASC)
				.withLimit(101L)
				.withOffset(10L);
		// call under test
		MapSqlParameterSource params = DBOSubscriptionDAOImpl.createParameters(request);
		assertNotNull(params);
		assertEquals(request.getObjectType().name(), params.getValue(OBJECT_TYPE));
		assertEquals(request.getSubscriberId(), params.getValue(SUBSCRIBER_ID));
		assertEquals(request.getObjectIds(), params.getValue(OBJECT_IDS));
		assertEquals(request.getProjectIds(), params.getValue(PROJECT_IDS));
		assertEquals(request.getLimit(), params.getValue(LIMIT));
		assertEquals(request.getOffset(), params.getValue(OFFSET));
	}
	
	@Test
	public void testCreateParametersMinimum() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.FORUM)
				.withSubscriberId("123")
				.withObjectIds(null)
				.withProjectIds(null)
				.withSortByType(null)
				.withSortDirection(null)
				.withLimit(null)
				.withOffset(null);
		// call under test
		MapSqlParameterSource params = DBOSubscriptionDAOImpl.createParameters(request);
		assertNotNull(params);
		assertEquals(request.getObjectType().name(), params.getValue(OBJECT_TYPE));
		assertEquals(request.getSubscriberId(), params.getValue(SUBSCRIBER_ID));
		assertFalse(params.hasValue(OBJECT_IDS));
		assertFalse(params.hasValue(PROJECT_IDS));
		assertFalse(params.hasValue(LIMIT));
		assertFalse(params.hasValue(OFFSET));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateParametersNullId() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.FORUM)
				.withSubscriberId(null);
		// call under test
		DBOSubscriptionDAOImpl.createParameters(request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateParametersNullType() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(null)
				.withSubscriberId("123");
		// call under test
		DBOSubscriptionDAOImpl.createParameters(request);
	}
	
	@Test
	public void testYieldEmptyResultNullProjectsNullObjects() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectIds(null)
				.withProjectIds(null);
		assertFalse(DBOSubscriptionDAOImpl.willYieldEmptyResult(request));
	}
	
	@Test
	public void testYieldEmptyResultEmptyProjectsNullObjects() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectIds(null)
				.withProjectIds(new HashSet<>());
		assertTrue(DBOSubscriptionDAOImpl.willYieldEmptyResult(request));
	}
	
	@Test
	public void testYieldEmptyResultNonEmptyProjectsNullObjects() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectIds(null)
				.withProjectIds(Sets.newHashSet(123L));
		assertFalse(DBOSubscriptionDAOImpl.willYieldEmptyResult(request));
	}
	
	@Test
	public void testYieldEmptyResultNullProjectsEmptyObjects() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectIds(new LinkedList<>())
				.withProjectIds(null);
		assertTrue(DBOSubscriptionDAOImpl.willYieldEmptyResult(request));
	}
	
	@Test
	public void testYieldEmptyResultNullProjectsNonEmptyObjects() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectIds(Lists.newArrayList("123"))
				.withProjectIds(null);
		assertFalse(DBOSubscriptionDAOImpl.willYieldEmptyResult(request));
	}
	
	@Test
	public void testListSubscriptionsNew() {
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.FORUM)
				.withSubscriberId("123")
				.withSortByType(SortByType.CREATED_ON)
				.withSortDirection(SortDirection.DESC);
		// call under test
		List<Subscription> subs = this.subscriptionDao.listSubscriptions(request);
		assertNotNull(subs);
	}
	
	/**
	 * Test that the sorting actual works with both directions.
	 * @throws InterruptedException
	 */
	@Test
	public void testListSubscriptionsSorted() throws InterruptedException {
		Subscription one = subscriptionDao.create(userId, threadId, SubscriptionObjectType.THREAD);
		Subscription two = subscriptionDao.create(userId, threadId2, SubscriptionObjectType.THREAD);

		// ascending
		SubscriptionListRequest request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.THREAD)
				.withSubscriberId(userId)
				.withSortByType(SortByType.SUBSCRIPTION_ID)
				.withSortDirection(SortDirection.ASC);
		// call under test
		List<Subscription> subs = this.subscriptionDao.listSubscriptions(request);
		assertNotNull(subs);
		assertEquals(2, subs.size());
		assertEquals(one, subs.get(0));
		assertEquals(two, subs.get(1));
		
		// Descending
		request = new SubscriptionListRequest()
				.withObjectType(SubscriptionObjectType.THREAD)
				.withSubscriberId(userId)
				.withSortByType(SortByType.SUBSCRIPTION_ID)
				.withSortDirection(SortDirection.DESC);
		// call under test
		subs = this.subscriptionDao.listSubscriptions(request);
		assertNotNull(subs);
		assertEquals(2, subs.size());
		assertEquals(two, subs.get(0));
		assertEquals(one, subs.get(1));
	}

}
