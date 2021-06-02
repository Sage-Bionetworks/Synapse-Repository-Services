package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import java.util.List;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

public class BooleanValueListValidator implements AnnotationsV2ValueListValidator {

	private static final String LOWER_FALSE = "false";
	private static final String LOWER_TRUE = "true";

	void validate(String key, String value) {
		if (value == null) {
			throw new IllegalArgumentException(NULL_IS_NOT_ALLOWED);
		}
		String lowerValue = value.toLowerCase();
		if (!LOWER_TRUE.equals(lowerValue) && !LOWER_FALSE.equals(lowerValue)) {
			throw new IllegalArgumentException(
					AnnotationsV2ValueListValidator.getIllegalValueMessage(key, AnnotationsValueType.BOOLEAN, value));
		}
	}

	@Override
	public void validate(String key, List<String> values) {
		for (String value : values) {
			validate(key, value);
		}
	}

}
