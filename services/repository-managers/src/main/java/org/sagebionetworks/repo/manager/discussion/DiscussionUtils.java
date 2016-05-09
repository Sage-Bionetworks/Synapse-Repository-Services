package org.sagebionetworks.repo.manager.discussion;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.util.ValidateArgument;

public class DiscussionUtils {
	private static final Pattern USER_MENTION_PATTERN = Pattern.compile("@\\S+");

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

}
