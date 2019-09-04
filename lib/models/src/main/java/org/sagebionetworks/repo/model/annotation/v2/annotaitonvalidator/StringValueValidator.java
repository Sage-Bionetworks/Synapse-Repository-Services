package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2ValueType;
import org.sagebionetworks.util.ValidateArgument;

class StringValueValidator implements AnnotationsV2ValueValidator {
	static final int MAX_STRING_SIZE = 500;

	@Override
	public void validate(String key, String value, AnnotationsV2ValueType annotationType) {
		ValidateArgument.required(value, "value");
		if (value.length() > MAX_STRING_SIZE){
			throw new IllegalArgumentException("String value too long. Can be at most " + MAX_STRING_SIZE + " characters.");
		}
	}
}
