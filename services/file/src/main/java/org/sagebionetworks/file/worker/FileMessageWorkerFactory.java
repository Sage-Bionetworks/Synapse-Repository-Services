package org.sagebionetworks.file.worker;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
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
	FileMetadataDao fileMetadataDao;
	
	@Override
	public Callable<List<Message>> createWorker(List<Message> messages) {
		// Create a new worker.
		return new PreviewWorker(fileMetadataDao, messages);
	}

}
