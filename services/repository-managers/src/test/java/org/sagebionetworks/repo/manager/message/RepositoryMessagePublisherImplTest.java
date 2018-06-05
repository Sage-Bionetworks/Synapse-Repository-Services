package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;

import com.amazonaws.services.sns.AmazonSNS;

/**
 * Unit test for RepositoryMessagePublisherImpl.
 * 
 * @author John
 *
 */
public class RepositoryMessagePublisherImplTest {
	
	ChangeMessage message;
	TransactionalMessenger mockTransactionalMessanger;
	AmazonSNS mockAwsSNSClient;
	
	RepositoryMessagePublisherImpl messagePublisher;
	
	@Before
	public void before(){
		mockTransactionalMessanger = Mockito.mock(TransactionalMessenger.class);
		mockAwsSNSClient = Mockito.mock(AmazonSNS.class);
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
	
}
