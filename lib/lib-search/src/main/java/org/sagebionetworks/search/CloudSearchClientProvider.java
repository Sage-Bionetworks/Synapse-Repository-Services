package org.sagebionetworks.search;

import org.sagebionetworks.repo.model.NotReadyException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Random;

public class CloudSearchClientProvider {
	@Autowired
	SearchDomainSetup searchDomainSetup;

	//TODO: Search enabled/disbaled should be the responsibility of the provider, not this SearchDomainSetup


	//TODO: Bean initlaizlation similart to old SearchDAO
	public CloudsSearchDomainClientAdapter getCloudSearchClient(){
		if(!){
			throw new IllegalStateException("Search has not yet been initialized. Please try again later!");
		}
	}


	@Override
	public boolean postInitialize() throws Exception {
		if (!searchDomainSetup.isSearchEnabled()) {
			log.info("SearchDaoImpl.initialize() will do nothing since search is disabled");
			return true;
		}

		if (!searchDomainSetup.postInitialize()) {
			return false;
		}

		//Note: even though we only gave it the search endpoint, the client seems to be able to change to the document upload endpoint automatically
		cloudSearchClientAdapter.setEndpoint(searchDomainSetup.getDomainSearchEndpoint());
		return true;
	}

	/**
	 * The initialization of a search index can take hours the first time it is run.
	 * While the search index is initializing we do not want to block the startup of the rest
	 * of the application.  Therefore, this initialization worker is executed on a separate
	 * thread.
	 */
	public void initialize(){
		try {
			/*
			 * Since each machine in the cluster will call this method and we only
			 * want one machine to initialize the search index, we randomly stagger
			 * the start for each machine.
			 */
			Random random = new Random();
			// random sleep time from zero to 1 sec.
			long randomSleepMS = random.nextInt(1000);
			log.info("Random wait to start search index: "+randomSleepMS+" MS");
			Thread.sleep(randomSleepMS);
			// wait for postInitialize() to finish
			if (!postInitialize()) {
				log.info("Search index not finished initializing...");
			} else {
				log.info("Search index initialized.");
			}
		} catch(Exception e) {
			log.error("Unexpected exception while starting the search index", e);
		}
	}
}
