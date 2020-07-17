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

	@Test //See ValueExpressionTest for additional testing when no as-clause present
	public void testGetDisplayNameWithoutAsClause() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("bar");
		assertEquals("bar", element.getDisplayName());
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
	public void testGetDisplayNameWithAsDoubleQuotesInsideAliasName() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("'bar' as \"fooo\"\"oooo\"");
		assertEquals("fooo\"oooo", element.getDisplayName());
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
	public void testGetReferencedColumnFunctionQuotesSingle() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max('bar')");
		assertEquals("'bar'", element.getReferencedColumn().toSql());
	}

	@Test
	public void testGetReferencedColumnFunctionAs() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(bar) as \"foo\"");
		assertEquals("bar", element.getReferencedColumn().toSql());
	}

	@Test
	public void testGetReferencedColumnFunctionDistinct() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(distinct bar)");
		assertEquals("bar", element.getReferencedColumn().toSql());
	}

	////////////////////////
	// getColumnSQL() tests
	////////////////////////
	@Test
	public void testGetColumnSQLWithAsClauseQuoted() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("max(\"bar\") as \"foo\"\"bar\"\"baz\"");
		assertEquals("\"foo\"\"bar\"\"baz\"", element.getColumnSQL());
	}

	@Test
	public void testGetColumnSQLWithoutAsClauseQuoted() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("\"foo\"\"bar\"\"baz\"");
		assertEquals("\"foo\"\"bar\"\"baz\"", element.getColumnSQL());
	}
}
