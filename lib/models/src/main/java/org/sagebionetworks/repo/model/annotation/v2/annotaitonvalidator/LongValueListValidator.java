package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import java.util.List;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.util.ValidateArgument;

class LongValueListValidator implements AnnotationsV2ValueListValidator {
	//used for printing a better error message
	private AnnotationsValueType annotationType;

	public LongValueListValidator(AnnotationsValueType annotationType) {
		this.annotationType = annotationType;
	}

	void validate(String key, String value) {
		if(value == null){
			throw new IllegalArgumentException("null is not allowed. To indicate no values, use an empty list.");
		}

		try {
			Long.valueOf(value);
		} catch (NumberFormatException e){
			throw new IllegalArgumentException("Value associated with key=" + key + " is not valid for type=" + annotationType.name() + ": " + value);
		}
	}

	@Override
	public void validate(String key, List<String> values) {
		for(String value: values){
			validate(key, value);
		}
	}
}
