package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import java.util.List;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

class LongValueListValidator implements AnnotationsV2ValueListValidator {
	// used for printing a better error message
	private AnnotationsValueType annotationType;

	public LongValueListValidator(AnnotationsValueType annotationType) {
		this.annotationType = annotationType;
	}

	void validate(String key, String value) {
		if (value == null) {
			throw new IllegalArgumentException(NULL_IS_NOT_ALLOWED);
		}

		try {
			Long.valueOf(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(
					AnnotationsV2ValueListValidator.getIllegalValueMessage(key, annotationType, value));
		}
	}

	@Override
	public void validate(String key, List<String> values) {
		for (String value : values) {
			validate(key, value);
		}
	}
}
