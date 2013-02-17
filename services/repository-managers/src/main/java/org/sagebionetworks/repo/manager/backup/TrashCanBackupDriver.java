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
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.dbo.dao.DBOTrashCanBackupDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class TrashCanBackupDriver implements GenericBackupDriver {

	@Autowired
	private DBOTrashCanBackupDao trashCanBackupDao;

	private Log log = LogFactory.getLog(ActivityBackupDriver.class);

	private static final String ZIP_ENTRY_SUFFIX = ".xml";

	@Override
	public boolean writeBackup(File destination, Progress progress,
			Set<String> entitiesToBackup) throws IOException,
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
		progress.setTotalCount(entitiesToBackup.size());
		// First write to the file
		FileOutputStream fos = new FileOutputStream(destination);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));

		try {
			progress.appendLog("Processing trashed entities:");
			for(String idToBackup: entitiesToBackup){
				progress.appendLog(idToBackup);
				progress.setMessage(idToBackup);
				TrashedEntity trash = trashCanBackupDao.get(idToBackup);											
				ZipEntry entry = new ZipEntry(idToBackup+ZIP_ENTRY_SUFFIX);
				zos.putNextEntry(entry);
				NodeSerializerUtil.writeTrashedEntityBackup(trash, zos);
				progress.incrementProgress();
				if(progress.shouldTerminate()){
					throw new InterruptedException("Trash entites backup terminated by the user.");
				}
			}
			zos.close();
			progress.appendLog("Finished processing trashed entities.");
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
			throws IOException, InterruptedException, DatastoreException,
			NotFoundException, InvalidModelException,
			ConflictingUpdateException {
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
				TrashedEntity backup = NodeSerializerUtil.readTrashedEntityBackup(zin);				
				trashCanBackupDao.update(backup);

				// Append this id to the log.
				progress.appendLog(backup.getEntityId().toString());
				
				progress.incrementProgressBy(entry.getCompressedSize());
				if(log.isTraceEnabled()){
					log.trace(progress.toString());			
				}
			}
			progress.appendLog("Finished processing activities.");
		}finally{
			if(fis != null){
				fis.close();
			}
		}
		return true;
	}

	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		trashCanBackupDao.delete(id);
	}
}
