package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ActualIdentifier;

public class ActualIdentifierTest {
		
	@Test
	public void testRegularToSQL() throws ParseException{
		ActualIdentifier element = new TableQueryParser("C123").actualIdentifier();
		assertEquals("C123", element.toString());
		assertFalse(element.hasQuotesRecursive());
	}
	
	@Test
	public void testDelimitedToSQL() throws ParseException{
		ActualIdentifier element = new TableQueryParser("\"has\"\"quote\"").actualIdentifier();
		assertEquals("\"has\"\"quote\"", element.toString());
		assertTrue(element.hasQuotesRecursive());
	}
	
	@Test
	public void testValueWithQuotes() throws ParseException{
		ActualIdentifier element = new TableQueryParser("\"foo\"").actualIdentifier();
		assertEquals("foo", element.toSqlWithoutQuotes());
		assertTrue(element.hasQuotesRecursive());
	}
	
	@Test
	public void testValueNoQuotes() throws ParseException{
		ActualIdentifier element = new TableQueryParser("foo").actualIdentifier();
		assertEquals("foo", element.toSqlWithoutQuotes());
		assertFalse(element.hasQuotesRecursive());
	}

	@Test
	public void testValueUnderscore() throws ParseException{
		ActualIdentifier element = new TableQueryParser("_foo_").actualIdentifier();
		assertEquals("_foo_", element.toSqlWithoutQuotes());
		assertFalse(element.hasQuotesRecursive());
	}
}
