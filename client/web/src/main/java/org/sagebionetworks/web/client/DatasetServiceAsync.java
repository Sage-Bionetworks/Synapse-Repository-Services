package org.sagebionetworks.web.client;

import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.Layer;
import org.sagebionetworks.web.shared.PaginatedDatasets;
import org.sagebionetworks.web.shared.TableResults;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface DatasetServiceAsync {

	void getDataset(String id, AsyncCallback<Dataset> callback);

	void getAllDatasets(int offset, int length, String sort, boolean ascending,
			AsyncCallback<PaginatedDatasets> callback);

	void getLayer(String datasetId, String layerId,
			AsyncCallback<Layer> callback);

	void getLayerPreviewMap(String datasetId, String layerId,
			AsyncCallback<TableResults> callback);

}
