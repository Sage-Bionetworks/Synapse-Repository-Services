package org.sagebionetworks.repo.model.gaejdo;

import org.sagebionetworks.repo.model.AnalysisResultDAO;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.DatasetAnalysisDAO;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.InputDataLayerDAO;
import org.sagebionetworks.repo.model.ProjectDAO;
import org.sagebionetworks.repo.model.ScriptDAO;

public class GAEJDODAOFactoryImpl implements DAOFactory {
	public ProjectDAO getProjectDAO() {
		return new GAEJDOProjectDAOImpl();
	}
	
	public ScriptDAO getScriptDAO() {
		return new GAEJDOScriptDAOImpl();
	}
	
	
	public DatasetDAO getDatasetDAO() {
		return new GAEJDODatasetDAOImpl();
	}
	
	public InputDataLayerDAO getInputDataLayerDAO() {
		return new GAEJDOInputDataLayerDAOImpl();
	}
	
	public AnalysisResultDAO getAnalysisResultDAO() {
		return new GAEJDOAnalysisResultDAOImpl();
	}
		
	public DatasetAnalysisDAO getDatasetAnalysisDAO() {
		return new GAEJDODatasetAnalysisDAOImpl();
	}
}
