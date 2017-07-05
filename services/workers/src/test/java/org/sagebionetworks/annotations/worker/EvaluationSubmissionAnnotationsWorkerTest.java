package org.sagebionetworks.annotations.worker;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.SubmissionStatusAnnotationsAsyncManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test for AnnotationsWorker
 */
public class EvaluationSubmissionAnnotationsWorkerTest {
	
	SubmissionStatusAnnotationsAsyncManager mockDAO;
	WorkerLogger mockWorkerLogger;
	EvaluationSubmissionAnnotationsWorker worker;
	ProgressCallback mockProgressCallback;
	
	@Before
	public void before(){
		mockDAO = Mockito.mock(SubmissionStatusAnnotationsAsyncManager.class);
		mockWorkerLogger = Mockito.mock(WorkerLogger.class);
		mockProgressCallback = Mockito.mock(ProgressCallback.class);
		worker = new EvaluationSubmissionAnnotationsWorker();
		ReflectionTestUtils.setField(worker, "ssAsyncMgr", mockDAO);
		ReflectionTestUtils.setField(worker, "workerLogger", mockWorkerLogger);
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
		// Make the call
		worker.run(mockProgressCallback, message);
		// the DAO should not be called
		verify(mockDAO, never()).updateEvaluationSubmissionStatuses(any(String.class),any(String.class));
		verify(mockDAO, never()).deleteEvaluationSubmissionStatuses(any(String.class),any(String.class));
	}
	
	@Test
	public void testUpdateSubmissionStatus() throws Exception{
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.EVALUATION_SUBMISSIONS);
		message.setChangeType(ChangeType.UPDATE);
		message.setObjectId("123");
		// Make the call
		worker.run(mockProgressCallback, message);
		// the manager should not be called
		verify(mockDAO).updateEvaluationSubmissionStatuses(eq(message.getObjectId()), anyString());
		verify(mockDAO, never()).deleteEvaluationSubmissionStatuses(eq(message.getObjectId()), anyString());
	}
	
	@Test
	public void testDeleteSubmission() throws Exception{
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.EVALUATION_SUBMISSIONS);
		message.setChangeType(ChangeType.DELETE);
		message.setObjectId("123");
		// Make the call
		worker.run(mockProgressCallback, message);
		// the manager should not be called
		verify(mockDAO, never()).updateEvaluationSubmissionStatuses(eq(message.getObjectId()), anyString());
		verify(mockDAO).deleteEvaluationSubmissionStatuses(any(String.class),any(String.class));
	}
	
	
	/**
	 * When a not found exception is thrown we want to process and remove the message from the queue.
	 * @throws Exception
	 */
	@Test
	public void testNotFound() throws Exception{
		// This will succeed
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.EVALUATION_SUBMISSIONS);
		message.setChangeType(ChangeType.UPDATE);
		String successId = "success";
		message.setObjectId(successId);
		// This will fail
		ChangeMessage message2 = new ChangeMessage();
		message2.setObjectType(ObjectType.EVALUATION_SUBMISSIONS);
		message2.setChangeType(ChangeType.UPDATE);
		String failId = "fail";
		message2.setObjectId(failId);
		// Simulate a not found
		doThrow(new NotFoundException()).when(mockDAO).updateEvaluationSubmissionStatuses(eq(failId), anyString());
		worker.run(mockProgressCallback, message2);
	}
	
	/**
	 * When an unknown exception occurs we should not clear the message from the queue.
	 * @throws Exception
	 */
	@Test
	public void testUnknownException() throws Exception{
		// This will succeed
		ChangeMessage message = new ChangeMessage();
		message.setObjectType(ObjectType.EVALUATION_SUBMISSIONS);
		message.setChangeType(ChangeType.UPDATE);
		String successId = "success";
		message.setObjectId(successId);
		// This will fail
		ChangeMessage message2 = new ChangeMessage();
		message2.setObjectType(ObjectType.EVALUATION_SUBMISSIONS);
		message2.setChangeType(ChangeType.UPDATE);
		String failId = "fail";
		message2.setObjectId(failId);
		// Simulate a runtime exception
		doThrow(new RuntimeException()).when(mockDAO).updateEvaluationSubmissionStatuses(eq(failId), anyString());
		worker.run(mockProgressCallback, message2);
	}
	
	@Test (expected=RecoverableMessageException.class)
	public void testDeadlockException() throws Exception {
		ChangeMessage message = new ChangeMessage();
		message.setChangeType(ChangeType.CREATE);
		message.setObjectEtag("etag");
		message.setObjectId("101");
		message.setObjectType(ObjectType.EVALUATION_SUBMISSIONS);
		doThrow(new TransientDataAccessException("foo", null) {
			private static final long serialVersionUID = 1L;
		}).when(mockDAO).createEvaluationSubmissionStatuses(anyString(), anyString());
		// Make the call
		worker.run(mockProgressCallback, message);
	}
}
