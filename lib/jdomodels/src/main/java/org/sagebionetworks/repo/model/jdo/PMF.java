package org.sagebionetworks.repo.model.jdo;

import java.util.Map;
import java.util.logging.Logger;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

/**
 * A factory for persistence managers.
 * 
 * @author bhoff
 * 
 */
public final class PMF {
	private static final Logger log = Logger.getLogger(PMF.class.getName());
	private static PersistenceManagerFactory pmfInstance = null;

	private PMF() {
	}

	/**
	 * Get the Persistence Manager. By default it returns a Persistence Manager
	 * for an in-memory database. If you set the system property or environment
	 * variable JDBC_CONNECTION_STRING it will attempt to make a connection to a
	 * MySQL database with that connection URL.
	 * 
	 * @return the PersistenceManager
	 */
	public static PersistenceManager get() {
		if (null == pmfInstance) {
			synchronized (PMF.class) {
				// Get the properties to use
				Map<Object, Object> props = PMFConfigUtils.getPersitanceManagerProperties();
				pmfInstance = JDOHelper.getPersistenceManagerFactory(props);
			}
		}
		return pmfInstance.getPersistenceManager();
	}
}