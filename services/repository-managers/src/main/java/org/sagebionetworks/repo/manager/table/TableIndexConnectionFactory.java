package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.entity.IdAndVersion;

/**
 * The 'truth' of a Synapse table consists of metadata in the main repository
 * RDS and changes sets that consist of compressed CSV files stored in S3. Each
 * change set is tracked in the main repository database. In order to query a
 * table, an 'index' must first be built from a table's metadata and change
 * sets. The 'index' of a table will consist of one or more relational tables
 * that reside in one of many database instances in a cluster of database.
 * 
 * A load balancer is use to distribute the index of each table across the
 * cluster of relational database as needed. Each table will be assigned a
 * single instance in the cluster to house its index data. A connection factory
 * is used to establish a connection to the database instance assigned to a
 * table's index.
 * 
 * In order to work with a table's index a connection to the assigned relational
 * database instance must be created using the connection factory. An instance
 * of this class will wrap such a connection and provide support for all
 * operations on a single table's index. A new instance must be acquired from
 * the connection factory for each table. A table can be assigned to a different
 * relational instances so, connections to a table's index should not be
 * persisted in any way.
 *
 */
public interface TableIndexConnectionFactory {

	/**
	 * Acquire an index manager that wraps a connection to the given table's
	 * assigned instances in the relational database cluster.
	 * 
	 * @param tableId
	 * @return
	 */
	TableIndexManager connectToTableIndex(IdAndVersion tableId) throws TableIndexConnectionUnavailableException;

	/**
	 * Acquire an index manager that wraps a connection to the first database in the cluster.
	 * 
	 * @return
	 */
	TableIndexManager connectToFirstIndex();

}
