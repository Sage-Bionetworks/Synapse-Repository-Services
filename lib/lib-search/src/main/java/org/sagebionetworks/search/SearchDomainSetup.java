package org.sagebionetworks.search;

import java.util.List;

import com.amazonaws.services.cloudsearchv2.model.DomainStatus;
import com.amazonaws.services.cloudsearchv2.model.IndexFieldStatus;

public interface SearchDomainSetup {
	
	/**
	 * Called by initializing worker. This method should check, initalize where necessary and return relatively quickly
	 * (i.e. no long waits)
	 * 
	 * @return true when post initialization is done
	 */
	public boolean postInitialize();

	/**
	 * The name of of the search domain.
	 * 
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
	 * Get the CloudSearch domain endpoint for search service.
	 * @return
	 */
	public String getDomainSearchEndpoint();

}
