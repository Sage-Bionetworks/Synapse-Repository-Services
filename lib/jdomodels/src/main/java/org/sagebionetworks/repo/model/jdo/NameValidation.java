package org.sagebionetworks.repo.model.jdo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.util.ModelConstants;

/**
 * Validation for entity names.
 * 
 */
public class NameValidation {

	public static final String INVALID_NAME_TEMPLATE = "Invalid Name: '%s'. Names may only contain: letters, numbers, spaces, underscores, hyphens, periods, plus signs, apostrophes, and parentheses";

	// match one or more whitespace characters
	private static final Pattern ALLOWABLE_CHARS = Pattern
			.compile(ModelConstants.VALID_ENTITY_NAME_REGEX);

	/**
	 * Validate the name
	 * 
	 * @param key
	 * @throws IllegalArgumentException
	 */
	public static String validateName(String key) {
		// In the case where name is null, we expect that no explicit setting of the name was intended.
		// For Entities, we later replace the null name with the entity ID.
		if (key == null){
			return null;
		}
		// In the case where it is a empty string or full of whitespace, the user intended to set a name, so we throw an error.
		key = key.trim();
		if ("".equals(key)) {
			throw new IllegalArgumentException("Name cannot be only whitespace or empty string");
		}
		Matcher matcher = ALLOWABLE_CHARS.matcher(key);
		if (!matcher.matches()) {
			throw new IllegalArgumentException(createInvalidMessage(key));
		}
		return key;
	}
	
	public static String createInvalidMessage(String key) {
		return String.format(INVALID_NAME_TEMPLATE, key);
	}
}
