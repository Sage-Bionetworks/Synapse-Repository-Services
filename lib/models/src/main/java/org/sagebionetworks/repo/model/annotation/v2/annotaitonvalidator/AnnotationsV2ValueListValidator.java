package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import java.util.List;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

public interface AnnotationsV2ValueListValidator {
	
	public static final String NULL_IS_NOT_ALLOWED = "null is not allowed. To indicate no values, use an empty list.";
	public static final String ILLEGAL_VALUE_MESSAGE_TEMPLATE = "Value associated with key=%s is not valid for type=%s: %s";

	public static String getIllegalValueMessage(String key, AnnotationsValueType type, String value) {
		return String.format(ILLEGAL_VALUE_MESSAGE_TEMPLATE, key, type.name(), value);
	}

	/**
	 * Validate the list of values
	 * * @param key used to give more context in the exception message if an exception is thrown
	 * @param values The list of value to be validated for a single annotation key
	 * @throws IllegalArgumentException if the value is not valid.
	 */
	public void validate(String key, List<String> values);
	

}
