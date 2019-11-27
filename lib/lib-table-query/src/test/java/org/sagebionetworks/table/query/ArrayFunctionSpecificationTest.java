package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.FunctionReturnType;
import org.sagebionetworks.table.query.model.ArrayFunctionSpecification;

public class ArrayFunctionSpecificationTest {

	@Test
	public void testUnnest() throws ParseException{
		ArrayFunctionSpecification element =
				new TableQueryParser("unnest(foo)").arrayFunctionSpecification();
		assertEquals("UNNEST(foo)", element.toSql());
		assertEquals(FunctionReturnType.MATCHES_PARAMETER, element.getFunctionReturnType());
	}
}
