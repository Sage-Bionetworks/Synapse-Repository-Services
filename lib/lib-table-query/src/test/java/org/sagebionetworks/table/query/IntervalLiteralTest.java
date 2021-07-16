package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.IntervalLiteral;

public class IntervalLiteralTest {

	@Test
	public void testInterval() throws ParseException {
		IntervalLiteral element = new TableQueryParser("interval 10 day").intervalLiteral();
		assertEquals("INTERVAL 10 DAY", element.toSql());
	}

	@Test
	public void testGetChildren() throws ParseException {
		IntervalLiteral element = new TableQueryParser("interval 10 day").intervalLiteral();
		assertEquals(Collections.singleton(element.getUnsignedInteger()), element.getChildren());
	}

}
