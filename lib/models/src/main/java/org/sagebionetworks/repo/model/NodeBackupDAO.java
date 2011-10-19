package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Interface for all Node C.R.U.D. operations.
 * 
 * @author jmhill
 * 
 */
public interface NodeBackupDAO {

	/**
	 * Get a revision of a node.
	 * 
	 * @param nodeId
	 * @param revisionId
	 * @return the revision expressed in a form suitable for backups
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public NodeRevisionBackup getNodeRevision(String nodeId, Long revisionId)
			throws NotFoundException, DatastoreException;

	/**
	 * Create a new revision from a backup.
	 * 
	 * @param rev
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void createNewRevision(NodeRevisionBackup rev)
			throws NotFoundException, DatastoreException;

	/**
	 * Update an existing revision from a backup.
	 * 
	 * @param rev
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void updateRevision(NodeRevisionBackup rev)
			throws NotFoundException, DatastoreException;

	/**
	 * Get the total node count
	 * 
	 * @return the total node count
	 */
	public long getTotalNodeCount();

}
