package org.sagebionetworks.table.query;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.BooleanFunctionPredicate;

public class BooleanFunctionPredicateTest {

	@Test
	public void testGetChildren() throws ParseException {
		BooleanFunctionPredicate element = new TableQueryParser("isInfinity(col5)").booleanFunctionPredicate();
		assertEquals(Collections.singleton(element.getColumnReference()), element.getChildren());
	}

}
