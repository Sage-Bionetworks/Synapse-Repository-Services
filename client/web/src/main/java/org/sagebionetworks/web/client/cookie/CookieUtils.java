package org.sagebionetworks.web.client.cookie;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple helper utilities for storing data in cookies
 * 
 * @author jmhill
 *
 */
public class CookieUtils {
	
	public static final String COOKIE_DELIMITER = ",";
	public static final String COOKIE_DEL_ESCAPE = "&#"+(int)COOKIE_DELIMITER.charAt(0);
	
	
	/**
	 * Create a delimited string from the given list.
	 * @param list
	 * @return
	 */
	public static String createStringFromList(List<String> list){
		// First make sure the delimiter gets replaced
		StringBuilder builder = new StringBuilder();
		for(int i=0; i<list.size(); i++){
			String key = list.get(i);
			if(i != 0){
				builder.append(COOKIE_DELIMITER);
			}
			builder.append(key.replaceAll(COOKIE_DELIMITER, COOKIE_DEL_ESCAPE));
		}
		return builder.toString();
	}
	
	/**
	 * Create a list from a delimited string
	 * @param delimitedString
	 * @return
	 */
	public static List<String> createListFromString(String delimitedString){
		// Split the string on the delimiter
		List<String> results = new ArrayList<String>();
		String[] split = delimitedString.split(COOKIE_DELIMITER);
		for(int i=0; i<split.length; i++){
			// Add the replaced any escaped delimiters
			results.add(split[i].replaceAll(COOKIE_DEL_ESCAPE, COOKIE_DELIMITER));
		}
		return results;
	}

}
