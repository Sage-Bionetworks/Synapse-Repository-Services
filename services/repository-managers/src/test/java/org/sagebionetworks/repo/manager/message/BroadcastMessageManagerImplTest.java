package org.sagebionetworks.repo.manager.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.markdown.MarkdownClientException;
import org.sagebionetworks.repo.manager.subscription.SubscriptionAndDiscussionAuthorizationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.principal.SynapseEmailService;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.broadcast.UserNotificationInfo;
import org.sagebionetworks.repo.model.dao.subscription.Subscriber;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.ses.EmailQuarantineDao;
import org.sagebionetworks.repo.model.message.BroadcastMessageDao;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.util.TimeoutUtils;
import org.sagebionetworks.util.progress.ProgressCallback;

import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;
import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class BroadcastMessageManagerImplTest {

	@Mock
	private SubscriptionDAO mockSubscriptionDAO;
	@Mock
	private BroadcastMessageDao mockBroadcastMessageDao;
	@Mock
	private DBOChangeDAO mockChangeDao;
	@Mock
	private SynapseEmailService mockSesClient;
	@Mock
	private TimeoutUtils mockTimeoutUtils;
	@Mock
	private DiscussionBroadcastMessageBuilder mockBroadcastMessageBuilder;
	@Mock
	private MessageBuilderFactory mockFactory;
	@Mock
	private UserInfo mockUser;
	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDao;
	@Mock
	private UserProfileDAO mockUserProfileDao;
	@Mock
	private UserManager mockUserManager;
	@Mock
	private SubscriptionAndDiscussionAuthorizationManager subscriptionAndDiscussionAuthorizationManager;
	@Mock
	private EmailQuarantineDao mockEmailQuarantineDao;

	@InjectMocks
	private BroadcastMessageManagerImpl manager;
	
	private ChangeMessage change;
	private List<Subscriber> subscribers;
	private Topic topic;

	@BeforeEach
	public void before() throws Exception{

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
		
		Subscriber sub1 = new Subscriber();
		sub1.setSubscriptionId("1");
		sub1.setSubscriberId("1");
		sub1.setNotificationEmail("email1");
		
		Subscriber sub2 = new Subscriber();
		sub2.setSubscriptionId("2");
		sub2.setSubscriberId("2");
		sub2.setNotificationEmail("email2");
		
		subscribers = Lists.newArrayList(sub1, sub2);
	
	}
	
	@Test
	public void testBroadcastThreadWithoutMentionedUsers() throws Exception{
		
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		when(mockChangeDao.doesChangeNumberExist(change.getChangeNumber())).thenReturn(true);
		when(mockBroadcastMessageDao.wasBroadcast(change.getChangeNumber())).thenReturn(false);
		when(mockFactory.createMessageBuilder(change.getObjectId(), change.getChangeType(), change.getUserId())).thenReturn(mockBroadcastMessageBuilder);
		when(mockBroadcastMessageBuilder.getBroadcastTopic()).thenReturn(topic);
		
		when(mockSubscriptionDAO.getAllEmailSubscribers(topic.getObjectId(), topic.getObjectType())).thenReturn(subscribers);
		when(mockBroadcastMessageBuilder.buildEmailForSubscriber(any(Subscriber.class))).thenReturn(SendRawEmailRequest.builder().build());
		when(mockBroadcastMessageBuilder.getRelatedUsers()).thenReturn(Collections.emptySet());
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
		// The message state should be sent.
		verify(mockBroadcastMessageDao).setBroadcast(change.getChangeNumber());
		// two messages should be sent
		verify(mockSesClient, times(2)).sendRawEmail(any(SendRawEmailRequest.class));
	}
	
	@Test
	public void testBroadcastWithIOException() throws Exception{
		
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		when(mockChangeDao.doesChangeNumberExist(change.getChangeNumber())).thenReturn(true);
		when(mockBroadcastMessageDao.wasBroadcast(change.getChangeNumber())).thenReturn(false);
		when(mockFactory.createMessageBuilder(change.getObjectId(), change.getChangeType(), change.getUserId())).thenReturn(mockBroadcastMessageBuilder);
		when(mockBroadcastMessageBuilder.getBroadcastTopic()).thenReturn(topic);
		
		when(mockSubscriptionDAO.getAllEmailSubscribers(topic.getObjectId(), topic.getObjectType())).thenReturn(subscribers);
		IOException io = new IOException("connection refused");
		when(mockBroadcastMessageBuilder.buildEmailForSubscriber(any(Subscriber.class))).thenThrow(io);
		
		Exception e = assertThrows(IOException.class, ()->{
			// call under test
			manager.broadcastMessage(mockUser, mockCallback, change);
		});
		assertEquals(io, e);

		verify(mockBroadcastMessageDao, never()).setBroadcast(anyLong());
		verify(mockSesClient, never()).sendRawEmail(any(SendRawEmailRequest.class));
	}

	@Test
	public void testBroadcastThreadWithMentionedUsers() throws Exception {
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		when(mockChangeDao.doesChangeNumberExist(change.getChangeNumber())).thenReturn(true);
		when(mockBroadcastMessageDao.wasBroadcast(change.getChangeNumber())).thenReturn(false);
		when(mockFactory.createMessageBuilder(change.getObjectId(), change.getChangeType(), change.getUserId())).thenReturn(mockBroadcastMessageBuilder);
		when(mockBroadcastMessageBuilder.getBroadcastTopic()).thenReturn(topic);
		
		when(mockSubscriptionDAO.getAllEmailSubscribers(topic.getObjectId(), topic.getObjectType())).thenReturn(subscribers);
		when(mockBroadcastMessageBuilder.buildEmailForSubscriber(any(Subscriber.class))).thenReturn(SendRawEmailRequest.builder().build());
		
		Set<String> userIds = new HashSet<String>();
		userIds.addAll(Arrays.asList("111", "222", "2"));
		when(mockBroadcastMessageBuilder.getRelatedUsers()).thenReturn(userIds);
		userIds.remove("2");
		UserNotificationInfo userNotificationInfo1 = new UserNotificationInfo();
		userNotificationInfo1.setUserId("111");
		UserNotificationInfo userNotificationInfo2 = new UserNotificationInfo();
		userNotificationInfo2.setUserId("222");
		when(mockUserProfileDao.getUserNotificationInfo(userIds)).thenReturn(Arrays.asList(userNotificationInfo1, userNotificationInfo2));
		UserInfo hasAccessUserInfo = new UserInfo(false, 111L, AuthorizationConstants.DEFAULT_REALM_ID);
		UserInfo accessDeniedUserInfo = new UserInfo(false, 222L, AuthorizationConstants.DEFAULT_REALM_ID);
		when(mockUserManager.getUserInfo(111L)).thenReturn(hasAccessUserInfo);
		when(mockUserManager.getUserInfo(222L)).thenReturn(accessDeniedUserInfo);
		when(subscriptionAndDiscussionAuthorizationManager.canSubscribe(hasAccessUserInfo, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.authorized());
		when(subscriptionAndDiscussionAuthorizationManager.canSubscribe(accessDeniedUserInfo, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.accessDenied(""));

		// Call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
		
		// The message state should be sent.
		verify(mockBroadcastMessageDao).setBroadcast(change.getChangeNumber());
		verify(mockBroadcastMessageBuilder).getRelatedUsers();
		verify(mockUserProfileDao).getUserNotificationInfo(userIds);
		verify(mockUserManager).getUserInfo(111L);
		verify(mockUserManager).getUserInfo(222L);
		verify(mockUserManager, never()).getUserInfo(2L);
		verify(subscriptionAndDiscussionAuthorizationManager).canSubscribe(hasAccessUserInfo, topic.getObjectId(), topic.getObjectType());
		verify(subscriptionAndDiscussionAuthorizationManager).canSubscribe(accessDeniedUserInfo, topic.getObjectId(), topic.getObjectType());
		verify(mockSesClient, times(2)).sendRawEmail(any(SendRawEmailRequest.class));
	}
	
	@Test
	public void testBroadcastMessageToSubscribersWithQuarantinedEmail() throws Exception {
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		when(mockChangeDao.doesChangeNumberExist(change.getChangeNumber())).thenReturn(true);
		when(mockBroadcastMessageDao.wasBroadcast(change.getChangeNumber())).thenReturn(false);
		when(mockFactory.createMessageBuilder(change.getObjectId(), change.getChangeType(), change.getUserId())).thenReturn(mockBroadcastMessageBuilder);
		when(mockBroadcastMessageBuilder.getBroadcastTopic()).thenReturn(topic);
		
		String quarantinedEmail = "quarantined@example.com";
		
		subscribers.get(0).setNotificationEmail(quarantinedEmail);
		
		when(mockEmailQuarantineDao.isQuarantined(quarantinedEmail)).thenReturn(true);
		when(mockSubscriptionDAO.getAllEmailSubscribers(topic.getObjectId(), topic.getObjectType())).thenReturn(subscribers);
		when(mockBroadcastMessageBuilder.buildEmailForSubscriber(any(Subscriber.class))).thenReturn(SendRawEmailRequest.builder().build());
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);

		verify(mockEmailQuarantineDao).isQuarantined(quarantinedEmail);
		// Only one message should be sent
		verify(mockSesClient).sendRawEmail(any(SendRawEmailRequest.class));
		verify(mockBroadcastMessageDao).setBroadcast(change.getChangeNumber());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSendMessageToNonSubscribersEmptyNonSubscribers() throws Exception {
		when(mockBroadcastMessageBuilder.getRelatedUsers()).thenReturn(new HashSet<String>());
		
		// Call under test
		manager.sendMessageToNonSubscribers(mockCallback, change, mockBroadcastMessageBuilder, new ArrayList<String>(), topic);
		
		verify(mockBroadcastMessageBuilder).getRelatedUsers();
		verify(mockUserProfileDao, never()).getUserNotificationInfo(any(Set.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSendMessageToNonSubscribersNullRelatedUsers() throws Exception {
		when(mockBroadcastMessageBuilder.getRelatedUsers()).thenReturn(null);
		
		// Call under test
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
		
		// Call under test
		manager.sendMessageToNonSubscribers(mockCallback, change, mockBroadcastMessageBuilder, new ArrayList<String>(), topic);
		
		verify(mockBroadcastMessageBuilder).getRelatedUsers();
		verify(mockUserProfileDao).getUserNotificationInfo(userIds);
		verify(mockUserManager, never()).getUserInfo(anyLong());
	}
	

	@Test
	public void testSendMessageToNonSubscribersAllWithPermission() throws Exception {
		when(mockBroadcastMessageBuilder.buildEmailForNonSubscriber(any(UserNotificationInfo.class))).thenReturn(SendRawEmailRequest.builder().build());
		
		Set<String> userIds = new HashSet<String>();
		userIds.addAll(Arrays.asList("111", "222"));
		UserNotificationInfo userNotificationInfo1 = new UserNotificationInfo();
		userNotificationInfo1.setUserId("111");
		UserNotificationInfo userNotificationInfo2 = new UserNotificationInfo();
		userNotificationInfo2.setUserId("222");
		when(mockBroadcastMessageBuilder.getRelatedUsers()).thenReturn(userIds);
		when(mockUserProfileDao.getUserNotificationInfo(userIds)).thenReturn(Arrays.asList(userNotificationInfo1, userNotificationInfo2));
		UserInfo hasAccessUserInfo = new UserInfo(false, 111L, AuthorizationConstants.DEFAULT_REALM_ID);
		UserInfo accessDeniedUserInfo = new UserInfo(false, 222L, AuthorizationConstants.DEFAULT_REALM_ID);
		when(mockUserManager.getUserInfo(111L)).thenReturn(hasAccessUserInfo);
		when(mockUserManager.getUserInfo(222L)).thenReturn(accessDeniedUserInfo);
		when(subscriptionAndDiscussionAuthorizationManager.canSubscribe(hasAccessUserInfo, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.authorized());
		when(subscriptionAndDiscussionAuthorizationManager.canSubscribe(accessDeniedUserInfo, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		
		// Call under test
		manager.sendMessageToNonSubscribers(mockCallback, change, mockBroadcastMessageBuilder, new ArrayList<String>(), topic);
		
		verify(mockBroadcastMessageBuilder).getRelatedUsers();
		verify(mockUserProfileDao).getUserNotificationInfo(userIds);
		verify(mockUserManager).getUserInfo(111L);
		verify(mockUserManager).getUserInfo(222L);
		verify(subscriptionAndDiscussionAuthorizationManager).canSubscribe(hasAccessUserInfo, topic.getObjectId(), topic.getObjectType());
		verify(subscriptionAndDiscussionAuthorizationManager).canSubscribe(accessDeniedUserInfo, topic.getObjectId(), topic.getObjectType());
		verify(mockSesClient).sendRawEmail(any(SendRawEmailRequest.class));
	}

	@Test
	public void testSendMessageToNonSubscribersWithUserWithoutPermission() throws Exception {
		when(mockBroadcastMessageBuilder.buildEmailForNonSubscriber(any(UserNotificationInfo.class))).thenReturn(SendRawEmailRequest.builder().build());
		
		Set<String> userIds = new HashSet<String>();
		userIds.addAll(Arrays.asList("111", "222"));
		UserNotificationInfo userNotificationInfo1 = new UserNotificationInfo();
		userNotificationInfo1.setUserId("111");
		UserNotificationInfo userNotificationInfo2 = new UserNotificationInfo();
		userNotificationInfo2.setUserId("222");
		when(mockBroadcastMessageBuilder.getRelatedUsers()).thenReturn(userIds);
		when(mockUserProfileDao.getUserNotificationInfo(userIds)).thenReturn(Arrays.asList(userNotificationInfo1, userNotificationInfo2));
		UserInfo hasAccessUserInfo1 = new UserInfo(false, 111L, AuthorizationConstants.DEFAULT_REALM_ID);
		UserInfo hasAccessUserInfo2 = new UserInfo(false, 222L, AuthorizationConstants.DEFAULT_REALM_ID);
		when(mockUserManager.getUserInfo(111L)).thenReturn(hasAccessUserInfo1);
		when(mockUserManager.getUserInfo(222L)).thenReturn(hasAccessUserInfo2);
		when(subscriptionAndDiscussionAuthorizationManager.canSubscribe(hasAccessUserInfo1, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.authorized());
		when(subscriptionAndDiscussionAuthorizationManager.canSubscribe(hasAccessUserInfo2, topic.getObjectId(), topic.getObjectType()))
				.thenReturn(AuthorizationStatus.authorized());
		
		// Call under test
		manager.sendMessageToNonSubscribers(mockCallback, change, mockBroadcastMessageBuilder, new ArrayList<String>(), topic);
		
		verify(mockBroadcastMessageBuilder).getRelatedUsers();
		verify(mockUserProfileDao).getUserNotificationInfo(userIds);
		verify(mockUserManager).getUserInfo(111L);
		verify(mockUserManager).getUserInfo(222L);
		verify(subscriptionAndDiscussionAuthorizationManager).canSubscribe(hasAccessUserInfo1, topic.getObjectId(), topic.getObjectType());
		verify(subscriptionAndDiscussionAuthorizationManager).canSubscribe(hasAccessUserInfo2, topic.getObjectId(), topic.getObjectType());
		verify(mockSesClient, times(2)).sendRawEmail(any(SendRawEmailRequest.class));
	}
	
	@Test
	public void testSendMessageToNonSubscribersWithQuarantinedEmail() throws Exception {
		
		String quarantinedEmail = "quarantined@example.com";

		when(mockEmailQuarantineDao.isQuarantined(quarantinedEmail)).thenReturn(true);
		when(mockBroadcastMessageBuilder.buildEmailForNonSubscriber(any(UserNotificationInfo.class))).thenReturn(SendRawEmailRequest.builder().build());
		
		
		Set<String> userIds = new HashSet<String>();
		userIds.addAll(Arrays.asList("111", "222"));
		UserNotificationInfo userNotificationInfo1 = new UserNotificationInfo();
		userNotificationInfo1.setUserId("111");
		userNotificationInfo1.setNotificationEmail(quarantinedEmail);
		UserNotificationInfo userNotificationInfo2 = new UserNotificationInfo();
		userNotificationInfo2.setUserId("222");
		
		when(mockBroadcastMessageBuilder.getRelatedUsers()).thenReturn(userIds);
		when(mockUserProfileDao.getUserNotificationInfo(userIds)).thenReturn(Arrays.asList(userNotificationInfo1, userNotificationInfo2));
		
		when(subscriptionAndDiscussionAuthorizationManager.canSubscribe(any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		// Call under test
		manager.sendMessageToNonSubscribers(mockCallback, change, mockBroadcastMessageBuilder, new ArrayList<String>(), topic);
		
		verify(mockBroadcastMessageBuilder).getRelatedUsers();
		verify(mockUserProfileDao).getUserNotificationInfo(userIds);
		verify(mockEmailQuarantineDao).isQuarantined(quarantinedEmail);
		verify(mockUserManager).getUserInfo(222L);
		
		// Only one should have been sent
		verify(mockSesClient).sendRawEmail(any(SendRawEmailRequest.class));
	}

	@Test
	public void testBroadcastFailToBuildMessage() throws Exception{
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		when(mockChangeDao.doesChangeNumberExist(change.getChangeNumber())).thenReturn(true);
		when(mockBroadcastMessageDao.wasBroadcast(change.getChangeNumber())).thenReturn(false);
		when(mockFactory.createMessageBuilder(change.getObjectId(), change.getChangeType(), change.getUserId())).thenReturn(mockBroadcastMessageBuilder);
		when(mockBroadcastMessageBuilder.getBroadcastTopic()).thenReturn(topic);
		
		when(mockSubscriptionDAO.getAllEmailSubscribers(topic.getObjectId(), topic.getObjectType())).thenReturn(subscribers);
		when(mockBroadcastMessageBuilder.buildEmailForSubscriber(any(Subscriber.class))).thenThrow(new MarkdownClientException(500, ""));
		
		Assertions.assertThrows(MarkdownClientException.class, () -> {
			// call under test
			manager.broadcastMessage(mockUser, mockCallback, change);
		});
	}
	
	@Test
	public void testBroadcastMessageUnauthorized() throws Exception{
		// not an admin.
		when(mockUser.isAdmin()).thenReturn(false);
		
		Assertions.assertThrows(UnauthorizedException.class, () -> {			
			// call under test
			manager.broadcastMessage(mockUser, mockCallback, change);
		});
		verifyNoMoreInteractions(mockBroadcastMessageDao);
	}
	
	@Test
	public void testBroadcastMessageExpired() throws Exception{
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		
		// setup expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(true);
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
		// should be ignored
		verify(mockBroadcastMessageDao, never()).setBroadcast(anyLong());
		verifyNoMoreInteractions(mockBroadcastMessageDao);
	}
	
	@Test
	public void testBroadcastMessageChangeDoesNotExist() throws Exception{
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		when(mockChangeDao.doesChangeNumberExist(change.getChangeNumber())).thenReturn(true);
		
		// change number does not exist
		when(mockChangeDao.doesChangeNumberExist(change.getChangeNumber())).thenReturn(false);
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
		// should be ignored
		verify(mockBroadcastMessageDao, never()).setBroadcast(anyLong());
		verifyNoMoreInteractions(mockBroadcastMessageDao);
	}
	
	@Test
	public void testBroadcastMessageAlreadyBroadcast() throws Exception{
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		when(mockChangeDao.doesChangeNumberExist(change.getChangeNumber())).thenReturn(true);
		when(mockBroadcastMessageDao.wasBroadcast(change.getChangeNumber())).thenReturn(false);
		
		// already broadcast.
		when(mockBroadcastMessageDao.wasBroadcast(change.getChangeNumber())).thenReturn(true);
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
		// should be ignored
		verify(mockBroadcastMessageDao, never()).setBroadcast(anyLong());
		verifyNoMoreInteractions(mockBroadcastMessageDao);
	}
	
	@Test
	public void testBroadcastMessageNoBuilder() throws Exception{
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		when(mockChangeDao.doesChangeNumberExist(change.getChangeNumber())).thenReturn(true);
		when(mockBroadcastMessageDao.wasBroadcast(change.getChangeNumber())).thenReturn(false);
		
		// there is no builder for this type.
		change.setObjectType(ObjectType.TABLE);
		Assertions.assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			manager.broadcastMessage(mockUser, mockCallback, change);
		});
	}
}
