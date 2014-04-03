package org.sagebionetworks.file.worker;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.file.preview.PreviewManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * The factory is an auto-wired singleton used to create workers at runtime.
 *  
 * @author John
 *
 */
public class FileMessageWorkerFactory implements MessageWorkerFactory {

	@Autowired
	PreviewManager previewManager;
	
	@Autowired
	WorkerLogger workerLogger;

	@Override
	public Callable<List<Message>> createWorker(List<Message> messages) {
		// Create a new worker.
		return new PreviewWorker(previewManager, messages, workerLogger);
	}

}
