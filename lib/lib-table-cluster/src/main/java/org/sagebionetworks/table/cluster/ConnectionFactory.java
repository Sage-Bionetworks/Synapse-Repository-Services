package org.sagebionetworks.table.cluster;

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
	 * Drop all tables in every database connectoin.
	 * 
	 */
	void dropAllTablesForAllConnections();
}
