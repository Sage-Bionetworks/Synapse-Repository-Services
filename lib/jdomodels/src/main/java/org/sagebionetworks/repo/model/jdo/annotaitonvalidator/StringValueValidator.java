package org.sagebionetworks.repo.model.jdo.annotaitonvalidator;

import org.sagebionetworks.util.ValidateArgument;

class StringValueValidator implements AnnotationsV2ValueValidator {
	static final int MAX_STRING_SIZE = 500;

	@Override
	public boolean isValidValue(String value) {
		ValidateArgument.required(value, "value");
		if (value.length() > MAX_STRING_SIZE){
			throw new IllegalArgumentException("String value too long. Can be at most " + MAX_STRING_SIZE + " characters.");
		}
		return true;
	}
}
