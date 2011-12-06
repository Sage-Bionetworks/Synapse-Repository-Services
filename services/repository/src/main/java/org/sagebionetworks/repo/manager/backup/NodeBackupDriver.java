package org.sagebionetworks.repo.manager.backup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeBackup;
import org.sagebionetworks.repo.web.NotFoundException;

public interface NodeBackupDriver {
	
	/**
	 * Create a backup, writing the results to the passed destination file.
	 * @param destination - The file that the backup will be written too.
	 * @param progress - Used to track the progress of the backup.
	 * @throws FileNotFoundException 
	 * @throws IOException 
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 * @throws InterruptedException 
	 */
	public boolean writeBackup(File destination, Progress progress, Set<String> entitiesToBackup) throws IOException, DatastoreException, NotFoundException, InterruptedException;
	
	/**
	 * Restore all data from the backup file.  Any node that does not exist will
	 * be created.  If the node already exists it will be updated.
	 * @param source - The backup file that is to be used to restore.
	 * @param progress - Used to track the progress of the restore.
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public boolean restoreFromBackup(File source, Progress progress) throws IOException, InterruptedException;

}
