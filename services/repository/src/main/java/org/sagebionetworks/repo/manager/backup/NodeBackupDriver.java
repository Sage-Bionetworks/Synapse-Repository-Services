package org.sagebionetworks.repo.manager.backup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.sagebionetworks.repo.model.DatastoreException;
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
	 */
	public void writeBackup(File destination, Progress progress) throws IOException, DatastoreException, NotFoundException;
	
	/**
	 * Restore all data from the backup file.  Any node that does not exist will
	 * be created.  If the node already exists it will be updated.
	 * @param source - The backup file that is to be used to restore.
	 * @param progress - Used to track the progress of the restore.
	 * @throws IOException
	 */
	public void restoreFromBackup(File source, Progress progress) throws IOException;

}
