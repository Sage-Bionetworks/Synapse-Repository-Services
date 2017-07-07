package org.sagebionetworks.repo.manager.team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	/*
	 * Returns the substring token in s that is representing the position tokenIndex in templatePieces
	 * if s matches the template represented by templatePieces. Otherwise returns null.
	 */
	public static String getTokenFromString(String s, List<String> templatePieces, int tokenIndex) {
		String regex = "";
		for (int i = 0; i < templatePieces.size(); i++) {
			if (i == tokenIndex) {
				// Current piece is the matching target
				regex += "(.*?)";
			} else {
				String piece = templatePieces.get(i);
				if (piece.charAt(0) == '#' && piece.charAt(piece.length() - 1) == '#') {
					// Current piece is a variable
					regex += ".*";
				} else {
					// Current piece is a literal string
					regex += Pattern.quote(piece);
				}
			}
		}
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(s);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return null;
	}
}
