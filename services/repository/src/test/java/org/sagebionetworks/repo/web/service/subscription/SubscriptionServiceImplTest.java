package org.sagebionetworks.repo.web.service.subscription;

import static org.mockito.Mockito.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.subscription.SubscriptionManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.subscription.SortByType;
import org.sagebionetworks.repo.model.subscription.SortDirection;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.SubscriptionRequest;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.springframework.test.util.ReflectionTestUtils;

public class SubscriptionServiceImplTest {
	@Mock
	private UserManager mockUserManager;
	@Mock
	private SubscriptionManager mockSubscriptionManager;
	private SubscriptionService service;

	private Long userId;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);

		service = new SubscriptionServiceImpl();
		ReflectionTestUtils.setField(service, "userManager", mockUserManager);
		ReflectionTestUtils.setField(service, "subscriptionManager", mockSubscriptionManager);

		userId = 1L;
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
