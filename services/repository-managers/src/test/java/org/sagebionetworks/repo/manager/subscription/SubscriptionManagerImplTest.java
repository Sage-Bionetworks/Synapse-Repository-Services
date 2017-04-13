package org.sagebionetworks.repo.manager.subscription;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.subscription.SubscriberCount;
import org.sagebionetworks.repo.model.subscription.SubscriberPagedResults;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

public class SubscriptionManagerImplTest {

	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private DBOChangeDAO mockChangeDao;
	@Mock
	private SubscriptionDAO mockDao;
	@Mock
	private AccessControlListDAO mockAclDao;
	private SubscriptionManagerImpl manager;
	private Topic topic;
	private String objectId;
	private UserInfo userInfo;
	private Long userId;
	private Long anotherUser;
	private Subscription sub;

	@Before
	public void before() {

		MockitoAnnotations.initMocks(this);
		manager = new SubscriptionManagerImpl();
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(manager, "subscriptionDao", mockDao);
		ReflectionTestUtils.setField(manager, "changeDao", mockChangeDao);
		ReflectionTestUtils.setField(manager, "aclDao", mockAclDao);

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
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockDao
				.create(userId.toString(), objectId, SubscriptionObjectType.FORUM))
				.thenReturn(sub);
		assertEquals(sub, manager.create(userInfo, topic));
	}

	@Test
	public void testCreateThreadSubscription(){
		when(mockAuthorizationManager
				.canSubscribe(userInfo, objectId, SubscriptionObjectType.THREAD))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
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

	@Test (expected=IllegalArgumentException.class)
	public void testGetListInvalidTopicForDataAccessSubmission() {
		SubscriptionRequest request = new SubscriptionRequest();
		request.setObjectType(SubscriptionObjectType.DATA_ACCESS_SUBMISSION);
		List<String> ids = new ArrayList<String>(1);
		ids.add(objectId);
		request.setIdList(ids);
		manager.getList(userInfo, request);
	}

	@Test
	public void testGetListForThread() {
		SubscriptionRequest request = new SubscriptionRequest();
		request.setObjectType(SubscriptionObjectType.THREAD);
		List<String> ids = new ArrayList<String>(1);
		ids.add(objectId);
		request.setIdList(ids);
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		when(mockDao.listSubscriptionForThread(userId.toString(), ids)).thenReturn(results);
		assertEquals(results, manager.getList(userInfo, request));
	}

	@Test
	public void testGetListForForum() {
		SubscriptionRequest request = new SubscriptionRequest();
		request.setObjectType(SubscriptionObjectType.FORUM);
		List<String> ids = new ArrayList<String>(1);
		ids.add(objectId);
		request.setIdList(ids);
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		when(mockDao.listSubscriptionForForum(userId.toString(), ids)).thenReturn(results);
		assertEquals(results, manager.getList(userInfo, request));
	}

	@Test
	public void testGetListForDataAccessSubmissionStatus() {
		SubscriptionRequest request = new SubscriptionRequest();
		request.setObjectType(SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS);
		List<String> ids = new ArrayList<String>(1);
		ids.add(objectId);
		request.setIdList(ids);
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		when(mockDao.listSubscriptions(userId.toString(), SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS, ids)).thenReturn(results);
		assertEquals(results, manager.getList(userInfo, request));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetAllInvalidUserInfo() {
		manager.getAll(null, 10L, 0L, null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetAllInvalidLimit() {
		manager.getAll(userInfo, null, 0L, null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetAllInvalidOffset() {
		manager.getAll(userInfo, 10L, null, null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetAllInvalidObjectType() {
		manager.getAll(userInfo, 10L, 0L, null);
	}

	@Test
	public void testGetAllThreadSubscriptions() {
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		Set<Long> projectIds = new HashSet<Long>();
		when(mockDao.getAllThreadSubscriptions(userId.toString(), 10L, 0L, projectIds )).thenReturn(results);
		when(mockDao.getAllProjectsUserHasThreadSubs(userId.toString())).thenReturn(projectIds);
		when(mockAclDao.getAccessibleBenefactors(userInfo.getGroups(), projectIds, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(projectIds);
		assertEquals(results, manager.getAll(userInfo, 10L, 0L, SubscriptionObjectType.THREAD));
	}

	@Test
	public void testGetAllForForum() {
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		Set<Long> projectIds = new HashSet<Long>();
		when(mockDao.getAllForumSubscriptions(userId.toString(), 10L, 0L, projectIds )).thenReturn(results);
		when(mockDao.getAllProjectsUserHasForumSubs(userId.toString())).thenReturn(projectIds);
		when(mockAclDao.getAccessibleBenefactors(userInfo.getGroups(), projectIds, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(projectIds);
		assertEquals(results, manager.getAll(userInfo, 10L, 0L, SubscriptionObjectType.FORUM));
	}

	@Test
	public void testGetAllSubscriptionForDataAccessSubmission() {
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		when(mockDao.getAllSubscriptions(userId.toString(), 10L, 0L, SubscriptionObjectType.DATA_ACCESS_SUBMISSION)).thenReturn(results);
		assertEquals(results, manager.getAll(userInfo, 10L, 0L, SubscriptionObjectType.DATA_ACCESS_SUBMISSION));
		verifyZeroInteractions(mockAclDao);
	}

	@Test
	public void testGetAllForDataAccessSubmissionStatus() {
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		when(mockDao.getAllSubscriptions(userId.toString(), 10L, 0L, SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS)).thenReturn(results);
		assertEquals(results, manager.getAll(userInfo, 10L, 0L, SubscriptionObjectType.DATA_ACCESS_SUBMISSION_STATUS));
		verifyZeroInteractions(mockAclDao);
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
	public void testGetEtagInvalidObjectId() {
		manager.getEtag(null, ObjectType.FORUM);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetEtagInvalidObjectType() {
		manager.getEtag(objectId, null);
	}

	@Test (expected=NumberFormatException.class)
	public void testGetEtagWithObjectIdNaN(){
		manager.getEtag("syn"+objectId, ObjectType.FORUM).getEtag();
	}

	@Test
	public void testGetEtagForEntity(){
		String etag = "etag";
		when(mockChangeDao
				.getEtag(KeyFactory.stringToKey(objectId), ObjectType.ENTITY))
				.thenReturn(etag);
		assertEquals(etag, manager.getEtag("syn"+objectId, ObjectType.ENTITY).getEtag());
	}

	@Test
	public void testGetEtagForNonEntity(){
		String etag = "etag";
		when(mockChangeDao
				.getEtag(Long.parseLong(objectId), ObjectType.FORUM))
				.thenReturn(etag);
		assertEquals(etag, manager.getEtag(objectId, ObjectType.FORUM).getEtag());
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
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.getSubscribers(userInfo, topic, new NextPageToken(1, 0).toToken());
	}

	@Test
	public void testGetSubscribersWithNullNextPageToken(){
		when(mockAuthorizationManager
				.canSubscribe(userInfo, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
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
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
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
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.getSubscriberCount(userInfo, topic);
	}

	@Test
	public void testGetSubscriberCountAuthorized(){
		when(mockAuthorizationManager
				.canSubscribe(userInfo, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
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
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.subscribeAll(userInfo, SubscriptionObjectType.THREAD);
	}

	@Test
	public void testSubscribeAllAuthorized() {
		when(mockAuthorizationManager
				.canSubscribe(userInfo, SubscriptionManagerImpl.ALL_OBJECT_IDS, SubscriptionObjectType.THREAD))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockDao
				.create(userId.toString(), SubscriptionManagerImpl.ALL_OBJECT_IDS, SubscriptionObjectType.THREAD))
				.thenReturn(sub);
		assertEquals(sub, manager.subscribeAll(userInfo, SubscriptionObjectType.THREAD));
		verify(mockAuthorizationManager).canSubscribe(userInfo, SubscriptionManagerImpl.ALL_OBJECT_IDS, SubscriptionObjectType.THREAD);
		verify(mockDao).create(userId.toString(), SubscriptionManagerImpl.ALL_OBJECT_IDS, SubscriptionObjectType.THREAD);
	}
}
