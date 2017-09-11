package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.NumericValueExpression;

public class NumericValueExpressionTest {

	@Test
	public void testSimpleExpression() throws ParseException{
		NumericValueExpression nve = new TableQueryParser("1234").numericValueExpression();
		assertEquals("1234", nve.toSql());
	}
	
	@Test
	public void testArithmeticExpression() throws ParseException{
		NumericValueExpression nve = new TableQueryParser("12+14-1/89*11").numericValueExpression();
		assertEquals("12+14-1/89*11", nve.toSql());
	}

}
