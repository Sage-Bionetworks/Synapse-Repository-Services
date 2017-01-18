package org.sagebionetworks.repo.model.table.parser;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class BooleanParserTest {
	
	BooleanParser parser;
	
	@Before
	public void before(){
		parser = new BooleanParser();
	}

	@Test
	public void testParseValueForDatabaseWriteTrue(){
		Boolean expected = new Boolean(true);
		Object objectValue = parser.parseValueForDatabaseWrite("true");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testParseValueForDatabaseWriteFalse(){
		Boolean expected = new Boolean(false);
		Object objectValue = parser.parseValueForDatabaseWrite("false");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testParseValueForDatabaseReadTrue(){
		assertEquals("false", parser.parseValueForDatabaseRead("0"));
		assertEquals("false", parser.parseValueForDatabaseRead("false"));
		assertEquals("true", parser.parseValueForDatabaseRead("1"));
		assertEquals("true", parser.parseValueForDatabaseRead("true"));
	}
	
}
