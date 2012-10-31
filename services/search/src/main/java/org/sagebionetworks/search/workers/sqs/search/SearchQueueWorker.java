package org.sagebionetworks.search.workers.sqs.search;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.ClientProtocolException;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.repo.manager.search.SearchDocumentDriver;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.search.SearchDao;
import org.sagebionetworks.utils.HttpClientHelperException;

import com.amazonaws.services.sqs.model.Message;

/**
 * This worker updates the search index based on messages recieved
 * 
 * @author John
 *
 */
public class SearchQueueWorker implements Callable<List<Message>> {
	
	static private Log log = LogFactory.getLog(SearchQueueWorker.class);
	
	private SearchDao searchDao;
	private SearchDocumentDriver documentProvider;
	private List<Message> messagesToProcess;
	
	private List<ChangeMessage> createOrUpdateMessages;
	private List<ChangeMessage> deleteMessages;
	

	/**
	 * Create a new worker to process jobs
	 * @param searchDao
	 * @param nodeWorkerManager
	 * @param messagesToProcess
	 */
	public SearchQueueWorker(SearchDao searchDao, SearchDocumentDriver documentProvider, List<Message> messagesToProcess) {
		if(searchDao == null) throw new IllegalArgumentException("SearchDao canot be null");
		if(documentProvider == null) throw new IllegalArgumentException("SearchDocumentDriver cannot be null");
		if(messagesToProcess == null) throw new IllegalArgumentException("messagesToProcess cannot be null");
		this.searchDao = searchDao;
		this.documentProvider = documentProvider;
		this.messagesToProcess = messagesToProcess;
	}

	@Override
	public List<Message> call() throws Exception {
		// Process each message
		for(Message message: messagesToProcess){
			// Extract the ChangeMessage
			ChangeMessage change = MessageUtils.extractMessageBody(message);
			// We only care about entity messages as this time
			if(ObjectType.ENTITY == change.getObjectType()){
				// Is this a create or update
				if(ChangeType.CREATE == change.getChangeType() || ChangeType.UPDATE == change.getChangeType()){
					addCreateOrUpdateEntity(change);
				}else if(ChangeType.DELETE == change.getChangeType()){
					addDeleteEntity(change);
				}else{
					throw new IllegalArgumentException("Unknown change type: "+change.getChangeType()+" for messageID =" +message.getMessageId());
				}
			}
		}
		processCreateUpdateBatch();
		processDeleteBatch();
		return messagesToProcess;
	}

	/**
	 * Add a create or update message to the batch.
	 * @param change
	 */
	private void addCreateOrUpdateEntity(ChangeMessage change){
		if(createOrUpdateMessages == null){
			createOrUpdateMessages = new LinkedList<ChangeMessage>();
		}
		createOrUpdateMessages.add(change);
	}
	/**
	 * Add a delete message to the batch.
	 * @param change
	 */
	private void addDeleteEntity(ChangeMessage change){
		if(deleteMessages == null){
			deleteMessages = new LinkedList<ChangeMessage>();
		}
		deleteMessages.add(change);
	}
	
	private void processDeleteBatch() throws ClientProtocolException, IOException, HttpClientHelperException {
		if(deleteMessages != null){
			log.debug("Processing "+deleteMessages.size()+" delete messages");
			Set<String> toDelete = new HashSet<String>();
			for(ChangeMessage message: deleteMessages){
				toDelete.add(message.getObjectId());
			}
			// Delete the batch
			searchDao.deleteDocuments(toDelete);
		}
	}

	/**
	 * Send the documents to search.
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 */
	private void processCreateUpdateBatch() throws DatastoreException, NotFoundException, ClientProtocolException, IOException, HttpClientHelperException {
		if(createOrUpdateMessages != null){
			log.debug("Processing "+createOrUpdateMessages.size()+" create/update messages");
			// Prepare a batch of documents
			List<Document> batch = new LinkedList<Document>();
			for(ChangeMessage message: createOrUpdateMessages){
				// We want to ignore this message if a document with this ID and Etag already exists in the search index.
				if(!searchDao.doesDocumentExist(message.getObjectId(), message.getObjectEtag())){
					// We want to ignore this message if a document with this ID and Etag are not in the repository as it is an old message.
					if(documentProvider.doesDocumentExist(message.getObjectId(), message.getObjectEtag())){
						// Create a document for this
						Document newDoc = documentProvider.formulateSearchDocument(message.getObjectId());
						batch.add(newDoc);
					}
				}
			}
			if(!batch.isEmpty()){
				searchDao.createOrUpdateSearchDocument(batch);
			}
		}
	}

}
