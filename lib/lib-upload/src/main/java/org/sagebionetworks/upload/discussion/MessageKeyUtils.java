package org.sagebionetworks.upload.discussion;

import java.util.UUID;

import org.apache.commons.lang3.math.NumberUtils;
import org.sagebionetworks.util.ValidateArgument;

public class MessageKeyUtils {

	private static final String THREAD_KEY_FORMAT = "%1$s/%2$s/%3$s";
	private static final String REPLY_KEY_FORMAT = "%1$s/%2$s/%3$s/%4$s";

	public static String getThreadId(String key) {
		String[] parts = validateThreadKey(key);
		return parts[1];
	}

	public static String getReplyId(String key) {
		String[] parts = validateReplyKey(key);
		return parts[2];
	}

	public static String generateThreadKey(String forumId, String threadId) {
		ValidateArgument.required(forumId, "forumId");
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.requirement(NumberUtils.isDigits(forumId), "forumId must be a number");
		ValidateArgument.requirement(NumberUtils.isDigits(threadId), "threadId must be a number");
		return String.format(THREAD_KEY_FORMAT, forumId, threadId, UUID.randomUUID().toString());
	}

	public static String generateReplyKey(String forumId, String threadId, String replyId) {
		ValidateArgument.required(forumId, "forumId");
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(replyId, "replyId");
		ValidateArgument.requirement(NumberUtils.isDigits(forumId), "forumId must be a number");
		ValidateArgument.requirement(NumberUtils.isDigits(threadId), "threadId must be a number");
		ValidateArgument.requirement(NumberUtils.isDigits(replyId), "replyId must be a number");
		return String.format(REPLY_KEY_FORMAT, forumId, threadId, replyId, UUID.randomUUID().toString());
	}

	public static String[] validateThreadKey(String key) {
		ValidateArgument.required(key, "key");
		String[] parts = key.split("/");
		ValidateArgument.requirement(parts.length == 3, "A thread message key must have 3 parts");
		ValidateArgument.requirement(NumberUtils.isDigits(parts[0]), "forumId must be a number");
		ValidateArgument.requirement(NumberUtils.isDigits(parts[1]), "threadId must be a number");
		ValidateArgument.requirement(!parts[2].equals(""), "UUID must be not be empty");
		return parts;
	}

	public static String[] validateReplyKey(String key) {
		ValidateArgument.required(key, "key");
		String[] parts = key.split("/");
		ValidateArgument.requirement(parts.length == 4, "A reply message key must have 4 parts");
		ValidateArgument.requirement(NumberUtils.isDigits(parts[0]), "forumId must be a number");
		ValidateArgument.requirement(NumberUtils.isDigits(parts[1]), "threadId must be a number");
		ValidateArgument.requirement(NumberUtils.isDigits(parts[2]), "replyId must be a number");
		ValidateArgument.requirement(!parts[3].equals(""), "UUID must be not be empty");
		return parts;
	}
}
