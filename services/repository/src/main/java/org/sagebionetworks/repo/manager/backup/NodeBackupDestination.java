package org.sagebionetworks.repo.manager.backup;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevisionBackup;
import org.sagebionetworks.repo.web.NotFoundException;

public interface NodeBackupDestination {
	
	/**
	 * Create or update a node with revisions.	
	 * @param backup
	 * @param revisions
	 */
	public void createOrUpdateNodeWithRevisions(NodeBackup backup, List<NodeRevisionBackup> revisions);
	
	/**
	 * Clear all data in preparation for the restore.
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void clearAllData();

}
