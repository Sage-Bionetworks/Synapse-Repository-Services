package org.sagebionetworks.repo.model.gaejdo;

import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.DatasetAnalysisDAO;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.LayerLocationsDAO;
import org.sagebionetworks.repo.model.LayerPreviewDAO;
import org.sagebionetworks.repo.model.ProjectDAO;
import org.sagebionetworks.repo.model.ScriptDAO;

/**
 * This class is the GAE JDO implementation of DAOFactory
 * 
 * @author bhoff
 * 
 */

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

	public LayerPreviewDAO getLayerPreviewDAO() {
		return new GAEJDOLayerPreviewDAOImpl();
	}

	public LayerLocationsDAO getLayerLocationsDAO() {
		return new GAEJDOLayerLocationsDAOImpl();
	}

	public DatasetAnalysisDAO getDatasetAnalysisDAO() {
		return new GAEJDODatasetAnalysisDAOImpl();
	}

}
