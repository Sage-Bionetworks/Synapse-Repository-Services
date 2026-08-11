package org.sagebionetworks.repo.manager.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.LocalStackMessage;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Unit test for RepositoryMessagePublisherImpl.
 * 
 * @author John
 *
 */
@ExtendWith(MockitoExtension.class)
public class RepositoryMessagePublisherImplTest {

	@Mock
	private TransactionalMessenger mockTransactionalMessanger;
	@Mock
	private SnsClient mockAwsSNSClient;
	@Mock
	private StackConfiguration mockConfig;
	
	@InjectMocks
	private RepositoryMessagePublisherImpl messagePublisher;
	
	private ChangeMessage message;
	
	@Mock
	private LocalStackMessage mockLocalMessage;
	
	@BeforeEach
	public void before(){
		message = new ChangeMessage();
		message.setChangeNumber(123l);
		message.setTimestamp(new Date());
		message.setChangeType(ChangeType.CREATE);
		message.setObjectId("syn456");
		message.setObjectType(ObjectType.ENTITY);
	}
	
	@Test
	public void testFireNull(){
		assertThrows(IllegalArgumentException.class, () -> {			
			messagePublisher.fireChangeMessage(null);
		});
	}
	
	@Test
	public void testFireNullChangeNumber(){
		message.setChangeNumber(null);
		assertThrows(IllegalArgumentException.class, () -> {
			messagePublisher.fireChangeMessage(message);
		});
	}

	@Test
	public void testFireNullObjectId(){
		message.setObjectId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			messagePublisher.fireChangeMessage(message);
		});
	}
	
	@Test
	public void testFireNullObjectType(){
		message.setObjectType(null);
		assertThrows(IllegalArgumentException.class, () -> {
			messagePublisher.fireChangeMessage(message);
		});
	}
	
	@Test
	public void testFireNullTimestamp(){
		message.setTimestamp(null);
		assertThrows(IllegalArgumentException.class, () -> {
			messagePublisher.fireChangeMessage(message);
		});
	}
	
	@Test
	public void testFire(){
		// This should work
		messagePublisher.fireChangeMessage(message);
	}
	
	@Test
	public void testFireLocalStackMessage() throws JSONObjectAdapterException {
		ObjectType type = ObjectType.TABLE_STATUS_EVENT;
		
		when(mockLocalMessage.getObjectType()).thenReturn(type);
		when(mockConfig.getRepositoryChangeTopic(any())).thenReturn("topic");
		when(mockAwsSNSClient.createTopic(any(CreateTopicRequest.class))).thenReturn(
				CreateTopicResponse.builder().topicArn("topicArn").build());

		String expectedJson = EntityFactory.createJSONStringForEntity(mockLocalMessage);

		// Call under test
		messagePublisher.fireLocalStackMessage(mockLocalMessage);

		verify(mockConfig).getRepositoryChangeTopic(type.name());
		verify(mockAwsSNSClient).createTopic(CreateTopicRequest.builder().name("topic").build());
		verify(mockAwsSNSClient).publish(PublishRequest.builder().topicArn("topicArn").message(expectedJson).build());
	}
	
	@Test
	public void testFireLocalStackMessageWithNoObjectType() throws JSONObjectAdapterException {
		ObjectType type = null;
		
		when(mockLocalMessage.getObjectType()).thenReturn(type);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			messagePublisher.fireLocalStackMessage(mockLocalMessage);
		}).getMessage();

		assertEquals("The message.objectType is required.", message);
		
		verifyNoMoreInteractions(mockConfig);
		verifyNoMoreInteractions(mockAwsSNSClient);
		
	}
	
	@Test
	public void testFireLocalStackMessageWithNoMessage() throws JSONObjectAdapterException {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			messagePublisher.fireLocalStackMessage(null);
		}).getMessage();

		assertEquals("The message is required.", message);

		verifyNoMoreInteractions(mockConfig);
		verifyNoMoreInteractions(mockAwsSNSClient);

	}

	@Test
	public void testPublishLocalStackMessageToTopic() throws JSONObjectAdapterException {
		ObjectType topicType = ObjectType.SOURCE_DEPENDENCY_EVENT;

		// The message carries a different object type than the destination topic.
		when(mockConfig.getRepositoryChangeTopic(any())).thenReturn("topic");
		when(mockAwsSNSClient.createTopic(any(CreateTopicRequest.class))).thenReturn(
				CreateTopicResponse.builder().topicArn("topicArn").build());

		String expectedJson = EntityFactory.createJSONStringForEntity(mockLocalMessage);

		// Call under test
		messagePublisher.publishLocalStackMessageToTopic(topicType, mockLocalMessage);

		// Routing uses the topicType, not the message's own object type.
		verify(mockConfig).getRepositoryChangeTopic(topicType.name());
		verify(mockAwsSNSClient).createTopic(CreateTopicRequest.builder().name("topic").build());
		verify(mockAwsSNSClient).publish(PublishRequest.builder().topicArn("topicArn").message(expectedJson).build());
		// Non-durable: no CHANGES/SENT_MESSAGES bookkeeping.
		verifyNoMoreInteractions(mockTransactionalMessanger);
	}

	@Test
	public void testPublishLocalStackMessageToTopicWithNullTopicType() throws JSONObjectAdapterException {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			messagePublisher.publishLocalStackMessageToTopic(null, mockLocalMessage);
		}).getMessage();

		assertEquals("The topicType is required.", message);

		verifyNoMoreInteractions(mockConfig);
		verifyNoMoreInteractions(mockAwsSNSClient);
	}

	@Test
	public void testPublishLocalStackMessageToTopicWithNullMessage() throws JSONObjectAdapterException {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			messagePublisher.publishLocalStackMessageToTopic(ObjectType.SOURCE_DEPENDENCY_EVENT, null);
		}).getMessage();

		assertEquals("The message is required.", message);

		verifyNoMoreInteractions(mockConfig);
		verifyNoMoreInteractions(mockAwsSNSClient);
	}

}
