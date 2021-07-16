package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ParenthesizedValueExpression;

public class ParenthesizedValueExpressionTest {

	@Test
	public void testParenthesizedValue() throws ParseException {
		ParenthesizedValueExpression element = new TableQueryParser(" ( 1 /2 )").parenthesizedValueExpression();
		assertEquals("(1/2)", element.toSql());
	}

	@Test
	public void testGetChildren() throws ParseException {
		ParenthesizedValueExpression element = new TableQueryParser(" ( 1 /2 )").parenthesizedValueExpression();
		assertEquals(Collections.singleton(element.getChild()), element.getChildren());
	}
}
