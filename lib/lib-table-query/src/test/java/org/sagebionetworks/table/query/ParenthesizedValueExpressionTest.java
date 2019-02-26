package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ParenthesizedValueExpression;

public class ParenthesizedValueExpressionTest {

	@Test
	public void testParenthesizedValue() throws ParseException{
		ParenthesizedValueExpression element = new TableQueryParser(" ( 1 /2 )").parenthesizedValueExpression();
		assertEquals("(1/2)", element.toSql());
	}

}
