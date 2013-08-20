package org.sagebionetworks.repo.model.dao;

import java.net.MalformedURLException;
import java.util.List;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.backup.FileHandleBackup;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for creating/updating/reading/deleting CRUD metadata about files. 
 * 
 * @author John
 *
 */
public interface FileHandleDao {
	
	/**
	 * Create S3 file metadata.
	 * 
	 * @param metadata
	 * @return
	 * @throws MalformedURLException 
	 */
	public <T extends FileHandle> T createFile(T metadata);
	
	/**
	 * Set the preview ID of a file.
	 * @param fileId
	 * @param previewId
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public void setPreviewId(String fileId, String previewId) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the file metadata by ID.
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public FileHandle get(String id) throws DatastoreException, NotFoundException;
	
	/**
	 * Get all of the file handles for a given list of IDs.
	 * @param ids - The list of FileHandle ids to fetch.
	 * @param includePreviews - When true, any preview handles will associated with each handle will also be returned.
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public FileHandleResults getAllFileHandles(List<String> ids, boolean includePreviews) throws DatastoreException, NotFoundException;

	/**
	 * Delete the file metadata.
	 * @param id
	 */
	public void delete(String id);
	
	/**
	 * Does the given file object exist?
	 * @param id
	 * @return true if it exists.
	 */
	public boolean doesExist(String id);

	/**
	 * Lookup the creator of a FileHandle.
	 * @param fileHandleId
	 * @return
	 * @throws NotFoundException 
	 */
	public String getHandleCreator(String fileHandleId) throws NotFoundException;

	/**
	 * Get the preview associated with a given file handle.
	 * 
	 * @param handleId
	 * @return
	 */
	public String getPreviewFileHandleId(String handleId) throws NotFoundException;

	/**
	 * Get the backup representation of a FileHandle
	 * @param idToBackup
	 * @return
	 * @throws NotFoundException 
	 */
	public FileHandleBackup getFileHandleBackup(String idToBackup) throws NotFoundException;
	
	/**
	 * 
	 * @param backup
	 */
	public boolean createOrUpdateFromBackup(FileHandleBackup backup);
	
	/**
	 * Find a FileHandle using the key and MD5
	 * @param key
	 * @param md5
	 * @return
	 */
	public List<String> findFileHandleWithKeyAndMD5(String key, String md5);

	long getCount() throws DatastoreException;

	long getMaxId() throws DatastoreException;
}
