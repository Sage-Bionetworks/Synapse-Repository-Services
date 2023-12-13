package org.sagebionetworks.db.pool;

import org.apache.commons.dbcp2.BasicDataSource;
import org.sagebionetworks.repo.model.DatabaseConnectionPoolStats;

/**
 * A basic implementation of {@link DatabaseConnectionPoolStats} that wraps a
 * {@link BasicDataSource}
 * 
 *
 */
public class Dbcp2DatabaseConnectionPoolStats implements DatabaseConnectionPoolStats {

	private BasicDataSource pool;
	private DatabaseType databaseType;
	private PoolType poolType;

	@Override
	public int getNumberOfIdleConnections() {
		return this.pool.getNumIdle();
	}

	@Override
	public int getNumberOfActiveConnections() {
		return this.pool.getNumActive();
	}

	@Override
	public DatabaseType getDatabaseType() {
		return databaseType;
	}

	@Override
	public PoolType getPoolType() {
		return poolType;
	}

	public Dbcp2DatabaseConnectionPoolStats setPool(BasicDataSource pool) {
		this.pool = pool;
		return this;
	}

	public Dbcp2DatabaseConnectionPoolStats setDatabaseType(DatabaseType databaseType) {
		this.databaseType = databaseType;
		return this;
	}

	public Dbcp2DatabaseConnectionPoolStats setPoolType(PoolType poolType) {
		this.poolType = poolType;
		return this;
	}

}
