package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;

public class DiscussionThreadUtils {
	public static final Charset UTF8 = Charset.forName("UTF-8");

	/**
	 * 
	 * @param createThread - the object to validate
	 */
	public static void validateCreateThreadAndThrowException(
			CreateDiscussionThread createThread) {
		if (createThread.getForumId() == null
				|| createThread.getTitle() == null
				|| createThread.getMessageMarkdown() == null) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Create a DBO from user's request
	 * 
	 * @param forumId
	 * @param title
	 * @param messageUrl
	 * @param userId
	 * @param id
	 * @param etag
	 * @return
	 */
	public static DBODiscussionThread createDBO(String forumId, String title,
			String messageUrl, Long userId, String id, String etag) {
		DBODiscussionThread dbo = new DBODiscussionThread();
		dbo.setId(Long.parseLong(id));
		dbo.setForumId(Long.parseLong(forumId));
		dbo.setTitle(title.getBytes(UTF8));
		if (messageUrl == null) throw new IllegalArgumentException("messageUrl must be initialized");
		dbo.setMessageUrl(messageUrl);
		if (userId == null) throw new IllegalArgumentException("userId must be initialized");
		dbo.setCreatedBy(userId);
		dbo.setIsEdited(false);
		dbo.setIsDeleted(false);
		if (etag == null) throw new IllegalArgumentException("etag must be initialized");
		dbo.setEtag(etag);
		return dbo;
	}

	/**
	 * 
	 * @param bytes
	 * @return
	 */
	public static String decompressUTF8(byte[] bytes) {
		return new String(bytes, UTF8);
	}

	/**
	 * 
	 * @param toCompress
	 * @return
	 */
	public static byte[] compressUTF8(String toCompress) {
		return toCompress.getBytes(UTF8);
	}

	/**
	 * convert an input String to a list of String, separated by comma.
	 * 
	 * @param inputString
	 * @return
	 */
	public static List<String> createList(String inputString) {
		List<String> list = new LinkedList<String>();
		inputString = inputString.replace("[", "");
		inputString = inputString.replace("]", "");
		String[] elements = inputString.split(",");
		for (String string : elements) {
			list.add(string.trim());
		}
		return list;
	}
}
