package org.sagebionetworks.repo.manager.backup.daemon;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.sagebionetworks.repo.manager.backup.Progress;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Abstraction for a backup driver.
 * @author jmhill
 *
 */
@Deprecated
public interface BackupDriver {

	/**
	 * Write the objects identified by the passed list to the provided zip file.
	 * 
	 * @param user
	 * @param destination
	 * @param progress
	 * @param type
	 * @param idsToBackup
	 * @param backupAliasType
	 * @return
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public boolean writeBackup(UserInfo user, File destination, Progress progress, MigrationType type, List<Long> idsToBackup, BackupAliasType backupAliasType) throws IOException, InterruptedException;
	
	/**
	 * 
	 * @param type
	 * @param user
	 * @param source
	 * @param progress
	 * @param backupAliasType
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws Exception 
	 */
	public boolean restoreFromBackup(UserInfo user, File source, Progress progress, BackupAliasType backupAliasType) throws IOException, InterruptedException, Exception;
}
