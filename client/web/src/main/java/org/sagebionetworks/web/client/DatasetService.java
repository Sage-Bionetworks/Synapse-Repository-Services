package org.sagebionetworks.web.client;

import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.PaginatedDatasets;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("dataset")
public interface DatasetService extends RemoteService {
	
	public PaginatedDatasets getAllDatasets(int offset, int length, String sort, boolean ascending);
	
	public Dataset getDataset(String id);

}
