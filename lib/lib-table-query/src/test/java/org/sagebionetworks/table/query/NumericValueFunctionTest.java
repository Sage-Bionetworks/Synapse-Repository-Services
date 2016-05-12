package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.FunctionType;
import org.sagebionetworks.table.query.model.NumericValueFunction;

public class NumericValueFunctionTest {
	
	@Test
	public void testIsAggregate() throws ParseException{
		NumericValueFunction function = new TableQueryParser("FOUND_ROWS()").numericValueFunction();
		assertTrue(function.isElementAggregate());
	}
	
	@Test
	public void testGetFunctionType() throws ParseException{
		NumericValueFunction function = new TableQueryParser("FOUND_ROWS()").numericValueFunction();
		assertEquals(FunctionType.FOUND_ROWS, function.getFunctionType());
	}

}
