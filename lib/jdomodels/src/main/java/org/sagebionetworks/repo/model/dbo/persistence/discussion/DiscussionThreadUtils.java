package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.util.ValidateArgument;

public class DiscussionThreadUtils {
	public static final Charset UTF8 = Charset.forName("UTF-8");

	/**
	 * 
	 * @param createThread - the object to validate
	 */
	public static void validateCreateThreadAndThrowException(
			CreateDiscussionThread createThread) {
		ValidateArgument.requirement(createThread.getForumId() != null, "forumId can not be null");
		ValidateArgument.requirement(createThread.getTitle() != null, "title cannot be null");
		ValidateArgument.requirement(createThread.getMessageMarkdown() != null, "message cannot be null");
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
		ValidateArgument.requirement(forumId != null, "forumId cannot be null");
		ValidateArgument.requirement(title != null, "title cannot be null");
		ValidateArgument.requirement(messageUrl != null, "messageUrl cannot be null");
		ValidateArgument.requirement(userId != null, "userId cannot be null");
		ValidateArgument.requirement(id != null, "id cannot be null");
		ValidateArgument.requirement(etag != null, "etag cannot be null");
		DBODiscussionThread dbo = new DBODiscussionThread();
		dbo.setId(Long.parseLong(id));
		dbo.setForumId(Long.parseLong(forumId));
		dbo.setTitle(title.getBytes(UTF8));
		dbo.setMessageUrl(messageUrl);
		dbo.setCreatedBy(userId);
		dbo.setIsEdited(false);
		dbo.setIsDeleted(false);
		dbo.setEtag(etag);
		return dbo;
	}

	/**
	 * convert an input String to a list of String
	 * the input has the following format:
	 * <string1>,<string2>,...
	 * 
	 * @param inputString
	 * @return
	 */
	public static List<String> toList(String inputString) {
		if (inputString == null) throw new IllegalArgumentException("input string cannot be null");
		List<String> list = new ArrayList<String>();
		if (inputString.equals("")) return list;
		String[] elements = inputString.split(",");
		for (String string : elements) {
			list.add(string);
		}
		return list;
	}

	/**
	 * convert a list of String to a String with format:
	 * <string1>,<string2>,...
	 * 
	 * @param listOfString
	 * @return
	 */
	public static String toString(List<String> listOfString) {
		String result = "";
		if (listOfString.isEmpty()) return result;
		if (listOfString.size() == 1) return listOfString.get(0);
		result += listOfString.get(0);
		for (int i = 1; i < listOfString.size(); i++) {
			result += ","+listOfString.get(i);
		}
		return result;
	}
}
