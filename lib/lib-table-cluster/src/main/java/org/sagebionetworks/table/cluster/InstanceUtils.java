package org.sagebionetworks.table.cluster;

import org.apache.commons.dbcp2.BasicDataSource;
import org.sagebionetworks.StackConfiguration;

public class InstanceUtils {
	
	/**
	 * Create a new database connection pool to a given DBInstance. 
	 * @param config
	 * @param instnace
	 * @return
	 */
	public static BasicDataSource createNewDatabaseConnectionPool(StackConfiguration config, InstanceInfo info){
		// Create a database connection pool.
		BasicDataSource connectionPool = new BasicDataSource();
		connectionPool.setDriverClassName(config.getTableDatabaseDriver());
		String url = info.getUrl();
		connectionPool.setUrl(url);
		// For now we use the same username and password as repo.
		connectionPool.setUsername(config.getRepositoryDatabaseUsername());
		connectionPool.setPassword(config.getRepositoryDatabasePassword());
		connectionPool.setMinIdle(Integer.parseInt(config.getDatabaseConnectionPoolMinNumberConnections()));
		connectionPool.setMaxTotal(Integer.parseInt(config.getDatabaseConnectionPoolMaxNumberConnections()));
		connectionPool.setTestOnBorrow(Boolean.parseBoolean(config.getDatabaseConnectionPoolShouldValidate()));
		connectionPool.setValidationQuery(config.getDatabaseConnectionPoolValidateSql());
		return connectionPool;
	}
}
