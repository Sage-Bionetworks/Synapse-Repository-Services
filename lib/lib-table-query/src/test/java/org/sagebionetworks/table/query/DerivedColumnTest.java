package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.util.SqlElementUtils;

public class DerivedColumnTest {

	@Test
	public void testDerivedColumnToSQL() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("james");
		assertEquals("james", element.toSql());
		assertFalse(element.hasAsClause());
	}

	@Test
	public void testDerivedColumnWithASToSQL() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("james as bond");
		assertEquals("james AS bond", element.toSql());
		assertTrue(element.hasAsClause());
	}

	@Test
	public void testDerivedColumnWithFunctionToSQL() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("min(bar)");
		assertEquals("MIN(bar)", element.toSql());
	}

	@Test // See ValueExpressionTest for additional testing when no as-clause present
	public void testGetDisplayNameWithoutAsClause() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("bar");
		assertEquals("bar", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithAs() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("'bar' as foo");
		assertEquals("foo", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithAggregateAndAs() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("count(bar) as foo");
		assertEquals("foo", element.getDisplayName());
	}

	@Test
	public void testGetNameWithFunction() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("date(foo)");
		assertEquals("DATE(foo)", element.getDisplayName());
	}

	@Test
	public void testGetNameWithArithmetic() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("5 div 2");
		assertEquals("5 DIV 2", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithAsQuotes() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("'bar' as \"foo\"");
		assertEquals("foo", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithAsDoubleQuotesInsideAliasName() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("'bar' as \"fooo\"\"oooo\"");
		assertEquals("fooo\"oooo", element.getDisplayName());
	}

	@Test
	public void testGetReferencedColumnCountStar() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("count(*)");
		assertEquals(null, element.getReferencedColumn());
	}

	@Test
	public void testGetReferencedColumnCountStarAs() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("count(*) as bar");
		assertEquals(null, element.getReferencedColumn());
	}

	@Test
	public void testGetReferencedColumnAs() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("foo as bar");
		assertEquals("foo", element.getReferencedColumn().toSql());
	}

	@Test
	public void testGetReferencedColumnAsQuotes() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("'foo' as \"bar\"");
		assertEquals("'foo'", element.getReferencedColumn().toSql());
	}

	@Test
	public void testGetReferencedColumnAsQuotesDouble() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("\"foo\" as \"bar\"");
		assertEquals("\"foo\"", element.getReferencedColumn().toSql());
	}

	@Test
	public void testGetReferencedColumnFunction() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("max(bar)");
		assertEquals("bar", element.getReferencedColumn().toSql());
	}

	@Test
	public void testGetReferencedColumnFunctionQuotes() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("max(\"bar\")");
		assertEquals("\"bar\"", element.getReferencedColumn().toSql());
	}

	@Test
	public void testGetReferencedColumnFunctionQuotesSingle() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("max('bar')");
		assertEquals("'bar'", element.getReferencedColumn().toSql());
	}

	@Test
	public void testGetReferencedColumnFunctionAs() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("max(bar) as \"foo\"");
		assertEquals("bar", element.getReferencedColumn().toSql());
	}

	@Test
	public void testGetReferencedColumnFunctionDistinct() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("max(distinct bar)");
		assertEquals("bar", element.getReferencedColumn().toSql());
	}

	@Test
	public void testGetChildren() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("max(bar) as \"foo\"");
		assertEquals(Arrays.asList(element.getAsClause(), element.getValueExpression()), element.getChildren());
	}
	
	@Test
	public void testWithTableName() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("syn123.bar");
		assertEquals("syn123.bar", element.toSql());
	}
	
	@Test
	public void testWithTableNameAndAlias() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("syn123.bar as foo");
		assertEquals("syn123.bar AS foo", element.toSql());
	}
	
	@Test
	public void testWithTableNameAndAliasInFunction() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("max(syn123.bar) as foo");
		assertEquals("MAX(syn123.bar) AS foo", element.toSql());
	}
	
	@Test
	public void testWithTableAlias() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("r.bar");
		assertEquals("r.bar", element.toSql());
	}
	
	@Test
	public void testWithTableAliaAndAs() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("r.bar as goo");
		assertEquals("r.bar AS goo", element.toSql());
	}
	
	@Test
	public void testWithTableAliaAndAsInFunction() throws ParseException {
		DerivedColumn element = SqlElementUtils.createDerivedColumn("min(r.bar) as goo");
		assertEquals("MIN(r.bar) AS goo", element.toSql());
	}
	
}
