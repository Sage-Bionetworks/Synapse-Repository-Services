package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.message.TransactionalMessengerObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
	
	@Before
	public void before(){
		assertNotNull(messagePublisher);
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
}
