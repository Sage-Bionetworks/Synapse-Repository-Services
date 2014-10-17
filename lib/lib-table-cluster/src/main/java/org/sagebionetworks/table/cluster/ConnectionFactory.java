package org.sagebionetworks.table.cluster;

import org.sagebionetworks.repo.model.dao.table.CurrentRowCacheDao;


/**
 * The connection factory provides database connections to the cluster of database used to support the Table feature.
 * @author jmhill
 *
 */
public interface ConnectionFactory {

	/**
	 * Get a connection used for interacting with a given table.
	 * 
	 * @param tableId
	 * @return
	 */
	TableIndexDAO getConnection(String tableId);
	
	/**
	 * Get a connection used for interacting with a given tables current row cache.
	 * 
	 * @param tableId
	 * @return
	 */
	CurrentRowCacheDao getCurrentRowCacheConnection(Long tableId);

	/**
	 * Get all unique connections used for interacting current tables
	 * 
	 * @param tableId
	 * @return
	 */
	Iterable<CurrentRowCacheDao> getCurrentRowCacheConnections();

	/**
	 * Drop all tables in every database connectoin.
	 * 
	 */
	void dropAllTablesForAllConnections();
}
