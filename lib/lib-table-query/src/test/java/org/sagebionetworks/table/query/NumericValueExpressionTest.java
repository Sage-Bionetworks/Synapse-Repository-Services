package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.table.query.model.NumericValueExpression;
import org.sagebionetworks.table.query.model.UnsignedInteger;

public class NumericValueExpressionTest {

	@Test
	public void testSimpleExpression() throws ParseException{
		NumericValueExpression nve = new TableQueryParser("1234").numericValueExpression();
		assertEquals("1234", nve.toSql());
	}
	
	@Test
	public void testArithmeticExpression() throws ParseException{
		NumericValueExpression nve = new TableQueryParser("12+14-1/89*11%2").numericValueExpression();
		assertEquals("12+14-1/89*11%2", nve.toSql());
	}
	
	@Test
	public void testSignedExponent() throws ParseException{
		NumericValueExpression nve = new TableQueryParser("-1.12e+15").numericValueExpression();
		assertEquals("-1.12E15", nve.toSql());
	}

}
