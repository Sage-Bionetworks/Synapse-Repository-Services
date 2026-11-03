package org.sagebionetworks.repo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.sagebionetworks.repo.model.table.RowSuppressionReasonCode;

public class RowSuppressionExceptionTest {

	@Test
	public void testConstructWithReasonCode() {
		// call under test
		RowSuppressionException exception = new RowSuppressionException(RowSuppressionReasonCode.QID_PROJECTED);

		assertEquals(RowSuppressionReasonCode.QID_PROJECTED, exception.getReasonCode());
	}

	@Test
	public void testConstructWithMessageCarryingReasonCode() {
		RowSuppressionException original = new RowSuppressionException(RowSuppressionReasonCode.QID_IN_GROUP_BY);

		// call under test
		RowSuppressionException exception = new RowSuppressionException(original.getMessage());

		assertEquals(original.getMessage(), exception.getMessage());
		assertEquals(RowSuppressionReasonCode.QID_IN_GROUP_BY, exception.getReasonCode());
	}

	@Test
	public void testConstructWithNullMessage() {
		// call under test
		RowSuppressionException exception = new RowSuppressionException((String) null);

		assertNull(exception.getMessage());
		assertNull(exception.getReasonCode());
	}

	@Test
	public void testConstructWithUnparseableMessage() {
		String message = "Some other message without a reason code.";

		// call under test
		RowSuppressionException exception = new RowSuppressionException(message);

		assertEquals(message, exception.getMessage());
		assertNull(exception.getReasonCode());
	}

	@ParameterizedTest
	@EnumSource(RowSuppressionReasonCode.class)
	public void testReasonCodeSurvivesAsyncRoundTrip(RowSuppressionReasonCode reasonCode) {
		// The async job framework persists only the class name and message, then reconstructs
		// the exception from the message via the message-only constructor. The reason code must
		// survive that round-trip for every value, so a change to the message template that broke
		// the embedded code would fail here rather than silently dropping the code to null.
		RowSuppressionException original = new RowSuppressionException(reasonCode);

		// call under test
		RowSuppressionException reconstructed = new RowSuppressionException(original.getMessage());

		assertEquals(original.getMessage(), reconstructed.getMessage());
		assertEquals(reasonCode, reconstructed.getReasonCode());
	}

}
