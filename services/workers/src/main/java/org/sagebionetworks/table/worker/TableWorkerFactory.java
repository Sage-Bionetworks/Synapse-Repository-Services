package org.sagebionetworks.table.worker;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;
import org.sagebionetworks.asynchronous.workers.sqs.WorkerProgress;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class TableWorkerFactory implements MessageWorkerFactory {

	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	StackConfiguration configuration;
	@Autowired
	TableRowManager tableRowManager;	
	
	@Override
	public Callable<List<Message>> createWorker(List<Message> messages, WorkerProgress workerProgress) {
		return new TableWorker(messages, tableConnectionFactory, tableRowManager,  configuration);
	}

}
