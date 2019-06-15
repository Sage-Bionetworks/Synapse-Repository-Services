package org.sagebionetworks.repo.manager.message;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
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
@RunWith(MockitoJUnitRunner.class)
public class RepositoryMessagePublisherImplTest {

	ChangeMessage message;
	@Mock
	TransactionalMessenger mockTransactionalMessanger;
	@Mock
	AmazonSNS mockAwsSNSClient;

	@InjectMocks
	RepositoryMessagePublisherImpl messagePublisher;
	
	@Before
	public void before(){
		message = new ChangeMessage();
		message.setChangeNumber(123l);
		message.setTimestamp(new Date());
		message.setChangeType(ChangeType.CREATE);
		message.setObjectId("syn456");
		message.setObjectType(ObjectType.ENTITY);
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
