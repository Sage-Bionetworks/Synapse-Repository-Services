package org.sagebionetworks.repo.model.dao;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.file.FileMetadata;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for creating/updating/reading/deleting CRUD metadata about files. 
 * 
 * @author John
 *
 */
public interface FileMetadataDao {
	
	/**
	 * Create S3 file metadata.
	 * 
	 * @param metadata
	 * @return
	 */
	public Long create(FileMetadata metadata);
	
	/**
	 * Set the preview ID of a file.
	 * @param fileId
	 * @param previewId
	 */
	public void setPreviewId(Long fileId, Long previewId);
	
	/**
	 * Get the file metadata by ID.
	 * @param id
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public FileMetadata get(Long id) throws DatastoreException, NotFoundException;

	/**
	 * Delete the file metadata.
	 * @param id
	 */
	public void delete(Long id);
	
	/**
	 * Does the given file object exist?
	 * @param id
	 * @return true if it exists.
	 */
	public boolean doesExist(Long id);
}
