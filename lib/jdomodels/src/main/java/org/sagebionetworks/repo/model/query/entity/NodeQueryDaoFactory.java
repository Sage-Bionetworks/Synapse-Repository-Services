package org.sagebionetworks.repo.model.query.entity;

/**
 * Abstraction for creating a query DAO connected to the index cluster.
 *
 */
public interface NodeQueryDaoFactory {

	/**
	 * Create a connection for a node query.
	 * 
	 * @return
	 */
	public NodeQueryDaoV2 createConnection();
}
