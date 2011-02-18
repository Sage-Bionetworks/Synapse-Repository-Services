package org.sagebionetworks.repo.model;

/**
 * This is the interface implemented by DAO Factories
 * 
 * @author bhoff
 * 
 */
public interface DAOFactory {

	public ProjectDAO getProjectDAO();

	public ScriptDAO getScriptDAO();

	public DatasetDAO getDatasetDAO();

	public DatasetAnalysisDAO getDatasetAnalysisDAO();

	public LayerPreviewDAO getLayerPreviewDAO();

	public LayerLocationsDAO getLayerLocationsDAO();
}
