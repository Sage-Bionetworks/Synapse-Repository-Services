package org.sagebionetworks.repo.manager.discussion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.discussion.DiscussionThreadEntityReference;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.util.ValidateArgument;

public class DiscussionUtils {
	private static final Pattern USER_MENTION_PATTERN = Pattern.compile("@[a-zA-Z0-9_.-]+"); //characters allowable in a username (any letter, digit, underscore, period, dash)
	private static final Pattern ENTITY_REF_PATTERN = Pattern.compile("([\\W&&\\D]*)(syn\\d+)([\\W&&\\D]*)(\\s|$)", Pattern.CASE_INSENSITIVE);
	private static final int SYN_ID_GROUP_NUMBER = 2;

	/**
	 * Extract all username that are mentioned in this markdown
	 * 
	 * @param markdown
	 * @return
	 */
	public static Set<String> getMentionedUsername(String markdown) {
		ValidateArgument.required(markdown, "markdown");
		Set<String> mentioned = new HashSet<String>();
		Matcher matcher = USER_MENTION_PATTERN.matcher(markdown);
		while (matcher.find()) {
			String username = matcher.group().replace("@", "");
			if (username != "") {
				mentioned.add(username);
			}
		}
		return mentioned;
	}

	/**
	 * Extract all entityId that are referenced in this markdown
	 * 
	 * @param markdown
	 * @param threadId
	 * @return
	 */
	public static List<DiscussionThreadEntityReference> getEntityReferences(String markdown, String threadId) {
		ValidateArgument.required(markdown, "markdown");
		ValidateArgument.required(threadId, "threadId");
		List<DiscussionThreadEntityReference> refs = new ArrayList<DiscussionThreadEntityReference>();
		Matcher matcher = ENTITY_REF_PATTERN.matcher(markdown);
		while (matcher.find()) {
			Long entityId = KeyFactory.stringToKey(matcher.group(SYN_ID_GROUP_NUMBER));
			DiscussionThreadEntityReference ref = new DiscussionThreadEntityReference();
			ref.setEntityId(entityId.toString());
			ref.setThreadId(threadId);
			refs.add(ref);
		}
		return refs;
	}

}
