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
	 * Uploads the passed file to S3 using the passed key
	 * @param toUpload
	 * @param key
	 */
	public boolean uploadToS3(File toUpload, String key);

}
