package org.sagebionetworks.repo.model.jdo.annotaitonvalidator;

import org.sagebionetworks.util.ValidateArgument;

class LongValueValidator implements AnnotationsV2ValueValidator {
	@Override
	public boolean isValidValue(String value) {
		ValidateArgument.required(value, "value");
		try {
			Long.valueOf(value);
			return true;
		} catch (NumberFormatException e){
			return false;
		}
	}
}
