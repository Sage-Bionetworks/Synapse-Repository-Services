package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.CorrelationName;

public class CorrelationNameTest {

	@Test
	public void testName() throws ParseException {
		CorrelationName name = new TableQueryParser("foo").correlationName();
		assertEquals("foo", name.toSql());
	}
	
	@Test
	public void testNameWithBackticks() throws ParseException {
		CorrelationName name = new TableQueryParser("`has space`").correlationName();
		assertEquals("`has space`", name.toSql());
	}
}
