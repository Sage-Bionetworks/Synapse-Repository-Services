package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.ValueExpression;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class ValueExpressionTest {


	@Test
	public void testIsEquivalentNull() throws ParseException{
		ValueExpression one = SqlElementUntils.createValueExpression("foo");
		ValueExpression two =  null;
		assertFalse(one.equivalent(two));
	}
	
	@Test
	public void testIsEquivalentDifferentType() throws ParseException{
		ValueExpression one = SqlElementUntils.createValueExpression("foo");
		DerivedColumn two =  SqlElementUntils.createDerivedColumn("foo");
		assertFalse(one.equivalent(two));
	}
	
	@Test
	public void testIsEquivalentFunctionSimpleEquivalent() throws ParseException{
		ValueExpression one = SqlElementUntils.createValueExpression("foo");
		ValueExpression two = SqlElementUntils.createValueExpression("foo");
		assertTrue(one.equivalent(two));
	}
	
	@Test
	public void testIsEquivalentFunctionSimpleNotEquivalent() throws ParseException{
		ValueExpression one = SqlElementUntils.createValueExpression("foo");
		ValueExpression two = SqlElementUntils.createValueExpression("fooo");
		assertFalse(one.equivalent(two));
	}
	
	@Test
	public void testIsEquivalentFunctionSingleQuotes() throws ParseException{
		ValueExpression one = SqlElementUntils.createValueExpression("count('foo')");
		ValueExpression two = SqlElementUntils.createValueExpression("COUNT( foo )");
		assertTrue(one.equivalent(two));
	}
	
	@Test
	public void testIsEquivalentDoubleQuotes() throws ParseException{
		ValueExpression one = SqlElementUntils.createValueExpression("'has space'");
		ValueExpression two = SqlElementUntils.createValueExpression("\"has space\"");
		assertTrue(one.equivalent(two));
	}
	
	@Test
	public void testArithmetic() throws ParseException{
		ValueExpression one = new TableQueryParser("100/1").valueExpression();
		assertEquals("100/1", one.toSql());
	}
	
	@Test
	public void testEntityId() throws ParseException {
		ValueExpression one = new TableQueryParser("syn11.22").valueExpression();
		assertEquals("syn11.22", one.toSql());
	}

	//////////////////////////
	// getDisplayName() tests
	//////////////////////////

	@Test
	public void testGetDisplayNameFunction() throws ParseException{
		ValueExpression element = SqlElementUntils.createValueExpression("min(bar)");
		assertEquals("MIN(bar)", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameFunctionQuotes() throws ParseException{
		ValueExpression element = SqlElementUntils.createValueExpression("count(\"has space\")");
		assertEquals("COUNT(\"has space\")", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameQuotes() throws ParseException{
		ValueExpression element = SqlElementUntils.createValueExpression("\"quoted\"");
		assertEquals("quoted", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameNoQuotes() throws ParseException{
		ValueExpression element = SqlElementUntils.createValueExpression("no_space");
		assertEquals("no_space", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameDouble() throws ParseException{
		ValueExpression element = SqlElementUntils.createValueExpression("1.23");
		assertEquals("1.23", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithSpace() throws ParseException{
		ValueExpression element = SqlElementUntils.createValueExpression("\"has space\"");
		assertEquals("has space", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithFunction() throws ParseException{
		ValueExpression element = SqlElementUntils.createValueExpression("date(foo)");
		assertEquals("DATE(foo)", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithAggregateAndNoQuotes() throws ParseException{
		ValueExpression element = SqlElementUntils.createValueExpression("count(foo)");
		assertEquals("COUNT(foo)", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithAggregateAndQuotes() throws ParseException{
		ValueExpression element = SqlElementUntils.createValueExpression("count(\"foo\")");
		assertEquals("COUNT(\"foo\")", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithAggregateAndWrappedEscapedQuotes() throws ParseException{
		ValueExpression element = SqlElementUntils.createValueExpression("count(\"foo\"\"quoted\")");
		assertEquals("COUNT(\"foo\"\"quoted\")", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithArithmetic() throws ParseException{
		ValueExpression element = SqlElementUntils.createValueExpression("5 div 2");
		assertEquals("5 DIV 2", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithArithmeticColumn() throws ParseException{
		ValueExpression element = SqlElementUntils.createValueExpression("5+foo");
		assertEquals("5+foo", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameSingleQuote() throws ParseException {
		ValueExpression element = SqlElementUntils.createValueExpression("\"single'quote\"");
		assertEquals("single'quote", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameDoubleQuote() throws ParseException{
		ValueExpression element = SqlElementUntils.createValueExpression("\"double\"\"quote\"");
		assertEquals("double\"quote", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameBacktick() throws ParseException {
		ValueExpression element = SqlElementUntils.createValueExpression("\"back`tick\"");
		assertEquals("back`tick", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameStringLiteral() throws ParseException {
		ValueExpression element = SqlElementUntils.createValueExpression("'Some String Literal'");
		assertEquals("Some String Literal", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameEmpty() throws ParseException {
		ValueExpression element = SqlElementUntils.createValueExpression("\"\"");
		assertEquals("", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameEmptySingleValue() throws ParseException {
		ValueExpression element = SqlElementUntils.createValueExpression("\'\'");
		assertEquals("", element.getDisplayName());
	}
}
