package org.sagebionetworks.rds.workers;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.sqs.model.Message;

/**
 * Test for RdsWorker
 * @author jmhill
 *
 */
public class RdsWorkerTest {
	
	AsynchronousDAO mockManager;
	
	@Before
	public void before(){
		mockManager = Mockito.mock(AsynchronousDAO.class);
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
		List<Message> resultList = worker.call();
		assertNotNull(resultList);
		// Non-entity messages should be returned so they can be removed from the queue.
		assertEquals("Non-entity messages must be returned so they can be removed from the queue!",list, resultList);
		// the manager should not be called
		verify(mockManager, never()).createEntity(any(String.class));
		verify(mockManager, never()).updateEntity(any(String.class));
		verify(mockManager, never()).deleteEntity(any(String.class));
	}
	
	@Test
	public void testCreateEntity() throws Exception{
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.ENTITY);
		message.setChangeType(ChangeType.CREATE);
		message.setObjectId("123");
		Message awsMessage = MessageUtils.createMessage(message, "abc", "handle");
		List<Message> list = new LinkedList<Message>();
		list.add(awsMessage);
		// Make the call
		RdsWorker worker = new RdsWorker(list, mockManager);
		list = worker.call();
		assertNotNull(list);
		// the manager should not be called
		verify(mockManager, times(1)).createEntity(message.getObjectId());
		verify(mockManager, never()).updateEntity(any(String.class));
		verify(mockManager, never()).deleteEntity(any(String.class));
	}
	
	@Test
	public void testUpdateEntity() throws Exception{
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.ENTITY);
		message.setChangeType(ChangeType.UPDATE);
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
		verify(mockManager, times(1)).updateEntity(message.getObjectId());
		verify(mockManager, never()).deleteEntity(any(String.class));
	}
	
	@Test
	public void testDeleteEntity() throws Exception{
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.ENTITY);
		message.setChangeType(ChangeType.DELETE);
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
		verify(mockManager, times(1)).deleteEntity(message.getObjectId());
	}
	
	
	/**
	 * When a not found exception is thrown we want to process and remove the message from the queue.
	 * @throws Exception
	 */
	@Test
	public void testNotFound() throws Exception{
		// Test the case where an error occurs and and there is success
		List<Message> list = new LinkedList<Message>();
		// This will succeed
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.ENTITY);
		message.setChangeType(ChangeType.UPDATE);
		String successId = "success";
		message.setObjectId(successId);
		Message awsMessage = MessageUtils.createMessage(message, "abc", "handle");
		list.add(awsMessage);
		// This will fail
		message = new ChangeMessage();
		message.setObjectType(ObjectType.ENTITY);
		message.setChangeType(ChangeType.UPDATE);
		String failId = "fail";
		message.setObjectId(failId);
		awsMessage = MessageUtils.createMessage(message, "abc", "handle");
		list.add(awsMessage);
		// Simulate a not found
		when(mockManager.updateEntity(failId)).thenThrow(new NotFoundException("NotFound"));
		RdsWorker worker = new RdsWorker(list, mockManager);
		List<Message> resultLIst = worker.call();
		assertEquals(list, resultLIst);
	}
	
	/**
	 * When an unknown exception occurs we should not clear the message from the queue.
	 * @throws Exception
	 */
	@Test
	public void testUnknownException() throws Exception{
		// Test the case where an error occurs and and there is success
		List<Message> list = new LinkedList<Message>();
		// This will succeed
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.ENTITY);
		message.setChangeType(ChangeType.UPDATE);
		String successId = "success";
		message.setObjectId(successId);
		Message awsMessage = MessageUtils.createMessage(message, "abc", "handle");
		list.add(awsMessage);
		// This will fail
		message = new ChangeMessage();
		message.setObjectType(ObjectType.ENTITY);
		message.setChangeType(ChangeType.UPDATE);
		String failId = "fail";
		message.setObjectId(failId);
		awsMessage = MessageUtils.createMessage(message, "abc", "handle");
		list.add(awsMessage);
		// Simulate a not found
		when(mockManager.updateEntity(failId)).thenThrow(new RuntimeException("Unknown exception"));
		RdsWorker worker = new RdsWorker(list, mockManager);
		List<Message> resultLIst = worker.call();
		// The result list should only contain the success message.
		// The error message must stay on the queue.
		assertEquals(1, resultLIst.size());
		Message resultMessage = resultLIst.get(0);
		ChangeMessage change = MessageUtils.extractMessageBody(resultMessage);
		assertNotNull(change);
		assertEquals(successId, change.getObjectId());
	}
}
