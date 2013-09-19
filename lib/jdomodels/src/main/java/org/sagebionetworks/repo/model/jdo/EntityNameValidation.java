package org.sagebionetworks.repo.model.jdo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.util.ModelConstants;

/**
 * Validation for entity names.
 * 
 * @author jmhill
 * 
 */
public class EntityNameValidation {
	


	// match one or more whitespace characters
	private static final Pattern ALLOWABLE_CHARS = Pattern
			.compile(ModelConstants.VALID_ENTITY_NAME_REGEX);

	/**
	 * Validate the name
	 * 
	 * @param key
	 * @throws IllegalArgumentException
	 */
	public static String valdiateName(String key) {
		if (key == null)
			throw new IllegalArgumentException("Entity names cannot be null");
		key = key.trim();
		if ("".equals(key))
			throw new IllegalArgumentException(
					"Entity names cannot be empty strings");
		Matcher matcher = ALLOWABLE_CHARS.matcher(key);
		if (!matcher.matches()) {
			throw new IllegalArgumentException(
					"Invalid Entity name: '"
							+ key
							+ "'. Entity names may only contain: letters, numbers, spaces, underscores, hypens, periods, plus signs, and parentheses");
		}
		return key;
	}
}
