package org.sagebionetworks.repo.manager.subscription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySetOf;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionListRequest;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.subscription.SortByType;
import org.sagebionetworks.repo.model.subscription.SortDirection;
import org.sagebionetworks.repo.model.subscription.SubscriberCount;
import org.sagebionetworks.repo.model.subscription.SubscriberPagedResults;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionManagerImplTest {

	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private DBOChangeDAO mockChangeDao;
	@Mock
	private SubscriptionDAO mockDao;
	@Mock
	private AccessControlListDAO mockAclDao;
	@Captor
	ArgumentCaptor<SubscriptionListRequest> subscriptionRequestCapture;
	@InjectMocks
	private SubscriptionManagerImpl manager;
	private Topic topic;
	private String objectId;
	private UserInfo userInfo;
	private Long userId;
	private Long anotherUser;
	private Subscription sub;
	private Set<Long> projectIds;
	private List<Subscription> subscriptions;
	private Long totalCount;

	@Before
	public void before() {
		objectId = "1";
		topic = new Topic();
		topic.setObjectId(objectId);
		topic.setObjectType(SubscriptionObjectType.FORUM);
		userId = 2L;
		anotherUser = 4L;
		userInfo = new UserInfo(false);
		userInfo.setId(userId);
		Set<Long> groups = new HashSet<Long>();
		groups.add(userId);
		userInfo.setGroups(groups);
		sub = new Subscription();
		sub.setObjectId(objectId);
		
		// default filter is a simple pass through
		doAnswer(new Answer<Set<Long>>() {
			@Override
			public Set<Long> answer(InvocationOnMock invocation) throws Throwable {
				// return the benefactors unmodified.
				return (Set<Long>) invocation.getArguments()[1];
			}
		}).when(mockAclDao).getAccessibleBenefactors(anySetOf(Long.class), anySetOf(Long.class), any(ObjectType.class),
				any(ACCESS_TYPE.class));
		
		projectIds = Sets.newHashSet(123L,456L);
		when(mockDao.getAllProjectsUserHasThreadSubs(any(String.class))).thenReturn(projectIds);
		when(mockDao.getAllProjectsUserHasForumSubs(any(String.class))).thenReturn(projectIds);
		
		Subscription sub = new Subscription();
		sub.setObjectId("123");
		subscriptions = Lists.newArrayList(sub);
		when(mockDao.listSubscriptions(any(SubscriptionListRequest.class))).thenReturn(subscriptions);
		totalCount = 101L;
		when(mockDao.listSubscriptionsCount(any(SubscriptionListRequest.class))).thenReturn(totalCount);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateInvalidUserInfo() {
		manager.create(null, topic);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateInvalidTopic() {
		manager.create(userInfo, null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateInvalidObjectId() {
		topic.setObjectId(null);
		manager.create(userInfo, topic);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateInvalidObjectType() {
		topic.setObjectType(null);
		manager.create(userInfo, topic);
	}

	@Test
	public void testCreateForumSubscription(){
		when(mockAuthorizationManager
				.canSubscribe(userInfo, objectId, SubscriptionObjectType.FORUM))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockDao
				.create(userId.toString(), objectId, SubscriptionObjectType.FORUM))
				.thenReturn(sub);
		assertEquals(sub, manager.create(userInfo, topic));
	}

	@Test
	public void testCreateThreadSubscription(){
		when(mockAuthorizationManager
				.canSubscribe(userInfo, objectId, SubscriptionObjectType.THREAD))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockDao
				.create(userId.toString(), objectId, SubscriptionObjectType.THREAD))
				.thenReturn(sub);
		topic.setObjectType(SubscriptionObjectType.THREAD);
		assertEquals(sub, manager.create(userInfo, topic));
		verify(mockAuthorizationManager).canSubscribe(userInfo, objectId, SubscriptionObjectType.THREAD);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetInvalidUserInfo() {
		manager.get(null, "1");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetInvalidSubscriptionId() {
		manager.get(userInfo, null);
	}

	@Test (expected=UnauthorizedException.class)
	public void testGetUnauthorized() {
		Long subscriptionId = 3L;
		sub.setSubscriberId(anotherUser.toString());
		when(mockDao.get(subscriptionId)).thenReturn(sub);
		manager.get(userInfo, subscriptionId.toString());
	}

	@Test
	public void testGetAuthorized() {
		Long subscriptionId = 3L;
		sub.setSubscriberId(userId.toString());
		when(mockDao.get(subscriptionId)).thenReturn(sub);
		assertEquals(sub, manager.get(userInfo, subscriptionId.toString()));
	}

	@Test
	public void testGetAllProjectsUserHasSubscriptionsOther() {
		SubscriptionObjectType objectType = SubscriptionObjectType.DATA_ACCESS_SUBMISSION;
		// call under test
		Set<Long> projects = manager.getAllProjectsUserHasSubscriptions(userInfo, objectType);
		assertEquals(null, projects);
		verifyNoMoreInteractions(mockDao);
		verifyNoMoreInteractions(mockAclDao);
	}
	
	@Test
	public void testGetAllProjectsUserHasSubscriptionsForum() {
		SubscriptionObjectType objectType = SubscriptionObjectType.FORUM;
		// call under test
		Set<Long> projects = manager.getAllProjectsUserHasSubscriptions(userInfo, objectType);
		assertEquals(projectIds, projects);
		verify(mockDao).getAllProjectsUserHasForumSubs(userInfo.getId().toString());
		verify(mockAclDao).getAccessibleBenefactors(userInfo.getGroups(), projectIds, ObjectType.ENTITY, ACCESS_TYPE.READ);
	}
	
	@Test
	public void testGetAllProjectsUserHasSubscriptionsThread() {
		SubscriptionObjectType objectType = SubscriptionObjectType.THREAD;
		// call under test
		Set<Long> projects = manager.getAllProjectsUserHasSubscriptions(userInfo, objectType);
		assertEquals(projectIds, projects);
		verify(mockDao).getAllProjectsUserHasThreadSubs(userInfo.getId().toString());
		verify(mockAclDao).getAccessibleBenefactors(userInfo.getGroups(), projectIds, ObjectType.ENTITY, ACCESS_TYPE.READ);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetListInvalidUserInfo() {
		SubscriptionRequest request = new SubscriptionRequest();
		request.setObjectType(SubscriptionObjectType.FORUM);
		request.setIdList(new ArrayList<String>(0));
		manager.getList(null, request);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetListInvalidObjectType() {
		SubscriptionRequest request = new SubscriptionRequest();
		request.setObjectType(null);
		request.setIdList(new ArrayList<String>(0));
		manager.getList(userInfo, request);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetListInvalidTopicsWithNullIdList() {
		SubscriptionRequest request = new SubscriptionRequest();
		request.setObjectType(SubscriptionObjectType.FORUM);
		request.setIdList(null);
		manager.getList(userInfo, request);
	}

	@Test
	public void testGetList() {
		SubscriptionRequest request = new SubscriptionRequest();
		request.setObjectType(SubscriptionObjectType.FORUM);
		List<String> ids = new ArrayList<String>(1);
		ids.add(objectId);
		request.setIdList(ids);
		request.setSortByType(SortByType.CREATED_ON);
		request.setSortDirection(SortDirection.DESC);
		// call under test
		SubscriptionPagedResults results = manager.getList(userInfo, request);
		assertNotNull(results);
		assertEquals(subscriptions, results.getResults());
		// size of the results are used for the total in this case.
		assertEquals(new Long(subscriptions.size()), results.getTotalNumberOfResults());
		// validate the passed parameters
		verify(mockDao).listSubscriptions(subscriptionRequestCapture.capture());
		SubscriptionListRequest passedRequest = subscriptionRequestCapture.getValue();
		assertEquals(userInfo.getId().toString(), passedRequest.getSubscriberId());
		assertEquals(ids, passedRequest.getObjectIds());
		assertEquals(SubscriptionObjectType.FORUM, passedRequest.getObjectType());
		assertEquals(SortByType.CREATED_ON, passedRequest.getSortByType());
		assertEquals(SortDirection.DESC, passedRequest.getSortDirection());
		// count is not used in this case
		verify(mockDao, never()).listSubscriptionsCount(any(SubscriptionListRequest.class));
	}
	
	@Test
	public void testGetAll() {
		Long limit = 9L;
		Long offset = 8L;
		SubscriptionObjectType objectType = SubscriptionObjectType.FORUM;
		SortByType sortByType = SortByType.SUBSCRIBER_ID;
		SortDirection sortDirection = SortDirection.ASC;
		// call under test
		SubscriptionPagedResults results = manager.getAll(userInfo, limit, offset, objectType, sortByType, sortDirection);
		assertNotNull(results);
		assertEquals(subscriptions, results.getResults());
		assertEquals(totalCount, results.getTotalNumberOfResults());
		
		// validate the passed parameters
		verify(mockDao).listSubscriptions(subscriptionRequestCapture.capture());
		SubscriptionListRequest passedRequest = subscriptionRequestCapture.getValue();
		assertEquals(userInfo.getId().toString(), passedRequest.getSubscriberId());
		assertEquals(projectIds, passedRequest.getProjectIds());
		assertEquals(objectType, passedRequest.getObjectType());
		assertEquals(sortByType, passedRequest.getSortByType());
		assertEquals(sortDirection, passedRequest.getSortDirection());
		assertEquals(limit, passedRequest.getLimit());
		assertEquals(offset, passedRequest.getOffset());
		// count should be called
		verify(mockDao).listSubscriptionsCount(passedRequest);
		verify(mockDao).getAllProjectsUserHasForumSubs(userInfo.getId().toString());
		verify(mockAclDao).getAccessibleBenefactors(userInfo.getGroups(), projectIds, ObjectType.ENTITY, ACCESS_TYPE.READ);
	}
	
	@Test
	public void testGetAllNullOptional() {
		Long limit = 9L;
		Long offset = 8L;
		SubscriptionObjectType objectType = SubscriptionObjectType.FORUM;
		// sort is optional
		SortByType sortByType = null;
		SortDirection sortDirection = null;
		// call under test
		SubscriptionPagedResults results = manager.getAll(userInfo, limit, offset, objectType, sortByType, sortDirection);
		assertNotNull(results);
		assertEquals(subscriptions, results.getResults());
		assertEquals(totalCount, results.getTotalNumberOfResults());
		
		// validate the passed parameters
		verify(mockDao).listSubscriptions(subscriptionRequestCapture.capture());
		SubscriptionListRequest passedRequest = subscriptionRequestCapture.getValue();
		assertEquals(userInfo.getId().toString(), passedRequest.getSubscriberId());
		assertEquals(projectIds, passedRequest.getProjectIds());
		assertEquals(objectType, passedRequest.getObjectType());
		assertEquals(null, passedRequest.getSortByType());
		assertEquals(null, passedRequest.getSortDirection());
		assertEquals(limit, passedRequest.getLimit());
		assertEquals(offset, passedRequest.getOffset());
		// count should be called
		verify(mockDao).listSubscriptionsCount(passedRequest);
		verify(mockDao).getAllProjectsUserHasForumSubs(userInfo.getId().toString());
		verify(mockAclDao).getAccessibleBenefactors(userInfo.getGroups(), projectIds, ObjectType.ENTITY, ACCESS_TYPE.READ);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetAllNullLimit() {
		Long limit = null;
		Long offset = 8L;
		SubscriptionObjectType objectType = SubscriptionObjectType.FORUM;
		SortByType sortByType = SortByType.SUBSCRIBER_ID;
		SortDirection sortDirection = SortDirection.ASC;
		// call under test
		 manager.getAll(userInfo, limit, offset, objectType, sortByType, sortDirection);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetAllNullOffset() {
		Long limit = 9L;
		Long offset = null;
		SubscriptionObjectType objectType = SubscriptionObjectType.FORUM;
		SortByType sortByType = SortByType.SUBSCRIBER_ID;
		SortDirection sortDirection = SortDirection.ASC;
		// call under test
		 manager.getAll(userInfo, limit, offset, objectType, sortByType, sortDirection);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetAllNullType() {
		Long limit = 9L;
		Long offset = 8L;
		SubscriptionObjectType objectType = null;
		SortByType sortByType = SortByType.SUBSCRIBER_ID;
		SortDirection sortDirection = SortDirection.ASC;
		// call under test
		 manager.getAll(userInfo, limit, offset, objectType, sortByType, sortDirection);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetAllNullUser() {
		Long limit = 9L;
		Long offset = 8L;
		SubscriptionObjectType objectType = SubscriptionObjectType.FORUM;
		SortByType sortByType = SortByType.SUBSCRIBER_ID;
		SortDirection sortDirection = SortDirection.ASC;
		userInfo = null;
		// call under test
		 manager.getAll(userInfo, limit, offset, objectType, sortByType, sortDirection);
	}
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteInvalidUserInfo() {
		manager.delete(null, "1");
	}

	@Test (expected=IllegalArgumentException.class)
	public void testDeleteInvalidSubscriptionId() {
		manager.delete(userInfo, null);
	}

	@Test (expected=UnauthorizedException.class)
	public void testDeleteUnauthorized() {
		Long subscriptionId = 3L;
		sub.setSubscriberId(anotherUser.toString());
		when(mockDao.get(subscriptionId)).thenReturn(sub);
		manager.delete(userInfo, subscriptionId.toString());
	}

	@Test
	public void testDeleteNonExisting() {
		Long subscriptionId = 3L;
		sub.setSubscriberId(anotherUser.toString());
		when(mockDao.get(subscriptionId)).thenThrow(new NotFoundException());
		manager.delete(userInfo, subscriptionId.toString());
		verify(mockDao, never()).delete(subscriptionId);
	}

	@Test
	public void testDeleteThreadSubscription() {
		Long subscriptionId = 3L;
		sub.setSubscriberId(userId.toString());
		sub.setObjectType(SubscriptionObjectType.THREAD);
		when(mockDao.get(subscriptionId)).thenReturn(sub);
		manager.delete(userInfo, subscriptionId.toString());
		verify(mockDao).delete(subscriptionId);
	}

	@Test
	public void testDeleteForumSubscription() {
		Long subscriptionId = 3L;
		sub.setSubscriberId(userId.toString());
		sub.setObjectType(SubscriptionObjectType.FORUM);
		when(mockDao.get(subscriptionId)).thenReturn(sub);
		manager.delete(userInfo, subscriptionId.toString());
		verify(mockDao).delete(subscriptionId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testDeleteAllInvalidUserInfo() {
		manager.deleteAll(null);
	}

	@Test
	public void testDeleteAll() {
		manager.deleteAll(userInfo);
		verify(mockDao).deleteAll(userInfo.getId());
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetSubscribersWithNullUserInfo(){
		manager.getSubscribers(null, topic, new NextPageToken(1, 0).toToken());
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetSubscribersWithNullTopic(){
		manager.getSubscribers(userInfo, null, new NextPageToken(1, 0).toToken());
	}

	@Test (expected=UnauthorizedException.class)
	public void testGetSubscribersUnauthorized(){
		when(mockAuthorizationManager
				.canSubscribe(userInfo, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		manager.getSubscribers(userInfo, topic, new NextPageToken(1, 0).toToken());
	}

	@Test
	public void testGetSubscribersWithNullNextPageToken(){
		when(mockAuthorizationManager
				.canSubscribe(userInfo, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.authorized());
		List<String> subscribers = new LinkedList<String>();
		when(mockDao.getSubscribers(topic.getObjectId(), topic.getObjectType(),
				NextPageToken.DEFAULT_LIMIT+1, NextPageToken.DEFAULT_OFFSET))
				.thenReturn(subscribers);
		SubscriberPagedResults results = manager.getSubscribers(userInfo, topic, null);
		assertNotNull(results);
		assertEquals(subscribers, results.getSubscribers());
		assertNull(results.getNextPageToken());
		verify(mockDao).getSubscribers(topic.getObjectId(), topic.getObjectType(),
				NextPageToken.DEFAULT_LIMIT+1, NextPageToken.DEFAULT_OFFSET);
	}

	@Test
	public void testGetSubscribersWithNextPageToken(){
		when(mockAuthorizationManager
				.canSubscribe(userInfo, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.authorized());
		List<String> subscribers = new ArrayList<String>();
		subscribers.addAll(Arrays.asList("1", "2"));
		when(mockDao.getSubscribers(topic.getObjectId(), topic.getObjectType(), 2, 0))
				.thenReturn(subscribers);
		SubscriberPagedResults results = manager.getSubscribers(userInfo, topic, new NextPageToken(1, 0).toToken());
		assertNotNull(results);
		assertEquals(subscribers, results.getSubscribers());
		assertEquals(new NextPageToken(1, 1).toToken(), results.getNextPageToken());
		verify(mockDao).getSubscribers(topic.getObjectId(), topic.getObjectType(), 2, 0);
	}


	@Test (expected=IllegalArgumentException.class)
	public void testGetSubscriberCountWithNullUserInfo(){
		manager.getSubscriberCount(null, topic);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetSubscriberCountWithNullTopic(){
		manager.getSubscriberCount(userInfo, null);
	}

	@Test (expected=UnauthorizedException.class)
	public void testGetSubscriberCountUnauthorized(){
		when(mockAuthorizationManager
				.canSubscribe(userInfo, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		manager.getSubscriberCount(userInfo, topic);
	}

	@Test
	public void testGetSubscriberCountAuthorized(){
		when(mockAuthorizationManager
				.canSubscribe(userInfo, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockDao.getSubscriberCount(topic.getObjectId(), topic.getObjectType()))
				.thenReturn(10L);
		SubscriberCount count = manager.getSubscriberCount(userInfo, topic);
		assertNotNull(count);
		assertEquals((Long) 10L, count.getCount());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testSubscribeAllWithNullUserInfo() {
		manager.subscribeAll(null, SubscriptionObjectType.THREAD);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testSubscribeAllWithNullSubscriptionObjectType() {
		manager.subscribeAll(userInfo, null);
	}

	@Test (expected = UnauthorizedException.class)
	public void testSubscribeAllUnauthorized() {
		when(mockAuthorizationManager
				.canSubscribe(userInfo, SubscriptionManagerImpl.ALL_OBJECT_IDS, SubscriptionObjectType.THREAD))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		manager.subscribeAll(userInfo, SubscriptionObjectType.THREAD);
	}

	@Test
	public void testSubscribeAllAuthorized() {
		when(mockAuthorizationManager
				.canSubscribe(userInfo, SubscriptionManagerImpl.ALL_OBJECT_IDS, SubscriptionObjectType.THREAD))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockDao
				.create(userId.toString(), SubscriptionManagerImpl.ALL_OBJECT_IDS, SubscriptionObjectType.THREAD))
				.thenReturn(sub);
		assertEquals(sub, manager.subscribeAll(userInfo, SubscriptionObjectType.THREAD));
		verify(mockAuthorizationManager).canSubscribe(userInfo, SubscriptionManagerImpl.ALL_OBJECT_IDS, SubscriptionObjectType.THREAD);
		verify(mockDao).create(userId.toString(), SubscriptionManagerImpl.ALL_OBJECT_IDS, SubscriptionObjectType.THREAD);
	}
}
