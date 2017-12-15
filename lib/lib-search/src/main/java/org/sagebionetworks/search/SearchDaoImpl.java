package org.sagebionetworks.search;

import static org.sagebionetworks.search.SearchConstants.FIELD_ETAG;
import static org.sagebionetworks.search.SearchConstants.FIELD_ID;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.amazonaws.services.cloudsearchdomain.model.QueryParser;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudsearchv2.AmazonCloudSearchClient;
import com.amazonaws.services.cloudsearchv2.model.DomainStatus;

/**
 * Implementation of the Search DAO.
 * 
 * @author jmhill
 * 
 */
public class SearchDaoImpl implements SearchDao {

	private static final String QUERY_BY_ID_AND_ETAG = "(and "+FIELD_ID+":'%1$s' "+FIELD_ETAG+":'%2$s')";
	
	private static final String QUERY_LIST_ALL_DOCUMENTS_ONE_PAGE = FIELD_ID+":'*'";

	static private Logger log = LogManager.getLogger(SearchDaoImpl.class);
	@Autowired
	AmazonCloudSearchClient awsSearchClient;


	//TODO: figure out initialization of this client
	@Autowired
	CloudsSearchDomainClientAdapter cloudSearchClientAdapter;


	


	/**
	 * @throws UnsupportedOperationException when search is disabled.
	 */
	public void validateSearchEnabled(){
		if(!searchDomainSetup.isSearchEnabled()){
			throw new UnsupportedOperationException("Search is disabled");
		}
	}

	@Override
	public void deleteDocument(String documentId) throws ClientProtocolException, IOException,
			ServiceUnavailableException {
		validateSearchEnabled();
		// This is just a batch delete of size one.
		HashSet<String> set = new HashSet<String>(1);
		set.add(documentId);
		deleteDocuments(set);
	}

	@Override
	public void deleteDocuments(Set<String> docIdsToDelete) throws ClientProtocolException, IOException,
			ServiceUnavailableException {
		CloudsSearchDomainClientAdapter searchClient = validateSearchAvailable();
		DateTime now = DateTime.now();
		// Note that we cannot use a JSONEntity here because the format is
		// just a JSON array
		JSONArray documentBatch = new JSONArray();
		for (String entityId : docIdsToDelete) {
			Document document = new Document();
			document.setType(DocumentTypeNames.delete);
			document.setId(entityId);
			document.setVersion(now.getMillis() / 1000);
			try {
				documentBatch.put(EntityFactory.createJSONObjectForEntity(document));
			} catch (JSONObjectAdapterException e) {
				// Convert to runtime
				throw new RuntimeException(e);
			}
		}
		// Delete the batch.
		searchClient.sendDocuments(documentBatch.toString());
	}

	@Override
	public SearchResults executeSearch(SearchRequest search) throws ClientProtocolException, IOException,
			ServiceUnavailableException, CloudSearchClientException {
		CloudsSearchDomainClientAdapter searchClient = validateSearchAvailable();
		return searchClient.rawSearch(search);
	}

	@Override
	public boolean doesDocumentExist(String id, String etag) throws ClientProtocolException, IOException,
			ServiceUnavailableException, CloudSearchClientException {
		validateSearchEnabled();
		// Search for the document
		String query = String.format(QUERY_BY_ID_AND_ETAG, id, etag);
		SearchResults results = executeSearch(new SearchRequest().withQuery(query).withQueryParser(QueryParser.Structured));
		return results.getHits().size() > 0;
	}

	@Override
	public SearchResults listSearchDocuments(long limit, long offset) throws ClientProtocolException, IOException,
			ServiceUnavailableException, CloudSearchClientException {
		validateSearchEnabled();
		return executeSearch(new SearchRequest().withQuery(QUERY_LIST_ALL_DOCUMENTS_ONE_PAGE)
												.withQueryParser(QueryParser.Structured)
												.withSize(limit).withStart(offset));
	}

	@Override
	public void deleteAllDocuments() throws ClientProtocolException, IOException, InterruptedException,
			ServiceUnavailableException, CloudSearchClientException {
		validateSearchEnabled();
		// Keep deleting as long as there are documents
		SearchResults sr = null;
		do{
			sr = listSearchDocuments(1000, 0);
			HashSet<String> idSet = new HashSet<String>();
			for(Hit hit: sr.getHits()){
				idSet.add(hit.getId());
			}
			// Delete the whole set
			if(!idSet.isEmpty()){
				log.warn("Deleting the following documents from the search index:"+idSet.toString());
				deleteDocuments(idSet);
				Thread.sleep(5000);
			}
		}while(sr.getFound() > 0);
	}

	@Override
	public boolean isSearchEnabled() {
		return searchDomainSetup.isSearchEnabled();
	}

	private CloudsSearchDomainClientAdapter validateSearchAvailable() throws ServiceUnavailableException {
		validateSearchEnabled();

		DomainStatus status = searchDomainSetup.getDomainStatus();
		if (status == null) {
			throw new ServiceUnavailableException("Search service not initialized...");
		} else {
			cloudSearchClientAdapter.setEndpoint(searchDomainSetup.getDomainSearchEndpoint());
			if (status.isProcessing()) {
				throw new ServiceUnavailableException("Search service processing...");
			}
		}

		return cloudSearchClientAdapter;
	}
}
