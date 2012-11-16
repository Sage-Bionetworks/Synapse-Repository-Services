package org.sagebionetworks.repo.model.dao;

import org.sagebionetworks.repo.model.file.FileMetadata;

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
	public String create(FileMetadata metadata);
	
	/**
	 * 
	 * @param toUpdate
	 * @return
	 */
	public void update(FileMetadata toUpdate);
	
	/**
	 * Get the file metadata by ID.
	 * @param id
	 * @return
	 */
	public FileMetadata get(String id);

	/**
	 * Delete the file metadata.
	 * @param id
	 */
	public void delete(String id);
}
