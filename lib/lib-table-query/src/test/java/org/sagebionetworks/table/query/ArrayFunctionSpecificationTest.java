package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.FunctionReturnType;
import org.sagebionetworks.table.query.model.ArrayFunctionSpecification;
import org.sagebionetworks.table.query.model.QuerySpecification;

public class ArrayFunctionSpecificationTest {

	@Test
	public void testUnnest() throws ParseException{
		ArrayFunctionSpecification element =
				new TableQueryParser("unnest(foo)").arrayFunctionSpecification();
		assertEquals("UNNEST(foo)", element.toSql());
		assertEquals(FunctionReturnType.UNNEST_PARAMETER, element.getFunctionReturnType());
	}

	@Test
	public void testUnnestFullQueryWithGroupBy() throws ParseException{
		QuerySpecification element =
				new TableQueryParser("SELECT UNNEST(foo), count(*) from syn123 Group by UNNEST(foo)").querySpecification();
		assertEquals("SELECT UNNEST(foo), COUNT(*) FROM syn123 GROUP BY UNNEST(foo)", element.toSql());
	}
}
