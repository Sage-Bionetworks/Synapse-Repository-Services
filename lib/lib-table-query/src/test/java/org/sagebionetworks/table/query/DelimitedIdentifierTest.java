package org.sagebionetworks.table.query;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;
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
	
	@Test
	public void testGetChildren() throws ParseException {
		DelimitedIdentifier element = new TableQueryParser("\"has double quotes\"").delimitedIdentifier();
		assertEquals(Collections.singleton(element.getChild()), element.getChildren());
	}
}
