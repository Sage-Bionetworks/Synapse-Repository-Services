package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.IntervalLiteral;

public class IntervalLiteralTest {

	@Test
	public void testInterval() throws ParseException{
		IntervalLiteral element = new TableQueryParser("interval 10 day").intervalLiteral();
		assertEquals("INTERVAL 10 DAY", element.toSql());
	}

}
