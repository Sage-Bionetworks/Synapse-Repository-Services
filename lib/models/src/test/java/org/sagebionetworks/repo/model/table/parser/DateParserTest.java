package org.sagebionetworks.repo.model.table.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class DateParserTest {
	
	DateParser parser;
	
	@Before
	public void before(){
		parser = new DateParser();
	}
	
	@Test
	public void testParseValueForDatabaseWriteDateString(){
		Date expected = new Date(123);
		Object objectValue = parser.parseValueForDatabaseWrite("1970-1-1 00:00:00.123");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testParseValueForDatabaseWriteLong(){
		Date expected = new Date(123);
		Object objectValue = parser.parseValueForDatabaseWrite("123");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testParseValueForDatabaseRead(){
		assertEquals("123", parser.parseValueForDatabaseRead("123"));
	}
	
	@Test
	public void testIsOfType(){
		assertTrue(parser.isOfType("1970-1-1 00:00:00.123"));
		assertTrue(parser.isOfType("123"));
		assertFalse(parser.isOfType("foo-bar"));
		assertFalse(parser.isOfType(null));
	}

}
