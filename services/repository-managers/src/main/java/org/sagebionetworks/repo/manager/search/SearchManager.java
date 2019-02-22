package org.sagebionetworks.repo.manager.search;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;

import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;

public interface SearchManager {

	/**
	 * Perform the search defined in the SearchQuery on behalf of the user
	 * @param userInfo user performing the search
	 * @param searchQuery defines the search
	 * @return the results of the search
	 */
	SearchResults proxySearch(UserInfo userInfo, SearchQuery searchQuery);

	/**
	 * Performs a raw search using AWS's SearchRequest. Used in tests
	 * @param searchRequest
	 * @return AWS search Result
	 */
	SearchResult rawSearch (SearchRequest searchRequest);

	/**
	 * Deletes all documents in the CloudSearch Domain
	 * @throws InterruptedException
	 */
	void deleteAllDocuments() throws InterruptedException;

	/**
	 * Returns whether a document exists for a given Synapse id and etag
	 * @param id id of the Synapse entity
	 * @param etag optional value representing etag of the Synapse entity(can be null)
	 * @return true if a document exists, false otherwise.
	 */
	boolean doesDocumentExist(String id, String etag);

	/**
	 * Creates a search document. If another document with the same ID exists, the existing document will be overwritten with
	 * the new document
	 * @param document the document to create
	 */
	void createOrUpdateSearchDocument(Document document);

	/**
	 * Creates a document based on Entity or Wiki changes that occurred in Synapse. Used by SearchQueueWorker.
	 * @param changeMessages a batch of ChangeMessages representing changes in Synapse
	 */
	void documentChangeMessages(List<ChangeMessage> changeMessages);
}
