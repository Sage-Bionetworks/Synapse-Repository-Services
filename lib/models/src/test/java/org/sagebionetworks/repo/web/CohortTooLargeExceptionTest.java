package org.sagebionetworks.repo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CohortTooLargeExceptionTest {

	@Test
	public void testConstructWithCohortAndMaximum() {
		// call under test
		CohortTooLargeException exception = new CohortTooLargeException("c1", 10000L);

		assertEquals("Cohort 'c1' exceeds the maximum of 10000 values.", exception.getMessage());
		assertEquals(10000L, exception.getMaximumValues());
		// It remains a bad request for any caller that only handles IllegalArgumentException.
		assertTrue(exception instanceof IllegalArgumentException);
	}

	@Test
	public void testConstructWithMessageCarryingMaximum() {
		String message = "Cohort 'c1' exceeds the maximum of 42 values.";

		// call under test
		CohortTooLargeException exception = new CohortTooLargeException(message);

		assertEquals(message, exception.getMessage());
		assertEquals(42L, exception.getMaximumValues());
	}

	@Test
	public void testConstructWithNullMessage() {
		// call under test
		CohortTooLargeException exception = new CohortTooLargeException((String) null);

		assertNull(exception.getMessage());
		assertNull(exception.getMaximumValues());
	}

	@Test
	public void testConstructWithUnparseableMessage() {
		String message = "Some other message without a maximum.";

		// call under test
		CohortTooLargeException exception = new CohortTooLargeException(message);

		assertEquals(message, exception.getMessage());
		assertNull(exception.getMaximumValues());
	}

	@Test
	public void testMaximumSurvivesAsyncRoundTrip() {
		// The async job framework persists only the class name and message, then reconstructs the
		// exception from the message via the message-only constructor.
		CohortTooLargeException original = new CohortTooLargeException("c1", 500L);

		// call under test
		CohortTooLargeException reconstructed = new CohortTooLargeException(original.getMessage());

		assertEquals(original.getMessage(), reconstructed.getMessage());
		assertEquals(500L, reconstructed.getMaximumValues());
	}

}
