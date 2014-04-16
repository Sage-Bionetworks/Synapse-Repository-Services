package org.sagebionetworks.annotations.worker;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.model.SubmissionStatusAnnotationsAsyncManager;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * The Annotations workers.
 */
public class AnnotationsWorkerFactory implements MessageWorkerFactory{
	
	@Autowired
	private SubmissionStatusAnnotationsAsyncManager ssAsyncMgr;
	
	@Autowired
	private WorkerLogger workerLogger;
	
	@Autowired
	private EvaluationDAO evaluationDAO;

	@Override
	public Callable<List<Message>> createWorker(List<Message> messages) {
		return new AnnotationsWorker(messages, ssAsyncMgr, workerLogger, evaluationDAO);
	}
	
}
