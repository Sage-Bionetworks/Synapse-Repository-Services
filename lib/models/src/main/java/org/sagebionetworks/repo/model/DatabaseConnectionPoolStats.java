package org.sagebionetworks.repo.model;

/**
 * Abstraction to provide basic statistics about a database connection pool.
 *
 */
public interface DatabaseConnectionPoolStats {

	public enum DatabaseType {
		idgen, main, tables
	}
	
	public enum PoolType {
		standard, migration
	}

	/**
	 * The type of database that this pool is connecting to.
	 * 
	 * @return
	 */
	DatabaseType getDatabaseType();
	
	/**
	 * The type of connection pool used.
	 * @return
	 */
	PoolType getPoolType();

	/**
	 * The number of connections that are currently idle in the pool.
	 * 
	 * @return
	 */
	int getNumberOfIdleConnections();

	/**
	 * The number of connections that are currently in use and are no longer in
	 * available in the pool.
	 * 
	 * @return
	 */
	int getNumberOfActiveConnections();
}
