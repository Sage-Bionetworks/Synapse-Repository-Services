package org.sagebionetworks.table.cluster;

import org.apache.commons.dbcp.BasicDataSource;
import org.sagebionetworks.StackConfiguration;

import com.amazonaws.services.rds.model.DBInstance;

public class InstanceUtils {

	public static final String DATABASE_INSTANCE_NAME_TEMPALTE = "%1$s-%2$s-table-%3$d";
	public static final String DATABASE_SCHEMA_NAME_TEMPALTE = "%1$s%2$s";
	public static final String DATABASE_URL_NAME_TEMPALTE = "jdbc:mysql://%1$s/%2$s?rewriteBatchedStatements=true";
	

	/**
	 * Create the database instances stack identifier.
	 * @param stack
	 * @param instanceNumber
	 * @param index
	 * @return
	 */
	public static String createDatabaseInstanceIdentifier(String stack, String instance, int index){
		return String.format(DATABASE_INSTANCE_NAME_TEMPALTE, stack, instance, index);
	}
	
	/**
	 * The default database schema name (or database name).
	 * 
	 * @param stack
	 * @param instance
	 * @return
	 */
	public static String createDatabaseSchemaName(String stack, String instance){
		return String.format(DATABASE_SCHEMA_NAME_TEMPALTE, stack, instance);
	}
	
	/**
	 * Create a database connection URL from an endpoint and schema.
	 * @param endpoint
	 * @param schema
	 * @return
	 */
	public static String createDatabaseConnectionURL(String endpoint, String schema){
		return String.format(DATABASE_URL_NAME_TEMPALTE, endpoint, schema);
	}
	
	/**
	 * Create the database instances stack identifier from the stack configuration.
	 * 
	 * @param index
	 * @return
	 */
	public static String createDatabaseInstanceIdentifier(int index){
		return createDatabaseInstanceIdentifier(StackConfiguration.getStack(), StackConfiguration.getStackInstance(), index);
	}
	
	/**
	 * Create a new database connection pool to a given DBInstance. 
	 * @param config
	 * @param instnace
	 * @return
	 */
	public static BasicDataSource createNewDatabaseConnectionPool(StackConfiguration config, DBInstance instance){
		// Create a database connection pool.
		BasicDataSource connectionPool = new BasicDataSource();
		connectionPool.setDriverClassName(config.getTableDatabaseDriver());
		String schema = createDatabaseSchemaName(config.getStack(), config.getStackInstance());
		String url = createDatabaseConnectionURL(instance.getEndpoint().getAddress(), schema);
		connectionPool.setUrl(url);
		// For now we use the same username and password as repo.
		connectionPool.setUsername(config.getRepositoryDatabaseUsername());
		connectionPool.setPassword(config.getRepositoryDatabasePassword());
		connectionPool.setMinIdle(Integer.parseInt(config.getDatabaseConnectionPoolMinNumberConnections()));
		connectionPool.setMaxActive(Integer.parseInt(config.getDatabaseConnectionPoolMaxNumberConnections()));
		connectionPool.setTestOnBorrow(Boolean.parseBoolean(config.getDatabaseConnectionPoolShouldValidate()));
		connectionPool.setValidationQuery(config.getDatabaseConnectionPoolValidateSql());
		return connectionPool;
	}
}
