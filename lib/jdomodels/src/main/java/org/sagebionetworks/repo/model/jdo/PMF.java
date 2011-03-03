package org.sagebionetworks.repo.model.jdo;

import java.util.HashMap;
import java.util.Map;

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
	private static PersistenceManagerFactory pmfInstance = null;
	private static String memoryPersistenceImpl = "memorydb-transactions-optional";
	private static String mysqlPersistenceImpl = "mysql-transactions-optional";

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
				if ((null == System.getProperty("JDBC_CONNECTION_STRING"))
						&& (null == System.getenv("JDBC_CONNECTION_STRING"))) {
					pmfInstance = JDOHelper
							.getPersistenceManagerFactory(memoryPersistenceImpl);
				} else {
					Map<String,String> overrides = new HashMap<String,String>();
					String jdbcConnection = (null != System
							.getProperty("JDBC_CONNECTION_STRING")) ? System
							.getProperty("JDBC_CONNECTION_STRING") : System
							.getenv("JDBC_CONNECTION_STRING");
					overrides.put("javax.jdo.option.ConnectionURL", jdbcConnection);
					pmfInstance = JDOHelper
							.getPersistenceManagerFactory(overrides, mysqlPersistenceImpl);

				}
			}
		}
		return pmfInstance.getPersistenceManager();
	}
}