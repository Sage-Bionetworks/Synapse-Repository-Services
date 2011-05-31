package org.sagebionetworks.web.client;

import org.sagebionetworks.web.shared.Annotations;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.DownloadLocation;
import org.sagebionetworks.web.shared.Layer;
import org.sagebionetworks.web.shared.LayerPreview;
import org.sagebionetworks.web.shared.PaginatedDatasets;
import org.sagebionetworks.web.shared.TableResults;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface DatasetServiceAsync {

	void getAllDatasets(int offset, int length, String sort, boolean ascending,
			AsyncCallback<PaginatedDatasets> callback);

	void getDataset(String id, AsyncCallback<Dataset> callback);

	void getDatasetAnnotations(String id,
			AsyncCallback<Annotations> callback);

	void getLayer(String datasetId, String layerId,
			AsyncCallback<Layer> callback);

	void getLayerPreview(String datasetId, String layerId,
			AsyncCallback<LayerPreview> callback);

	void getLayerPreviewMap(String datasetId, String layerId,
			AsyncCallback<TableResults> callback);

	void getLayerDownloadLocation(String datasetId, String layerId,
			AsyncCallback<DownloadLocation> callback);

	void createDataset(Dataset toCreate, AsyncCallback<String> callback);

	void updateDatasetAnnotations(String datasetId, Annotations newAnnotations,
			AsyncCallback<String> callback);

	void delete(String id, AsyncCallback<Void> callback);

}
