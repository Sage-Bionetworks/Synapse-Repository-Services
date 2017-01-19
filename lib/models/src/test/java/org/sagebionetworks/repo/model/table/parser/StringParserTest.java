package org.sagebionetworks.repo.model.table.parser;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class StringParserTest {

	StringParser parser;
	
	@Before
	public void before(){
		parser = new StringParser();
	}

	@Test
	public void testParseValueForDatabaseWrite(){
		String expected = "a string";
		Object result = parser.parseValueForDatabaseWrite("a string");
		assertEquals(expected, result);
	}
	
	@Test
	public void test(){
		assertEquals("a string", parser.parseValueForDatabaseRead("a string"));
	}
}
