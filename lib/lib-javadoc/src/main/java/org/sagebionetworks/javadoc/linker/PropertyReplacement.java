package org.sagebionetworks.javadoc.linker;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple property replacement utility.
 * 
 * @author jmhill
 *
 */
public class PropertyReplacement {
	
	/**
	 * ${...}
	 */
	private static final String PROPERTY_REG_EX = "(\\$\\{)([^\\{\\}\\$]*)(\\})";
	private static final Pattern PROPERTY_PATTERN = Pattern.compile(PROPERTY_REG_EX);

	/**
	 * Replace all properties in the input string with values from the replacement map
	 * A property is identified in the input string as: '${<key>}'. The entire regular
	 * expression will then be replaced with the value from the replacement map that 
	 * Corresponds with the <key>.
	 * 
	 * For example, if the input string is:
	 * 'this is a ${foo.key}.'
	 * And the replacement map contains:
	 * Map<String,String> replacement = new HashMap<String,String>();
	 * replacement.put("foo.key", "bar");
	 * Then the resulting string will be:
	 * 'this is a bar.'
	 * 
	 * @param input
	 * @param replacements
	 * @return
	 */
	public static String replaceProperties(String input, Map<String, String> replacements){
		if(input == null) throw new IllegalArgumentException("Input string cannot be null");
		if(replacements == null) throw new IllegalArgumentException("Replacement map cannot be null"); 
		Matcher matcher = PROPERTY_PATTERN.matcher(input);
        boolean result = matcher.find();
        if (result) {
        	// This will contain the new string
            StringBuffer sb = new StringBuffer();
            do {
            	// The group will be a raw value like: ${<key>}
            	String group = matcher.group();
            	// extract the key by removing the first two and last characters.
            	String key = group.substring(2, group.length()-1);
            	// Lookup the replacement value from the provided map
            	String value = replacements.get(key);
            	if(value == null) {
            		throw new IllegalArgumentException("No replacement found for key: "+key);
            	}
            	// Replace the entire group with the value.
            	matcher.appendReplacement(sb, value);
                result = matcher.find();
            } while (result);
            // Add add anything left
            matcher.appendTail(sb);
            return sb.toString();
        }
        // There were no matches.
        return input;
	}

}
