package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.CastOperand;

public class CastOperandTest {

	@Test
	public void testCastCastOperandWithNonNull() throws ParseException {
		CastOperand element = new TableQueryParser("foo").castOperand();
		assertEquals("foo", element.toSql());
	}
	
	@Test
	public void testCastCastOperandWithNull() throws ParseException {
		CastOperand element = new TableQueryParser("null").castOperand();
		assertEquals("NULL", element.toSql());
	}
}
