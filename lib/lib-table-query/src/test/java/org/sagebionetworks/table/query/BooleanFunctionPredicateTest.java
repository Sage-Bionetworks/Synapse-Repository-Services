package org.sagebionetworks.table.query;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.BooleanFunctionPredicate;
import org.sagebionetworks.table.query.model.Element;

public class BooleanFunctionPredicateTest {

	@Test
	public void testGetChildren() throws ParseException {
		BooleanFunctionPredicate element = new TableQueryParser("isInfinity(col5)").booleanFunctionPredicate();
		List<String> children = element.getChildrenStream().map(Element::toSql).collect(Collectors.toList());
		assertEquals(Arrays.asList(element.getColumnReference().toSql()), children);
	}

}
