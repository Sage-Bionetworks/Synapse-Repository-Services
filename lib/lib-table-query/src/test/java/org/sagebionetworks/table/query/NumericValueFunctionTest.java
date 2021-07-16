package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.NumericValueExpression;
import org.sagebionetworks.table.query.model.NumericValueFunction;

public class NumericValueFunctionTest {

	@Test
	public void testMySqlFunction() throws ParseException {
		NumericValueExpression element = new TableQueryParser("unix_timestamp(CURRENT_TIMESTAMP - INTERVAL 1 MONTH)")
				.numericValueExpression();
		assertEquals("UNIX_TIMESTAMP(CURRENT_TIMESTAMP-INTERVAL 1 MONTH)", element.toSql());
		assertFalse(element.hasAnyAggregateElements());
	}

	@Test
	public void testGetChildren() throws ParseException {
		NumericValueFunction element = new TableQueryParser("unix_timestamp(CURRENT_TIMESTAMP - INTERVAL 1 MONTH)")
				.numericValueFunction();
		assertEquals(Collections.singleton(element.getChild()), element.getChildren());
	}

}
