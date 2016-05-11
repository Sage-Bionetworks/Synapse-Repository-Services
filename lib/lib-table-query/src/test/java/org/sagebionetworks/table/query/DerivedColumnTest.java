package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.FunctionType;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class DerivedColumnTest {
	
	@Test
	public void testDerivedColumnToSQL() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("james");
		assertEquals("james", element.toString());
	}
	
	@Test
	public void testDerivedColumnWithASToSQL() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("james as bond");
		assertEquals("james AS bond", element.toString());
	}
	
	@Test
	public void testDerivedColumnWithFunctionToSQL() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("min(bar)");
		assertEquals("MIN(bar)", element.toString());
	}
	
	@Test
	public void testDerivedColumnGetNameFunction() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("min(bar)");
		assertEquals("MIN(bar)", element.getDisplayName());
	}
	
	@Test
	public void testDerivedColumnGetNameFunctionQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("count('has space')");
		assertEquals("COUNT('has space')", element.getDisplayName());
	}

	@Test
	public void testDerivedColumnGetNameQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("'has space'");
		assertEquals("has space", element.getDisplayName());
	}
	
	@Test
	public void testDerivedColumnGetNameNoQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("no_space");
		assertEquals("no_space", element.getDisplayName());
	}
	
	@Test
	public void testDerivedColumnGetNameDouble() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("1.23");
		assertEquals("1.23", element.getDisplayName());
	}
	
	@Test
	public void testGetNameWithAs() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("'bar' as foo");
		assertEquals("foo", element.getDisplayName());
	}
	@Test
	public void testGetNameWithAsQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("'bar' as \"foo\"");
		assertEquals("foo", element.getDisplayName());
	}
	
	@Test
	public void testGetNameWithAsAndFunction() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("count(bar) as foo");
		assertEquals("foo", element.getDisplayName());
	}
	
	@Test
	public void testGetFunctionTypeNoFunctionCount() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("foo as bar");
		assertEquals(null, element.getFunctionType());
	}
	
	@Test
	public void testGetFunctionTypeCount() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("count(*) as foo");
		assertEquals(FunctionType.COUNT, element.getFunctionType());
	}
	
	@Test
	public void testGetFunctionTypeFoundRows() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("FOUND_ROWS()");
		assertEquals(FunctionType.FOUND_ROWS, element.getFunctionType());
	}
	
	@Test
	public void testGetFunctionTypeMax() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(foo) as bar");
		assertEquals(FunctionType.MAX, element.getFunctionType());
	}
	
	@Test
	public void testGetFunctionTypeMin() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("min('foo')");
		assertEquals(FunctionType.MIN, element.getFunctionType());
	}
	
	@Test
	public void testGetFunctionTypeSum() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("sum('foo')");
		assertEquals(FunctionType.SUM, element.getFunctionType());
	}
	
	@Test
	public void testGetFunctionTypeAvg() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("avg('foo')");
		assertEquals(FunctionType.AVG, element.getFunctionType());
	}
	
	@Test
	public void testGetReferencedColumnCountStar() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("count(*)");
		assertEquals(null, element.getReferencedColumnName());
		assertEquals(null, element.isReferencedColumnSurroundedWithQuotes());
	}
	
	@Test
	public void testGetReferencedColumnFoundRows() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("found_rows()");
		assertEquals(null, element.getReferencedColumnName());
		assertEquals(null, element.isReferencedColumnSurroundedWithQuotes());
	}
	
	@Test
	public void testGetReferencedColumnCountStarAs() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("count(*) as bar");
		assertEquals(null, element.getReferencedColumnName());
		assertEquals(null, element.isReferencedColumnSurroundedWithQuotes());
	}
	
	@Test
	public void testGetReferencedColumnAs() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("foo as bar");
		assertEquals("foo", element.getReferencedColumnName());
		assertFalse(element.isReferencedColumnSurroundedWithQuotes());
	}
	
	@Test
	public void testGetReferencedColumnAsQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("'foo' as \"bar\"");
		assertEquals("foo", element.getReferencedColumnName());
		assertTrue(element.isReferencedColumnSurroundedWithQuotes());
	}
	
	@Test
	public void testGetReferencedColumnAsQuotesDouble() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("\"foo\" as \"bar\"");
		assertEquals("foo", element.getReferencedColumnName());
		assertTrue(element.isReferencedColumnSurroundedWithQuotes());
	}
	
	@Test
	public void testGetReferencedColumnFunction() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(bar)");
		assertEquals("bar", element.getReferencedColumnName());
		assertFalse(element.isReferencedColumnSurroundedWithQuotes());
	}
	
	@Test
	public void testGetReferencedColumnFunctionQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(\"bar\")");
		assertEquals("bar", element.getReferencedColumnName());
		assertTrue(element.isReferencedColumnSurroundedWithQuotes());
	}
	
	@Test
	public void testGetReferencedColumnNameFunctionQuotesSingle() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max('bar')");
		assertEquals("bar", element.getReferencedColumnName());
		assertTrue(element.isReferencedColumnSurroundedWithQuotes());
	}
	
	@Test
	public void testGetReferencedColumnNameFunctionAs() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(bar) as \"foo\"");
		assertEquals("bar", element.getReferencedColumnName());
		assertFalse(element.isReferencedColumnSurroundedWithQuotes());
	}

	@Test
	public void testGetReferencedColumnNameFunctionDistinct() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(distinct bar)");
		assertEquals("bar", element.getReferencedColumnName());
		assertFalse(element.isReferencedColumnSurroundedWithQuotes());
	}
	
}
