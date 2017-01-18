package org.sagebionetworks.repo.model.table.parser;

import static org.junit.Assert.*;

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

}
