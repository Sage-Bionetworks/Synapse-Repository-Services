package org.sagebionetworks.repo.model.jdo.temp;

import java.util.Map;
import java.util.Set;

public interface TempResourceManager {
	
	public enum ResourceTypes {
		DATSET,
		DATASET_ANNOTATIONS,
		LAYER,
		LAYER_ANNOTATIONS;
	}
	
	/**
	 * Fetch various resources from a datasets.
	 * @param datsetId - The id of the dataset to fetch.
	 * @param typesToFetch - The types of data to fetch.
	 * @return A map of each ResourceType requested in the set.
	 */
	public Map<ResourceTypes, Object> getDatasetData(String datsetId, Set<ResourceTypes> typesToFetch);

}
