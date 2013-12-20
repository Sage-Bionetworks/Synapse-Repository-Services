package org.sagebionetworks.repo.model.principal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This maps the alias types and the regular expression used to validate each.
 * 
 * @author John
 *
 */
public enum AliasEnum {
	
	USER_NAME("^[a-z0-9._-]{3,}", "User names can only contain letters, numbers, dot (.), dash (-) and underscore (_) and must be at least 3 characters long."),
	TEAM_NAME("^[a-z0-9 ._-]{3,}", "Team names can only contain letters, numbers, spaces, dot (.), dash (-), underscore (_) and must be at least 3 characters long."),
	USER_EMAIL("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,4}", "User emails must be a valid email address"),
	USER_OPEN_ID("^(https?)://[-a-z0-9+&@#/%?=~_|!:,.;]*[-a-z0-9+&@#/%=~_|]","User OpenIDs must be a valid URL.");
	
	private AliasEnum(String regEx, String description){
		this.description = description;
		this.regEx = regEx;
		this.pattern = Pattern.compile(regEx);
	}
	
	private String description;
	private Pattern pattern;
	private String regEx;
	public Pattern getPattern() {
		return pattern;
	}
	public String getRegEx() {
		return regEx;
	}
	public String getDescription() {
		return description;
	}

	/**
	 * Validate an alias against types regular expression.
	 * 
	 * @param toValidate
	 */
	public void validateAlias(String toValidate){
		if(toValidate == null) throw new IllegalArgumentException("Alias cannot be null");
		// validate the lower case version of the string.
		String lower = toValidate.toLowerCase();
		Matcher m = pattern.matcher(lower);
		if (!m.matches()) {
			throw new IllegalArgumentException(this.description);
		}
	}
}
