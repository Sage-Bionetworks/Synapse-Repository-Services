package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

public enum AnnotationsV2TypeToValidator {

	STRING(AnnotationsValueType.STRING, new StringValueValidator()),
	DOUBLE(AnnotationsValueType.DOUBLE, new DoubleValueValidator()),
	LONG(AnnotationsValueType.LONG, new LongValueValidator()),
	//timestamps are just special longs so they are validated as longs
	TIMESTAMP_MS(AnnotationsValueType.TIMESTAMP_MS, new LongValueValidator());

	private AnnotationsValueType type;
	private AnnotationsV2ValueValidator validator;

	AnnotationsV2TypeToValidator(AnnotationsValueType type, AnnotationsV2ValueValidator validator) {
		this.type = type;
		this.validator = validator;
	}

	public static AnnotationsV2ValueValidator validatorFor(AnnotationsValueType type){
		for(AnnotationsV2TypeToValidator validatorMapping : values()){
			if(validatorMapping.type == type){
				return validatorMapping.validator;
			}
		}
		throw new IllegalArgumentException("No validator exists for type: " + type.name());
	}
}
