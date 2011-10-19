package org.sagebionetworks.repo.manager.backup;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * An implementation of this interface can server as a backup source.
 * @author jmhill
 *
 */
public interface NodeBackupSource {
	
	/**
	 * Get the root node.
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public NodeBackup getRoot() throws DatastoreException, NotFoundException;
	
	/**
	 * Fetch a node using its Id
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public NodeBackup getNode(String id) throws NotFoundException, DatastoreException;
	
	
	/**
	 * Get a specific revision of a node.
	 * @param nodeId
	 * @param revisionId
	 * @return
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public NodeRevisionBackup getNodeRevision(String nodeId, Long revisionId) throws NotFoundException, DatastoreException;
	
	/**
	 * How many nodes are there?  This is used to track progress.
	 * @return
	 */
	public long getTotalNodeCount();


}
