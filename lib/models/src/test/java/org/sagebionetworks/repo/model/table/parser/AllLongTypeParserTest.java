package org.sagebionetworks.repo.model.table.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class AllLongTypeParserTest {

	AllLongTypeParser parser;
	
	@Before
	public void before(){
		parser = new AllLongTypeParser();
	}
	
	@Test (expected=NumberFormatException.class)
	public void testParseValueForDatabaseWriteString(){
		parser.parseValueForDatabaseWrite("a string");
	}

	@Test
	public void testParseValueForDatabaseWrite(){
		assertEquals(new Long(123), parser.parseValueForDatabaseWrite("123"));
		assertEquals(new Long(123), parser.parseValueForDatabaseWrite("syn123"));
		assertEquals(new Long(123), parser.parseValueForDatabaseWrite("1970-1-1 00:00:00.123"));
	}
	
	@Test (expected=UnsupportedOperationException.class)
	public void testParseValueForDatabaseRead(){
		parser.parseValueForDatabaseRead("value");
	}
	
	@Test
	public void testIsOfType(){
		assertTrue(parser.isOfType("123"));
		assertTrue(parser.isOfType("syn123"));
		assertTrue(parser.isOfType("1970-1-1 00:00:00.123"));
		assertFalse(parser.isOfType("goo"));
		assertFalse(parser.isOfType(null));
	}

}
