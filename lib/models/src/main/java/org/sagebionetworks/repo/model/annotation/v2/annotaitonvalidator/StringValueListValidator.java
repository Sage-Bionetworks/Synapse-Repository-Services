package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import java.util.List;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.util.ValidateArgument;

class StringValueListValidator implements AnnotationsV2ValueListValidator {
	
	static final int MAX_STRING_SIZE = 500;
	
	@Override
	public void validate(String key, List<String> values) {
		int totalCharacters = 0;
		for(String value: values){
			if(value == null){
				throw new IllegalArgumentException(NULL_IS_NOT_ALLOWED);
			}

			totalCharacters += value.length();
			if (totalCharacters > MAX_STRING_SIZE){
				throw new IllegalArgumentException("Total String value too long. Can be at most " + MAX_STRING_SIZE + " characters.");
			}
		}
	}
}
