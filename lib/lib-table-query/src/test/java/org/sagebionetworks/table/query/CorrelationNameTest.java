package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
		assertThrows(ParseException.class, ()->{
			new TableQueryParser("`has space`").correlationName();
		});
	}
	
	@Test
	public void testNameWithDoubleQuotes() throws ParseException {
		assertThrows(ParseException.class, ()->{
			new TableQueryParser("\"has space\"").correlationName();
		});
	}
}
