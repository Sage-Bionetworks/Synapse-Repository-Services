package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.WithListElement;

public class WithListElementTest {

	@Test
	public void testWithListElement() throws ParseException {
		WithListElement element = new TableQueryParser("W (Y, Z) AS (SELECT * from syn1)").withListElement();
		assertEquals("W (Y, Z) AS (SELECT * FROM syn1)", element.toSql());
	}
	
	@Test
	public void testWithListElementWithNoColumnsList() throws ParseException {
		WithListElement element = new TableQueryParser("W AS (SELECT * from syn1)").withListElement();
		assertEquals("W AS (SELECT * FROM syn1)", element.toSql());
	}
}
