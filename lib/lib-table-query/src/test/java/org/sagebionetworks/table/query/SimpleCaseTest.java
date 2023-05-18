package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.SimpleCase;

public class SimpleCaseTest {
	
	@Test
	public void testSimpleCase() throws ParseException {
		SimpleCase element = new TableQueryParser("bar when 100 then 0").simpleCase();
		assertEquals("bar WHEN 100 THEN 0", element.toSql());
	}
	
	@Test
	public void testSimpleCaseWithElse() throws ParseException {
		SimpleCase element = new TableQueryParser("bar when 100 then 0 else 1").simpleCase();
		assertEquals("bar WHEN 100 THEN 0 ELSE 1", element.toSql());
	}
	
	@Test
	public void testSimpleCaseWithMultipleWhen() throws ParseException {
		SimpleCase element = new TableQueryParser("bar when 100 then 0 when 200 then 1 when 300 then 3 else -1").simpleCase();
		assertEquals("bar WHEN 100 THEN 0", element.toSql());
	}
}
