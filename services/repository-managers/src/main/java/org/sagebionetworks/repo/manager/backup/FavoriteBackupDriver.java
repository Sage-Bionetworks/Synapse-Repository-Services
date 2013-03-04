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
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.dbo.dao.UserProfileUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

@Deprecated // This needs to be replaced with GenericBackupDriverImpl and should not be copied.
public class FavoriteBackupDriver implements GenericBackupDriver {
	
	@Autowired
	private FavoriteDAO favoriteDAO;	

	public FavoriteBackupDriver() {}

	/**
	 * For testing
	 * @param activityDAO
	 */
	public FavoriteBackupDriver(FavoriteDAO favoriteDAO) {
		this.favoriteDAO = favoriteDAO;
	}

	static private Log log = LogFactory.getLog(FavoriteBackupDriver.class);
	
	private static final String ZIP_ENTRY_SUFFIX = ".xml";
	
	@Override
	public boolean writeBackup(File destination, Progress progress,
			Set<String> favoritesIdsToBackup) throws IOException,
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
		progress.setTotalCount(favoritesIdsToBackup.size());
		// First write to the file
		FileOutputStream fos = new FileOutputStream(destination);
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
		try {
			progress.appendLog("Processing Favorites:");
			for(String idToBackup: favoritesIdsToBackup){
				Thread.yield();
				progress.appendLog(idToBackup);
				progress.setMessage(idToBackup);
				
				Favorite favorite = favoriteDAO.getIndividualFavorite(
						UserProfileUtils.getFavoritePrincipalIdFromId(idToBackup),
						UserProfileUtils.getFavoriteEntityIdFromId(idToBackup));
				ZipEntry entry = new ZipEntry(idToBackup+ZIP_ENTRY_SUFFIX);
				zos.putNextEntry(entry);
				NodeSerializerUtil.writeFavoriteBackup(favorite, zos);
				progress.incrementProgress();
				if(progress.shouldTerminate()){
					throw new InterruptedException("Favorites Backup terminated by the user.");
				}
			}
			zos.close();
			progress.appendLog("Finished processing Favorites.");
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
			progress.appendLog("Processing Favorites:");
			while((entry = zin.getNextEntry()) != null) {
				progress.setMessage(entry.getName());
				// Check for termination.
				if(progress.shouldTerminate()){
					throw new InterruptedException("Favorite restoration terminated by the user.");
				}
				
				// This is a backup file.
				Favorite backup = NodeSerializerUtil.readFavoriteBackup(zin);				
				createFavorite(backup);
				
				// Append this id to the log.
				progress.appendLog(UserProfileUtils.getFavoriteId(backup));
				
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
	
	private void createFavorite(Favorite favorite) throws DatastoreException, NotFoundException, InvalidModelException, ConflictingUpdateException {
		// create the Favorite		
		Favorite existingFavorite = null;
		try {
			existingFavorite = favoriteDAO.getIndividualFavorite(favorite.getPrincipalId(), favorite.getEntityId());
		} catch (NotFoundException e) {
			existingFavorite = null;
		}
		if (null==existingFavorite) {
			// create
			favoriteDAO.add(favorite);
		}		
	}

	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		favoriteDAO.remove(UserProfileUtils.getFavoritePrincipalIdFromId(id),
				UserProfileUtils.getFavoriteEntityIdFromId(id));

	}

}
