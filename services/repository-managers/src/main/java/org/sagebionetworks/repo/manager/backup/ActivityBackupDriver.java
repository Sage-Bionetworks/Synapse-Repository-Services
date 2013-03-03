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
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

@Deprecated // This needs to be replaced with GenericBackupDriverImpl and should not be copied.
public class ActivityBackupDriver implements GenericBackupDriver {
	
	@Autowired
	private ActivityDAO activityDAO;	

	public ActivityBackupDriver() {}

	/**
	 * For testing
	 * @param activityDAO
	 */
	public ActivityBackupDriver(ActivityDAO activityDAO) {
		this.activityDAO = activityDAO;
	}

	static private Log log = LogFactory.getLog(ActivityBackupDriver.class);
	
	private static final String ZIP_ENTRY_SUFFIX = ".xml";
	
	@Override
	public boolean writeBackup(File destination, Progress progress,
			Set<String> activityIdsToBackup) throws IOException,
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
		progress.setTotalCount(activityIdsToBackup.size());
		// First write to the file
		FileOutputStream fos = new FileOutputStream(destination);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		try {
			progress.appendLog("Processing activities:");
			for(String idToBackup: activityIdsToBackup){
				Thread.yield();
				progress.appendLog(idToBackup);
				progress.setMessage(idToBackup);
				
				Activity activity = activityDAO.get(idToBackup);												
				ZipEntry entry = new ZipEntry(idToBackup+ZIP_ENTRY_SUFFIX);
				zos.putNextEntry(entry);
				NodeSerializerUtil.writeActivityBackup(activity, zos);
				progress.incrementProgress();
				if(progress.shouldTerminate()){
					throw new InterruptedException("Activities Backup terminated by the user.");
				}
			}
			zos.close();
			progress.appendLog("Finished processing Activities.");
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
			progress.appendLog("Processing activities:");
			while((entry = zin.getNextEntry()) != null) {
				progress.setMessage(entry.getName());
				// Check for termination.
				if(progress.shouldTerminate()){
					throw new InterruptedException("Activity restoration terminated by the user.");
				}
				
				// This is a backup file.
				Activity backup = NodeSerializerUtil.readActivityBackup(zin);				
				createOrUpdateActivity(backup);
				
				// Append this id to the log.
				progress.appendLog(backup.getId().toString());
				
				progress.incrementProgressBy(entry.getCompressedSize());
				if(log.isTraceEnabled()){
					log.trace(progress.toString());			
				}
				// This is run in a tight loop so to be CPU friendly we should yield				
				Thread.yield();
			}
			progress.appendLog("Finished processing activities.");
		}finally{
			if(fis != null){
				fis.close();
			}
		}
		return true;
	}
	
	private void createOrUpdateActivity(Activity act) throws DatastoreException, NotFoundException, InvalidModelException, ConflictingUpdateException {
		// create the activity		
		Activity existingActivity = null;
		try {
			existingActivity = activityDAO.get(act.getId().toString());
		} catch (NotFoundException e) {
			existingActivity = null;
		}
		if (null==existingActivity) {
			// create
			activityDAO.create(act);
		} else {
			// Update only when backup is different from the current system
			if (!existingActivity.getEtag().equals(act.getEtag())) {
				act = activityDAO.updateFromBackup(act);
			}
		}		
	}

	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		activityDAO.delete(id);
	}

}
