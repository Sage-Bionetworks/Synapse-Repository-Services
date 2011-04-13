package org.sagebionetworks.web.client.widget.table;

import org.sagebionetworks.web.client.SearchServiceAsync;

import com.google.inject.Inject;

public class QueryServiceTableResourceProvider {
	private QueryServiceTableView view; 
	private SearchServiceAsync service;	
	
	@Inject
	public QueryServiceTableResourceProvider(QueryServiceTableView view, SearchServiceAsync service) {
		this.view = view;
		this.service = service;
	}

	public QueryServiceTableView getView() {
		return view;
	}

	public SearchServiceAsync getService() {
		return service;
	}

}
