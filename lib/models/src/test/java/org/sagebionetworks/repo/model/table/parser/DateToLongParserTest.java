package org.sagebionetworks.repo.model.table.parser;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class DateToLongParserTest {

	DateToLongParser parser;
	
	@Before
	public void before(){
		parser = new DateToLongParser();
	}
	
	@Test
	public void testParseValueForDatabaseWriteDateString(){
		Long expected = new Long(123);
		Object objectValue = parser.parseValueForDatabaseWrite("1970-1-1 00:00:00.123");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testParseValueForDatabaseWriteLong(){
		Long expected = new Long(123);
		Object objectValue = parser.parseValueForDatabaseWrite("123");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testParseValueForDatabaseRead(){
		assertEquals("123", parser.parseValueForDatabaseRead("123"));
	}

}
