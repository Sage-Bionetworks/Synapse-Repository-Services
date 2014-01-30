package org.sagebionetworks.table.cluster;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

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
	SimpleJdbcTemplate getConnection(String tableId);
}
