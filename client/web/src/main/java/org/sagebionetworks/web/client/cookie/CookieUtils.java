package org.sagebionetworks.web.client.cookie;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.web.shared.QueryConstants.WhereOperator;
import org.sagebionetworks.web.shared.WhereCondition;

/**
 * Simple helper utilities for storing data in cookies
 * 
 * @author jmhill
 *
 */
public class CookieUtils {
	
	public static final String COOKIE_DELIMITER = ",";
	public static final String COOKIE_DEL_ESCAPE = "&#"+(int)COOKIE_DELIMITER.charAt(0);
	public static final String WHERE_DELIMITER = ";";
	public static final String WHERE_DEL_ESCAPE = "&#" + (int)WHERE_DELIMITER.charAt(0);
	
	
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
	
	/**
	 * Create a delimited string from the given list of WhereConditions
	 * @param list
	 * @return
	 */
	public static String createStringFromWhereList(List<WhereCondition> list) {
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < list.size(); i++) {
			WhereCondition key = list.get(i);
			if (i != 0) {
				builder.append(COOKIE_DELIMITER);
			}
			String id = key.getId();//.replaceAll(WHERE_DELIMITER, WHERE_DEL_ESCAPE);
			String operator = key.getOperator();//.replaceAll(WHERE_DELIMITER, WHERE_DEL_ESCAPE);
			String value = key.getValue();//.replaceAll(WHERE_DELIMITER, WHERE_DEL_ESCAPE);
			
			String where = id + WHERE_DELIMITER + operator + WHERE_DELIMITER + value;
			
			builder.append(where.replaceAll(COOKIE_DELIMITER, COOKIE_DEL_ESCAPE));
		}
		
		return builder.toString();		
	}
	
	/**
	 * Create a list of WhereConditions from a delimited string
	 * @param delimitedString
	 * @return
	 */
	public static List<WhereCondition> createWhereListFromString(String delimitedString) {
		if(delimitedString != null && !delimitedString.equals("")) {
			// Split the string on the delimiter
			List<WhereCondition> results = new ArrayList<WhereCondition>();
			String[] split = delimitedString.split(COOKIE_DELIMITER);
			for(int i = 0; i < split.length; i++) {
				// Replace any escaped delimiters
				split[i].replaceAll(COOKIE_DEL_ESCAPE, COOKIE_DELIMITER);
	
				// Split the string again for the separate where condition fields
				String[] conditions = split[i].split(WHERE_DELIMITER);
				
				// Replace any escaped delimiters
				for(int j = 0; j < conditions.length; j++) {
					conditions[j].replaceAll(WHERE_DEL_ESCAPE, WHERE_DELIMITER);
				}
				
				// Assign the fields for a new WhereCondition
				WhereCondition whereCondition = new WhereCondition(conditions[0], WhereOperator.EQUALS, conditions[2]);
//				whereCondition.setOperator(conditions[1]);
				
				results.add(whereCondition);
			}
			return results;
		}
		return null;
	}
	
}
