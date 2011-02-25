package org.sagebionetworks.repo.model.gaejdo;

import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.DatasetAnalysisDAO;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.LayerLocationsDAO;
import org.sagebionetworks.repo.model.LayerPreviewDAO;
import org.sagebionetworks.repo.model.ProjectDAO;
import org.sagebionetworks.repo.model.ScriptDAO;
import org.sagebionetworks.repo.model.UserCredentialsDAO;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserGroupDAO;

/**
 * This class is the GAE JDO implementation of DAOFactory
 * 
 * @author bhoff
 * 
 */

public class GAEJDODAOFactoryImpl implements DAOFactory {

	public ProjectDAO getProjectDAO() {
		//return new GAEJDOProjectDAOImpl();
		throw new RuntimeException("Not yet implemented");
	}

	public ScriptDAO getScriptDAO() {
		return new GAEJDOScriptDAOImpl();
	}

	public DatasetDAO getDatasetDAO(String userId) {
		return new GAEJDODatasetDAOImpl(userId);
	}

	/**
	 * 
	 * @param userId the unique id of the user or null if anonymous
	 * @return
	 */
	public UserDAO getUserDAO(String userId) {
		return new GAEJDOUserDAOImpl(userId);
	}
	
	public UserCredentialsDAO getUserCredentialsDAO(String userId) {
		return new GAEJDOUserCredentialsDAOImpl(userId);
	}
	
	/**
	 * 
	 * @param userId the unique id of the user or null if anonymous
	 * @return
	 */
	public UserGroupDAO getUserGroupDAO(String userId)  {
		return new GAEJDOUserGroupDAOImpl(userId);
	}

	public LayerPreviewDAO getLayerPreviewDAO(String userId) {
		return new GAEJDOLayerPreviewDAOImpl(userId);
	}


	public LayerLocationsDAO getLayerLocationsDAO(String userId) {
		return new GAEJDOLayerLocationsDAOImpl(userId);
	}

	public DatasetAnalysisDAO getDatasetAnalysisDAO() {
		return new GAEJDODatasetAnalysisDAOImpl();
	}

}
