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
