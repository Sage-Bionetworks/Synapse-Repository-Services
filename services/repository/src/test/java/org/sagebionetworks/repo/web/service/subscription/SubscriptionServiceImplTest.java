package org.sagebionetworks.repo.web.service.subscription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.subscription.SubscriptionManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.subscription.SortByType;
import org.sagebionetworks.repo.model.subscription.SortDirection;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionServiceImplTest {
	@Mock
	private UserManager mockUserManager;
	@Mock
	private SubscriptionManager mockSubscriptionManager;

	@InjectMocks
	private SubscriptionServiceImpl service;

	private Long userId;
	private UserInfo userInfo;

	@Before
	public void before() {
		userId = 1L;
		userInfo = new UserInfo(false, userId);
		when(mockUserManager.getUserInfo(userId)).thenReturn(userInfo);
	}

	@Test
	public void testCreate() {
		Topic topic = new Topic();
		service.create(userId, topic);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockSubscriptionManager).create(any(UserInfo.class), any(Topic.class));
	}

	@Test
	public void testGetList() {
		SubscriptionRequest request = new SubscriptionRequest();
		request.setObjectType(SubscriptionObjectType.FORUM);
		request.setIdList(new ArrayList<String>(0));
		service.getList(userId, request);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockSubscriptionManager).getList(any(UserInfo.class), eq(request));
	}

	@Test
	public void testGetAll(){
		Long limit = 10L;
		Long offset = 0L;
		SubscriptionObjectType objectType = SubscriptionObjectType.FORUM;
		SortByType sortByType = SortByType.CREATED_ON;
		SortDirection sortDirection = SortDirection.ASC;
		service.getAll(userId, limit, offset, objectType, sortByType, sortDirection);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockSubscriptionManager).getAll(any(UserInfo.class), eq(limit), eq(offset), eq(objectType), eq(sortByType), eq(sortDirection));
	}

	@Test
	public void testDelete() {
		String subscriptionId = "2";
		service.delete(userId, subscriptionId);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockSubscriptionManager).delete(any(UserInfo.class), eq(subscriptionId));
	}

	@Test
	public void testDeleteAll() {
		service.deleteAll(userId);
		verify(mockUserManager).getUserInfo(userId);
		verify(mockSubscriptionManager).deleteAll(any(UserInfo.class));
	}
}
