package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.NumericValueExpression;

public class NumericValueExpressionTest {

	@Test
	public void testSimpleExpression() throws ParseException {
		NumericValueExpression nve = new TableQueryParser("1234").numericValueExpression();
		assertEquals("1234", nve.toSql());
	}

	@Test
	public void testArithmeticExpression() throws ParseException {
		NumericValueExpression nve = new TableQueryParser("12+14-1/89*11%2").numericValueExpression();
		assertEquals("12+14-1/89*11%2", nve.toSql());
	}

	@Test
	public void testSignedExponent() throws ParseException {
		NumericValueExpression nve = new TableQueryParser("-1.12e+15").numericValueExpression();
		assertEquals("-1.12E15", nve.toSql());
	}

	@Test
	public void testGetChildren() throws ParseException {
		NumericValueExpression element = new TableQueryParser("-1.12e+15 * 15.1 + 3.1 -4.6").numericValueExpression();
		List<Element> expected = new LinkedList<>();
		expected.add(element.getTerm());
		expected.addAll(element.getPrimeList());
		assertEquals(expected, element.getChildren());
	}

}
