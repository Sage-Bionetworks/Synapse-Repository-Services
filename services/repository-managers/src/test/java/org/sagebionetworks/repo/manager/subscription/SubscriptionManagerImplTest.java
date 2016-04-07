package org.sagebionetworks.repo.manager.subscription;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.springframework.test.util.ReflectionTestUtils;

public class SubscriptionManagerImplTest {

	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private DiscussionThreadDAO mockThreadDao;
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
		ReflectionTestUtils.setField(manager, "threadDao", mockThreadDao);
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
		List<String> threadIdList = Arrays.asList("1");
		when(mockThreadDao.getAllThreadIdForForum(objectId)).thenReturn(threadIdList);
		assertEquals(sub, manager.create(userInfo, topic));
		verify(mockAuthorizationManager).canSubscribe(userInfo, objectId, SubscriptionObjectType.FORUM);
		verify(mockThreadDao).getAllThreadIdForForum(objectId);
		verify(mockDao).subscribeAll(userId.toString(), threadIdList, SubscriptionObjectType.THREAD);
	}

	@SuppressWarnings("unchecked")
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
		verify(mockThreadDao, never()).getAllThreadIdForForum(anyString());
		verify(mockDao, never()).subscribeAll(anyString(), any(List.class), any(SubscriptionObjectType.class));
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
	public void testGetListInvalidTopics() {
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
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		when(mockDao.getSubscriptionList(userId.toString(), SubscriptionObjectType.FORUM, ids)).thenReturn(results);
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
	public void testGetAll() {
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		Set<Long> projectIds = new HashSet<Long>();
		when(mockDao.getAll(userId.toString(), 10L, 0L, SubscriptionObjectType.DISCUSSION_THREAD, projectIds )).thenReturn(results);
		when(mockDao.getAllProjects(userId.toString(), SubscriptionObjectType.DISCUSSION_THREAD)).thenReturn(projectIds);
		when(mockAclDao.getAccessibleBenefactors(userInfo.getGroups(), projectIds, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(projectIds);
		assertEquals(results, manager.getAll(userInfo, 10L, 0L, SubscriptionObjectType.DISCUSSION_THREAD));
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

	@SuppressWarnings("unchecked")
	@Test
	public void testDeleteThreadSubscription() {
		Long subscriptionId = 3L;
		sub.setSubscriberId(userId.toString());
		sub.setObjectType(SubscriptionObjectType.THREAD);
		when(mockDao.get(subscriptionId)).thenReturn(sub);
		manager.delete(userInfo, subscriptionId.toString());
		verify(mockDao).delete(subscriptionId);
		verify(mockThreadDao, never()).getAllThreadIdForForum(anyString());
		verify(mockDao, never()).deleteList(anyString(), any(List.class), any(SubscriptionObjectType.class));
	}

	@Test
	public void testDeleteForumSubscription() {
		Long subscriptionId = 3L;
		sub.setSubscriberId(userId.toString());
		sub.setObjectType(SubscriptionObjectType.FORUM);
		when(mockDao.get(subscriptionId)).thenReturn(sub);
		List<String> threadIdList = Arrays.asList("1");
		when(mockThreadDao.getAllThreadIdForForum(objectId)).thenReturn(threadIdList);
		manager.delete(userInfo, subscriptionId.toString());
		verify(mockDao).delete(subscriptionId);
		verify(mockThreadDao).getAllThreadIdForForum(anyString());
		verify(mockDao).deleteList(userId.toString(), threadIdList, SubscriptionObjectType.THREAD);
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
}
