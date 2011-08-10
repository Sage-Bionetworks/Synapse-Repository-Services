package org.sagebionetworks.web.client;

import java.util.List;

import org.sagebionetworks.web.shared.ColumnsForType;
import org.sagebionetworks.web.shared.FilterEnumeration;
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
	
	/**
	 * Gets all of the column metadata for a given type.
	 * @param type
	 * @return
	 */
	public ColumnsForType getColumnsForType(String type);
	
	/**
	 * Get the list of filter enumerations.
	 * @return
	 */
	public List<FilterEnumeration> getFilterEnumerations();
	
	

}
