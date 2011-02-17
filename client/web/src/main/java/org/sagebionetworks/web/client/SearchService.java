package org.sagebionetworks.web.client;

import java.util.List;

import org.sagebionetworks.web.shared.ColumnMetadata;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.TableResults;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("search")
public interface SearchService extends RemoteService{
	
	/**
	 * What are the default columns for datast?
	 * @return
	 */
	public List<String> getDefaultDatasetColumnIds();
	
	public List<ColumnMetadata> getColumnMetadata();
	
	public TableResults executeSearch(SearchParameters params);

}
