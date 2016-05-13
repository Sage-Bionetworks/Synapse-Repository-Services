package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ActualIdentifier;

public class ActualIdentifierTest {
	
	@Test (expected=IllegalArgumentException.class)
	public void testOr(){
		// an actual-identifier can be a regular-identifier or delimited-identifier but not both.
		new ActualIdentifier("one", "two");
	}
	
	@Test
	public void testRegularToSQL(){
		ActualIdentifier ai = new ActualIdentifier("C123", null);
		assertEquals("C123", ai.toString());
	}
	
	@Test
	public void testDelimitedToSQL(){
		ActualIdentifier ai = new ActualIdentifier(null, "has\"quote");
		assertEquals("\"has\"\"quote\"", ai.toString());
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

}
