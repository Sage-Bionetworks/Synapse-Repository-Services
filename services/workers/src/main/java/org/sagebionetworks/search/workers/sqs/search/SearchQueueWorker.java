package org.sagebionetworks.search.workers.sqs.search;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.search.SearchManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudsearchdomain.model.AmazonCloudSearchDomainException;

/**
 * This worker updates the search index based on messages received
 * 
 * @author John
 * 
 */
public class SearchQueueWorker implements BatchChangeMessageDrivenRunner {

	static private Logger log = LogManager.getLogger(SearchQueueWorker.class);


	@Autowired
	private WorkerLogger workerLogger;

	@Autowired
	private SearchManager searchManager;


	@Override
	public void run(ProgressCallback progressCallback, List<ChangeMessage> changes)
			throws RecoverableMessageException{
		try {
			searchManager.documentChangeMessages(changes);
		} catch (IllegalStateException e){
			// If the feature is disabled then we simply swallow all messages
		} catch (TemporarilyUnavailableException | AmazonCloudSearchDomainException e) {
			workerLogger.logWorkerFailure(SearchQueueWorker.class.getName(), e,true);
			throw new RecoverableMessageException();
		} catch (Exception e){
			workerLogger.logWorkerFailure(SearchQueueWorker.class.getName(), e,false);
			throw e;
		}
	}



}
