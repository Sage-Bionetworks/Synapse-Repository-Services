package org.sagebionetworks.repo.manager.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessages;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.LocalStackMessage;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.message.TransactionalMessengerObserver;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.PublishRequest;

/**
 * Autowire test for RepositoryMessageObserverImpl.
 * 
 * @author John
 *
 */
// Cannot use test-context.xml which disables messages
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context-schedulers.xml" })
public class RepositoryMessagePublisherImplAutowireTest {
	
	@Autowired
	private TransactionalMessenger transactionalMessanger;
	
	@Autowired
	private RepositoryMessagePublisher messagePublisher;
	
	@Autowired
	private DBOChangeDAO changeDao;
	
	private AmazonSNS mockSNSClient;
	private LocalStackMessage mockLocalMessage;
	
	@BeforeEach
	public void before(){
		assertNotNull(messagePublisher);
		this.changeDao.deleteAllChanges();
		assertEquals(0, changeDao.getCurrentChangeNumber(), "Failed to delete all change messages");
		// We do not want to actually send messages as part of this test so we mock the client
		mockSNSClient = Mockito.mock(AmazonSNS.class);
		mockLocalMessage = Mockito.mock(LocalStackMessage.class);
		
		messagePublisher.setAwsSNSClient(mockSNSClient);
		when(mockSNSClient.createTopic(any(CreateTopicRequest.class))).thenReturn(new CreateTopicResult().withTopicArn("topicArn"));
	}
	
	@AfterEach
	public void after(){
		this.changeDao.deleteAllChanges();
	}
	
	@Test
	public void testGetArn(){
		String arn = messagePublisher.getTopicArn(ObjectType.ENTITY);
		System.out.println("Arn: "+arn);
		assertNotNull(arn);
	}
	
	@Test
	public void testInitialize(){
		// Make sure the RepositoryMessagePublisher is registered as a listener to the transactional messages.
		List<TransactionalMessengerObserver> observers = transactionalMessanger.getAllObservers();
		assertNotNull(observers);
		assertTrue(observers.size() > 0, "Expected to find the RepositoryMessagePublisher as an observer of the transactionalMessanger");
		boolean found = false;
		for(TransactionalMessengerObserver observer: observers){
			if(observer instanceof RepositoryMessagePublisher){
				found = true;
				break;
			}
		}
		assertTrue(found, "Failed to find the RepositoryMessagePublisher on the list of transactionalMessanger observers");
		assertNotNull(messagePublisher.getTopicName(ObjectType.ENTITY));
	}
	
	@Test
	public void testFireChangeMessage() throws InterruptedException, JSONObjectAdapterException{
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.CREATE);
		message.setObjectType(ObjectType.ENTITY);
		message.setObjectId("123");
		message.setChangeNumber(1l);
		message.setTimestamp(new Date());
		message = changeDao.replaceChange(message);
		// Before we first there should be one unsent message
		List<ChangeMessage> unsent = changeDao.listUnsentMessages(Long.MAX_VALUE);
		assertEquals(1, unsent.size());
		messagePublisher.fireChangeMessage(message);
		// The message will be published on a timer, so we wait for that to occur.
		Thread.sleep(2000);
		// Validate that our message was fired.
		ChangeMessages messages = new ChangeMessages();
		messages.setList(Arrays.asList(message));
		String json = EntityFactory.createJSONStringForEntity(messages);
		// The message should be published once and only once.
		verify(mockSNSClient, times(1)).publish(new PublishRequest(messagePublisher.getTopicArn(ObjectType.ENTITY), json));
		// The message should be sent
		unsent = changeDao.listUnsentMessages(Long.MAX_VALUE);
		assertEquals(0, unsent.size());
	}
	
	@Test
	public void testFireStaggaredChangeMessage() throws InterruptedException, JSONObjectAdapterException{
		// Fire multiple messages
		List<String> messageBodyList = new LinkedList<String>();
		for(int i=0; i<5; i++){
			ChangeMessage message = new ChangeMessage();
			message.setChangeType(ChangeType.CREATE);
			message.setObjectType(ObjectType.ENTITY);
			message.setObjectId(""+i);
			message.setChangeNumber(1l);
			message.setTimestamp(new Date());
			message = changeDao.replaceChange(message);
			messagePublisher.fireChangeMessage(message);
			
			// Keep this body for the check
			ChangeMessages messages = new ChangeMessages();
			messages.setList(Arrays.asList(message));
			String json = EntityFactory.createJSONStringForEntity(messages);
			messageBodyList.add(json);
			// Sleep between messages.
			Thread.sleep(50);
		}

		// The message will be published on a timer, so we wait for that to occur.
		Thread.sleep(2000);
		// Validate that all of the messages were fired.
		for(String messageBody: messageBodyList){
			System.out.println("Checking for message body: "+messageBody);
			// The message should be published once and only once.
			verify(mockSNSClient, times(1)).publish(new PublishRequest(messagePublisher.getTopicArn(ObjectType.ENTITY), messageBody));
		}
	}
	
	@Test
	public void testFireLocalStackMessage() throws JSONObjectAdapterException {
		when(mockLocalMessage.getObjectType()).thenReturn(ObjectType.TABLE_STATUS_EVENT);
		
		String expectcedJson = EntityFactory.createJSONStringForEntity(mockLocalMessage);
		
		// Call under test
		messagePublisher.fireLocalStackMessage(mockLocalMessage);
		
		verify(mockSNSClient).publish(new PublishRequest(messagePublisher.getTopicArn(ObjectType.TABLE_STATUS_EVENT), expectcedJson));
	}

}
