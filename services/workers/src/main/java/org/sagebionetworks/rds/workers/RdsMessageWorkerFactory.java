package org.sagebionetworks.rds.workers;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * The RDS message workers.
 * 
 * @author jmhill
 *
 */
public class RdsMessageWorkerFactory implements MessageWorkerFactory{
	
	@Autowired
	AsynchronousDAO asynchronousDAO;

	@Override
	public Callable<List<Message>> createWorker(List<Message> messages) {
		return new RdsWorker(messages, asynchronousDAO);
	}

}
