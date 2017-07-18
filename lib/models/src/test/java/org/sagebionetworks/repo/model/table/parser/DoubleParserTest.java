package org.sagebionetworks.repo.model.table.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
	public void testParseValueForDatabaseWriteNaN(){
		Object objectValue = parser.parseValueForDatabaseWrite("nan");
		assertEquals(Double.NaN, objectValue);
		objectValue = parser.parseValueForDatabaseWrite("NaN");
		assertEquals(Double.NaN, objectValue);
		objectValue = parser.parseValueForDatabaseWrite("NAN");
		assertEquals(Double.NaN, objectValue);
	}
	
	@Test
	public void testParseValueForDatabaseWriteInfinity(){
		assertEquals(Double.POSITIVE_INFINITY, parser.parseValueForDatabaseWrite("inf"));
		assertEquals(Double.POSITIVE_INFINITY, parser.parseValueForDatabaseWrite("+inf"));
		assertEquals(Double.POSITIVE_INFINITY, parser.parseValueForDatabaseWrite("infinity"));
		assertEquals(Double.POSITIVE_INFINITY, parser.parseValueForDatabaseWrite("+infinity"));
		assertEquals(Double.POSITIVE_INFINITY, parser.parseValueForDatabaseWrite("INF"));
		assertEquals(Double.POSITIVE_INFINITY, parser.parseValueForDatabaseWrite("+INF"));
		assertEquals(Double.POSITIVE_INFINITY, parser.parseValueForDatabaseWrite("Infinity"));
		assertEquals(Double.POSITIVE_INFINITY, parser.parseValueForDatabaseWrite("+Infinity"));
	}
	
	@Test
	public void testParseValueForDatabaseWriteNegativeInfinity(){
		assertEquals(Double.NEGATIVE_INFINITY, parser.parseValueForDatabaseWrite("-inf"));
		assertEquals(Double.NEGATIVE_INFINITY, parser.parseValueForDatabaseWrite("-infinity"));
		assertEquals(Double.NEGATIVE_INFINITY, parser.parseValueForDatabaseWrite("-INF"));
		assertEquals(Double.NEGATIVE_INFINITY, parser.parseValueForDatabaseWrite("-Infinity"));
	}
	
	@Test
	public void testParseValueForDatabaseRead(){
		assertEquals("123.456", parser.parseValueForDatabaseRead("123.456"));
	}
	
	@Test
	public void testIsOfType(){
		assertTrue(parser.isOfType("1"));
		assertTrue(parser.isOfType("1.2e-12"));
		assertTrue(parser.isOfType("Infinity"));
		assertTrue(parser.isOfType("-INF"));
		assertFalse(parser.isOfType("foo-bar"));
		assertFalse(parser.isOfType(null));
	}
}
