package org.sagebionetworks.repo.model.table.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.table.AbstractDouble;

public class DoubleTypeParserTest {

	DoubleTypeParser parser;
	
	@Before
	public void before(){
		parser = new DoubleTypeParser();
	}

	@Test (expected=IllegalArgumentException.class)
	public void testParseValueForDatabaseWriteFinite(){
		// Illegal argument is thrown for finite values.
		parser.parseValueForDatabaseWrite("123.456");
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testParseValueForDatabaseWriteString(){
		parser.parseValueForDatabaseWrite("some string");
	}
	
	@Test
	public void testParseValueForDatabaseWrite(){
		assertEquals(AbstractDouble.NAN.getEnumerationValue(), parser.parseValueForDatabaseWrite("NaN"));
		assertEquals(AbstractDouble.POSITIVE_INFINITY.getEnumerationValue(), parser.parseValueForDatabaseWrite("inf"));
		assertEquals(AbstractDouble.NEGATIVE_INFINITY.getEnumerationValue(), parser.parseValueForDatabaseWrite("-inf"));
	}
	
	@Test
	public void testParseValueForDatabaseWriteInfinity(){
		assertEquals("Infinity", parser.parseValueForDatabaseWrite("inf"));
		assertEquals("Infinity", parser.parseValueForDatabaseWrite("+inf"));
		assertEquals("Infinity", parser.parseValueForDatabaseWrite("infinity"));
		assertEquals("Infinity", parser.parseValueForDatabaseWrite("+infinity"));
		assertEquals("Infinity", parser.parseValueForDatabaseWrite("INF"));
		assertEquals("Infinity", parser.parseValueForDatabaseWrite("+INF"));
		assertEquals("Infinity", parser.parseValueForDatabaseWrite("Infinity"));
		assertEquals("Infinity", parser.parseValueForDatabaseWrite("+Infinity"));
	}
	
	@Test
	public void testParseValueForDatabaseWriteNegativeInfinity(){
		assertEquals("-Infinity", parser.parseValueForDatabaseWrite("-inf"));
		assertEquals("-Infinity", parser.parseValueForDatabaseWrite("-infinity"));
		assertEquals("-Infinity", parser.parseValueForDatabaseWrite("-INF"));
		assertEquals("-Infinity", parser.parseValueForDatabaseWrite("-Infinity"));
	}
	
	@Test
	public void testParseValueForDatabaseWriteNegativeNaN(){
		assertEquals("NaN", parser.parseValueForDatabaseWrite("nan"));
		assertEquals("NaN", parser.parseValueForDatabaseWrite("NAN"));
	}
	
	@Test
	public void testParseValueForDatabaseRead(){
		assertEquals("123.456", parser.parseValueForDatabaseRead("123.456"));
	}
	
	@Test
	public void testIsOfType(){
		assertFalse(parser.isOfType("1"));
		assertFalse(parser.isOfType("1.2e-12"));
		assertTrue(parser.isOfType("Infinity"));
		assertTrue(parser.isOfType("-INF"));
		assertFalse(parser.isOfType("foo-bar"));
		assertFalse(parser.isOfType(null));
	}
}
