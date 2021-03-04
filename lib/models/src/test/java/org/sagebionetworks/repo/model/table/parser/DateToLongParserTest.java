package org.sagebionetworks.repo.model.table.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

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
	public void testParseValueForDatabaseWriteISO8601(){
		long expected = LocalDateTime.of(2018,9,25,14,37,27)
				.toInstant(ZoneOffset.UTC).toEpochMilli();
		Object objectValue = parser.parseValueForDatabaseWrite("2018-09-25T14:37:27Z");
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
