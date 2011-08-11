package org.sagebionetworks.repo.manager.backup;

import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.model.NodeRevision;

public interface NodeBackupDestination {
	
	/**
	 * Create or update the given node.
	 * @param backup
	 */
	public void createOrUpdateNode(NodeBackup backup);
	
	/**
	 * Create or update the given revision.
	 * @param rev
	 */
	public void createOrUpdateRevision(NodeRevision rev);

}
