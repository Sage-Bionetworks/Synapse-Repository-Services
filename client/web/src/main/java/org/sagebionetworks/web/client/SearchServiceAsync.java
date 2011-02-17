package org.sagebionetworks.web.client;

import java.util.List;

import org.sagebionetworks.web.shared.ColumnMetadata;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.TableResults;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SearchServiceAsync {

	void getColumnMetadata(AsyncCallback<List<ColumnMetadata>> callback);

	void getDefaultDatasetColumnIds(AsyncCallback<List<String>> callback);

	void executeSearch(SearchParameters params, AsyncCallback<TableResults> callback);

}
