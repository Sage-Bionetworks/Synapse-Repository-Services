package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ValueExpressionList;

public class ValueExpressionListTest {

	@Test
	public void testValueExpressionListWithOneValue() throws ParseException {
		ValueExpressionList element = new TableQueryParser("foo").valueExpressionList();
		assertEquals("foo", element.toSql());
	}

	@Test
	public void testValueExpressionListWithMultipleValues() throws ParseException {
		ValueExpressionList element = new TableQueryParser("foo, bar, foo_bar").valueExpressionList();
		assertEquals("foo, bar, foo_bar", element.toSql());
	}
}
