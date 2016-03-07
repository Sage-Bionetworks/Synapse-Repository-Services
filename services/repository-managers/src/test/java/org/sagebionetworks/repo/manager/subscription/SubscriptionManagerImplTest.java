package org.sagebionetworks.repo.manager.subscription;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.repo.manager.AuthorizationManagerImpl.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.subscription.Subscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionPagedResults;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.springframework.test.util.ReflectionTestUtils;

public class SubscriptionManagerImplTest {

	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private SubscriptionDAO mockDao;
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

		objectId = "1";
		topic = new Topic();
		topic.setObjectId(objectId);
		topic.setObjectType(SubscriptionObjectType.FORUM);
		userId = 2L;
		anotherUser = 4L;
		userInfo = new UserInfo(false);
		userInfo.setId(userId);
		sub = new Subscription();
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

	@Test (expected=UnauthorizedException.class)
	public void testCreateAnonymous(){
		when(mockAuthorizationManager
				.canSubscribe(userInfo, objectId, SubscriptionObjectType.FORUM))
				.thenReturn(AuthorizationManagerUtil.accessDenied(ANONYMOUS_ACCESS_DENIED_REASON));
		manager.create(userInfo, topic);
	}

	@Test (expected=UnauthorizedException.class)
	public void testCreateAccessDenied(){
		when(mockAuthorizationManager
				.canSubscribe(userInfo, objectId, SubscriptionObjectType.FORUM))
				.thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.create(userInfo, topic);
	}

	@Test
	public void testCreateAuthorized(){
		when(mockAuthorizationManager
				.canSubscribe(userInfo, objectId, SubscriptionObjectType.FORUM))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockDao
				.create(userId.toString(), objectId, SubscriptionObjectType.FORUM))
				.thenReturn(sub);
		assertEquals(sub, manager.create(userInfo, topic));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetListInvalidUserInfo() {
		manager.getList(null, SubscriptionObjectType.FORUM, new ArrayList<Long>(0));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetListInvalidObjectType() {
		manager.getList(userInfo, null, new ArrayList<Long>(0));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetListInvalidTopics() {
		manager.getList(userInfo, SubscriptionObjectType.FORUM, null);
	}

	@Test
	public void testGetList() {
		List<Long> ids = new ArrayList<Long>(1);
		ids.add(Long.parseLong(objectId));
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		when(mockDao.getSubscriptionList(userId.toString(), SubscriptionObjectType.FORUM, ids)).thenReturn(results);
		assertEquals(results, manager.getList(userInfo, SubscriptionObjectType.FORUM, ids));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetAllInvalidUserInfo() {
		manager.getAll(null, 10L, 0L, null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetListInvalidLimit() {
		manager.getAll(userInfo, null, 0L, null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetListInvalidOffset() {
		manager.getAll(userInfo, 10L, null, null);
	}

	@Test
	public void testGetAll() {
		SubscriptionPagedResults results = new SubscriptionPagedResults();
		when(mockDao.getAll(userId.toString(), 10L, 0L, null)).thenReturn(results);
		assertEquals(results, manager.getAll(userInfo, 10L, 0L, null));
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
	public void testDeleteAuthorized() {
		Long subscriptionId = 3L;
		sub.setSubscriberId(userId.toString());
		when(mockDao.get(subscriptionId)).thenReturn(sub);
		manager.delete(userInfo, subscriptionId.toString());
		verify(mockDao).delete(subscriptionId);
	}
}
