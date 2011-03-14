package org.sagebionetworks.repo.model.jdo;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
				// Prefer the value specified by the property over that in the
				// environment
				String jdbcConnectionString = (null != System
						.getProperty("JDBC_CONNECTION_STRING")) ? System
						.getProperty("JDBC_CONNECTION_STRING") : System
						.getenv("JDBC_CONNECTION_STRING");

				// Elastic Beanstalk passes the empty string by default so check
				// for that too
				if ((null == jdbcConnectionString)
						|| (0 == jdbcConnectionString.length())) {
					log.info("JDO Persistence unit to be used: "
							+ memoryPersistenceImpl);
					pmfInstance = JDOHelper
							.getPersistenceManagerFactory(memoryPersistenceImpl);
				} else {
					Map<String, String> overrides = new HashMap<String, String>();

					/**
					 * User Elastic Beanstalk's variable name of
					 * JDBC_CONNECTION_STRING for the database url and also to
					 * indicate that we want to use MySQL in the first place
					 * since the memory db is our default
					 */
					String jdbcConnection = (null != System
							.getProperty("JDBC_CONNECTION_STRING")) ? System
							.getProperty("JDBC_CONNECTION_STRING") : System
							.getenv("JDBC_CONNECTION_STRING");
					overrides.put("javax.jdo.option.ConnectionURL",
							jdbcConnection);
					log
							.info("Overriding MySQL connection url from jdoconfig.xml with "
									+ jdbcConnection);

					/**
					 * User Elastic Beanstalk's variable name of PARAM1 for the
					 * database user
					 */
					String username = (null != System.getProperty("PARAM1")) ? System
							.getProperty("PARAM1")
							: System.getenv("PARAM1");
					if (null != username) {
						overrides.put("javax.jdo.option.ConnectionUserName",
								username);
						log
								.info("Overriding MySQL user from jdoconfig.xml with user "
										+ username);
					}

					/**
					 * User Elastic Beanstalk's variable name of PARAM2 for the
					 * database password
					 */
					String password = (null != System.getProperty("PARAM2")) ? System
							.getProperty("PARAM2")
							: System.getenv("PARAM2");
					if (null != password) {
						overrides.put("javax.jdo.option.ConnectionPassword",
								password);
						log
								.info("Overriding MySQL password from jdoconfig.xml");

					}

					log.info("JDO Persistence unit to be used: "
							+ mysqlPersistenceImpl);
					pmfInstance = JDOHelper.getPersistenceManagerFactory(
							overrides, mysqlPersistenceImpl);

				}
			}
		}
		return pmfInstance.getPersistenceManager();
	}
}