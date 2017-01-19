package org.sagebionetworks.repo.model.table.parser;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class DoubleParserTest {

	DoubleParser parser;
	
	@Before
	public void before(){
		parser = new DoubleParser();
	}

	@Test
	public void testParseValueForDatabaseWrite(){
		Double expected = new Double(123.456);
		Object objectValue = parser.parseValueForDatabaseWrite("123.456");
		assertEquals(expected, objectValue);
	}
	
	@Test
	public void testParseValueForDatabaseRead(){
		assertEquals("123.456", parser.parseValueForDatabaseRead("123.456"));
	}
}
