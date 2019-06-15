package org.sagebionetworks.repo.manager.message;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.markdown.MarkdownClientException;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.principal.SynapseEmailService;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.broadcast.UserNotificationInfo;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.BroadcastMessageDao;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.util.TimeoutUtils;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.google.common.collect.Lists;

public class BroadcastMessageManagerImplTest {
	
	Map<ObjectType, BroadcastMessageBuilder> builderMap;

	@Mock
	SubscriptionDAO mockSubscriptionDAO;
	@Mock
	BroadcastMessageDao mockBroadcastMessageDao;
	@Mock
	DBOChangeDAO mockChangeDao;
	@Mock
	SynapseEmailService mockSesClient;
	@Mock
	TimeoutUtils mockTimeoutUtils;
	@Mock
	DiscussionBroadcastMessageBuilder mockBroadcastMessageBuilder;
	@Mock
	MessageBuilderFactory mockFactory;
	@Mock
	UserInfo mockUser;
	@Mock
	ProgressCallback mockCallback;
	@Mock
	PrincipalAliasDAO mockPrincipalAliasDao;
	@Mock
	UserProfileDAO mockUserProfileDao;
	@Mock
	UserManager mockUserManager;
	@Mock
	AuthorizationManager mockAuthManager;

	@InjectMocks
	BroadcastMessageManagerImpl manager;
	ChangeMessage change;
	List<Subscriber> subscribers;
	Topic topic;

	@Before
	public void before() throws Exception{
		MockitoAnnotations.initMocks(this);

		Map<ObjectType, MessageBuilderFactory> factoryMap = new HashMap<ObjectType, MessageBuilderFactory>();
		factoryMap.put(ObjectType.REPLY, mockFactory);
		manager.setMessageBuilderFactoryMap(factoryMap);
		
		// default setup
		change = new ChangeMessage();
		change.setChangeNumber(123L);
		change.setChangeType(ChangeType.CREATE);
		change.setObjectId("456");
		change.setObjectType(ObjectType.REPLY);
		change.setTimestamp(new Date(123000L));
		change.setUserId(789L);
		
		topic = new Topic();
		topic.setObjectId("5555");
		topic.setObjectType(SubscriptionObjectType.THREAD);

		
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		when(mockBroadcastMessageDao.wasBroadcast(change.getChangeNumber())).thenReturn(false);
		when(mockChangeDao.doesChangeNumberExist(change.getChangeNumber())).thenReturn(true);
		
		when(mockFactory.createMessageBuilder(change.getObjectId(), change.getChangeType(), change.getUserId())).thenReturn(mockBroadcastMessageBuilder);

		when(mockBroadcastMessageBuilder.getBroadcastTopic()).thenReturn(topic);
		
		
		Subscriber sub1 = new Subscriber();
		sub1.setSubscriptionId("1");
		sub1.setSubscriberId("1");
		Subscriber sub2 = new Subscriber();
		sub2.setSubscriptionId("2");
		sub2.setSubscriberId("2");
		subscribers = Lists.newArrayList(sub1, sub2);
		
		when(mockSubscriptionDAO.getAllEmailSubscribers(topic.getObjectId(), topic.getObjectType())).thenReturn(subscribers);
		when(mockBroadcastMessageBuilder.buildEmailForSubscriber(any(Subscriber.class))).thenReturn(new SendRawEmailRequest());
		when(mockBroadcastMessageBuilder.buildEmailForNonSubscriber(any(UserNotificationInfo.class))).thenReturn(new SendRawEmailRequest());

	}
	
	@Test
	public void testBroadcastThreadWithoutMentionedUsers() throws Exception{
		when(mockBroadcastMessageBuilder.getMarkdown()).thenReturn("");
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
		// The message state should be sent.
		verify(mockBroadcastMessageDao).setBroadcast(change.getChangeNumber());
		// two messages should be sent
		verify(mockSesClient, times(2)).sendRawEmail(any(SendRawEmailRequest.class));
	}

	@Test
	public void testBroadcastThreadWithMentionedUsers() throws Exception {
		Set<String> userIds = new HashSet<String>();
		userIds.addAll(Arrays.asList("111", "222", "2"));
		when(mockBroadcastMessageBuilder.getRelatedUsers()).thenReturn(userIds);
		userIds.remove("2");
		UserNotificationInfo userNotificationInfo1 = new UserNotificationInfo();
		userNotificationInfo1.setUserId("111");
		UserNotificationInfo userNotificationInfo2 = new UserNotificationInfo();
		userNotificationInfo2.setUserId("222");
		when(mockUserProfileDao.getUserNotificationInfo(userIds)).thenReturn(Arrays.asList(userNotificationInfo1, userNotificationInfo2));
		UserInfo hasAccessUserInfo = new UserInfo(false);
		hasAccessUserInfo.setId(111L);
		UserInfo accessDeniedUserInfo = new UserInfo(false);
		accessDeniedUserInfo.setId(222L);
		when(mockUserManager.getUserInfo(111L)).thenReturn(hasAccessUserInfo);
		when(mockUserManager.getUserInfo(222L)).thenReturn(accessDeniedUserInfo);
		when(mockAuthManager.canSubscribe(hasAccessUserInfo, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canSubscribe(accessDeniedUserInfo, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.accessDenied(""));

		manager.broadcastMessage(mockUser, mockCallback, change);
		// The message state should be sent.
		verify(mockBroadcastMessageDao).setBroadcast(change.getChangeNumber());
		verify(mockBroadcastMessageBuilder).getRelatedUsers();
		verify(mockUserProfileDao).getUserNotificationInfo(userIds);
		verify(mockUserManager).getUserInfo(111L);
		verify(mockUserManager).getUserInfo(222L);
		verify(mockUserManager, never()).getUserInfo(2L);
		verify(mockAuthManager).canSubscribe(hasAccessUserInfo, topic.getObjectId(), topic.getObjectType());
		verify(mockAuthManager).canSubscribe(accessDeniedUserInfo, topic.getObjectId(), topic.getObjectType());
		verify(mockSesClient, times(3)).sendRawEmail(any(SendRawEmailRequest.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSendMessageToNonSubscribersEmptyNonSubscribers() throws Exception {
		when(mockBroadcastMessageBuilder.getRelatedUsers()).thenReturn(new HashSet<String>());
		manager.sendMessageToNonSubscribers(mockCallback, change, mockBroadcastMessageBuilder, new ArrayList<String>(), topic);
		verify(mockBroadcastMessageBuilder).getRelatedUsers();
		verify(mockUserProfileDao, never()).getUserNotificationInfo(any(Set.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSendMessageToNonSubscribersNullRelatedUsers() throws Exception {
		when(mockBroadcastMessageBuilder.getRelatedUsers()).thenReturn(null);
		manager.sendMessageToNonSubscribers(mockCallback, change, mockBroadcastMessageBuilder, new ArrayList<String>(), topic);
		verify(mockBroadcastMessageBuilder).getRelatedUsers();
		verify(mockUserProfileDao, never()).getUserNotificationInfo(any(Set.class));
	}

	@Test
	public void testSendMessageToNonSubscribersNoneReceiveNotification() throws Exception {
		Set<String> userIds = new HashSet<String>();
		userIds.addAll(Arrays.asList("111", "222", "2"));
		when(mockBroadcastMessageBuilder.getRelatedUsers()).thenReturn(userIds);
		when(mockUserProfileDao.getUserNotificationInfo(userIds)).thenReturn(new ArrayList<UserNotificationInfo>());
		manager.sendMessageToNonSubscribers(mockCallback, change, mockBroadcastMessageBuilder, new ArrayList<String>(), topic);
		verify(mockBroadcastMessageBuilder).getRelatedUsers();
		verify(mockUserProfileDao).getUserNotificationInfo(userIds);
		verify(mockUserManager, never()).getUserInfo(anyLong());
	}
	

	@Test
	public void testSendMessageToNonSubscribersAllWithPermission() throws Exception {
		Set<String> userIds = new HashSet<String>();
		userIds.addAll(Arrays.asList("111", "222"));
		UserNotificationInfo userNotificationInfo1 = new UserNotificationInfo();
		userNotificationInfo1.setUserId("111");
		UserNotificationInfo userNotificationInfo2 = new UserNotificationInfo();
		userNotificationInfo2.setUserId("222");
		when(mockBroadcastMessageBuilder.getRelatedUsers()).thenReturn(userIds);
		when(mockUserProfileDao.getUserNotificationInfo(userIds)).thenReturn(Arrays.asList(userNotificationInfo1, userNotificationInfo2));
		UserInfo hasAccessUserInfo = new UserInfo(false);
		hasAccessUserInfo.setId(111L);
		UserInfo accessDeniedUserInfo = new UserInfo(false);
		accessDeniedUserInfo.setId(222L);
		when(mockUserManager.getUserInfo(111L)).thenReturn(hasAccessUserInfo);
		when(mockUserManager.getUserInfo(222L)).thenReturn(accessDeniedUserInfo);
		when(mockAuthManager.canSubscribe(hasAccessUserInfo, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canSubscribe(accessDeniedUserInfo, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		manager.sendMessageToNonSubscribers(mockCallback, change, mockBroadcastMessageBuilder, new ArrayList<String>(), topic);
		verify(mockBroadcastMessageBuilder).getRelatedUsers();
		verify(mockUserProfileDao).getUserNotificationInfo(userIds);
		verify(mockUserManager).getUserInfo(111L);
		verify(mockUserManager).getUserInfo(222L);
		verify(mockAuthManager).canSubscribe(hasAccessUserInfo, topic.getObjectId(), topic.getObjectType());
		verify(mockAuthManager).canSubscribe(accessDeniedUserInfo, topic.getObjectId(), topic.getObjectType());
		verify(mockSesClient, times(1)).sendRawEmail(any(SendRawEmailRequest.class));
	}

	@Test
	public void testSendMessageToNonSubscribersWithUserWithoutPermission() throws Exception {
		Set<String> userIds = new HashSet<String>();
		userIds.addAll(Arrays.asList("111", "222"));
		UserNotificationInfo userNotificationInfo1 = new UserNotificationInfo();
		userNotificationInfo1.setUserId("111");
		UserNotificationInfo userNotificationInfo2 = new UserNotificationInfo();
		userNotificationInfo2.setUserId("222");
		when(mockBroadcastMessageBuilder.getRelatedUsers()).thenReturn(userIds);
		when(mockUserProfileDao.getUserNotificationInfo(userIds)).thenReturn(Arrays.asList(userNotificationInfo1, userNotificationInfo2));
		UserInfo hasAccessUserInfo1 = new UserInfo(false);
		hasAccessUserInfo1.setId(111L);
		UserInfo hasAccessUserInfo2 = new UserInfo(false);
		hasAccessUserInfo2.setId(222L);
		when(mockUserManager.getUserInfo(111L)).thenReturn(hasAccessUserInfo1);
		when(mockUserManager.getUserInfo(222L)).thenReturn(hasAccessUserInfo2);
		when(mockAuthManager.canSubscribe(hasAccessUserInfo1, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockAuthManager.canSubscribe(hasAccessUserInfo2, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.authorized());
		manager.sendMessageToNonSubscribers(mockCallback, change, mockBroadcastMessageBuilder, new ArrayList<String>(), topic);
		verify(mockBroadcastMessageBuilder).getRelatedUsers();
		verify(mockUserProfileDao).getUserNotificationInfo(userIds);
		verify(mockUserManager).getUserInfo(111L);
		verify(mockUserManager).getUserInfo(222L);
		verify(mockAuthManager).canSubscribe(hasAccessUserInfo1, topic.getObjectId(), topic.getObjectType());
		verify(mockAuthManager).canSubscribe(hasAccessUserInfo2, topic.getObjectId(), topic.getObjectType());
		verify(mockSesClient, times(2)).sendRawEmail(any(SendRawEmailRequest.class));
	}

	@Test (expected = MarkdownClientException.class)
	public void testBroadcastFailToBuildMessage() throws Exception{
		when(mockBroadcastMessageBuilder.buildEmailForSubscriber(any(Subscriber.class))).thenThrow(new MarkdownClientException(500, ""));
		manager.broadcastMessage(mockUser, mockCallback, change);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testBroadcastMessageUnauthorized() throws Exception{
		// not an adim.
		when(mockUser.isAdmin()).thenReturn(false);
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
	}
	
	@Test
	public void testBroadcastMessageExpired() throws Exception{
		// setup expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(true);
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
		// should be ignored
		verify(mockBroadcastMessageDao, never()).setBroadcast(anyLong());
	}
	
	@Test
	public void testBroadcastMessageChangeDoesNotExist() throws Exception{
		// change number does not exist
		when(mockChangeDao.doesChangeNumberExist(change.getChangeNumber())).thenReturn(false);
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
		// should be ignored
		verify(mockBroadcastMessageDao, never()).setBroadcast(anyLong());
	}
	
	@Test
	public void testBroadcastMessageAlreadyBroadcast() throws Exception{
		// already broadcast.
		when(mockBroadcastMessageDao.wasBroadcast(change.getChangeNumber())).thenReturn(true);
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
		// should be ignored
		verify(mockBroadcastMessageDao, never()).setBroadcast(anyLong());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBroadcastMessageNoBuilder() throws Exception{
		// there is no builder for this type.
		change.setObjectType(ObjectType.TABLE);
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
	}
}
