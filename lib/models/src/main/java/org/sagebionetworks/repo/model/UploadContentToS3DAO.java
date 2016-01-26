package org.sagebionetworks.repo.model;

import java.io.IOException;

public interface UploadContentToS3DAO {

	/**
	 * upload thread message to a file in S3
	 * 
	 * @param content to upload
	 * @param forumId
	 * @param threadId
	 * @return the S3 key
	 * @throws IOException 
	 */
	public String uploadThreadMessage(String content, String forumId, String threadId) throws IOException;

	/**
	 * upload reply message to a file in S3
	 * 
	 * @param content to upload
	 * @param forumId
	 * @param threadId
	 * @param replyId
	 * @return the S3 key
	 * @throws IOException 
	 */
	public String uploadReplyMessage(String content, String forumId, String threadId, String replyId) throws IOException;
	/**
	 * get the URL from a reply key
	 * 
	 * @param key
	 * @return
	 */
	public String getReplyUrl(String key);

	/**
	 * get the URL from a thread key
	 * 
	 * @param key
	 * @return
	 */
	public String getThreadUrl(String key);
}
