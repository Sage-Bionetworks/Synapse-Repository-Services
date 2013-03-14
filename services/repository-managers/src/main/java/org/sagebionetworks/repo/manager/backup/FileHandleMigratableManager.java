package org.sagebionetworks.repo.manager.backup;

import java.io.InputStream;
import java.io.OutputStream;

import org.sagebionetworks.repo.model.backup.FileHandleBackup;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author John
 *
 */
public class FileHandleMigratableManager implements MigratableManager {
	
	@Autowired
	FileHandleDao fileHandleDao;
	
	public FileHandleMigratableManager(){
		
	}

	/**
	 * IoC constructor.
	 * @param fileHandleDao
	 */
	public FileHandleMigratableManager(FileHandleDao fileHandleDao) {
		super();
		this.fileHandleDao = fileHandleDao;
	}

	@Override
	public void writeBackupToOutputStream(String idToBackup, OutputStream out) {
		// Capture the FileHandle backup
		try {
			FileHandleBackup backup = fileHandleDao.getFileHandleBackup(idToBackup);
			// Write this object to the stream.
			NodeSerializerUtil.writeToStream(backup, out);
		} catch (Exception e) {
			// Convert any exception to a runtime.
			throw new RuntimeException(e);
		}
	}

	@Override
	public String createOrUpdateFromBackupStream(InputStream in) {
		FileHandleBackup backup = NodeSerializerUtil.readFromStream(in, FileHandleBackup.class);
		// Send it to the dao.
		fileHandleDao.createOrUpdateFromBackup(backup);
		return backup.getId().toString();
	}

	@Override
	public void deleteByMigratableId(String id) {
		fileHandleDao.delete(id);
	}

}
