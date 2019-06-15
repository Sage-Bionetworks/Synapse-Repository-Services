package org.sagebionetworks.repo.model.table.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class BooleanParserTest {
	
	BooleanParser parser;
	
	@Before
	public void before(){
		parser = new BooleanParser();
	}

	@Test
	public void testParseValueForDatabaseWrite(){
		assertEquals(Boolean.TRUE, parser.parseValueForDatabaseWrite("True"));
		assertEquals(Boolean.TRUE, parser.parseValueForDatabaseWrite("true"));
		assertEquals(Boolean.FALSE, parser.parseValueForDatabaseWrite("FALSE"));
		assertEquals(Boolean.FALSE, parser.parseValueForDatabaseWrite("false"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testParseValueForDatabaseWriteNonBoolean(){
		parser.parseValueForDatabaseWrite("not a boolean");
	}
	
	@Test
	public void testParseValueForDatabaseReadTrue(){
		assertEquals("false", parser.parseValueForDatabaseRead("0"));
		assertEquals("true", parser.parseValueForDatabaseRead("1"));
		assertEquals("false", parser.parseValueForDatabaseRead("False"));
		assertEquals("true", parser.parseValueForDatabaseRead("True"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testParseValueForDatabaseReadNonBoolean(){
		parser.parseValueForDatabaseRead("not a boolean");
	}
	
	@Test
	public void testIsOfType(){
		assertFalse(parser.isOfType("0"));
		assertTrue(parser.isOfType("False"));
		assertFalse(parser.isOfType("1"));
		assertTrue(parser.isOfType("True"));
	}
	
}
