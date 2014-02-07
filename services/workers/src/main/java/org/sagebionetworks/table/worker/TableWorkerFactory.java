package org.sagebionetworks.table.worker;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;
import org.sagebionetworks.repo.model.dao.semaphore.SemaphoreDao;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dao.table.TableStatusDAO;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.TableIndexDAOImpl;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

public class TableWorkerFactory implements MessageWorkerFactory {

	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	StackConfiguration configuration;
	@Autowired
	TableRowTruthDAO tableTruthDAO;
	@Autowired
	ColumnModelDAO columnModelDAO;
	@Autowired
	SemaphoreDao semaphoreDao;
	@Autowired
	TableStatusDAO tableStatusDAO;
	
	// This class is not currently a bean since it does not need to be.
	TableIndexDAO tableIndexDAO = new TableIndexDAOImpl();
	
	
	@Override
	public Callable<List<Message>> createWorker(List<Message> messages) {
		return new TableWorker(messages, tableConnectionFactory, tableTruthDAO, columnModelDAO, tableIndexDAO, semaphoreDao, tableStatusDAO, configuration);
	}

}
