package org.sagebionetworks.repo.manager.message;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;

/**
 * Unit test for RepositoryMessagePublisherImpl.
 * 
 * @author John
 *
 */
public class RepositoryMessagePublisherImplTest {
	
	ChangeMessage message;
	TransactionalMessenger mockTransactionalMessanger;
	AmazonSNSClient mockAwsSNSClient;
	
	RepositoryMessagePublisherImpl messagePublisher;
	
	@Before
	public void before(){
		mockTransactionalMessanger = Mockito.mock(TransactionalMessenger.class);
		mockAwsSNSClient = Mockito.mock(AmazonSNSClient.class);
		message = new ChangeMessage();
		message.setChangeNumber(123l);
		message.setTimestamp(new Date());
		message.setChangeType(ChangeType.CREATE);
		message.setObjectId("syn456");
		message.setObjectType(ObjectType.ENTITY);
		messagePublisher = new RepositoryMessagePublisherImpl("prefix", "name", mockTransactionalMessanger, mockAwsSNSClient);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testFireNull(){
		messagePublisher.fireChangeMessage(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testFireNullChangeNumber(){
		message.setChangeNumber(null);
		messagePublisher.fireChangeMessage(message);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testFireNullObjectId(){
		message.setObjectId(null);
		messagePublisher.fireChangeMessage(message);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testFireNullObjectType(){
		message.setObjectType(null);
		messagePublisher.fireChangeMessage(message);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testFireNullTimestamp(){
		message.setTimestamp(null);
		messagePublisher.fireChangeMessage(message);
	}
	
	@Test
	public void testFire(){
		// This should work
		messagePublisher.fireChangeMessage(message);
	}
	
	/**
	 * verify that if we cannot register the message as sent 
	 */
	@Test
	public void testPLFM_2821(){
		doThrow(new IllegalArgumentException()).when(mockTransactionalMessanger).registerMessageSent(message);
		try {
			messagePublisher.publishToTopic(message);
			fail("Exception should have been thrown.");
		} catch (IllegalArgumentException e) {
			// expected
		}
		verify(mockAwsSNSClient, never()).publish(any(PublishRequest.class));
	}
	
	@Test
	public void testGroupMessagesByObjectType(){
		ChangeMessage one = new ChangeMessage();
		one.setObjectType(ObjectType.ENTITY);
		one.setObjectId("one");
		ChangeMessage two = new ChangeMessage();
		two.setObjectType(ObjectType.FILE);
		two.setObjectId("two");
		ChangeMessage three = new ChangeMessage();
		three.setObjectType(ObjectType.ENTITY);
		three.setObjectId("three");
		ChangeMessage four = new ChangeMessage();
		four.setObjectType(ObjectType.PRINCIPAL);
		four.setObjectId("four");
		List<ChangeMessage> batch = Arrays.asList(one, two, three, four);
		
		Map<ObjectType, List<ChangeMessage>> groups = RepositoryMessagePublisherImpl.groupMessagesByObjectType(batch);
		assertNotNull(groups);
		assertEquals(3, groups.size());
		// entity should have two
		List<ChangeMessage> group = groups.get(ObjectType.ENTITY);
		assertEquals(Arrays.asList(one,three), group);
		// file should have one
		group = groups.get(ObjectType.FILE);
		assertEquals(Arrays.asList(two), group);
		// principal should have one.
		group = groups.get(ObjectType.PRINCIPAL);
		assertEquals(Arrays.asList(four), group);
	}
}
