package org.sagebionetworks.repo.model.table.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class LongParserTest {
	
	LongParser parser;
	
	@Before
	public void before(){
		parser = new LongParser();
	}
	
	@Test (expected=NumberFormatException.class)
	public void testParseValueForDatabaseWriteString(){
		parser.parseValueForDatabaseWrite("a string");
	}

	@Test
	public void testParseValueForDatabaseWrite(){
		Long expected = new Long(123);
		Object result = parser.parseValueForDatabaseWrite("123");
		assertEquals(expected, result);
	}
	
	@Test
	public void test(){
		assertEquals("123", parser.parseValueForDatabaseRead("123"));
	}
	
	@Test
	public void testIsOfType(){
		assertTrue(parser.isOfType("123"));
		assertFalse(parser.isOfType("1.1"));
		assertFalse(parser.isOfType("foo-bar"));
		assertFalse(parser.isOfType(null));
	}
}
