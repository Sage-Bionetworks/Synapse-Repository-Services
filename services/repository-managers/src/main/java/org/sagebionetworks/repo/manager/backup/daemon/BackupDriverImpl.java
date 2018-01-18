package org.sagebionetworks.repo.manager.backup.daemon;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.manager.backup.Progress;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupAliasType;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Migration driver updates the progress and read/writes migration data to zip files.
 * 
 * @author John
 * 
 */
@Deprecated
public class BackupDriverImpl implements BackupDriver {

	static private Log log = LogFactory.getLog(BackupDriverImpl.class);

	private static final String ZIP_ENTRY_SUFFIX = ".xml";

	@Autowired
	private MigrationManager migrationManager;

	/**
	 * Write the objects identified by the passed list to the provided zip file.
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
	@Override
	public boolean writeBackup(UserInfo user, File destination, Progress progress, MigrationType type, List<Long> idsToBackup, BackupAliasType backupAliasType) throws IOException, InterruptedException {
		if (destination == null)
			throw new IllegalArgumentException(
					"Destination file cannot be null");
		if (!destination.exists())
			throw new IllegalArgumentException(
					"Destination file does not exist: "
							+ destination.getAbsolutePath());

		log.info("Starting a backup to file: " + destination.getAbsolutePath());
		progress.appendLog("Starting a backup to file: "
				+ destination.getAbsolutePath());
		progress.setTotalCount(idsToBackup.size());
		checkForTermination(progress);
		// First write to the file
		FileOutputStream fos = new FileOutputStream(destination);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		try {
//			progress.appendLog(idsToBackup.toString());
			progress.setMessage("Backup id count: "+idsToBackup.size());
			ZipEntry entry = new ZipEntry(type.name() + ZIP_ENTRY_SUFFIX);
			zos.putNextEntry(entry);
			Writer zipWriter = new OutputStreamWriter(zos, "UTF-8");
			migrationManager.writeBackupBatch(user, type, idsToBackup, zipWriter, backupAliasType);
			zipWriter.flush();
			progress.incrementProgress();
			// If this type has secondary types then add them to the zip as well.
			List<MigrationType> secondaryTypes = migrationManager.getSecondaryTypes(type);
			if(secondaryTypes != null){
				for(MigrationType secondary: secondaryTypes){
					checkForTermination(progress);
					Thread.yield();
					entry = new ZipEntry(secondary.name() + ZIP_ENTRY_SUFFIX);
					zos.putNextEntry(entry);
					zipWriter = new OutputStreamWriter(zos, "UTF-8");
					migrationManager.writeBackupBatch(user, secondary, idsToBackup, zipWriter, backupAliasType);
					zipWriter.flush();
					progress.incrementProgress();
				}
			}
			zipWriter.close();
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

	@WriteTransaction
	@Override
	public boolean restoreFromBackup(UserInfo user, File source, Progress progress, BackupAliasType backupAliasType) throws Exception {
		if (source == null)
			throw new IllegalArgumentException("Source file cannot be null");
		if (!source.exists())
			throw new IllegalArgumentException("Source file dose not exist: "
					+ source.getAbsolutePath());
		if (progress == null)
			throw new IllegalArgumentException("Progress cannot be null");
		FileInputStream fis = new FileInputStream(source);
		try {
			log.info("Restoring: " + source.getAbsolutePath());
			progress.appendLog("Restoring: " + source.getAbsolutePath());
			// First clear all data
			ZipInputStream zin = new ZipInputStream(
					new BufferedInputStream(fis));
			progress.setMessage("Reading: " + source.getAbsolutePath());
			progress.setCurrentIndex(0);
			progress.setTotalCount(source.length());

			ZipEntry entry;
			progress.appendLog("Processing:");
			while ((entry = zin.getNextEntry()) != null) {
				progress.setMessage(entry.getName());
				checkForTermination(progress);

				MigrationType type = getTypeFromFileName(entry.getName());
				
				if (! migrationManager.isMigrationTypeUsed(user, type)) {
					// This is a entry for a type at the source that does not exist at destination, skip
					progress.appendLog("Skipping entry " + entry.getName() + ", unused migration type.");
				} else {
					// This is a backup file.
					List<Long> primaryIds = migrationManager.createOrUpdateBatch(user, type, zin, backupAliasType);
					// If this is a primary type then we must clear all data for secondary types
					// that have these backup ids.
					List<MigrationType> secondaryTypes = migrationManager.getSecondaryTypes(type);
					if(secondaryTypes != null){
						for(MigrationType secondary: secondaryTypes){
							migrationManager.deleteObjectsById(user, secondary, primaryIds);
						}
					}
				}

				progress.incrementProgressBy(entry.getCompressedSize());
				if (log.isTraceEnabled()) {
					log.trace(progress.toString());
				}
				// This is run in a tight loop so to be CPU friendly we should
				// yield
				Thread.yield();
			}
			progress.appendLog("Finished processing.");
		} finally {
			if (fis != null) {
				fis.close();
			}
		}
		return true;
	}
	
	/**
	 * Get a type from a file name.
	 * @param name
	 * @return
	 */
	public static MigrationType getTypeFromFileName(String name){
		MigrationType t = MigrationType.valueOf(name.substring(0, name.length()-ZIP_ENTRY_SUFFIX.length()));
		return t;
	}
	
	/**
	 * Create a file name for a type.
	 * @param type
	 * @return
	 */
	public static String getFileNameForType(MigrationType type){
		return type.name() + ZIP_ENTRY_SUFFIX;
	}

	private void checkForTermination(Progress progress)
			throws InterruptedException {
		// Check for termination.
		if (progress.shouldTerminate()) {
			throw new InterruptedException(
					"Restoration terminated by the user.");
		}
	}

}
