package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.ValueExpression;
import org.sagebionetworks.table.query.util.SqlElementUtils;

public class ValueExpressionTest {


	@Test
	public void testIsEquivalentNull() throws ParseException{
		ValueExpression one = SqlElementUtils.createValueExpression("foo");
		ValueExpression two =  null;
		assertFalse(one.equivalent(two));
	}
	
	@Test
	public void testIsEquivalentDifferentType() throws ParseException{
		ValueExpression one = SqlElementUtils.createValueExpression("foo");
		DerivedColumn two =  SqlElementUtils.createDerivedColumn("foo");
		assertFalse(one.equivalent(two));
	}
	
	@Test
	public void testIsEquivalentFunctionSimpleEquivalent() throws ParseException{
		ValueExpression one = SqlElementUtils.createValueExpression("foo");
		ValueExpression two = SqlElementUtils.createValueExpression("foo");
		assertTrue(one.equivalent(two));
	}
	
	@Test
	public void testIsEquivalentFunctionSimpleNotEquivalent() throws ParseException{
		ValueExpression one = SqlElementUtils.createValueExpression("foo");
		ValueExpression two = SqlElementUtils.createValueExpression("fooo");
		assertFalse(one.equivalent(two));
	}
	
	@Test
	public void testIsEquivalentFunctionSingleQuotes() throws ParseException{
		ValueExpression one = SqlElementUtils.createValueExpression("count('foo')");
		ValueExpression two = SqlElementUtils.createValueExpression("COUNT( foo )");
		assertTrue(one.equivalent(two));
	}
	
	@Test
	public void testIsEquivalentDoubleQuotes() throws ParseException{
		ValueExpression one = SqlElementUtils.createValueExpression("'has space'");
		ValueExpression two = SqlElementUtils.createValueExpression("\"has space\"");
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
		ValueExpression element = SqlElementUtils.createValueExpression("min(bar)");
		assertEquals("MIN(bar)", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameFunctionQuotes() throws ParseException{
		ValueExpression element = SqlElementUtils.createValueExpression("count(\"has space\")");
		assertEquals("COUNT(\"has space\")", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameQuotes() throws ParseException{
		ValueExpression element = SqlElementUtils.createValueExpression("\"quoted\"");
		assertEquals("quoted", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameNoQuotes() throws ParseException{
		ValueExpression element = SqlElementUtils.createValueExpression("no_space");
		assertEquals("no_space", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameDouble() throws ParseException{
		ValueExpression element = SqlElementUtils.createValueExpression("1.23");
		assertEquals("1.23", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithSpace() throws ParseException{
		ValueExpression element = SqlElementUtils.createValueExpression("\"has space\"");
		assertEquals("has space", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithFunction() throws ParseException{
		ValueExpression element = SqlElementUtils.createValueExpression("date(foo)");
		assertEquals("DATE(foo)", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithAggregateAndNoQuotes() throws ParseException{
		ValueExpression element = SqlElementUtils.createValueExpression("count(foo)");
		assertEquals("COUNT(foo)", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithAggregateAndQuotes() throws ParseException{
		ValueExpression element = SqlElementUtils.createValueExpression("count(\"foo\")");
		assertEquals("COUNT(\"foo\")", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithAggregateAndWrappedEscapedQuotes() throws ParseException{
		ValueExpression element = SqlElementUtils.createValueExpression("count(\"foo\"\"quoted\")");
		assertEquals("COUNT(\"foo\"\"quoted\")", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithArithmetic() throws ParseException{
		ValueExpression element = SqlElementUtils.createValueExpression("5 div 2");
		assertEquals("5 DIV 2", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameWithArithmeticColumn() throws ParseException{
		ValueExpression element = SqlElementUtils.createValueExpression("5+foo");
		assertEquals("5+foo", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameSingleQuote() throws ParseException {
		ValueExpression element = SqlElementUtils.createValueExpression("\"single'quote\"");
		assertEquals("single'quote", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameDoubleQuote() throws ParseException{
		ValueExpression element = SqlElementUtils.createValueExpression("\"double\"\"quote\"");
		assertEquals("double\"quote", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameBacktick() throws ParseException {
		ValueExpression element = SqlElementUtils.createValueExpression("\"back`tick\"");
		assertEquals("back`tick", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameStringLiteral() throws ParseException {
		ValueExpression element = SqlElementUtils.createValueExpression("'Some String Literal'");
		assertEquals("Some String Literal", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameEmpty() throws ParseException {
		ValueExpression element = SqlElementUtils.createValueExpression("\"\"");
		assertEquals("", element.getDisplayName());
	}

	@Test
	public void testGetDisplayNameEmptySingleValue() throws ParseException {
		ValueExpression element = SqlElementUtils.createValueExpression("\'\'");
		assertEquals("", element.getDisplayName());
	}
	
	@Test
	public void testGetChildren() throws ParseException {
		ValueExpression element = SqlElementUtils.createValueExpression("'Some String Literal'");
		assertEquals(Collections.singleton(element.getChild()), element.getChildren());
	}
}
