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

}
