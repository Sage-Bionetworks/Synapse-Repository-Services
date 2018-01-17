package org.sagebionetworks.search;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Random;

/**
 * This class is responsible for initializing the setup of AWS CloudSearch
 * and providing a configured CloudSearch client once setup has completed.
 */
public class CloudSearchClientProvider {
	static private Logger log = LogManager.getLogger(CloudSearchClientProvider.class);

	@Autowired
	SearchDomainSetup searchDomainSetup;

	@Autowired
	AmazonCloudSearchDomainClient awsCloudSearchDomainClient;

	private boolean isSearchEnabled;

	private static CloudsSearchDomainClientAdapter singletonWrapper;

	public CloudsSearchDomainClientAdapter getCloudSearchClient(){
		if(!isSearchEnabled()){
			throw new IllegalStateException("The search feature must be enabled in production");
		}

		if (singletonWrapper != null){
			return singletonWrapper;
		}else{
			if(searchDomainSetup.postInitialize()) {
				awsCloudSearchDomainClient.setEndpoint(searchDomainSetup.getDomainSearchEndpoint());
				singletonWrapper = new CloudsSearchDomainClientAdapter(awsCloudSearchDomainClient);
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
