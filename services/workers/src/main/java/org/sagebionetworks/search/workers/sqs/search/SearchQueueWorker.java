package org.sagebionetworks.search.workers.sqs.search;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils.MessageBundle;
import org.sagebionetworks.cloudwatch.WorkerLogger;
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
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.utils.HttpClientHelperException;

import com.amazonaws.services.sqs.model.Message;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * This worker updates the search index based on messages recieved
 * 
 * @author John
 *
 */
public class SearchQueueWorker implements Callable<List<Message>> {
	
	static private Logger log = LogManager.getLogger(SearchQueueWorker.class);
	
	private SearchDao searchDao;
	private SearchDocumentDriver documentProvider;
	private List<Message> messagesToProcess;
	private V2WikiPageDao wikiPageDao;
	
	WorkerLogger workerLogger;

	/**
	 * Create a new worker to process jobs
	 * @param searchDao
	 * @param nodeWorkerManager
	 * @param messagesToProcess
	 */
	public SearchQueueWorker(SearchDao searchDao, SearchDocumentDriver documentProvider, List<Message> messagesToProcess, V2WikiPageDao wikiPageDao, WorkerLogger workerProfiler) {
		if(searchDao == null) throw new IllegalArgumentException("SearchDao canot be null");
		if(documentProvider == null) throw new IllegalArgumentException("SearchDocumentDriver cannot be null");
		if(messagesToProcess == null) throw new IllegalArgumentException("messagesToProcess cannot be null");
		this.searchDao = searchDao;
		this.documentProvider = documentProvider;
		this.messagesToProcess = messagesToProcess;
		this.wikiPageDao = wikiPageDao;
		this.workerLogger = workerProfiler;
	}

	@Override
	public List<Message> call() throws Exception {
		// If the feature is disabled then we simply swallow all messages
		if (!searchDao.isSearchEnabled()) {
			return messagesToProcess;
		}
		List<MessageBundle> createOrUpdateMessages = Lists.newLinkedList();
		List<MessageBundle> deleteMessages = Lists.newLinkedList();

		// Process each message
		for(Message message: messagesToProcess){
			// Extract the ChangeMessage
			MessageBundle change = MessageUtils.extractMessageBundle(message);
			// We only care about entity messages as this time
			if (ObjectType.ENTITY == change.getChangeMessage().getObjectType()) {
				// Is this a create or update
				if (ChangeType.CREATE == change.getChangeMessage().getChangeType()
						|| ChangeType.UPDATE == change.getChangeMessage().getChangeType()) {
					createOrUpdateMessages.add(change);
				} else if (ChangeType.DELETE == change.getChangeMessage().getChangeType()) {
					deleteMessages.add(change);
				}else{
					throw new IllegalArgumentException("Unknown change type: " + change.getChangeMessage().getChangeType()
							+ " for messageID =" + message.getMessageId());
				}
			}
			// Is this a wikipage?
			if (ObjectType.WIKI == change.getChangeMessage().getObjectType()) {
				// Lookup the owner of the page
				try{
					WikiPageKey key = wikiPageDao.lookupWikiKey(change.getChangeMessage().getObjectId());
					// If the owner of the wiki is a an entity then pass along the message.
					if(ObjectType.ENTITY == key.getOwnerObjectType()){
						// We need the current document etag
						ChangeMessage newMessage = new ChangeMessage();
						newMessage.setChangeType(ChangeType.UPDATE);
						newMessage.setObjectId(key.getOwnerObjectId());
						newMessage.setObjectType(ObjectType.ENTITY);
						newMessage.setObjectEtag(null);
						createOrUpdateMessages.add(new MessageBundle(message, newMessage));
					}
				}catch(NotFoundException e){
					// Nothing to do if the wiki does not exist
					log.debug("Wiki not found for id: " + change.getChangeMessage().getObjectId() + " Message:" + e.getMessage());
				}
			}
		}

		// assume all messages handled, unless removed from this set
		Set<Message> messagesHandled = Sets.newHashSet(messagesToProcess);

		processCreateUpdateBatch(createOrUpdateMessages, messagesHandled);
		processDeleteBatch(deleteMessages, messagesHandled);
		return Lists.newArrayList(messagesHandled);
	}

	private void processDeleteBatch(List<MessageBundle> deleteMessages, Set<Message> messagesHandled) throws ClientProtocolException,
			IOException, HttpClientHelperException {
		if (!deleteMessages.isEmpty()) {
			log.debug("Processing "+deleteMessages.size()+" delete messages");
			Set<String> toDelete = new HashSet<String>();
			for (MessageBundle messageBundle : deleteMessages) {
				toDelete.add(messageBundle.getChangeMessage().getObjectId());
			}
			// Delete the batch
			try {
				searchDao.deleteDocuments(toDelete);
			} catch (Throwable e) {
				processDeleteBatchAsSingle(deleteMessages, messagesHandled);
			}
		}
	}
	
	private void processDeleteBatchAsSingle(List<MessageBundle> deleteMessages, Set<Message> messagesHandled) {
		log.debug("Re-processing delete batch as single messages");
		for (MessageBundle messageBundle : deleteMessages) {
			try {
				searchDao.deleteDocument(messageBundle.getChangeMessage().getObjectId());
			} catch (Throwable e) {
				workerLogger.logWorkerFailure(SearchQueueWorker.class, messageBundle.getChangeMessage(), e, true);
				messagesHandled.remove(messageBundle.getMessage());
			}
		}
	}

	/**
	 * Send the documents to search.
	 * 
	 * @param messagesHandled
	 * @param createOrUpdateMessages
	 * 
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 * @throws ServiceUnavailableException
	 */
	private void processCreateUpdateBatch(List<MessageBundle> createOrUpdateMessages, Set<Message> messagesHandled)
			throws DatastoreException, ClientProtocolException, IOException, HttpClientHelperException, ServiceUnavailableException {
		if (!createOrUpdateMessages.isEmpty()) {
			log.debug("Processing "+createOrUpdateMessages.size()+" create/update messages");
			// Prepare a batch of documents
			List<Document> batch = new LinkedList<Document>();
			for (MessageBundle messageBundle : createOrUpdateMessages) {
				Document newDoc = getDocFromMessage(messageBundle.getChangeMessage());
				if (newDoc != null) {
					batch.add(newDoc);
				}
			}
			if(!batch.isEmpty()){
				try {
					searchDao.createOrUpdateSearchDocument(batch);
				} catch (Throwable e) {
					processCreateUpdateBatchAsSingle(createOrUpdateMessages, messagesHandled);
				}
			}
		}
	}

	private void processCreateUpdateBatchAsSingle(List<MessageBundle> createOrUpdateMessages, Set<Message> messagesHandled) {
		log.debug("Re-processing createUpdate batch as single messages");
		for (MessageBundle messageBundle : createOrUpdateMessages) {
			try {
				Document newDoc = getDocFromMessage(messageBundle.getChangeMessage());
				if (newDoc != null) {
					searchDao.createOrUpdateSearchDocument(newDoc);
				}
			} catch (Throwable e) {
				workerLogger.logWorkerFailure(SearchQueueWorker.class, messageBundle.getChangeMessage(), e, true);
				messagesHandled.remove(messageBundle.getMessage());
			}
		}
	}

	private Document getDocFromMessage(ChangeMessage changeMessage) throws ClientProtocolException, DatastoreException, IOException,
			HttpClientHelperException, ServiceUnavailableException {
		// We want to ignore this message if a document with this ID and Etag already exists in the search index.
		if (!searchDao.doesDocumentExist(changeMessage.getObjectId(), changeMessage.getObjectEtag())) {
			// We want to ignore this message if a document with this ID and Etag are not in the repository as it is an
			// old message.
			if (changeMessage.getObjectEtag() == null
					|| documentProvider.doesDocumentExist(changeMessage.getObjectId(), changeMessage.getObjectEtag())) {
				try {
					return documentProvider.formulateSearchDocument(changeMessage.getObjectId());
				} catch (NotFoundException e) {
					// There is nothing to do if it does not exist
					log.debug("Node not found for id: " + changeMessage.getObjectId() + " Message:" + e.getMessage());
				}
			}
		}
		return null;
	}
}
