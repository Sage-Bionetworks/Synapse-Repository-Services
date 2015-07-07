package org.sagebionetworks.rds.workers;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test for RdsWorker
 *
 */
public class EntityAnnotationWorkerTest {
	
	ProgressCallback<ChangeMessage> mockProgressCallback;
	AsynchronousDAO mockManager;
	WorkerLogger mockWorkerLogger;
	EntityAnnotationsWorker worker;
	
	@Before
	public void before(){
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		mockManager = Mockito.mock(AsynchronousDAO.class);
		mockWorkerLogger = Mockito.mock(WorkerLogger.class);
		worker = new EntityAnnotationsWorker();
		ReflectionTestUtils.setField(worker, "asynchronousDAO", mockManager);
		ReflectionTestUtils.setField(worker, "workerLogger", mockWorkerLogger);
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
		//call under test
		worker.run(mockProgressCallback, message);
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
		//call under test
		worker.run(mockProgressCallback, message);
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
		//call under test
		worker.run(mockProgressCallback, message);
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
		//call under test
		worker.run(mockProgressCallback, message);
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
		// This will succeed
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.ENTITY);
		message.setChangeType(ChangeType.UPDATE);
		String successId = "success";
		message.setObjectId(successId);
		// This will fail
		ChangeMessage messageFail = new ChangeMessage();
		messageFail.setObjectType(ObjectType.ENTITY);
		messageFail.setChangeType(ChangeType.UPDATE);
		String failId = "fail";
		messageFail.setObjectId(failId);
		// Simulate a not found
		when(mockManager.updateEntity(failId)).thenThrow(new NotFoundException("NotFound"));
		//call under test
		worker.run(mockProgressCallback, message);
		worker.run(mockProgressCallback, messageFail);
	}
	
	/**
	 * When an unknown exception occurs we should not clear the message from the queue.
	 * @throws Exception
	 */
	@Test
	public void testUnknownException() throws Exception{
		// Test the case where an error occurs and and there is success
		// This will succeed
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.ENTITY);
		message.setChangeType(ChangeType.UPDATE);
		String successId = "success";
		message.setObjectId(successId);
		// This will fail
		ChangeMessage messageFailed = new ChangeMessage();
		messageFailed.setObjectType(ObjectType.ENTITY);
		messageFailed.setChangeType(ChangeType.UPDATE);
		String failId = "fail";
		messageFailed.setObjectId(failId);
		// Simulate a not found
		Exception expectedException = new RuntimeException("Unknown exception");
		when(mockManager.updateEntity(failId)).thenThrow(expectedException);
		//call under test
		worker.run(mockProgressCallback, message);
		// The error message must stay on the queue.
		try {
			worker.run(mockProgressCallback, messageFailed);
			fail("Should have thrown an exception.");
		} catch (RecoverableMessageException e) {
			// expected
		}
		verify(mockWorkerLogger).logWorkerFailure(EntityAnnotationsWorker.class, messageFailed, expectedException, true);
	}
}
