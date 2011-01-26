package org.sagebionetworks.repo.model;

/**
 * This is the interface implemented by DAO Factories
 * 
 * @author bhoff
 * 
 */
public interface DAOFactory {

	public BaseDAO getDAO(Class theModelClass);

	public ProjectDAO getProjectDAO();

	public ScriptDAO getScriptDAO();

	public DatasetDAO getDatasetDAO();

	public InputDataLayerDAO getInputDataLayerDAO();

	public AnalysisResultDAO getAnalysisResultDAO();

	public DatasetAnalysisDAO getDatasetAnalysisDAO();
}
