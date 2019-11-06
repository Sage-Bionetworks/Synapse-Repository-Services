package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.table.query.model.DelimitedIdentifier;

public class DelimitedIdentifierTest {

	@Test
	public void testDoubleQuotes() throws ParseException {
		DelimitedIdentifier element = new TableQueryParser("\"has double quotes\"").delimitedIdentifier();
		assertTrue(element.hasQuotes());
		assertEquals("\"has double quotes\"", element.toSql());
	}
	
	@Test
	public void tesBacktickQuotes() throws ParseException {
		DelimitedIdentifier element = new TableQueryParser("\"has double quotes\"").delimitedIdentifier();
		assertTrue(element.hasQuotes());
		assertEquals("\"has double quotes\"", element.toSql());
	}
}
