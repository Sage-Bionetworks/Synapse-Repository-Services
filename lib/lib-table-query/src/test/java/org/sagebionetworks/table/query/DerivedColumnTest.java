package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class DerivedColumnTest {
	
	@Test
	public void testDerivedColumnToSQL() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("james");
		assertEquals("james", element.toSql());
	}
	
	@Test
	public void testDerivedColumnWithASToSQL() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("james as bond");
		assertEquals("james AS bond", element.toSql());
	}
	
	@Test
	public void testDerivedColumnWithFunctionToSQL() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("min(bar)");
		assertEquals("MIN(bar)", element.toSql());
	}
	
	@Test
	public void testDerivedColumnGetNameFunction() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("min(bar)");
		assertEquals("MIN(bar)", element.getDisplayName());
	}
	
	@Test
	public void testDerivedColumnGetNameFunctionQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("count(\"has space\")");
		assertEquals("COUNT(\"has space\")", element.getDisplayName());
	}

	@Test
	public void testDerivedColumnGetNameQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("\"has space\"");
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
	public void testGetDisplayNameWithAs() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("'bar' as foo");
		assertEquals("foo", element.getDisplayName());
	}
	@Test
	public void testGetDisplayNameWithAsQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("'bar' as \"foo\"");
		assertEquals("foo", element.getDisplayName());
	}
	
	@Test
	public void testGetDisplayNameWithAsAndFunction() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("count(bar) as foo");
		assertEquals("foo", element.getDisplayName());
	}
	//TODO: A lot of these tests that test the 'as' clause seem unnecessary
	@Test
	public void testGetDisplayNameWithAsAndDoubleQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("\"bar\"\"baz\" as foo");
		assertEquals("foo", element.getDisplayName());
	}
	
	@Test
	public void testGetDisplayNameWithSpace() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("\"has space\"");
		assertEquals("has space", element.getDisplayName());
	}
	
	@Test
	public void testGetDisplayNameWithFunction() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("date(foo)");
		assertEquals("DATE(foo)", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithAggregate() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("count(foo)");
		assertEquals("COUNT(foo)", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithAggregateAndQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("count(\"foo\")");
		assertEquals("COUNT(\"foo\")", element.getDisplayName());
	}
	
	@Test
	public void testGetDisplayNameWithArithmetic() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("5 div 2");
		assertEquals("5 DIV 2", element.getDisplayName());
	}
	
	@Test
	public void testGetDisplayNameWithArithmeticColumn() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("5+foo");
		assertEquals("5+foo", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameSingleQuote() throws ParseException {
		DerivedColumn element = SqlElementUntils.createDerivedColumn("\"single'quote\"");
		assertEquals("single'quote", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameDoubleQuote() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("\"double\"\"quote\"");
		assertEquals("double\"quote", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameBacktick() throws ParseException {
		DerivedColumn element = SqlElementUntils.createDerivedColumn("\"back`tick\"");
		assertEquals("back`tick", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameEmptyk() throws ParseException {
		DerivedColumn element = SqlElementUntils.createDerivedColumn("\"\"");
		assertEquals("", element.getDisplayName());
	}

	@Test
	public void testGetReferencedColumnCountStar() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("count(*)");
		assertEquals(null, element.getReferencedColumn());
	}

	@Test
	public void testGetReferencedColumnCountStarAs() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("count(*) as bar");
		assertEquals(null, element.getReferencedColumn());
	}

	@Test
	public void testGetReferencedColumnAs() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("foo as bar");
		assertEquals("foo", element.getReferencedColumn().toSql());
	}

	@Test
	public void testGetReferencedColumnAsQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("'foo' as \"bar\"");
		assertEquals("'foo'", element.getReferencedColumn().toSql());
	}

	@Test
	public void testGetReferencedColumnAsQuotesDouble() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("\"foo\" as \"bar\"");
		assertEquals("\"foo\"", element.getReferencedColumn().toSql());
	}

	@Test
	public void testGetReferencedColumnFunction() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(bar)");
		assertEquals("bar", element.getReferencedColumn().toSql());
	}

	@Test
	public void testGetReferencedColumnFunctionQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(\"bar\")");
		assertEquals("\"bar\"", element.getReferencedColumn().toSql());
	}

	@Test
	public void testgetReferencedColumnFunctionQuotesSingle() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max('bar')");
		assertEquals("'bar'", element.getReferencedColumn().toSql());
	}

	@Test
	public void testgetReferencedColumnFunctionAs() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(bar) as \"foo\"");
		assertEquals("bar", element.getReferencedColumn().toSql());
	}

	@Test
	public void testgetReferencedColumnFunctionDistinct() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(distinct bar)");
		assertEquals("bar", element.getReferencedColumn().toSql());
	}

}
