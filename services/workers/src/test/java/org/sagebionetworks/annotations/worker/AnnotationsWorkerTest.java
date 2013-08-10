package org.sagebionetworks.annotations.worker;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.annotations.worker.AnnotationsWorker;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.model.SubmissionStatusAnnotationsAsyncManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.sqs.model.Message;

/**
 * Test for AnnotationsWorker
 */
public class AnnotationsWorkerTest {
	
	SubmissionStatusAnnotationsAsyncManager mockDAO;
	
	@Before
	public void before(){
		mockDAO = Mockito.mock(SubmissionStatusAnnotationsAsyncManager.class);
	}
	
	/**
	 * non entity messages should be ignored.
	 * @throws Exception 
	 */
	@Test
	public void testCallNonSubmission() throws Exception{
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.ENTITY);
		message.setObjectId("123");
		Message awsMessage = MessageUtils.createMessage(message, "abc", "handle");
		List<Message> list = new LinkedList<Message>();
		list.add(awsMessage);
		// Make the call
		AnnotationsWorker worker = new AnnotationsWorker(list, mockDAO);
		List<Message> resultList = worker.call();
		assertNotNull(resultList);
		// Non-Submission messages should be returned so they can be removed from the queue.
		assertEquals("Non-Submission messages must be returned so they can be removed from the queue!",list, resultList);
		// the DAO should not be called
		verify(mockDAO, never()).updateSubmissionStatus(any(String.class));
		verify(mockDAO, never()).deleteSubmission(any(String.class));
	}
	
	@Test
	public void testUpdateSubmissionStatus() throws Exception{
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.SUBMISSION);
		message.setChangeType(ChangeType.UPDATE);
		message.setObjectId("123");
		Message awsMessage = MessageUtils.createMessage(message, "abc", "handle");
		List<Message> list = new LinkedList<Message>();
		list.add(awsMessage);
		// Make the call
		AnnotationsWorker worker = new AnnotationsWorker(list, mockDAO);
		list = worker.call();
		assertNotNull(list);
		// the manager should not be called
		verify(mockDAO).updateSubmissionStatus(message.getObjectId());
		verify(mockDAO, never()).deleteSubmission(any(String.class));
	}
	
	@Test
	public void testDeleteSubmission() throws Exception{
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.SUBMISSION);
		message.setChangeType(ChangeType.DELETE);
		message.setObjectId("123");
		Message awsMessage = MessageUtils.createMessage(message, "abc", "handle");
		List<Message> list = new LinkedList<Message>();
		list.add(awsMessage);
		// Make the call
		AnnotationsWorker worker = new AnnotationsWorker(list, mockDAO);
		list = worker.call();
		assertNotNull(list);
		// the manager should not be called
		verify(mockDAO, never()).updateSubmissionStatus(message.getObjectId());
		verify(mockDAO).deleteSubmission(any(String.class));
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
		message.setObjectType(ObjectType.SUBMISSION);
		message.setChangeType(ChangeType.UPDATE);
		String successId = "success";
		message.setObjectId(successId);
		Message awsMessage = MessageUtils.createMessage(message, "abc", "handle");
		list.add(awsMessage);
		// This will fail
		message = new ChangeMessage();
		message.setObjectType(ObjectType.SUBMISSION);
		message.setChangeType(ChangeType.UPDATE);
		String failId = "fail";
		message.setObjectId(failId);
		awsMessage = MessageUtils.createMessage(message, "abc", "handle");
		list.add(awsMessage);
		// Simulate a not found
		doThrow(new NotFoundException()).when(mockDAO).updateSubmissionStatus(eq(failId));
		AnnotationsWorker worker = new AnnotationsWorker(list, mockDAO);
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
		message.setObjectType(ObjectType.SUBMISSION);
		message.setChangeType(ChangeType.UPDATE);
		String successId = "success";
		message.setObjectId(successId);
		Message awsMessage = MessageUtils.createMessage(message, "abc", "handle");
		list.add(awsMessage);
		// This will fail
		message = new ChangeMessage();
		message.setObjectType(ObjectType.SUBMISSION);
		message.setChangeType(ChangeType.UPDATE);
		String failId = "fail";
		message.setObjectId(failId);
		awsMessage = MessageUtils.createMessage(message, "abc", "handle");
		list.add(awsMessage);
		// Simulate a runtime exception
		doThrow(new RuntimeException()).when(mockDAO).updateSubmissionStatus(eq(failId));
		AnnotationsWorker worker = new AnnotationsWorker(list, mockDAO);
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
