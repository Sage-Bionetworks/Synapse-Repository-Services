package org.sagebionetworks.repo.model.table.parser;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class LongParserTest {
	
	LongParser parser;
	
	@Before
	public void before(){
		parser = new LongParser();
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
}
