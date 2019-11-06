package org.sagebionetworks.repo.model.table.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class EntityIdParserTest {
	
	EntityIdParser parser;
	
	@Before
	public void before(){
		parser = new EntityIdParser();
	}

	@Test
	public void testParseValueForDatabaseWrite(){
		Long expected = new Long(123);
		Object result = parser.parseValueForDatabaseWrite("syn123");
		assertEquals(expected, result);
	}
	
	@Test
	public void testParseValueForDatabaseWriteUpder(){
		Long expected = new Long(123);
		Object result = parser.parseValueForDatabaseWrite("SYN123");
		assertEquals(expected, result);
	}
	
	@Test
	public void testParseValueForDatabaseWriteNoSyn(){
		Long expected = new Long(123);
		Object result = parser.parseValueForDatabaseWrite("123");
		assertEquals(expected, result);
	}
	
	public void testParseValueForDatabaseRead(){
		assertEquals("syn123", parser.parseValueForDatabaseRead("123"));
	}
	
	@Test
	public void testIsOfType(){
		assertTrue(parser.isOfType("123"));
		assertTrue(parser.isOfType("syn123"));
		assertFalse(parser.isOfType("foo-bar"));
		assertFalse(parser.isOfType(null));
	}
}
