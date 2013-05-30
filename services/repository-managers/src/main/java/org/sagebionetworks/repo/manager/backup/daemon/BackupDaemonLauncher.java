package org.sagebionetworks.repo.manager.backup.daemon;

import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.web.NotFoundException;

public interface BackupDaemonLauncher {
	
	/**
	 * Terminate an existing backup daemon.
	 * @param user
	 * @param id
	 * @throws UnauthorizedException
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void terminate(UserInfo user, String id) throws UnauthorizedException, DatastoreException, NotFoundException;
	
	/**
	 * Get the status of a running daemon.
	 * @param username
	 * @param id
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public BackupRestoreStatus getStatus(UserInfo username, String id) throws UnauthorizedException, DatastoreException, NotFoundException;
	
	/**
	 * Start a backup daemon that will marshal all data for each object identified provided ID to a file stored on S3.
	 * This file will then be used to restore the data to a destination stack.
	 * 
	 * @param username
	 * @param type
	 * @param idsToBackup
	 * @return
	 */
	public BackupRestoreStatus startBackup(UserInfo username, MigrationType type, List<Long> idsToBackup);
	
	/**
	 * Start a restore daemon that will read data from the passed file and write it to the database.
	 * @param username
	 * @param fileName
	 * @param type
	 * @return
	 */
	public BackupRestoreStatus startRestore(UserInfo username, String fileName, MigrationType type);


}
