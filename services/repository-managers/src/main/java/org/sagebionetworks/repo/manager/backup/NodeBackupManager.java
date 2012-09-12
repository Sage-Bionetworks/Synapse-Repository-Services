package org.sagebionetworks.repo.manager.backup;

/**
 * Acts as a node backup source for creating a backup and a node backup destination
 * for restoring from a backup.
 * 
 * @author jmhill
 *
 */
public interface NodeBackupManager extends NodeBackupSource, NodeBackupDestination {

}
