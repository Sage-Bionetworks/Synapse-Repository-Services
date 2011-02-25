package org.sagebionetworks.web.client;

import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.TableResults;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("search")
public interface SearchService extends RemoteService{
	
	public static final String KEY_RESULTS = "results";

	public static final String KEY_TOTAL_NUMBER_OF_RESULTS = "totalNumberOfResults";
	

	/**
	 * Execute a Search.
	 * @param params
	 * @return
	 */
	public TableResults executeSearch(SearchParameters params);

}
