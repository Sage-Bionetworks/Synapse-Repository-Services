package org.sagebionetworks.repo.model.jdo;

import java.util.Map;
import java.util.logging.Logger;

import javax.jdo.SageJDOHelper;

public class PMFConfigUtils {
	private static final Logger log = Logger.getLogger(PMFConfigUtils.class.getName());
	private static String memoryPersistenceImpl = "memorydb-transactions-optional";
	private static String mysqlPersistenceImpl = "mysql-transactions-optional";

	/**
	 * Load the properties from 
	 * @return
	 */
	public static Map<Object, Object> getPersitanceManagerProperties() {
		// First get the connection string
		String jdbcConnectionString = PMFConfigUtils.getJdbcConnectionString();
		if (jdbcConnectionString == null) {
			log.info("JDO Persistence unit to be used: "
					+ memoryPersistenceImpl);
			// Just use the hyper-sonic config.
			return SageJDOHelper
					.getPropertiesFromJDOConfig(memoryPersistenceImpl);
		} else {
			// Use the MySQL config, and override any of the required values.
			Map<Object, Object> map = SageJDOHelper
					.getPropertiesFromJDOConfig(mysqlPersistenceImpl);

			map.put("javax.jdo.option.ConnectionURL", jdbcConnectionString);
			log.info("Overriding MySQL connection url from jdoconfig.xml with "
					+ jdbcConnectionString);

			/**
			 * User Elastic Beanstalk's variable name of PARAM1 for the database
			 * user
			 */
			String username = PMFConfigUtils.getUserNameFromParam1();
			if (null != username) {
				map.put("javax.jdo.option.ConnectionUserName", username);
				log.info("Overriding MySQL user from jdoconfig.xml with user "
						+ username);
			}

			/**
			 * User Elastic Beanstalk's variable name of PARAM2 for the database
			 * password
			 */
			String password = PMFConfigUtils.getPasswordFromParam2();
			if (null != password) {
				map.put("javax.jdo.option.ConnectionPassword", password);
				log.info("Overriding MySQL password from jdoconfig.xml");

			}
			log.info("JDO Persistence unit to be used: " + mysqlPersistenceImpl);
			return map;
		}
	}

	/**
	 * User Elastic Beanstalk's variable name of JDBC_CONNECTION_STRING for the
	 * database url and also to indicate that we want to use MySQL in the first
	 * place since the memory db is our default
	 * 
	 * @return
	 */
	public static String getJdbcConnectionString() {
		String jdbcConnection = (null != System
				.getProperty("JDBC_CONNECTION_STRING")) ? System
				.getProperty("JDBC_CONNECTION_STRING") : System
				.getenv("JDBC_CONNECTION_STRING");
		return jdbcConnection;
	}

	/**
	 * Get the password from all of the places where it can be set.
	 * 
	 * @return
	 */
	private static String getPasswordFromParam2() {
		String password = (null != System.getProperty("PARAM2")) ? System
				.getProperty("PARAM2") : System.getenv("PARAM2");
		return password;
	}

	/**
	 * User Elastic Beanstalk's variable name of PARAM1 for the username
	 * 
	 * @return
	 */
	private static String getUserNameFromParam1() {
		String username = (null != System.getProperty("PARAM1")) ? System
				.getProperty("PARAM1") : System.getenv("PARAM1");
		return username;
	}

}
