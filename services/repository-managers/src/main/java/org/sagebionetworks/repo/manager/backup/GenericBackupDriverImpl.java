package org.sagebionetworks.repo.manager.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * A reusable implementation of a backup driver.
 * 
 * This should replace all other backup drivers. Specific implementation logic is delegated to the MigratableManager.
 * 
 * Note: This is not an Auto-wired class. A new instance should be created for each MigratableManager implementation.
 * 
 * @author John
 *
 */
public class GenericBackupDriverImpl implements GenericBackupDriver {
	
	static private Log log = LogFactory.getLog(GenericBackupDriverImpl.class);
	
	private static final String ZIP_ENTRY_SUFFIX = ".xml";
	
	private MigratableManager migratableManager;
	
	/**
	 * This class is used to migrate many types of objects.
	 * @param migratableDao
	 */
	public GenericBackupDriverImpl(MigratableManager migratableDao) {
		super();
		this.migratableManager = migratableDao;
	}

	@Override
	public boolean writeBackup(File destination, Progress progress,
			Set<String> idsToBackup) throws IOException,
			DatastoreException, NotFoundException, InterruptedException {
		if (destination == null)
			throw new IllegalArgumentException(
					"Destination file cannot be null");
		if (!destination.exists())
			throw new IllegalArgumentException(
					"Destination file does not exist: "
							+ destination.getAbsolutePath());

		log.info("Starting a backup to file: " + destination.getAbsolutePath());
		progress.appendLog("Starting a backup to file: " + destination.getAbsolutePath());
		progress.setTotalCount(idsToBackup.size());
		// First write to the file
		FileOutputStream fos = new FileOutputStream(destination);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		try {
			progress.appendLog("Processing Favorites:");
			for(String idToBackup: idsToBackup){
				Thread.yield();
				progress.appendLog(idToBackup);
				progress.setMessage(idToBackup);
				
				ZipEntry entry = new ZipEntry(idToBackup+ZIP_ENTRY_SUFFIX);
				zos.putNextEntry(entry);
				migratableManager.writeBackupToOutputStream(idToBackup, zos);
				progress.incrementProgress();
				if(progress.shouldTerminate()){
					throw new InterruptedException("Backup terminated by the user.");
				}
			}
			zos.close();
			progress.appendLog("Finished processing");
		} finally {
			if (fos != null) {
				fos.flush();
				fos.close();
			}
		}
		return true;
	}

	@Override
	public boolean restoreFromBackup(File source, Progress progress)
			throws IOException, InterruptedException, DatastoreException, NotFoundException, InvalidModelException, ConflictingUpdateException {
		if(source == null) throw new IllegalArgumentException("Source file cannot be null");
		if(!source.exists()) throw new IllegalArgumentException("Source file dose not exist: "+source.getAbsolutePath());
		if(progress == null) throw new IllegalArgumentException("Progress cannot be null");
		FileInputStream fis = new FileInputStream(source);
		try{
			log.info("Restoring: "+source.getAbsolutePath());
			progress.appendLog("Restoring: "+source.getAbsolutePath());
			// First clear all data
			ZipInputStream zin = new  ZipInputStream(new BufferedInputStream(fis));
			progress.setMessage("Reading: "+source.getAbsolutePath());
			progress.setTotalCount(source.length());

			ZipEntry entry;
			progress.appendLog("Processing:");
			while((entry = zin.getNextEntry()) != null) {
				progress.setMessage(entry.getName());
				// Check for termination.
				if(progress.shouldTerminate()){
					throw new InterruptedException("Restoration terminated by the user.");
				}
				
				// This is a backup file.
				String id = migratableManager.createOrUpdateFromBackupStream(zin);
				// Append this id to the log.
				progress.appendLog(id);
				
				progress.incrementProgressBy(entry.getCompressedSize());
				if(log.isTraceEnabled()){
					log.trace(progress.toString());			
				}
				// This is run in a tight loop so to be CPU friendly we should yield				
				Thread.yield();
			}
			progress.appendLog("Finished processing.");
		}finally{
			if(fis != null){
				fis.close();
			}
		}
		return true;
	}
	

	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		// Pass along the delete
		migratableManager.deleteByMigratableId(id);
	}
}
