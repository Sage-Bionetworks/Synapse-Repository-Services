package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

class AnnotationsTypeToValidatorTest {
	@Test
	public void testValidatorFor(){
		assertTrue(AnnotationsV2TypeToValidator.validatorFor(AnnotationsValueType.STRING) instanceof StringValueValidator);
		assertTrue(AnnotationsV2TypeToValidator.validatorFor(AnnotationsValueType.DOUBLE) instanceof DoubleValueValidator);
		assertTrue(AnnotationsV2TypeToValidator.validatorFor(AnnotationsValueType.LONG) instanceof LongValueValidator);
		assertTrue(AnnotationsV2TypeToValidator.validatorFor(AnnotationsValueType.TIMESTAMP_MS) instanceof LongValueValidator);
	}

	@Test
	public void testValidatorExistForEachType(){
		//test should fail if a new AnnotationsV2ValueType is ever added but no validator was mapped to it
		for(AnnotationsValueType type : AnnotationsValueType.values()) {
			assertNotNull(AnnotationsV2TypeToValidator.validatorFor(type));
		}
	}
}