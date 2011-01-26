package org.sagebionetworks.repo.model;

import java.util.Collection;

/**
 * This interface defines the methods to be supported by any DAO for Datasets.
 * 
 * @author bhoff
 * 
 */
public interface DatasetDAO extends BaseDAO<Dataset>, AnnotatableDAO<Dataset>,
		RevisableDAO<Dataset> {

	/**
	 * 
	 * @param datasetId
	 *            the ID of the dataset to which the layer is to be added
	 * @param layer the layer to add, including the 'shallow' values
	 * @return the id of the created layer
	 */
	public String createLayer(String datasetId, LayerMetadata layer) throws DatastoreException, InvalidModelException;

	/**
	 * 
	 * @param datasetId
	 *            the ID of the dataset of interest
	 * @return IDs of all the layers in the dataset
	 */
	public Collection<String> getLayers(String datasetId) throws DatastoreException;
	
	/**
	 * 
	 * @param id id for the layer of interest
	 * @return the DTO for the layer
	 */
	public LayerMetadata getLayer(String id);
	/**
	 * 
	 * @param layer the layer to update (id field required)
	 */
	public void updateLayer(LayerMetadata layer) throws DatastoreException, InvalidModelException;

	
	/**
	 * 
	 * @param layer the layer to remove (id field required)
	 */
	public void removeLayer(LayerMetadata layer) throws DatastoreException;


}
