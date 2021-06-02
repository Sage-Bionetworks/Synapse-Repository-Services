package org.sagebionetworks.repo.model.annotation.v2.annotaitonvalidator;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

public class AnnotationsTypeToValidatorTest {
	@Test
	public void testValidatorFor(){
		assertTrue(AnnotationsV2TypeToValidator.validatorFor(AnnotationsValueType.STRING) instanceof StringValueListValidator);
		assertTrue(AnnotationsV2TypeToValidator.validatorFor(AnnotationsValueType.DOUBLE) instanceof DoubleValueListValidator);
		assertTrue(AnnotationsV2TypeToValidator.validatorFor(AnnotationsValueType.LONG) instanceof LongValueListValidator);
		assertTrue(AnnotationsV2TypeToValidator.validatorFor(AnnotationsValueType.TIMESTAMP_MS) instanceof LongValueListValidator);
		assertTrue(AnnotationsV2TypeToValidator.validatorFor(AnnotationsValueType.BOOLEAN) instanceof BooleanValueListValidator);
	}

	@Test
	public void testValidatorExistForEachType(){
		//test should fail if a new AnnotationsV2ValueType is ever added but no validator was mapped to it
		for(AnnotationsValueType type : AnnotationsValueType.values()) {
			assertNotNull(AnnotationsV2TypeToValidator.validatorFor(type));
		}
	}
}