package org.sagebionetworks.web.client.services;

import org.sagebionetworks.web.shared.Project;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("project")
public interface ProjectService extends RemoteService {	

	public Project getProject(String id);
	
}
