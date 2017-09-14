package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ActualIdentifier;

public class ActualIdentifierTest {
		
	@Test
	public void testRegularToSQL() throws ParseException{
		ActualIdentifier element = new TableQueryParser("C123").actualIdentifier();
		assertEquals("C123", element.toString());
	}
	
	@Test
	public void testDelimitedToSQL() throws ParseException{
		ActualIdentifier element = new TableQueryParser("\"has\"\"quote\"").actualIdentifier();
		assertEquals("\"has\"\"quote\"", element.toString());
	}
	
	@Test
	public void testValueWithQuotes() throws ParseException{
		ActualIdentifier element = new TableQueryParser("\"foo\"").actualIdentifier();
		assertEquals("foo", element.getValueWithoutQuotes());
		assertTrue(element.isSurrounedeWithQuotes());
	}
	
	@Test
	public void testValueNoQuotes() throws ParseException{
		ActualIdentifier element = new TableQueryParser("foo").actualIdentifier();
		assertEquals("foo", element.getValueWithoutQuotes());
		assertFalse(element.isSurrounedeWithQuotes());
	}

	@Test
	public void testValueUnderscore() throws ParseException{
		ActualIdentifier element = new TableQueryParser("_foo_").actualIdentifier();
		assertEquals("_foo_", element.getValueWithoutQuotes());
		assertFalse(element.isSurrounedeWithQuotes());
	}
}
