package org.sagebionetworks.search;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomainClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Random;

public class CloudSearchClientProvider {
	static private Logger log = LogManager.getLogger(CloudSearchClientProvider.class);

	@Autowired
	SearchDomainSetup searchDomainSetup;

	@Autowired
	AmazonCloudSearchDomainClient cloudSearchDomainClient;

	private boolean isSearchEnabled;

	private boolean setupCompleted;

	//TODO: Search enabled/disbaled should be the responsibility of the provider, not this SearchDomainSetup


	//TODO: Bean initlaizlation similart to old SearchDAO
	public CloudsSearchDomainClientAdapter getCloudSearchClient(){
		if(!isSearchEnabled()){
			throw new UnsupportedOperationException("The search feature was disabled."); //TODO: what HTTP code does this map to?
		}
		if(isCurrentlyInitializing()){
			throw new IllegalStateException("Search has not yet been initialized. Please try again later!"); //TODO: different exception? to map to 503 HTTP error?
		}

		return new CloudsSearchDomainClientAdapter(cloudSearchDomainClient); //TODO: maybe make this as a singleton?
	}

	/**
	 * Injected via Spring
	 * @param isSearchEnabled
	 */
	public void setSearchEnabled(boolean isSearchEnabled) {
		this.isSearchEnabled = isSearchEnabled;
	}

	@Override
	public boolean isSearchEnabled() {
		return isSearchEnabled;
	}


	public boolean isCurrentlyInitializing(){
		if(setupCompleted){//searchDomainSetup must make
			return false;
		}

		if (searchDomainSetup.postInitialize()) {
			cloudSearchDomainClient.setEndpoint(searchDomainSetup.getDomainSearchEndpoint());
			setupCompleted = true;
			return false;
		}else{
			return true;
		}
	}

	/**
	 * The initialization of a search index can take hours the first time it is run.
	 * While the search index is initializing we do not want to block the startup of the rest
	 * of the application.  Therefore, this initialization worker is executed on a separate
	 * thread.
	 */
	public void initialize(){
		try {
			if(isSearchEnabled()) {
				/*
				 * Since each machine in the cluster will call this method and we only
				 * want one machine to initialize the search index, we randomly stagger
				 * the start for each machine.
				 */
				Random random = new Random();
				// random sleep time from zero to 1 sec.
				long randomSleepMS = random.nextInt(1000);
				log.info("Random wait to start search index: " + randomSleepMS + " MS");
				Thread.sleep(randomSleepMS);
				// wait for postInitialize() to finish
				if (isCurrentlyInitializing()) {
					log.info("Search index not finished initializing...");
				} else {
					log.info("Search index initialized.");
				}
			}
		} catch(Exception e) {
			log.error("Unexpected exception while starting the search index", e);
		}
	}
}
