package org.sagebionetworks.repo.model;

/**
 * This interface defines the methods to be supported by any DAO for Datasets.
 * 
 * @author bhoff
 * 
 */
public interface DatasetDAO extends BaseDAO<Dataset>, AnnotatableDAO<Dataset>,
		RevisableDAO<Dataset> {

	public InputDataLayerDAO getInputDataLayerDAO(String datasetId);

}

//
// /**
// *
// * @param datasetId
// * the ID of the dataset to which the layer is to be added
// * @param datasetLayer the layer to add, including the 'shallow' values
// * @return the id of the created layer
// */
// public String createInputDataLayer(String datasetId, InputDataLayer
// datasetLayer) throws DatastoreException, InvalidModelException;
//
// /**
// *
// * @param datasetId
// * the ID of the dataset of interest
// * @return IDs of all the layers in the dataset
// */
// public Collection<String> getInputDataLayers(String datasetId) throws
// DatastoreException, NotFoundException;
//
// /**
// *
// * @param id id for the layer of interest
// * @return the DTO for the layer
// */
// public InputDataLayer getInputDataLayer(String id) throws NotFoundException;
// /**
// *
// * @param datasetLayer the layer to update (id field required)
// */
// public void updateInputDataLayer(InputDataLayer datasetLayer) throws
// DatastoreException, InvalidModelException, NotFoundException;
//
//
// /**
// *
// * @param datasetLayer the layer to remove (id field required)
// */
// public void removeInputDataLayer(String id) throws DatastoreException,
// NotFoundException;

