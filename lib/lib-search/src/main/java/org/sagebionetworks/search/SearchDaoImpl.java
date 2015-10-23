package org.sagebionetworks.search;

import static org.sagebionetworks.search.SearchConstants.FIELD_ETAG;
import static org.sagebionetworks.search.SearchConstants.FIELD_ID;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.sagebionetworks.repo.model.search.AwesomeSearchFactory;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.search.Hit;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.AdapterFactoryImpl;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudsearch.AmazonCloudSearchClient;

/**
 * Implementation of the Search DAO.
 * 
 * @author jmhill
 * 
 */
public class SearchDaoImpl implements SearchDao {

	private static final String QUERY_BY_ID_AND_ETAG = "bq=(and+"+FIELD_ID+":'%1$s'+"+FIELD_ETAG+":'%2$s')";
	
	private static final String QUERY_LIST_ALL_DOCUMENTS_ONE_PAGE = "bq="+FIELD_ID+":'*'&size=%1$s&start=%2$s";

	static private Logger log = LogManager.getLogger(SearchDaoImpl.class);
	
	private static final AwesomeSearchFactory searchResultsFactory = new AwesomeSearchFactory(new AdapterFactoryImpl());
	
	@Autowired
	AmazonCloudSearchClient awsSearchClient;
	@Autowired
	SearchDomainSetup searchDomainSetup;
	@Autowired
	CloudSearchClient cloudHttpClient = null;
	
	@Override
	public boolean postInitialize() throws Exception {
		if (!searchDomainSetup.isSearchEnabled()) {
			log.info("SearchDaoImpl.initialize() will do nothing since search is disabled");
			return true;
		}

		if (!searchDomainSetup.postInitialize()) {
			return false;
		}

		String searchEndPoint = searchDomainSetup.getSearchEndpoint();
		log.info("Search endpoint: " + searchEndPoint);
		cloudHttpClient.setSearchServiceEndpoint(searchEndPoint);
		String documentEndPoint = searchDomainSetup.getDocumentEndpoint();
		log.info("Document endpoint: " + documentEndPoint);
		cloudHttpClient.setDocumentServiceEndpoint(documentEndPoint);
		//cloudHttpClient = new CloudSearchClient(searchEndPoint,	documentEndPoint);
		//cloudHttpClient._init();
		return true;
	}
	
	/**
	 * The initialization of a search index can take hours the first time it is run.
	 * While the search index is initializing we do not want to block the startup of the rest
	 * of the application.  Therefore, this initialization worker is executed on a separate
	 * thread.
	 */
	public void initialize(){
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try{
					/*
					 * Since each machine in the cluster will call this method and we only 
					 * want one machine to initialize the search index, we randomly stagger
					 * the start for each machine.
					 */
					Random random = new Random();
					// random sleep time from zero to 1 mins.
					long randomSleepMS = random.nextInt(1000*60);
					log.info("Random wait to start search index: "+randomSleepMS+" MS");
					Thread.sleep(randomSleepMS);
					// wait for postInitialize() to finish
					while(!postInitialize()){
						log.info("Waiting for search index to finish initializing...");
						Thread.sleep(5000);
					}
				}catch(Exception e){
					log.error("Unexcpeted exception while starting the search index", e);
				}
			}
		});
		thread.start();
	}

	/**
	 * @throws UnsupportedOperationException when search is disabled.
	 */
	public void validateSearchEnabled(){
		if(!searchDomainSetup.isSearchEnabled()){
			throw new UnsupportedOperationException("Search is disabled");
		}
	}
	
	@Override
	public void createOrUpdateSearchDocument(Document document) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException {
		validateSearchEnabled();
		if(document == null) throw new IllegalArgumentException("Document cannot be null");
		List<Document> list = new LinkedList<Document>();
		list.add(document);
		createOrUpdateSearchDocument(list);
	}
	
	@Override
	public void createOrUpdateSearchDocument(List<Document> batch) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException {
		CloudSearchClient searchClient = validateSearchAvailable();
		// Cleanup they data
		byte[] bytes = cleanSearchDocuments(batch);
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		// Pass along to the client
		searchClient.sendDocuments(in, bytes.length);
	}
	
	/**
	 * Remove any character that is not compatible with cloud search.
	 * @param document
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws JSONObjectAdapterException
	 */
	static byte[] cleanSearchDocuments(List<Document> documents) {
		String serializedDocument;
		try {
			StringBuilder builder = new StringBuilder();
			builder.append("[");
			int count = 0;
			for(Document document: documents){
				prepareDocument(document);
				serializedDocument = EntityFactory.createJSONStringForEntity(document);
				// AwesomeSearch pukes on control characters. Some descriptions have
				// control characters in them for some reason, in any case, just get rid
				// of all control characters in the search document
				String cleanedDocument = serializedDocument.replaceAll("\\p{Cc}", "");

				// Get rid of escaped control characters too
				cleanedDocument = cleanedDocument.replaceAll("\\\\u00[0,1][0-9,a-f]","");
				if(count > 0){
					builder.append(", ");
				}
				builder.append(cleanedDocument);
				count++;
			}
			builder.append("]");
//			log.warn(builder.toString());
			// AwesomeSearch expects UTF-8
			return builder.toString().getBytes("UTF-8");
		} catch (JSONObjectAdapterException e) {
			// Convert to runtime
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			// Convert to runtime
			throw new RuntimeException(e);
		}
	}

	/**
	 * Prepare the document to be sent.
	 * @param document
	 */
	public static void prepareDocument(Document document) {
		// the version is always the current time.
		DateTime now = DateTime.now();
		document.setVersion(now.getMillis() / 1000);
		document.setType(DocumentTypeNames.add);
		document.setLang("en");
		if(document.getFields() == null){
			document.setFields(new DocumentFields());
		}
		// The id field must match the document's id.
		document.getFields().setId(document.getId());
	}

	@Override
	public void deleteDocument(String documentId) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException {
		validateSearchEnabled();
		// This is just a batch delete of size one.
		HashSet<String> set = new HashSet<String>(1);
		set.add(documentId);
		deleteDocuments(set);
	}

	@Override
	public void deleteDocuments(Set<String> docIdsToDelete) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException {
		CloudSearchClient searchClient = validateSearchAvailable();
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
	public SearchResults executeSearch(String search) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException {
		CloudSearchClient searchClient = validateSearchAvailable();
		String results = searchClient.performSearch(search);
		try {
			return searchResultsFactory.fromAwesomeSearchResults(results);
		} catch (JSONObjectAdapterException e) {
			// Convert to runtime
			throw new RuntimeException(e);
		}
	}

	@Override
	public String executeRawSearch(String search) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException {
		CloudSearchClient searchClient = validateSearchAvailable();
		return searchClient.performSearch(search);
	}

	@Override
	public boolean doesDocumentExist(String id, String etag) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException {
		validateSearchEnabled();
		// Search for the document
		String query = String.format(QUERY_BY_ID_AND_ETAG, id, etag);
		SearchResults results = executeSearch(query);
		return results.getHits().size() > 0;
	}

	@Override
	public SearchResults listSearchDocuments(long limit, long offset) throws ClientProtocolException, IOException, HttpClientHelperException,
			ServiceUnavailableException {
		validateSearchEnabled();
		String query = String.format(QUERY_LIST_ALL_DOCUMENTS_ONE_PAGE, limit, offset);
		return executeSearch(query);
	}

	@Override
	public void deleteAllDocuments() throws ClientProtocolException, IOException, HttpClientHelperException, InterruptedException,
			ServiceUnavailableException {
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

	private CloudSearchClient validateSearchAvailable() throws ServiceUnavailableException {
		validateSearchEnabled();
		if (cloudHttpClient == null) {
			throw new ServiceUnavailableException("Search service still initializing...");
		}
		return cloudHttpClient;
	}
}
