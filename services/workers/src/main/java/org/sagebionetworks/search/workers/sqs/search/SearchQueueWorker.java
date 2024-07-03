package org.sagebionetworks.search.workers.sqs.search;

import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.search.SearchManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.cloudsearchdomain.model.AmazonCloudSearchDomainException;

/**
 * This worker updates the search index based on messages received
 * 
 * @author John
 * 
 */
@Service
public class SearchQueueWorker implements BatchChangeMessageDrivenRunner {

	@Autowired
	private WorkerLogger workerLogger;

	@Autowired
	private SearchManager searchManager;

	@Override
	public void run(ProgressCallback progressCallback, List<ChangeMessage> changes)
			throws RecoverableMessageException{
		List<ChangeMessage> entitiesToBeSearched = changes.stream()
				.filter(m -> ChangeType.CREATE.equals(m.getChangeType())
						|| ChangeType.UPDATE.equals(m.getChangeType())
						|| (ChangeType.DELETE.equals(m.getChangeType()) && m.getObjectVersion() == null))
				.collect(Collectors.toList());

		try {
			searchManager.documentChangeMessages(entitiesToBeSearched);
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
