package org.sagebionetworks.repo.model.jdo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.model.util.ModelConstants;

/**
 * Validation for entity names.
 * 
 */
public class NameValidation {

	/**
	 * This represents the maximum number of characters that Files.NAME, Evaluation.NAME, and Node.NAME can contain.
	 * The ddls for these tables can be found in `Files-ddl.sql`, `Evaluation-ddl.sql`, and `Node-ddl.sql` respectively.
	 */
	public static final int MAX_NAME_CHARS = 256;
	
	public static final String INVALID_NAME_TEMPLATE = "Invalid Name: '%s'. Names may only contain: letters, numbers, spaces, underscores, hyphens, periods, plus signs, apostrophes, and parentheses";
	public static final String NAME_LENGTH_TOO_LONG = String.format("Name length cannot be longer than %s characters.", MAX_NAME_CHARS);
	
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
		if (key.length() > MAX_NAME_CHARS) {
			throw new IllegalArgumentException(NAME_LENGTH_TOO_LONG);
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
