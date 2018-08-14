package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.DerivedColumn;
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
	public void testGetNameWithDoubleQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("\"has space\"");
		assertEquals("has space", element.getDisplayName());
	}
	
	@Test
	public void testGetNameWithFunction() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("date(foo)");
		assertEquals("DATE(foo)", element.getDisplayName());
	}
	
	@Test
	public void testGetNameWithArithmetic() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("5 div 2");
		assertEquals("5 DIV 2", element.getDisplayName());
	}
	
	@Test
	public void testGetNameWithArithmeticColumn() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("5+foo");
		assertEquals("5+foo", element.getDisplayName());
	}
	
	@Test
	public void testGetReferencedColumnCountStar() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("count(*)");
		assertEquals(null, element.getReferencedColumnName());
	}
	
	@Test
	public void testGetReferencedColumnCountStarAs() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("count(*) as bar");
		assertEquals(null, element.getReferencedColumnName());
	}
	
	@Test
	public void testGetReferencedColumnAs() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("foo as bar");
		assertEquals("foo", element.getReferencedColumnName());
	}
	
	@Test
	public void testGetReferencedColumnAsQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("'foo' as \"bar\"");
		assertEquals("foo", element.getReferencedColumnName());
	}
	
	@Test
	public void testGetReferencedColumnAsQuotesDouble() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("\"foo\" as \"bar\"");
		assertEquals("foo", element.getReferencedColumnName());
	}
	
	@Test
	public void testGetReferencedColumnFunction() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(bar)");
		assertEquals("bar", element.getReferencedColumnName());
	}
	
	@Test
	public void testGetReferencedColumnFunctionQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(\"bar\")");
		assertEquals("bar", element.getReferencedColumnName());
	}
	
	@Test
	public void testGetReferencedColumnNameFunctionQuotesSingle() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max('bar')");
		assertEquals("bar", element.getReferencedColumnName());
	}
	
	@Test
	public void testGetReferencedColumnNameFunctionAs() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(bar) as \"foo\"");
		assertEquals("bar", element.getReferencedColumnName());
	}

	@Test
	public void testGetReferencedColumnNameFunctionDistinct() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(distinct bar)");
		assertEquals("bar", element.getReferencedColumnName());
	}
	
	@Test
	public void testStripLeadingAndTailingQuotesSingleQuote() {
		String result = DerivedColumn.stripLeadingAndTailingQuotes("'single'quote'");
		assertEquals("single'quote", result);
	}
	
	@Test
	public void testStripLeadingAndTailingQuotesDouble() {
		String result = DerivedColumn.stripLeadingAndTailingQuotes("\"double\"quote\"");
		assertEquals("double\"quote", result);
	}
	
	@Test
	public void testStripLeadingAndTailingQuotesBacktick() {
		String result = DerivedColumn.stripLeadingAndTailingQuotes("`back`tick`");
		assertEquals("back`tick", result);
	}
	
	@Test
	public void testStripLeadingAndTailingQuotesNoQuotes() {
		String result = DerivedColumn.stripLeadingAndTailingQuotes("count('foo')");
		assertEquals("count('foo')", result);
	}
	
	@Test
	public void testStripLeadingAndTailingQuotesEmptyk() {
		String result = DerivedColumn.stripLeadingAndTailingQuotes("");
		assertEquals("", result);
	}
	
	@Test
	public void testStripLeadingAndTailingQuotesNull() {
		String result = DerivedColumn.stripLeadingAndTailingQuotes(null);
		assertEquals(null, result);
	}
	
	@Test
	public void testStripLeadingAndTailingQuotesJustQuotes() {
		String result = DerivedColumn.stripLeadingAndTailingQuotes("''");
		assertEquals("", result);
	}
	
}
