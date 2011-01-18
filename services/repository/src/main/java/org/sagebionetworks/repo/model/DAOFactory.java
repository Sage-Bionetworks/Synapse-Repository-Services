package org.sagebionetworks.repo.model;


public interface DAOFactory {
	public ProjectDAO getProjectDAO();
	
	public ScriptDAO getScriptDAO();
	
	public DatasetDAO getDatasetDAO();
	
	public InputDataLayerDAO getInputDataLayerDAO();
	
	public AnalysisResultDAO getAnalysisResultDAO();

	public DatasetAnalysisDAO getDatasetAnalysisDAO();
}
