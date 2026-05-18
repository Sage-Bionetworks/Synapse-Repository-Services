package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnConstants;

class StringValueListValidator implements AnnotationsV2ValueListValidator {

	@Override
	public void validate(String key, List<String> values) {
		long totalCharacters = 0;
		for(String value: values){
			if(value == null){
				throw new IllegalArgumentException(NULL_IS_NOT_ALLOWED);
			}
			if(value.length() > ColumnConstants.MAX_ALLOWED_STRING_SIZE){
				throw new IllegalArgumentException("A single string annotation value cannot exceed "
						+ ColumnConstants.MAX_ALLOWED_STRING_SIZE + " characters.");
			}
			totalCharacters += value.length();
			if(totalCharacters > ColumnConstants.MAX_ALLOWED_LIST_TOTAL_CHARACTERS){
				throw new IllegalArgumentException("The sum of all string values for annotation '"
						+ key + "' cannot exceed " + ColumnConstants.MAX_ALLOWED_LIST_TOTAL_CHARACTERS + " characters.");
			}
		}
	}
}
