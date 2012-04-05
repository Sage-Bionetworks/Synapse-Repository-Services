package org.sagebionetworks.repo.manager;

import java.io.File;

import org.sagebionetworks.repo.model.DatastoreException;

/**
 * This is a utility that uploads and downloads files from S3.
 * @author jmhill
 *
 */
public interface AmazonS3Utility {
	
	/**
	 * Downloads the passed key from S3.
	 * @param key
	 * @return The resulting file is a temp file that should be deleted when finished with it.
	 * @throws DatastoreException 
	 */
	public File downloadFromS3(String key) throws DatastoreException;
	
	/**
	 * Does the given key exist?
	 * @param key
	 * @return
	 */
	public boolean doesExist(String key);
	
	/**
	 * Uploads the passed file to S3 using the passed key
	 * @param toUpload
	 * @param key
	 */
	public boolean uploadToS3(File toUpload, String key);
	
	/**
	 * Cleanup a file from S3
	 * @return
	 */
	public boolean deleteFromS3(String key);

}
