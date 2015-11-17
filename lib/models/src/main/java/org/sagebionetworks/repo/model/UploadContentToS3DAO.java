package org.sagebionetworks.repo.model;

public interface UploadContentToS3DAO {

	/**
	 * upload content to a fileName in S3s
	 * 
	 * @param content to upload
	 * @param fileName
	 * @return the URL to download the file
	 */
	public String upload(String content, String fileName);
}
