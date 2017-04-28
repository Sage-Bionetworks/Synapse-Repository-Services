package org.sagebionetworks.table.cluster;

import java.util.List;

import javax.sql.DataSource;

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
	 * Drop all tables in every database connections.
	 * 
	 */
	void dropAllTablesForAllConnections();
	
	/**
	 * Get all connections.
	 * @return
	 */
	List<TableIndexDAO> getAllConnections();

	/**
	 * Get a connection to the first database.
	 * @return
	 */
	TableIndexDAO getFirstConnection();
	
	/**
	 * Get the first DataSource connection.
	 * 
	 * @return
	 */
	DataSource getFirstDataSource();
	
}
