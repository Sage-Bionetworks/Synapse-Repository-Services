package org.sagebionetworks.search;

import java.util.Iterator;
import java.util.Set;

import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import org.sagebionetworks.repo.model.search.Document;

/**
 * Abstraction for interacting with the search index.
 * 
 * @author jmhill
 *
 */
public interface SearchDao {

	 /**
	 * Create a new search document.
	 * 
	 * @param toCreate
	 */
	void createOrUpdateSearchDocument(Document toCreate);

	/**
	 * Delete all documents with the passed set of document ids.
	 *
	 * @param docIdsToDelete
	 */
	@Deprecated
	void deleteDocuments(Set<String> docIdsToDelete);

	void sendDocuments(Iterator<Document> documentIterator);

	/**
	 * Execute a query.
	 * 
	 * @param search
	 * @return
	 */
	SearchResult executeSearch(SearchRequest search);


	/**
	 * Does a document already exist with the given id and etag?
	 * 
	 * @param id
	 * @param etag
	 * @return
	 */
	boolean doesDocumentExistInSearchIndex(String id, String etag);

	 
	 /**
	 * Clear all data in the search index.
	 *
	 * @throws InterruptedException
	 */
	void deleteAllDocuments() throws InterruptedException;

}
