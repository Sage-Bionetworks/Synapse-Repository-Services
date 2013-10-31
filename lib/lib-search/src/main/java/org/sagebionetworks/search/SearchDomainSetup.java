package org.sagebionetworks.search;

import java.net.UnknownHostException;
import java.util.List;

import com.amazonaws.services.cloudsearch.model.AccessPoliciesStatus;
import com.amazonaws.services.cloudsearch.model.DomainStatus;
import com.amazonaws.services.cloudsearch.model.IndexFieldStatus;

public interface SearchDomainSetup {

	/**
	 * Spring will call this method when the bean is first initialize.
	 * @throws InterruptedException 
	 * @throws UnknownHostException 
	 */
	public void initialize() throws InterruptedException, UnknownHostException;
	
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
	  * Get the search endpoint.
	  * @return
	  */
	 public String getSearchEndpoint();
	 
	 /**
	  * Get the document endpoint.
	  * @return
	  */
	 public String getDocumentEndpoint();

	 /**
	  * Is Search enabled?
	  * @return
	  */
	boolean isSearchEnabled();
}
