package org.sagebionetworks.search;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

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
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ServiceUnavailableException
	 */
	void createOrUpdateSearchDocument(Document toCreate) throws ClientProtocolException, IOException,
			ServiceUnavailableException;
	 
	 /**
	 * Create or update a batch of search documents
	 * 
	 * @param batch
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ServiceUnavailableException
	 */
	void createOrUpdateSearchDocument(List<Document> batch) throws ClientProtocolException, IOException,
			ServiceUnavailableException;
	 
	 /**
	 * Delete a document using its id.
	 * 
	 * @param documentId
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ServiceUnavailableException
	 */
	void deleteDocument(String docIdToDelete) throws ClientProtocolException, IOException,
			ServiceUnavailableException;
	 
	 /**
	 * Delete all documents with the passed set of document ids.
	 * 
	 * @param docIdsToDelete
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ServiceUnavailableException
	 */
	void deleteDocuments(Set<String> docIdsToDelete) throws ClientProtocolException, IOException,
			ServiceUnavailableException;
	 
	 /**
	 * Execute a query.
	 * 
	 * @param search
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ServiceUnavailableException
	 * @throws CloudSearchClientException 
	 */
	SearchResults executeSearch(String search) throws ClientProtocolException, IOException,
			ServiceUnavailableException, CloudSearchClientException;
	 
	 /**
	 * The unprocessed form of the search
	 * 
	 * @param search
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws ServiceUnavailableException
	 * @throws CloudSearchClientException 
	 */
	String executeRawSearch(String search) throws ClientProtocolException, IOException,
			ServiceUnavailableException, CloudSearchClientException;
	 
	 /**
	 * Does a document already exist with the given id and etag?
	 * 
	 * @param id
	 * @param etag
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ServiceUnavailableException
	 * @throws CloudSearchClientException 
	 */
	boolean doesDocumentExist(String id, String etag) throws ClientProtocolException, IOException,
			ServiceUnavailableException, CloudSearchClientException;
	 
	 /**
	 * List all documents in the search index.
	 * 
	 * @param limit
	 * @param offset
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws ServiceUnavailableException
	 * @throws CloudSearchClientException 
	 */
	SearchResults listSearchDocuments(long limit, long offset) throws ClientProtocolException, IOException,
			ServiceUnavailableException, CloudSearchClientException;
	 
	 /**
	 * Clear all data in the search index.
	 * 
	 * @throws IOException
	 * @throws ClientProtocolException
	 * @throws InterruptedException
	 * @throws ServiceUnavailableException
	 * @throws CloudSearchClientException 
	 */
	void deleteAllDocuments() throws ClientProtocolException, IOException, InterruptedException,
			ServiceUnavailableException, CloudSearchClientException;
	 
	/**
	 * Is the search feature enabled?
	 * @return
	 */
	boolean isSearchEnabled();

}
