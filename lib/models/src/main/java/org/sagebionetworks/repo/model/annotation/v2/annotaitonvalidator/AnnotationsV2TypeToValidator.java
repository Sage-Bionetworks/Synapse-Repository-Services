package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

public enum AnnotationsV2TypeToValidator {

	STRING(AnnotationsValueType.STRING, new StringValueListValidator()),
	DOUBLE(AnnotationsValueType.DOUBLE, new DoubleValueListValidator()),
	LONG(AnnotationsValueType.LONG, new LongValueListValidator(AnnotationsValueType.LONG)),
	//timestamps are just special longs so they are validated as longs
	TIMESTAMP_MS(AnnotationsValueType.TIMESTAMP_MS, new LongValueListValidator(AnnotationsValueType.TIMESTAMP_MS)),
	BOOLEAN(AnnotationsValueType.BOOLEAN, new BooleanValueListValidator());

	private AnnotationsValueType type;
	private AnnotationsV2ValueListValidator validator;

	AnnotationsV2TypeToValidator(AnnotationsValueType type, AnnotationsV2ValueListValidator validator) {
		this.type = type;
		this.validator = validator;
	}

	public static AnnotationsV2ValueListValidator validatorFor(AnnotationsValueType type){
		for(AnnotationsV2TypeToValidator validatorMapping : values()){
			if(validatorMapping.type == type){
				return validatorMapping.validator;
			}
		}
		throw new IllegalArgumentException("No validator exists for type: " + type.name());
	}
}
