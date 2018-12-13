package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.NumericValueExpression;


public class NumericValueFunctionTest {

	@Test
	public void testMySqlFunction() throws ParseException{
		NumericValueExpression element = new TableQueryParser("unix_timestamp(CURRENT_TIMESTAMP - INTERVAL 1 MONTH)").numericValueExpression();
		assertEquals("UNIX_TIMESTAMP(CURRENT_TIMESTAMP-INTERVAL 1 MONTH)", element.toSql());
		assertFalse(element.hasAnyAggregateElements());
	}

}
