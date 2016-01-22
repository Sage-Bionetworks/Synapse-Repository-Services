package org.sagebionetworks.repo.model;

import java.io.IOException;

import org.sagebionetworks.repo.model.discussion.MessageURL;

public interface UploadContentToS3DAO {

	/**
	 * upload content to a file in S3
	 * 
	 * @param content to upload
	 * @param forumId
	 * @param threadId
	 * @return the S3 key
	 * @throws IOException 
	 */
	public String uploadDiscussionContent(String content, String forumId, String threadId) throws IOException;

	/**
	 * get the URL from key
	 * 
	 * @param key
	 * @return
	 */
	public MessageURL getUrl(String key);
}
