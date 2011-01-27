package org.sagebionetworks.repo.model.gaejdo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatasetAnalysisDAO;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.ProjectDAO;
import org.sagebionetworks.repo.model.ScriptDAO;

/**
 * This class is the GAE JDO implementation of DAOFactory
 * 
 * @author bhoff
 * 
 */

public class GAEJDODAOFactoryImpl implements DAOFactory {

	private static final Logger log = Logger
			.getLogger(GAEJDODAOFactoryImpl.class.getName());

	private static final Map<Class, Class> MODEL2DAO;
	static {
		Map<Class, Class> model2dao = new HashMap<Class, Class>();
		model2dao.put(Dataset.class, GAEJDODatasetDAOImpl.class);
		MODEL2DAO = Collections.unmodifiableMap(model2dao);

	}

	public ProjectDAO getProjectDAO() {
		return new GAEJDOProjectDAOImpl();
	}

	public ScriptDAO getScriptDAO() {
		return new GAEJDOScriptDAOImpl();
	}

	public DatasetDAO getDatasetDAO() {
		return new GAEJDODatasetDAOImpl();
	}

	// public InputDataLayerDAO getInputDataLayerDAO() {
	// return new GAEJDOInputDataLayerDAOImpl();
	// }
	//
	// public AnalysisResultDAO getAnalysisResultDAO() {
	// return new GAEJDOAnalysisResultDAOHelper();
	// }

	public DatasetAnalysisDAO getDatasetAnalysisDAO() {
		return new GAEJDODatasetAnalysisDAOImpl();
	}

	public BaseDAO getDAO(Class theModelClass) {
		if (MODEL2DAO.containsKey(theModelClass)) {
			Class theDAOClass = MODEL2DAO.get(theModelClass);
			try {
				return (BaseDAO) theDAOClass.newInstance();
			} catch (Exception e) {
				// This should not happen since we are referring to Class
				// objects
				// in the map (as opposed to class names)
				log.log(Level.SEVERE,
						"Unable to instantiate DAO class of type "
								+ theDAOClass + " for model class "
								+ theModelClass, e);
			}
		}
		return null;
	}
}
