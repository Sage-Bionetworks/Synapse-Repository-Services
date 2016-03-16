package org.sagebionetworks.repo.manager.message;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.NotificationManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.BroadcastMessageDao;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.util.TimeoutUtils;
import org.springframework.test.util.ReflectionTestUtils;

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
	TimeoutUtils mockTimeoutUtils;
	@Mock
	NotificationManager mockNotificationManager;
	@Mock
	BroadcastMessageBuilder mockBroadcastMessageBuilder;
	@Mock
	UserInfo mockUser;
	
	BroadcastMessageManagerImpl manager;
	
	ChangeMessage change;
	
	Long messageToUserId;
	MessageToUser messageToUser;
	BroadcastMessage broadcastMessage;
	List<String> subscribers;

	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		manager = new BroadcastMessageManagerImpl();
		ReflectionTestUtils.setField(manager, "subscriptionDAO", mockSubscriptionDAO);
		ReflectionTestUtils.setField(manager, "broadcastMessageDao", mockBroadcastMessageDao);
		ReflectionTestUtils.setField(manager, "changeDao", mockChangeDao);
		ReflectionTestUtils.setField(manager, "timeoutUtils", mockTimeoutUtils);
		ReflectionTestUtils.setField(manager, "notificationManager", mockNotificationManager);
		
		Map<ObjectType, BroadcastMessageBuilder> builderMap = new HashMap<ObjectType, BroadcastMessageBuilder>();
		builderMap.put(ObjectType.REPLY, mockBroadcastMessageBuilder);
		manager.setBuilderMap(builderMap);
		
		// default setup
		change = new ChangeMessage();
		change.setChangeNumber(123L);
		change.setChangeType(ChangeType.CREATE);
		change.setObjectId("456");
		change.setObjectType(ObjectType.REPLY);
		change.setTimestamp(new Date(123000L));
		
		subscribers = Lists.newArrayList("801", "802");
		
		broadcastMessage = new BroadcastMessage();
		Topic topic = new Topic();
		topic.setObjectId("5555");
		topic.setObjectType(SubscriptionObjectType.DISCUSSION_THREAD);
		broadcastMessage.setTopic(topic);
		broadcastMessage.setBody("The email body");
		broadcastMessage.setContentType(ContentType.TEXT_HTML);
		broadcastMessage.setSubject("subject");
		
		when(mockUser.isAdmin()).thenReturn(true);
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		when(mockBroadcastMessageDao.wasBroadcast(change.getChangeNumber())).thenReturn(false);
		when(mockChangeDao.doesChangeNumberExist(change.getChangeNumber())).thenReturn(true);
		
		when(mockBroadcastMessageBuilder.buildMessage(change.getObjectId(), change.getChangeType())).thenReturn(broadcastMessage);
		
		messageToUserId = 999L;
		messageToUser = new MessageToUser();
		messageToUser.setId(messageToUserId.toString());
		when(mockNotificationManager.sendNotification(any(UserInfo.class), any(MessageToUserAndBody.class))).thenReturn(messageToUser);
		
	}
	
	
	@Test
	public void testHappyBroadcast(){
		// All of the data that should be sent in the message.
		MessageToUser expectedMessageToUser = new MessageToUser();
		expectedMessageToUser.setRecipients(new HashSet<String>(subscribers));
		expectedMessageToUser.setSubject(broadcastMessage.getSubject());
		MessageToUserAndBody expectedMessageToUserAndBody = new MessageToUserAndBody();
		expectedMessageToUserAndBody.setBody(broadcastMessage.getBody());
		expectedMessageToUserAndBody.setMimeType(broadcastMessage.getContentType().getMimeType());
		
		// call under test
		manager.broadcastMessage(mockUser, change);
		verify(mockNotificationManager).sendNotification(mockUser, expectedMessageToUserAndBody);
		// The message state should be sent.
		verify(mockBroadcastMessageDao).setBroadcast(change.getChangeNumber(), messageToUserId);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testBroadcastMessageUnauthorized(){
		// not an adim.
		when(mockUser.isAdmin()).thenReturn(false);
		// call under test
		manager.broadcastMessage(mockUser, change);
	}
	
	@Test
	public void testBroadcastMessageExpired(){
		// setup expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(true);
		// call under test
		manager.broadcastMessage(mockUser, change);
		// should be ignored
		verify(mockNotificationManager, never()).sendNotification(any(UserInfo.class), any(MessageToUserAndBody.class));
		verify(mockBroadcastMessageDao, never()).setBroadcast(anyLong(), anyLong());
	}
	
	@Test
	public void testBroadcastMessageChangeDoesNotExist(){
		// change number does not exist
		when(mockChangeDao.doesChangeNumberExist(change.getChangeNumber())).thenReturn(false);
		// call under test
		manager.broadcastMessage(mockUser, change);
		// should be ignored
		verify(mockNotificationManager, never()).sendNotification(any(UserInfo.class), any(MessageToUserAndBody.class));
		verify(mockBroadcastMessageDao, never()).setBroadcast(anyLong(), anyLong());
	}
	
	@Test
	public void testBroadcastMessageAlreadyBroadcast(){
		// already broadcast.
		when(mockBroadcastMessageDao.wasBroadcast(change.getChangeNumber())).thenReturn(true);
		// call under test
		manager.broadcastMessage(mockUser, change);
		// should be ignored
		verify(mockNotificationManager, never()).sendNotification(any(UserInfo.class), any(MessageToUserAndBody.class));
		verify(mockBroadcastMessageDao, never()).setBroadcast(anyLong(), anyLong());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBroadcastMessageNoBuilder(){
		// there is no builder for this type.
		change.setObjectType(ObjectType.TABLE);
		// call under test
		manager.broadcastMessage(mockUser, change);
		// should be ignored
		verify(mockNotificationManager, never()).sendNotification(any(UserInfo.class), any(MessageToUserAndBody.class));
		verify(mockBroadcastMessageDao, never()).setBroadcast(anyLong(), anyLong());
	}
}
