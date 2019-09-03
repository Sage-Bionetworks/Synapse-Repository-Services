package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2ValueType;

public interface AnnotationsV2ValueValidator {

	/**
	 * Validate the value
	 * @param key used to give more context in the exception message if an exception is thrown
	 * @param value The value to be validated
	 * @param annotationType used to give more context in the exception message if an exception is thrown
	 * @throws IllegalArgumentException if the value is not valid.
	 */
	public void validate(String key, String value, AnnotationsV2ValueType annotationType);
}
