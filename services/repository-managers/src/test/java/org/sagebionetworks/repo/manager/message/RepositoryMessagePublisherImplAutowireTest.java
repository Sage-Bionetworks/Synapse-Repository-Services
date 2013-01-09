package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.message.TransactionalMessengerObserver;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;

/**
 * Autowire test for RepositoryMessageObserverImpl.
 * 
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:aws-topic-publisher.spb.xml" })
public class RepositoryMessagePublisherImplAutowireTest {

	
	@Autowired
	TransactionalMessenger transactionalMessanger;
	
	@Autowired
	RepositoryMessagePublisher messagePublisher;
	
	AmazonSNSClient mockSNSClient;
	
	@Before
	public void before(){
		assertNotNull(messagePublisher);
		// We do not want to actually send messages as part of this test so we mock the client
		mockSNSClient = Mockito.mock(AmazonSNSClient.class);
		messagePublisher.setAwsSNSClient(mockSNSClient);
	}
	
	
	@Test
	public void testGetArn(){
		String arn = messagePublisher.getTopicArn();
		System.out.println("Arn: "+arn);
		assertNotNull(arn);
	}
	
	@Test
	public void testInitialize(){
		// Make sure the RepositoryMessagePublisher is registered as a listener to the transactional messages.
		List<TransactionalMessengerObserver> observers = transactionalMessanger.getAllObservers();
		assertNotNull(observers);
		assertTrue("Expected to find the RepositoryMessagePublisher as an observer of the transactionalMessanger",observers.size() > 0);
		boolean found = false;
		for(TransactionalMessengerObserver observer: observers){
			if(observer instanceof RepositoryMessagePublisher){
				found = true;
				break;
			}
		}
		assertTrue("Failed to find the RepositoryMessagePublisher on the list of transactionalMessanger observers",found);
		assertNotNull(messagePublisher.getTopicName());
	}
	
	@Test
	public void testFireMessage() throws InterruptedException, JSONObjectAdapterException{
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.CREATE);
		message.setObjectType(ObjectType.ENTITY);
		message.setObjectId("123");
		message.setObjectEtag("ABCDEFG");
		message.setChangeNumber(1l);
		message.setTimestamp(new Date());
		messagePublisher.fireChangeMessage(message);
		// The message will be published on a timer, so we wait for that to occur.
		Thread.sleep(200);
		// Validate that our message was fired.
		String json = EntityFactory.createJSONStringForEntity(message);
		// The message should be published once and only once.
		verify(mockSNSClient, times(1)).publish(new PublishRequest(messagePublisher.getTopicArn(), json));
	}
	
	@Test
	public void testFireStaggaredMessage() throws InterruptedException, JSONObjectAdapterException{
		// Fire multiple messages
		List<String> messageBodyList = new LinkedList<String>();
		for(int i=0; i<5; i++){
			ChangeMessage message = new ChangeMessage();
			message.setChangeType(ChangeType.CREATE);
			message.setObjectType(ObjectType.ENTITY);
			message.setObjectId(""+i);
			message.setObjectEtag("ABCDEFG"+i);
			message.setChangeNumber(1l);
			message.setTimestamp(new Date());
			messagePublisher.fireChangeMessage(message);
			
			// Keep this body for the check
			String json = EntityFactory.createJSONStringForEntity(message);
			messageBodyList.add(json);
			// Sleep between messages.
			Thread.sleep(50);
		}

		// The message will be published on a timer, so we wait for that to occur.
		Thread.sleep(200);
		// Validate that all of the messages were fired.
		for(String messageBody: messageBodyList){
			System.out.println("Checking for message body: "+messageBody);
			// The message should be published once and only once.
			verify(mockSNSClient, times(1)).publish(new PublishRequest(messagePublisher.getTopicArn(), messageBody));
		}

	}
}
