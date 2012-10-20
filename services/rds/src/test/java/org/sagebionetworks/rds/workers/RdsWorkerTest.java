package org.sagebionetworks.rds.workers;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ObjectType;

import com.amazonaws.services.sqs.model.Message;

/**
 * Test for RdsWorker
 * @author jmhill
 *
 */
public class RdsWorkerTest {
	
	AsynchronousManager mockManager;
	
	@Before
	public void before(){
		mockManager = Mockito.mock(AsynchronousManager.class);
	}
	
	/**
	 * non entity messages should be ignored.
	 * @throws Exception 
	 */
	@Test
	public void testCallNonEntity() throws Exception{
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.PRINCIPAL);
		message.setObjectId("123");
		Message awsMessage = MessageUtils.createMessage(message, "abc", "handle");
		List<Message> list = new LinkedList<Message>();
		list.add(awsMessage);
		// Make the call
		RdsWorker worker = new RdsWorker(list, mockManager);
		list = worker.call();
		assertNotNull(list);
		// the manager should not be called
		verify(mockManager, never()).createEntity(any(String.class));
		verify(mockManager, never()).updateEntity(any(String.class));
		verify(mockManager, never()).deleteEntity(any(String.class));
	}
}
