package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import java.util.List;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

public interface AnnotationsV2ValueListValidator {

	/**
	 * Validate the list of values
	 * * @param key used to give more context in the exception message if an exception is thrown
	 * @param values The list of value to be validated for a single annotation key
	 * @throws IllegalArgumentException if the value is not valid.
	 */
	public void validate(String key, List<String> values);
}
