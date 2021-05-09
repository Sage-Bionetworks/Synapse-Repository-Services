package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import java.util.List;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.util.doubles.DoubleUtils;

class DoubleValueListValidator implements AnnotationsV2ValueListValidator {

	void validate(String key, String value) {
		if (value == null) {
			throw new IllegalArgumentException(NULL_IS_NOT_ALLOWED);
		}

		try {
			DoubleUtils.fromString(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(
					AnnotationsV2ValueListValidator.getIllegalValueMessage(key, AnnotationsValueType.DOUBLE, value));
		}
	}

	@Override
	public void validate(String key, List<String> values) {
		for (String value : values) {
			validate(key, value);
		}
	}
}
