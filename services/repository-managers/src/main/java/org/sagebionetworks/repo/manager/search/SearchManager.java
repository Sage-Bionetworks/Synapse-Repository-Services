package org.sagebionetworks.repo.manager.search;

import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;

import java.io.IOException;

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
	 * Creates a document based on an Entity or Wiki change that occurred in Synapse. Used by SearchQueueWorker.
	 * @param change a ChangeMessage representing a change in Synapse
	 * @throws IOException
	 */
	void documentChangeMessage(ChangeMessage change) throws IOException;
}
