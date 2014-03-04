package org.sagebionetworks.search.workers.sqs.search;

import java.util.List;
import java.util.concurrent.Callable;

import org.sagebionetworks.asynchronous.workers.sqs.MessageWorkerFactory;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.search.SearchDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.amazonaws.services.sqs.model.Message;

/**
 * This factory is auto-wired so the workers can be created on the fly.
 * @author John
 *
 */
public class SearchQueueWorkerFactory implements MessageWorkerFactory {
	
	@Autowired
	private SearchDao searchDao;

	@Autowired
	private SearchDocumentDriver searchDocumentDriver;
	
	@Autowired
	private V2WikiPageDao wikPageDao;
	
	public void initialize(){
	}

	@Override
	public Callable<List<Message>> createWorker(List<Message> messages) {
		// Create a new worker
		return new SearchQueueWorker(searchDao, searchDocumentDriver, messages, wikPageDao);
	}

}
