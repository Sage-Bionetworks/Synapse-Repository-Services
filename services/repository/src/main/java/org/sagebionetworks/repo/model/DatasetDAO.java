package org.sagebionetworks.repo.model;

import java.util.Collection;

public interface DatasetDAO extends BaseDAO<Dataset>, AnnotatableDAO<Dataset>,
		RevisableDAO<Dataset> {
	public void addLayer(String datasetId, String layerId);

	public void removeLayer(String datasetId, String layerId);

	public Collection<String> getLayers(String datasetId);
}
