package org.sagebionetworks.table.cluster.config;

import java.util.List;

import org.apache.commons.dbcp2.BasicDataSource;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.table.cluster.InstanceDiscovery;
import org.sagebionetworks.table.cluster.InstanceInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TableClusterConfig {
	
	@Bean
	public BasicDataSource tableDatabaseConnectionPool(StackConfiguration stackConfig, InstanceDiscovery instanceDiscovery) {
		// There is nothing to do if the table feature is not enabled.
		// The features is enabled so we must find all database instances that we can use
		List<InstanceInfo> instances = instanceDiscovery.discoverAllInstances();
		if (instances == null || instances.isEmpty())
			throw new IllegalArgumentException("Did not find at least one database instances.");

		// This will be improved in the future. For now we just use the first database we find
		InstanceInfo instance = instances.get(0);
		// Use the one instance to create a single connection pool
		return createNewDatabaseConnectionPool(stackConfig, instance);
	}
	
	public static BasicDataSource createNewDatabaseConnectionPool(StackConfiguration config, InstanceInfo info){
		// Create a database connection pool.
		BasicDataSource connectionPool = new BasicDataSource();
		connectionPool.setDriverClassName(config.getTableDatabaseDriver());
		connectionPool.setUrl(info.getUrl());
		// For now we use the same username and password as repo.
		connectionPool.setUsername(config.getRepositoryDatabaseUsername());
		connectionPool.setPassword(config.getRepositoryDatabasePassword());
		connectionPool.setMinIdle(Integer.parseInt(config.getDatabaseConnectionPoolMinNumberConnections()));
		connectionPool.setMaxTotal(Integer.parseInt(config.getDatabaseConnectionPoolMaxNumberConnections()));
		connectionPool.setMaxIdle(Integer.parseInt(config.getDatabaseConnectionPoolMaxNumberConnections()));
		connectionPool.setTestOnBorrow(Boolean.parseBoolean(config.getDatabaseConnectionPoolShouldValidate()));
		connectionPool.setValidationQuery(config.getDatabaseConnectionPoolValidateSql());
		return connectionPool;
	}

}
