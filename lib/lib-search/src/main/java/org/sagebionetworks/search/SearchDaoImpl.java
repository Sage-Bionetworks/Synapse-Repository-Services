package org.sagebionetworks.search;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudsearch.AmazonCloudSearchClient;
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

/**
 * Implementation of the Search DAO.
 * 
 * @author jmhill
 * 
 */
public class SearchDaoImpl implements SearchDao {

	private static final String SEARCH_DOMAIN_NAME_TEMPLATE = "%1$s-%2$s-sagebase-org";

	static private Log log = LogFactory.getLog(SearchDaoImpl.class);

	@Autowired
	AmazonCloudSearchClient awsSearchClient;

	/**
	 * Spring will call this method when the bean is first initialize.
	 * @throws InterruptedException 
	 * @throws UnknownHostException 
	 */
	public void initialize() throws InterruptedException, UnknownHostException {
		log.info("initialize...");
		
		InetAddress thisIp =InetAddress.getLocalHost();
		System.out.println("IP:"+thisIp.getHostAddress());
		// Do we have a search index?
		String domainName = getSearchDomainName();
		log.info("Search domain name: "+domainName);
		DescribeDomainsResult result = awsSearchClient.describeDomains(new DescribeDomainsRequest().withDomainNames(domainName));
		if(result.getDomainStatusList().size() < 1){
			log.info("Search domain does not exist for: "+domainName+". A new search domain will be created.");
			// Create the search domain.
			awsSearchClient.createDomain(new CreateDomainRequest().withDomainName(domainName));
		}
		
		// Make sure this machine has permission to make search HTTP calls
		DescribeServiceAccessPoliciesResult dsapr = awsSearchClient.describeServiceAccessPolicies(new DescribeServiceAccessPoliciesRequest().withDomainName(domainName));
		System.out.println(dsapr);
		
		// Now wait for the domain if needed
		waitForDomainProcessing(domainName);
		// Define the schema
		defineAndValidateSchema(domainName);
		// Run indexing if needed
		runIndexIfNeeded(domainName);
		// Now wait for the domain if needed
		waitForDomainProcessing(domainName);

		log.info("Finished initializing search index");
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
	 * 
	 * @param domainName
	 * @throws InterruptedException 
	 */
	public void waitForDomainProcessing(String domainName) throws InterruptedException{
		DomainStatus status = getDomainStatus(domainName);
		long start = System.currentTimeMillis();
		while(status.isProcessing()){
			if(status.isDeleted()) throw new IllegalStateException("Search domain: "+domainName+" has been deleted!");
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
		if(result.getDomainStatusList().size() != 1) throw new IllegalArgumentException("Expected one and only one search domain with the name: "+domainName+" but found: "+result.getDomainStatusList().size() );
		return result.getDomainStatusList().get(0);
	}
	/**
	 * The domain name of the search index.
	 * 
	 * @return
	 */
	public String getSearchDomainName(){
		return String.format(SEARCH_DOMAIN_NAME_TEMPLATE, StackConfiguration.getStack(), StackConfiguration.getStackInstance());
	}

}
