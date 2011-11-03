package org.sagebionetworks.web.client.services;


import org.sagebionetworks.repo.model.Project;

import com.google.gwt.user.client.rpc.AsyncCallback;


public interface ProjectServiceAsync {

	void getProject(String id, AsyncCallback<Project> callback);
	
}
