package org.sagebionetworks.search;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.utils.HttpClientHelperException;

import com.amazonaws.services.cloudsearch.model.AccessPoliciesStatus;
import com.amazonaws.services.cloudsearch.model.DomainStatus;
import com.amazonaws.services.cloudsearch.model.IndexFieldStatus;

/**
 * Abstraction for interacting with the search index.
 * 
 * @author jmhill
 *
 */
public interface SearchDao {
	
	/**
	 * The name of of the search domain.
	 * @return
	 */
	String getSearchDomainName();
	
	/**
	 * Get the status of the domain.
	 * @return
	 */
	DomainStatus getDomainStatus();

	/**
	 * The list of all IndexFields and their status.
	 * @return
	 */
	List<IndexFieldStatus> getIndexFieldStatus();
	
	/**
	 * The JSON of the current access policy.
	 * @return
	 */
	 AccessPoliciesStatus getAccessPoliciesStatus();
	 
	 /**
	  * Create a new search document.
	  * @param toCreate
	 * @throws HttpClientHelperException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	  */
	 void createOrUpdateSearchDocument(Document toCreate) throws ClientProtocolException, IOException, HttpClientHelperException;
	 
	 /**
	  * Create or update a batch of search documents
	  * @param batch
	 * @throws HttpClientHelperException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	  */
	 void createOrUpdateSearchDocument(List<Document> batch) throws ClientProtocolException, IOException, HttpClientHelperException;
	 
	 /**
	  * Delete a document using its id.
	  * @param documentId
	 * @throws HttpClientHelperException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	  */
	 void deleteDocument(String docIdToDelete) throws ClientProtocolException, IOException, HttpClientHelperException;
	 
	 /**
	  * Delete all documents with the passed set of document ids.
	  * 
	  * @param docIdsToDelete
	 * @throws HttpClientHelperException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	  */
	 void deleteDocuments(Set<String> docIdsToDelete) throws ClientProtocolException, IOException, HttpClientHelperException;
	 
	 /**
	  * Execute a query.
	  * @param search
	  * @return
	 * @throws HttpClientHelperException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	  */
	 SearchResults executeSearch(String search) throws ClientProtocolException, IOException, HttpClientHelperException;
	 
	 /**
	  * The unprocessed form of the search
	  * @param search
	  * @return
	  * @throws ClientProtocolException
	  * @throws IOException
	  * @throws HttpClientHelperException
	  */
	 String executeRawSearch(String search) throws ClientProtocolException, IOException, HttpClientHelperException;
	 
	 /**
	  * Does a document already exist with the given id and etag?
	  * @param id
	  * @param etag
	  * @return
	 * @throws HttpClientHelperException 
	 * @throws IOException 
	 * @throws ClientProtocolException 
	  */
	 boolean doesDocumentExist(String id, String etag) throws ClientProtocolException, IOException, HttpClientHelperException;

}
