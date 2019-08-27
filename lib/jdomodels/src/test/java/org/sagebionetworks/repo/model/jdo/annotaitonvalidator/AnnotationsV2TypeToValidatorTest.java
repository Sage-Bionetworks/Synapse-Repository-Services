package org.sagebionetworks.repo.model.jdo.annotaitonvalidator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2ValueType;

class AnnotationsV2TypeToValidatorTest {
	@Test
	public void testValidatorFor(){
		assertTrue(AnnotationsV2TypeToValidator.validatorFor(AnnotationsV2ValueType.STRING) instanceof StringValueValidator);
		assertTrue(AnnotationsV2TypeToValidator.validatorFor(AnnotationsV2ValueType.DOUBLE) instanceof DoubleValueValidator);
		assertTrue(AnnotationsV2TypeToValidator.validatorFor(AnnotationsV2ValueType.LONG) instanceof LongValueValidator);
		assertTrue(AnnotationsV2TypeToValidator.validatorFor(AnnotationsV2ValueType.TIMESTAMP_MS) instanceof LongValueValidator);
	}

	@Test
	public void testValidatorExistForEachType(){
		//test should fail if a new AnnotationsV2ValueType is ever added but no validator was mapped to it
		for(AnnotationsV2ValueType type : AnnotationsV2ValueType.values()) {
			assertNotNull(AnnotationsV2TypeToValidator.validatorFor(type));
		}
	}
}