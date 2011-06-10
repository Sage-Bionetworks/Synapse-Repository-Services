package org.sagebionetworks.repo.web.controller.metadata;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer.LayerTypeNames;
import org.sagebionetworks.repo.model.UserInfo;
import org.springframework.beans.factory.InitializingBean;

/**
 * A cache of the layer type counts for each dataset.
 * @author John
 *
 */
public interface LayerTypeCountCache extends InitializingBean {
	
	
	/**
	 * Get the number of layers of a given type, that are children of the given dataset.
	 * These values are cached to improve performance. http://sagebionetworks.jira.com/browse/PLFM-228. 
	 * @param dataset
	 * @param layerType
	 * @return
	 * @throws DatastoreException 
	 */
	public long getCountFor(String datasetId, LayerTypeNames layerType, UserInfo userInfo) throws DatastoreException;
	
	/**
	 * Clear the cached value for a given dataset's layer type.
	 * @param datasetId
	 * @param layerType
	 */
	public void clearCacheFor(String datasetId, LayerTypeNames layerType);
	
	/**
	 * Clear all entries for a given dataset.
	 * @param datasetId
	 */
	public void clearCacheFor(String datasetId);
	
	/**
	 * How many entries are there in the cache.
	 * @return
	 */
	public int getCacheSize();
	
	/**
	 * Clear the entire cache.
	 * @return
	 */
	public void clearAll();
	

}
