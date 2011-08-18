package org.sagebionetworks.web.client.widget.table;

import org.sagebionetworks.web.client.SearchServiceAsync;
import org.sagebionetworks.web.client.security.AuthenticationController;

import com.google.inject.Inject;

public class QueryServiceTableResourceProvider {
	private QueryServiceTableView view; 
	private SearchServiceAsync service;	
	private AuthenticationController authenticationController;
	
	@Inject
	public QueryServiceTableResourceProvider(QueryServiceTableView view, SearchServiceAsync service, AuthenticationController authenticationController) {
		this.view = view;
		this.service = service;
		this.authenticationController = authenticationController;
	}

	public QueryServiceTableView getView() {
		return view;
	}

	public SearchServiceAsync getService() {
		return service;
	}
	
	public AuthenticationController getAuthenticationController() {
		return authenticationController;
	}

}
