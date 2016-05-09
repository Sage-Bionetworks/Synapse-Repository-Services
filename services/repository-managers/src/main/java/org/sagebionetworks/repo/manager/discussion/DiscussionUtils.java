package org.sagebionetworks.repo.manager.discussion;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.util.ValidateArgument;

public class DiscussionUtils {
	private static final Pattern USER_MENTION_PATTERN = Pattern.compile("@\\S+");

	public static List<String> getMentionedUsername(String markdown) {
		ValidateArgument.required(markdown, "markdown");
		List<String> mentioned = new ArrayList<String>();
		Matcher matcher = USER_MENTION_PATTERN.matcher(markdown);
		while (matcher.find()) {
			mentioned.add(matcher.group().replace("@", ""));
		}
		return mentioned;
	}

}
