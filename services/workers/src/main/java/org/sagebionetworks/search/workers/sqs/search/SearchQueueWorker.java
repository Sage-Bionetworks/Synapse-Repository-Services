package org.sagebionetworks.search.workers.sqs.search;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.manager.search.SearchManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.search.CloudSearchClientException;
import org.sagebionetworks.search.CloudSearchServerException;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.search.SearchDisabledException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker updates the search index based on messages received
 * 
 * @author John
 * 
 */
public class SearchQueueWorker implements ChangeMessageDrivenRunner {

	static private Logger log = LogManager.getLogger(SearchQueueWorker.class);


	@Autowired
	private WorkerLogger workerLogger;

	@Autowired
	private SearchManager searchManager;


	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage change)
			throws RecoverableMessageException{
		try {
			searchManager.documentChangeMessage(change);
		} catch (SearchDisabledException e){
			// If the feature is disabled then we simply swallow all messages
		} catch (TemporarilyUnavailableException | CloudSearchServerException | IOException e) {
			workerLogger.logWorkerFailure(SearchQueueWorker.class, change, e,true);
			throw new RecoverableMessageException();
		} catch (Exception e){
			workerLogger.logWorkerFailure(SearchQueueWorker.class, change, e,false);
			throw e;
		}
	}



}
