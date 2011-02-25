package org.sagebionetworks.web.client;

import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.TableResults;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SearchServiceAsync {


	void executeSearch(SearchParameters params, AsyncCallback<TableResults> callback);


}
