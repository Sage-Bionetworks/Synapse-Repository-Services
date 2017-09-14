package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.DelimitedIdentifier;

public class DelimitedIdentifierTest {

	@Test
	public void testDelimitedIdentifier() throws ParseException{
		DelimitedIdentifier element = new TableQueryParser("\"has double quotes\"").delimitedIdentifier();
		assertEquals("\"has double quotes\"", element.toSql());
		assertTrue(element.isSurrounedeWithQuotes());
		assertEquals("has double quotes", element.getValueWithoutQuotes());
	}
	
	@Test
	public void testDelimitedIdentifierContainsDoubleQuotes() throws ParseException{
		DelimitedIdentifier element = new TableQueryParser("\"contains \"\"doubles\"\" quotes\"").delimitedIdentifier();
		assertEquals("\"contains \"\"doubles\"\" quotes\"", element.toSql());
		assertTrue(element.isSurrounedeWithQuotes());
		assertEquals("contains \"doubles\" quotes", element.getValueWithoutQuotes());
	}
	
	@Test
	public void testDelimitedIdentifierEmptyString() throws ParseException{
		DelimitedIdentifier element = new TableQueryParser("\"\"").delimitedIdentifier();
		assertEquals("\"\"", element.toSql());
		assertTrue(element.isSurrounedeWithQuotes());
		assertEquals("", element.getValueWithoutQuotes());
	}
}
