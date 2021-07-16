package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.DoubleQuoteDelimitedIdentifier;

public class DoubleQuoteDelimitedIdentifierTest {

	@Test
	public void testDelimitedIdentifier() throws ParseException {
		DoubleQuoteDelimitedIdentifier element = new TableQueryParser("\"has double quotes\"")
				.doubleQuoteDelimitedIdentifier();
		assertEquals("\"has double quotes\"", element.toSql());
		assertTrue(element.hasQuotes());
		assertEquals("has double quotes", element.toSqlWithoutQuotes());
	}

	@Test
	public void testDelimitedIdentifierContainsDoubleQuotes() throws ParseException {
		DoubleQuoteDelimitedIdentifier element = new TableQueryParser("\"contains \"\"doubles\"\" quotes\"")
				.doubleQuoteDelimitedIdentifier();
		assertEquals("\"contains \"\"doubles\"\" quotes\"", element.toSql());
		assertTrue(element.hasQuotes());
		assertEquals("contains \"doubles\" quotes", element.toSqlWithoutQuotes());
	}

	@Test
	public void testDelimitedIdentifierEmptyString() throws ParseException {
		DoubleQuoteDelimitedIdentifier element = new TableQueryParser("\"\"").doubleQuoteDelimitedIdentifier();
		assertEquals("\"\"", element.toSql());
		assertTrue(element.hasQuotes());
		assertEquals("", element.toSqlWithoutQuotes());
	}

	@Test
	public void testGetChildren() throws ParseException {
		DoubleQuoteDelimitedIdentifier element = new TableQueryParser("\"contains \"\"doubles\"\" quotes\"")
				.doubleQuoteDelimitedIdentifier();
		assertEquals(Collections.emptyList(), element.getChildren());
	}
}
