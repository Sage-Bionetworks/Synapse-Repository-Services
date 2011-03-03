package org.sagebionetworks.web.client;

import java.util.List;

import org.sagebionetworks.web.shared.ColumnsForType;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.TableResults;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SearchServiceAsync {


	void executeSearch(SearchParameters params, AsyncCallback<TableResults> callback);

	void getColumnsForType(String type, AsyncCallback<ColumnsForType> callback);

}
