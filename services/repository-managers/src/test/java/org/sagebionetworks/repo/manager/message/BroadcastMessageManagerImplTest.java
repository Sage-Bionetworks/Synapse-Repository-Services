package org.sagebionetworks.repo.manager.message;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.principal.SynapseEmailService;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.BroadcastMessageDao;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.subscription.Subscriber;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.subscription.Topic;
import org.sagebionetworks.util.TimeoutUtils;
import org.springframework.test.util.ReflectionTestUtils;

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
	BroadcastMessageBuilder mockBroadcastMessageBuilder;
	@Mock
	MessageBuilderFactory mockFactory;
	@Mock
	UserInfo mockUser;
	@Mock
	ProgressCallback<ChangeMessage> mockCallback;
	
	BroadcastMessageManagerImpl manager;
	
	ChangeMessage change;

	List<Subscriber> subscribers;

	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		manager = new BroadcastMessageManagerImpl();
		ReflectionTestUtils.setField(manager, "subscriptionDAO", mockSubscriptionDAO);
		ReflectionTestUtils.setField(manager, "broadcastMessageDao", mockBroadcastMessageDao);
		ReflectionTestUtils.setField(manager, "changeDao", mockChangeDao);
		ReflectionTestUtils.setField(manager, "timeoutUtils", mockTimeoutUtils);
		ReflectionTestUtils.setField(manager, "sesClient", mockSesClient);
		
		Map<ObjectType, MessageBuilderFactory> factoryMap = new HashMap<ObjectType, MessageBuilderFactory>();
		factoryMap.put(ObjectType.REPLY, mockFactory);
		manager.setFactoryMap(factoryMap);
		
		// default setup
		change = new ChangeMessage();
		change.setChangeNumber(123L);
		change.setChangeType(ChangeType.CREATE);
		change.setObjectId("456");
		change.setObjectType(ObjectType.REPLY);
		change.setTimestamp(new Date(123000L));
		change.setUserId(789L);
		
		Topic topic = new Topic();
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
		
	}
	
	
	@Test
	public void testHappyBroadcast(){
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
		// The message state should be sent.
		verify(mockBroadcastMessageDao).setBroadcast(change.getChangeNumber());
		// progress should be made for each subscriber
		verify(mockCallback, times(2)).progressMade(change);
		// two messages should be sent
		verify(mockSesClient, times(2)).sendRawEmail(any(SendRawEmailRequest.class));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testBroadcastMessageUnauthorized(){
		// not an adim.
		when(mockUser.isAdmin()).thenReturn(false);
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
	}
	
	@Test
	public void testBroadcastMessageExpired(){
		// setup expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(true);
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
		// should be ignored
		verify(mockBroadcastMessageDao, never()).setBroadcast(anyLong());
	}
	
	@Test
	public void testBroadcastMessageChangeDoesNotExist(){
		// change number does not exist
		when(mockChangeDao.doesChangeNumberExist(change.getChangeNumber())).thenReturn(false);
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
		// should be ignored
		verify(mockBroadcastMessageDao, never()).setBroadcast(anyLong());
	}
	
	@Test
	public void testBroadcastMessageAlreadyBroadcast(){
		// already broadcast.
		when(mockBroadcastMessageDao.wasBroadcast(change.getChangeNumber())).thenReturn(true);
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
		// should be ignored
		verify(mockBroadcastMessageDao, never()).setBroadcast(anyLong());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testBroadcastMessageNoBuilder(){
		// there is no builder for this type.
		change.setObjectType(ObjectType.TABLE);
		// call under test
		manager.broadcastMessage(mockUser, mockCallback, change);
	}
}
