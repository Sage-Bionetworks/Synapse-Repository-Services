package org.sagebionetworks.annotations.worker;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.SubmissionStatusAnnotationsAsyncManager;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.dao.TransientDataAccessException;

import com.amazonaws.services.sqs.model.Message;

public class AnnotationsWorkerAutowiredTest {

	// test that a DeadlockLoserDataAccessException will cause the create/update to be retried
	@Test
	public void testDeadlockException() throws Exception {
		Message message = MessageUtils.buildMessage(ChangeType.CREATE, "101", ObjectType.EVALUATION_SUBMISSIONS, "98976");
		List<Message> messages = Collections.singletonList(message);
		WorkerLogger mockWorkerLogger = Mockito.mock(WorkerLogger.class);
		SubmissionStatusAnnotationsAsyncManager ssAsyncMgr = 
				Mockito.mock(SubmissionStatusAnnotationsAsyncManager.class);
		doThrow(new TransientDataAccessException("foo", null) {
			private static final long serialVersionUID = 1L;
		}).when(ssAsyncMgr).createEvaluationSubmissionStatuses(anyString(), anyString());
		AnnotationsWorker worker = new AnnotationsWorker(messages, ssAsyncMgr, mockWorkerLogger);
		List<Message> completedMessages = worker.call();
		// if the message is not in the list it means it is not completed and will be retried
		assertFalse(completedMessages.contains(message));
	}

}
