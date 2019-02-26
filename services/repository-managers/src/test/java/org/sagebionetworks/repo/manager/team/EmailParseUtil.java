package org.sagebionetworks.repo.manager.team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sagebionetworks.repo.manager.EmailUtils;

public class EmailParseUtil {
	
	/*
	 * This test helper function splits an email template according to the specified
	 * delimiters.  E.g. splitting "foo #delim# bar" with the (one member) delim list "#delim#"
	 * would return "foo ", "#delim#", " bar".  
	 */
	public static List<String> splitEmailTemplate(String templateName, List<String> delims) {
		List<String> result = new ArrayList<String>();
		result.add(EmailUtils.readMailTemplate(templateName, Collections.EMPTY_MAP));
		for (String delim : delims) {
			// we assume each delimiter appears at most once
			List<String> reparsed = new ArrayList<String>();
			for (String s : result) {
				int i = s.indexOf(delim);
				if (i>=0) {
					if (i>0) reparsed.add(s.substring(0, i));
					reparsed.add(delim);
					if (i+delim.length()<s.length()) reparsed.add(s.substring(i+delim.length()));
				} else {
					reparsed.add(s);
				}
			}
			result = reparsed;
		}
		return result;
	}
	
	public static String getTokenFromString(String s, String startString, String endString) throws Exception {
		int endpointIndex = s.indexOf(startString);
		int tokenStart = endpointIndex+startString.length();
		if (tokenStart<0) throw new IllegalArgumentException();
		int tokenEnd = s.indexOf(endString, tokenStart);
		if (tokenEnd<0) throw new IllegalArgumentException();
		String token = s.substring(tokenStart, tokenEnd);
		return token;
	}
}
