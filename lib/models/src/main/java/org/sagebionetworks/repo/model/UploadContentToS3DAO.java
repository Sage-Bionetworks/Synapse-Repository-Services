package org.sagebionetworks.repo.model;

import java.io.UnsupportedEncodingException;

public interface UploadContentToS3DAO {

	/**
	 * upload content to a file in S3
	 * 
	 * @param content to upload
	 * @param forumId
	 * @param threadId
	 * @return the S3 key
	 * @throws UnsupportedEncodingException 
	 */
	public String uploadDiscussionContent(String content, String forumId, String threadId) throws UnsupportedEncodingException;

	/**
	 * get the URL from key
	 * 
	 * @param key
	 * @return
	 */
	public String getUrl(String key);
}
