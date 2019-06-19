package org.sagebionetworks.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.aws.AwsClientFactory;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomain;

/**
 * This class is responsible for initializing the setup of AWS CloudSearch
 * and providing a configured CloudSearch client once setup has completed.
 */
public class CloudSearchClientProvider {
	static private Logger log = LogManager.getLogger(CloudSearchClientProvider.class);

	//stateful fields that determine whether or not a CloudsSearchDomainClientAdapter can be provided
	AmazonCloudSearchDomain awsCloudSearchDomainClient;
	private boolean isSearchEnabled;
	private static CloudsSearchDomainClientAdapter singletonWrapper = null;

	//autowired fields
	@Autowired
	SearchDomainSetup searchDomainSetup;
	@Autowired
	CloudSearchDocumentBatchIteratorProvider cloudSearchDocumentBatchIteratorProvider;
	@Autowired
	CloudSearchLogger recordLogger;

	public CloudsSearchDomainClientAdapter getCloudSearchClient(){
		if(!isSearchEnabled()){
			throw new IllegalStateException("The search feature must be enabled in production");
		}

		if (singletonWrapper != null){
			return singletonWrapper;
		}else{
			if(searchDomainSetup.postInitialize()) {
				awsCloudSearchDomainClient = AwsClientFactory.createAmazonCloudSearchDomain(searchDomainSetup.getDomainSearchEndpoint());
				singletonWrapper = new CloudsSearchDomainClientAdapter(awsCloudSearchDomainClient, cloudSearchDocumentBatchIteratorProvider, recordLogger);
				return singletonWrapper;
			} else{
				throw new TemporarilyUnavailableException("Search has not yet been initialized. Please try again later!");
			}
		}
	}

	/**
	 * Injected via Spring
	 * @param isSearchEnabled
	 */
	public void setSearchEnabled(boolean isSearchEnabled) {
		this.isSearchEnabled = isSearchEnabled;
	}

	public boolean isSearchEnabled() {
		return isSearchEnabled;
	}

}
