package org.sagebionetworks.search;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.utils.HttpClientHelperException;

/**
 * Abstraction for interacting with the search index.
 * 
 * @author jmhill
 *
 */
public interface SearchDao {
	
	/**
	 * Called by initializing worker. This method should check, initialize where necessary and return relatively quickly
	 * (i.e. no long waits)
	 * 
	 * @return true when post initialization is done
	 */
	public boolean postInitialize() throws Exception;

	 /**
	 * Create a new search document.
	 * 
	 * @param toCreate
	 * @throws HttpClientHelperException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ServiceUnavailableException
	 */
	void createOrUpdateSearchDocument(Document toCreate) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException;
	 
	 /**
	 * Create or update a batch of search documents
	 * 
	 * @param batch
	 * @throws HttpClientHelperException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ServiceUnavailableException
	 */
	void createOrUpdateSearchDocument(List<Document> batch) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException;
	 
	 /**
	 * Delete a document using its id.
	 * 
	 * @param documentId
	 * @throws HttpClientHelperException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ServiceUnavailableException
	 */
	void deleteDocument(String docIdToDelete) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException;
	 
	 /**
	 * Delete all documents with the passed set of document ids.
	 * 
	 * @param docIdsToDelete
	 * @throws HttpClientHelperException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ServiceUnavailableException
	 */
	void deleteDocuments(Set<String> docIdsToDelete) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException;
	 
	 /**
	 * Execute a query.
	 * 
	 * @param search
	 * @return
	 * @throws HttpClientHelperException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ServiceUnavailableException
	 */
	SearchResults executeSearch(String search) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException;
	 
	 /**
	 * The unprocessed form of the search
	 * 
	 * @param search
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws HttpClientHelperException
	 * @throws ServiceUnavailableException
	 */
	String executeRawSearch(String search) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException;
	 
	 /**
	 * Does a document already exist with the given id and etag?
	 * 
	 * @param id
	 * @param etag
	 * @return
	 * @throws HttpClientHelperException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ServiceUnavailableException
	 */
	boolean doesDocumentExist(String id, String etag) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException;
	 
	 /**
	 * List all documents in the search index.
	 * 
	 * @param limit
	 * @param offset
	 * @return
	 * @throws HttpClientHelperException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ServiceUnavailableException
	 */
	SearchResults listSearchDocuments(long limit, long offset) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException;
	 
	 /**
	 * Clear all data in the search index.
	 * 
	 * @throws HttpClientHelperException
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws InterruptedException
	 * @throws ServiceUnavailableException
	 */
	void deleteAllDocuments() throws ClientProtocolException, IOException, HttpClientHelperException, InterruptedException,
			ServiceUnavailableException;
	 
	/**
	 * Is the search feature enabled?
	 * @return
	 */
	boolean isSearchEnabled();

}
