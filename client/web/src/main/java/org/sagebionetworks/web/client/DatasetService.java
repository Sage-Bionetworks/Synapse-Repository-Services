package org.sagebionetworks.web.client;

import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.DatasetAnnotations;
import org.sagebionetworks.web.shared.DownloadLocation;
import org.sagebionetworks.web.shared.Layer;
import org.sagebionetworks.web.shared.LayerPreview;
import org.sagebionetworks.web.shared.PaginatedDatasets;
import org.sagebionetworks.web.shared.TableResults;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("dataset")
public interface DatasetService extends RemoteService {
	
	public PaginatedDatasets getAllDatasets(int offset, int length, String sort, boolean ascending);
	
	public Dataset getDataset(String id);
	
	public DatasetAnnotations getDatasetAnnotations(String id);
	
	public Layer getLayer(String datasetId, String layerId);
	
	public LayerPreview getLayerPreview(String datasetId, String layerId);
	
	public TableResults getLayerPreviewMap(String datasetId, String layerId);
	
	public DownloadLocation getLayerDownloadLocation(String datasetId, String layerId);

}
