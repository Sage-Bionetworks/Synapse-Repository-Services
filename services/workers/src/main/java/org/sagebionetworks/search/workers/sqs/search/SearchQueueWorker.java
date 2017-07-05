package org.sagebionetworks.search.workers.sqs.search;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.search.CloudSearchClientException;
import org.sagebionetworks.search.SearchDao;
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
	private SearchDao searchDao;

	@Autowired
	private SearchDocumentDriver searchDocumentDriver;

	@Autowired
	private V2WikiPageDao wikPageDao;

	@Autowired
	private WorkerLogger workerLogger;

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage change)
			throws RecoverableMessageException, Exception {
		// If the feature is disabled then we simply swallow all messages
		if (!searchDao.isSearchEnabled()) {
			return;
		}
		// We only care about entity messages as this time
		if (ObjectType.ENTITY == change.getObjectType()) {
			// Is this a create or update
			if (ChangeType.CREATE == change.getChangeType()
					|| ChangeType.UPDATE == change.getChangeType()) {
				processCreateUpdate(change);
			} else if (ChangeType.DELETE == change.getChangeType()) {
				processDelete(change);
			} else {
				throw new IllegalArgumentException("Unknown change type: "
						+ change.getChangeType());
			}
		}
		// Is this a wikipage?
		if (ObjectType.WIKI == change.getObjectType()) {
			// Lookup the owner of the page
			try {
				WikiPageKey key = wikPageDao
						.lookupWikiKey(change.getObjectId());
				// If the owner of the wiki is a an entity then pass along the
				// message.
				if (ObjectType.ENTITY == key.getOwnerObjectType()) {
					// We need the current document etag
					ChangeMessage newMessage = new ChangeMessage();
					newMessage.setChangeType(ChangeType.UPDATE);
					newMessage.setObjectId(key.getOwnerObjectId());
					newMessage.setObjectType(ObjectType.ENTITY);
					newMessage.setObjectEtag(null);
					processCreateUpdate(newMessage);
				}
			} catch (NotFoundException e) {
				// Nothing to do if the wiki does not exist
				log.debug("Wiki not found for id: " + change.getObjectId()
						+ " Message:" + e.getMessage());
			}
		}
	}

	/**
	 * A single delete
	 * 
	 * @param message
	 * @throws RecoverableMessageException
	 */
	private void processDelete(ChangeMessage message)
			throws RecoverableMessageException {
		try {
			searchDao.deleteDocument(message.getObjectId());
		} catch (Throwable e) {
			workerLogger.logWorkerFailure(SearchQueueWorker.class, message, e,
					true);
			throw new RecoverableMessageException();
		}
	}

	public void processCreateUpdate(ChangeMessage change)
			throws RecoverableMessageException {
		try {
			Document newDoc = getDocFromMessage(change);
			if (newDoc != null) {
				searchDao.createOrUpdateSearchDocument(newDoc);
			}
		} catch (Throwable e) {
			workerLogger.logWorkerFailure(SearchQueueWorker.class, change, e,
					true);
			throw new RecoverableMessageException();
		}
	}

	private Document getDocFromMessage(ChangeMessage changeMessage)
			throws ClientProtocolException, DatastoreException, IOException,
			ServiceUnavailableException, CloudSearchClientException {
		// We want to ignore this message if a document with this ID and Etag
		// already exists in the search index.
		if (!searchDao.doesDocumentExist(changeMessage.getObjectId(),
				changeMessage.getObjectEtag())) {
			// We want to ignore this message if a document with this ID and
			// Etag are not in the repository as it is an
			// old message.
			if (changeMessage.getObjectEtag() == null
					|| searchDocumentDriver.doesDocumentExist(
							changeMessage.getObjectId(),
							changeMessage.getObjectEtag())) {
				try {
					return searchDocumentDriver
							.formulateSearchDocument(changeMessage
									.getObjectId());
				} catch (NotFoundException e) {
					// There is nothing to do if it does not exist
					log.debug("Node not found for id: "
							+ changeMessage.getObjectId() + " Message:"
							+ e.getMessage());
				}
			}
		}
		return null;
	}

}
