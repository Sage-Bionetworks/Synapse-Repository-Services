package org.sagebionetworks.repo.model.jdo;

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
import org.sagebionetworks.repo.model.query.QueryDAO;
import org.sagebionetworks.repo.model.query.jdo.JDOQueryDAOImpl;

/**
 * This class is the JDO implementation of DAOFactory
 * 
 * @author bhoff
 * 
 */

public class JDODAOFactoryImpl implements DAOFactory {

	public ProjectDAO getProjectDAO() {
		//return new JDOProjectDAOImpl();
		throw new RuntimeException("Not yet implemented");
	}

	public ScriptDAO getScriptDAO() {
		return new JDOScriptDAOImpl();
	}

	public DatasetDAO getDatasetDAO(String userId) {
		return new JDODatasetDAOImpl(userId);
	}

	/**
	 * 
	 * @param userId the unique id of the user or null if anonymous
	 * @return
	 */
	public UserDAO getUserDAO(String userId) {
		return new JDOUserDAOImpl(userId);
	}
	
	public UserCredentialsDAO getUserCredentialsDAO(String userId) {
		return new JDOUserCredentialsDAOImpl(userId);
	}
	
	/**
	 * 
	 * @param userId the unique id of the user or null if anonymous
	 * @return
	 */
	public UserGroupDAO getUserGroupDAO(String userId)  {
		return new JDOUserGroupDAOImpl(userId);
	}

	public LayerPreviewDAO getLayerPreviewDAO(String userId) {
		return new JDOLayerPreviewDAOImpl(userId);
	}


	public LayerLocationsDAO getLayerLocationsDAO(String userId) {
		return new JDOLayerLocationsDAOImpl(userId);
	}

	public DatasetAnalysisDAO getDatasetAnalysisDAO() {
		return new JDODatasetAnalysisDAOImpl();
	}
	
	public QueryDAO getQueryDao(){
		return new JDOQueryDAOImpl();
	}

}
