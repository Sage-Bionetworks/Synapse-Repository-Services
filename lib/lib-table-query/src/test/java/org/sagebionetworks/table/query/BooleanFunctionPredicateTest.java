package org.sagebionetworks.table.query;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.BooleanFunctionPredicate;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.ReplaceableBox;

public class BooleanFunctionPredicateTest {

	@Test
	public void testGetChildren() throws ParseException {
		BooleanFunctionPredicate element = new TableQueryParser("isInfinity(col5)").booleanFunctionPredicate();
		List<Element> children = element.getChildrenStream().collect(Collectors.toList());
		assertEquals(Arrays.asList(new ReplaceableBox<ColumnReference>(element.getLeftHandSide())), children);
	}

}
