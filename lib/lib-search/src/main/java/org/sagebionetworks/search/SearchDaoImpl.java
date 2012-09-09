package org.sagebionetworks.search;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.search.AwesomeSearchFactory;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.AdapterFactoryImpl;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudsearch.AmazonCloudSearchClient;
import com.amazonaws.services.cloudsearch.model.AccessPoliciesStatus;
import com.amazonaws.services.cloudsearch.model.CreateDomainRequest;
import com.amazonaws.services.cloudsearch.model.DefineIndexFieldRequest;
import com.amazonaws.services.cloudsearch.model.DescribeDomainsRequest;
import com.amazonaws.services.cloudsearch.model.DescribeDomainsResult;
import com.amazonaws.services.cloudsearch.model.DescribeIndexFieldsRequest;
import com.amazonaws.services.cloudsearch.model.DescribeIndexFieldsResult;
import com.amazonaws.services.cloudsearch.model.DescribeServiceAccessPoliciesRequest;
import com.amazonaws.services.cloudsearch.model.DescribeServiceAccessPoliciesResult;
import com.amazonaws.services.cloudsearch.model.DomainStatus;
import com.amazonaws.services.cloudsearch.model.IndexDocumentsRequest;
import com.amazonaws.services.cloudsearch.model.IndexField;
import com.amazonaws.services.cloudsearch.model.IndexFieldStatus;
import com.amazonaws.services.cloudsearch.model.UpdateServiceAccessPoliciesRequest;

/**
 * Implementation of the Search DAO.
 * 
 * @author jmhill
 * 
 */
public class SearchDaoImpl implements SearchDao {

	private static final String POLICY_TEMPLATE = "{\"Statement\": [{\"Effect\":\"Allow\", \"Action\": \"*\", \"Resource\": \"%1$s\", \"Condition\": { \"IpAddress\": { \"aws:SourceIp\": [\"%3$s\"] } }}, {\"Effect\":\"Allow\", \"Action\": \"*\", \"Resource\": \"%2$s\", \"Condition\": { \"IpAddress\": { \"aws:SourceIp\": [\"%3$s\"] } }} ] }";

	private static final String SEARCH_DOMAIN_NAME_TEMPLATE = "%1$s-%2$s-sagebase-org";
	
	private static final String CLOUD_SEARCH_API_VERSION = "2011-02-01";
	private static final String SEARCH_ENDPOINT_TEMPALTE = "http://%1$s/"+CLOUD_SEARCH_API_VERSION+"/search";
	private static final String DOCUMENT_ENDPOINT_TEMPALTE = "httpS://%1$s/"+CLOUD_SEARCH_API_VERSION+"/documents/batch";
	
	private static final String SEARCH_DOMAIN_NAME = String.format(SEARCH_DOMAIN_NAME_TEMPLATE, StackConfiguration.getStack(), StackConfiguration.getStackInstance());

	static private Log log = LogFactory.getLog(SearchDaoImpl.class);
	
	private static final AwesomeSearchFactory searchResultsFactory = new AwesomeSearchFactory(new AdapterFactoryImpl());
	
	private static final HttpClient httpClient;

	static {
		ThreadSafeClientConnManager connectionManager;
		try {
			connectionManager = HttpClientHelper.createClientConnectionManager(true);
			// ensure that we can have *many* simultaneous connections to
			// CloudSearch
			connectionManager.setDefaultMaxPerRoute(StackConfiguration.getHttpClientMaxConnsPerRoute());
			HttpParams clientParams = new BasicHttpParams();
			clientParams.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,10*1000);
			clientParams.setParameter(CoreConnectionPNames.SO_TIMEOUT, 10*1000);
			httpClient = new DefaultHttpClient(connectionManager, clientParams);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Autowired
	AmazonCloudSearchClient awsSearchClient;
	
	CloudSearchClient cloudHttpClient;

	/**
	 * Spring will call this method when the bean is first initialize.
	 * @throws InterruptedException 
	 * @throws UnknownHostException 
	 */
	public void initialize() throws InterruptedException, UnknownHostException {
		log.info("initialize...");
		long start = System.currentTimeMillis();
		// Do we have a search index?
		String domainName = getSearchDomainName();
		log.info("Search domain name: "+domainName);
		// If the domain is currently processing then wait for it.
		// Note: We need to wait when the search domain is deleted before we can re-create it.
		waitForDomainProcessing(domainName);
		// Create the domain it it does not already exist.
		createDomainIfNeeded(domainName);
		// Set the policy.
		setPolicyIfNeeded(domainName);
		// Define the schema
		defineAndValidateSchema(domainName);
		// Run indexing if needed
		runIndexIfNeeded(domainName);
		// Now wait for the domain if needed
		waitForDomainProcessing(domainName);
		long elapse = System.currentTimeMillis()-start;
		log.info(String.format("Finished initializing search index: Elapse time: %1$tM:%1$tS:%1$tL (Min:Sec:MS)", elapse));
		// create the cloud search client
		DomainStatus status = getDomainStatus();
		String searchEndPoint = String.format(SEARCH_ENDPOINT_TEMPALTE, status.getSearchService().getEndpoint());
		log.info("Search endpoint: "+searchEndPoint);
		String documentEndPoint = String.format(DOCUMENT_ENDPOINT_TEMPALTE, status.getDocService().getEndpoint());
		log.info("Document endpoint: "+documentEndPoint);
		cloudHttpClient = new CloudSearchClient(httpClient, searchEndPoint, documentEndPoint);
	}

	/**
	 * @param domainName
	 */
	public void setPolicyIfNeeded(String domainName) {
		DescribeServiceAccessPoliciesResult dsapr = awsSearchClient.describeServiceAccessPolicies(new DescribeServiceAccessPoliciesRequest().withDomainName(domainName));
		DomainStatus status = getDomainStatus(domainName);
		// Set the policy.
		// Until we figure out a better plan, we are opening this up to 0.0.0.0
		String policyJson = String.format(POLICY_TEMPLATE, status.getDocService().getArn(), status.getSearchService().getArn(), "0.0.0.0/0");
		log.debug("Expected Policy: "+policyJson);
		if(!policyJson.equals(dsapr.getAccessPolicies().getOptions())){
			log.info("Updateing the Search Access policy as it does not match the expected policy");
			// Add the policy.
			awsSearchClient.updateServiceAccessPolicies(new UpdateServiceAccessPoliciesRequest().withDomainName(domainName).withAccessPolicies(policyJson));
		}else{
			log.info("Search Access policy is already set.");
		}
	}

	/**
	 * If the passed domain name does not exist, it will be created.
	 * 
	 * @param domainName
	 * @throws InterruptedException 
	 */
	public void createDomainIfNeeded(String domainName) throws InterruptedException {
		DescribeDomainsResult result = awsSearchClient.describeDomains(new DescribeDomainsRequest().withDomainNames(domainName));
		if(result.getDomainStatusList().size() < 1){
			log.info("Search domain does not exist for: "+domainName+". A new search domain will be created.");
			// Create the search domain.
			awsSearchClient.createDomain(new CreateDomainRequest().withDomainName(domainName));
		}
	}
	
	/**
	 * Run indexing if needed.
	 * @param domainName
	 */
	private void runIndexIfNeeded(String domainName){
		DomainStatus status = getDomainStatus(domainName);
		if(status.isRequiresIndexDocuments()){
			log.info("Need to run indexing on the search domain...");
			awsSearchClient.indexDocuments(new IndexDocumentsRequest().withDomainName(domainName));
		}
	}


	/**
	 * Define and validate the schema.
	 * @param domainName
	 */
	private void defineAndValidateSchema(String domainName) {
		// Now make sure all of the fields are configured.
		List<IndexField> indexList = SearchSchemaLoader.loadSearchDomainSchema();
		for(IndexField field: indexList){
			// Determine if this field already exists
			DescribeIndexFieldsResult difr = awsSearchClient.describeIndexFields(new DescribeIndexFieldsRequest().withDomainName(domainName).withFieldNames(field.getIndexFieldName()));
			if(difr.getIndexFields().size() < 1){
				// We need to create it.
				log.info("IndexField: "+field.getIndexFieldName()+" does not exist, so it will be created...");
				// Create the field
				awsSearchClient.defineIndexField(new DefineIndexFieldRequest().withDomainName(domainName).withIndexField(field));
			}else{
				if(difr.getIndexFields().size() != 1) throw new IllegalStateException("Expected one and only one IndexField with the name: "+field.getIndexFieldName()+" but found: "+difr.getIndexFields().size());
				// It already exists
				log.info("IndexField: "+field.getIndexFieldName()+" already exists");
				// Validate the field has not changed
				IndexFieldStatus status = difr.getIndexFields().get(0);
				// Is the existing field different than the expected.
				if(!status.getOptions().equals(field)){
					log.warn(String.format("IndexField already exists and does not match the expected value.  Expected: %1$s Actual: %2$s", field.toString(), status.getOptions().toString()));
					log.info("Updating IndexField: "+field.getIndexFieldName());
					awsSearchClient.defineIndexField(new DefineIndexFieldRequest().withDomainName(domainName).withIndexField(field));
				}
			}
		}
	}
	
	
	/**
	 * Wait for a domain
	 * @param domainName
	 * @throws InterruptedException 
	 */
	public void waitForDomainProcessing(String domainName) throws InterruptedException{
		DomainStatus status = getDomainStatus(domainName);
		if(status == null) {
			// The domain does not exist
			return;
		}
		long start = System.currentTimeMillis();
		while(status.isProcessing()){
			if(status.isDeleted()){
				log.warn("Search domain: "+domainName+" has been deleted!");
			}
			long elapse = System.currentTimeMillis() - start;
			log.info(String.format("Waiting for search domain. Elapse time: %1$tM:%1$tS:%1$tL. Current status: %2$s", elapse, status.toString()));
			Thread.sleep(5*1000);
			status = getDomainStatus(domainName);
		}
		log.info("Search domain is ready: "+status.toString());
	}
	
	/**
	 * Fetch the current domain status.
	 * @param domainName
	 * @return
	 */
	private DomainStatus getDomainStatus(String domainName){
		DescribeDomainsResult result = awsSearchClient.describeDomains(new DescribeDomainsRequest().withDomainNames(domainName));
		if(result.getDomainStatusList().size() == 0) return null;
		if(result.getDomainStatusList().size() != 1) throw new IllegalArgumentException("Expected one and only one search domain with the name: "+domainName+" but found: "+result.getDomainStatusList().size() );
		return result.getDomainStatusList().get(0);
	}
	/**
	 * The domain name of the search index.
	 * 
	 * @return
	 */
	public String getSearchDomainName(){
		return SEARCH_DOMAIN_NAME;
	}

	@Override
	public DomainStatus getDomainStatus() {
		return getDomainStatus(getSearchDomainName());
	}

	@Override
	public List<IndexFieldStatus> getIndexFieldStatus() {
		DescribeIndexFieldsResult difr = awsSearchClient.describeIndexFields(new DescribeIndexFieldsRequest().withDomainName(getSearchDomainName()));
		return difr.getIndexFields();
	}

	@Override
	public AccessPoliciesStatus getAccessPoliciesStatus() {
		DescribeServiceAccessPoliciesResult dsapr = awsSearchClient.describeServiceAccessPolicies(new DescribeServiceAccessPoliciesRequest().withDomainName(getSearchDomainName()));
		return dsapr.getAccessPolicies();
	}

	@Override
	public void createOrUpdateSearchDocument(Document document) throws ClientProtocolException, IOException, HttpClientHelperException {
		if(document == null) throw new IllegalArgumentException("Document cannot be null");
		// the version is always the current time.
		DateTime now = DateTime.now();
		document.setVersion(now.getMillis() / 1000);
		document.setType(DocumentTypeNames.add);
		document.setLang("en");
		// Cleanup they data
		byte[] bytes = cleanSearchDocument(document);
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		// Pass along to the client
		cloudHttpClient.sendDocuments(in, bytes.length);
	}
	
	/**
	 * Remove any character that is not compatible with cloud search.
	 * @param document
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws JSONObjectAdapterException
	 */
	static byte[] cleanSearchDocument(Document document) {
		String serializedDocument;
		try {
			serializedDocument = EntityFactory.createJSONStringForEntity(document);
			// AwesomeSearch pukes on control characters. Some descriptions have
			// control characters in them for some reason, in any case, just get rid
			// of all control characters in the search document
			String cleanedDocument = serializedDocument.replaceAll("\\p{Cc}", "");

			// Get rid of escaped control characters too
			cleanedDocument = cleanedDocument.replaceAll("\\\\u00[0,1][0-9,a-f]","");
			StringBuilder builder = new StringBuilder();
			builder.append("[");
			builder.append(cleanedDocument);
			builder.append("]");
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

	@Override
	public void deleteDocument(String documentId) throws ClientProtocolException, IOException, HttpClientHelperException {
		// This is just a batch delete of size one.
		HashSet<String> set = new HashSet<String>(1);
		set.add(documentId);
		deleteDocuments(set);
	}

	@Override
	public void deleteDocuments(Set<String> docIdsToDelete) throws ClientProtocolException, IOException, HttpClientHelperException {
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
		cloudHttpClient.sendDocuments(documentBatch.toString());
	}

	@Override
	public SearchResults executeSearch(String search) throws ClientProtocolException, IOException, HttpClientHelperException {
		String results = cloudHttpClient.performSearch(search);
		try {
			return searchResultsFactory.fromAwesomeSearchResults(results);
		} catch (JSONObjectAdapterException e) {
			// Convert to runtime
			throw new RuntimeException(e);
		}
	}

}
