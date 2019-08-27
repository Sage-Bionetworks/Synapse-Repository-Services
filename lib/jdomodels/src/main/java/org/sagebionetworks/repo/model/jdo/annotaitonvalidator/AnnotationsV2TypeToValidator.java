package org.sagebionetworks.repo.model.jdo.annotaitonvalidator;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2ValueType;

public enum AnnotationsV2TypeToValidator {

	STRING(AnnotationsV2ValueType.STRING, new StringValueValidator()),
	DOUBLE(AnnotationsV2ValueType.DOUBLE, new DoubleValueValidator()),
	LONG(AnnotationsV2ValueType.LONG, new LongValueValidator()),
	//timestamps are just special longs so they are validated as longs
	TIMESTAMP_MS(AnnotationsV2ValueType.TIMESTAMP_MS, new LongValueValidator());

	private AnnotationsV2ValueType type;
	private AnnotationsV2ValueValidator validator;

	AnnotationsV2TypeToValidator(AnnotationsV2ValueType type, AnnotationsV2ValueValidator validator) {
		this.type = type;
		this.validator = validator;
	}

	public static AnnotationsV2ValueValidator validatorFor(AnnotationsV2ValueType type){
		for(AnnotationsV2TypeToValidator validatorMapping : values()){
			if(validatorMapping.type == type){
				return validatorMapping.validator;
			}
		}
		throw new IllegalArgumentException("No validator exists for type: " + type.name());
	}
}
