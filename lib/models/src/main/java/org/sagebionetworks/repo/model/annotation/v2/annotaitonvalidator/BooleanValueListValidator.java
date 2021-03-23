package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import java.util.List;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

public class BooleanValueListValidator implements AnnotationsV2ValueListValidator {

	void validate(String key, String value) {
		if (value == null) {
			throw new IllegalArgumentException(NULL_IS_NOT_ALLOWED);
		}
		if (value.length() > 5) {
			throw new IllegalArgumentException(
					AnnotationsV2ValueListValidator.getIllegalValueMessage(key, AnnotationsValueType.BOOLEAN, value));
		}
		Boolean.parseBoolean(value);
	}

	@Override
	public void validate(String key, List<String> values) {
		for (String value : values) {
			validate(key, value);
		}
	}

}
