package org.sagebionetworks.web.client;

import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.PaginatedDatasets;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface DatasetServiceAsync {

	void getDataset(String id, AsyncCallback<Dataset> callback);

	void getAllDatasets(int offset, int length, String sort, boolean ascending,
			AsyncCallback<PaginatedDatasets> callback);

}
