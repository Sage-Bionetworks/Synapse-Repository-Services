package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import java.util.List;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.util.ValidateArgument;

class StringValueListValidator implements AnnotationsV2ValueListValidator {
	static final int MAX_STRING_SIZE = 500;
	//used for printing a better error message
	private AnnotationsValueType annotationType;

	public StringValueListValidator(AnnotationsValueType annotationType) {
		this.annotationType = annotationType;
	}

	@Override
	public void validate(String key, List<String> values) {
		int totalCharacters = 0;
		for(String value: values){
			if(value == null){
				throw new IllegalArgumentException("null is not allowed. To indicate no values, use an empty list.");
			}

			totalCharacters += value.length();
			if (totalCharacters > MAX_STRING_SIZE){
				throw new IllegalArgumentException("Total Stri value too long. Can be at most " + MAX_STRING_SIZE + " characters.");
			}
		}
	}
}
