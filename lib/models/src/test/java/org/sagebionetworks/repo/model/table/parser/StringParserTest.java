package org.sagebionetworks.repo.model.table.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

import org.json.JSONArray;
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
	
	@Test
	public void testIsOfType(){
		assertTrue(parser.isOfType("a string"));
		assertFalse(parser.isOfType(null));
	}


	@Test
	public void temptest(){
		List<Double> jsonArray = new ArrayList<>();
		jsonArray.add(Double.POSITIVE_INFINITY);
		jsonArray.add(Double.NEGATIVE_INFINITY);
		jsonArray.add(Double.NaN);
		System.out.println(jsonArray);
	}
}
