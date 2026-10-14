package org.sagebionetworks.repo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class BelowThresholdExceptionTest {

	@Test
	public void testConstructWithThreshold() {
		// call under test
		BelowThresholdException exception = new BelowThresholdException(10L);

		assertEquals("The number of records matched by this query is below the minimum threshold of 10 required to return a result.",
				exception.getMessage());
		assertEquals(10L, exception.getSuppressionThreshold());
	}

	@Test
	public void testConstructWithMessageCarryingThreshold() {
		String message = "The number of records matched by this query is below the minimum threshold of 42 required to return a result.";

		// call under test
		BelowThresholdException exception = new BelowThresholdException(message);

		assertEquals(message, exception.getMessage());
		assertEquals(42L, exception.getSuppressionThreshold());
	}

	@Test
	public void testConstructWithNullMessage() {
		// call under test
		BelowThresholdException exception = new BelowThresholdException((String) null);

		assertNull(exception.getMessage());
		assertNull(exception.getSuppressionThreshold());
	}

	@Test
	public void testConstructWithUnparseableMessage() {
		String message = "Some other message without a threshold.";

		// call under test
		BelowThresholdException exception = new BelowThresholdException(message);

		assertEquals(message, exception.getMessage());
		assertNull(exception.getSuppressionThreshold());
	}

	@Test
	public void testSuppressionThresholdSurvivesAsyncRoundTrip() {
		// The async job framework persists only the class name and message, then reconstructs
		// the exception from the message via the message-only constructor. The threshold must
		// survive that round-trip.
		BelowThresholdException original = new BelowThresholdException(500L);

		// call under test
		BelowThresholdException reconstructed = new BelowThresholdException(original.getMessage());

		assertEquals(original.getMessage(), reconstructed.getMessage());
		assertEquals(500L, reconstructed.getSuppressionThreshold());
	}

}
