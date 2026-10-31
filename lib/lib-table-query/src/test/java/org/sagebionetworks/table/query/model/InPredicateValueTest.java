package org.sagebionetworks.table.query.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class InPredicateValueTest {

	@Test
	public void testConstructorWithCohort() {
		// call under test
		InPredicateValue value = new InPredicateValue(InPredicateValueFunctionName.cohort, new RegularIdentifier("c1"));
		assertEquals("c1", value.getCohortReference().get().getName());
		assertEquals("COHORT(c1)", value.toSql());
	}

	@Test
	public void testConstructorWithNullFunctionName() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new InPredicateValue(null, new RegularIdentifier("c1"));
		}).getMessage();
		assertEquals("Unsupported IN predicate function: null", message);
	}
}
