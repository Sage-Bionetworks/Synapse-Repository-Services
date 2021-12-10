package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ArrayHasPredicate;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.InPredicateValue;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.ReplaceableBox;
import org.sagebionetworks.table.query.util.SqlElementUtils;

public class ArrayHasPredicateTest {

	@Test
	public void testArrayHasPredicateToSQL() throws ParseException {
		ColumnReference columnReferenceLHS = SqlElementUtils.createColumnReference("bar");
		Boolean not = null;
		InPredicateValue ArrayHasPredicateValue = SqlElementUtils.createInPredicateValue("(1)");
		ArrayHasPredicate element = new ArrayHasPredicate(columnReferenceLHS, not, ArrayHasPredicateValue);
		assertEquals("bar HAS ( 1 )", element.toString());
	}

	@Test
	public void testArrayHasPredicateToSQLNot() throws ParseException {
		ColumnReference columnReferenceLHS = SqlElementUtils.createColumnReference("bar");
		Boolean not = Boolean.TRUE;
		InPredicateValue ArrayHasPredicateValue = SqlElementUtils.createInPredicateValue("(1, 2)");
		ArrayHasPredicate element = new ArrayHasPredicate(columnReferenceLHS, not, ArrayHasPredicateValue);
		assertEquals("bar NOT HAS ( 1, 2 )", element.toString());
	}

	@Test
	public void testHasPredicate() throws ParseException {
		Predicate predicate = new TableQueryParser("foo has (1,'2',3)").predicate();
		ArrayHasPredicate element = predicate.getFirstElementOfType(ArrayHasPredicate.class);
		assertEquals("foo", element.getLeftHandSide().toSql());

		List<String> expectedValues = Arrays.asList("1", "2", "3");

		List<String> values = StreamSupport.stream(element.getRightHandSideValues().spliterator(), false)
				.map(l -> l.toSqlWithoutQuotes()).collect(Collectors.toList());

		assertEquals(expectedValues, values);
	}

	@Test
	public void testGetChidren() throws ParseException {
		Predicate predicate = new TableQueryParser("foo has (1,'2',3)").predicate();
		ArrayHasPredicate element = predicate.getFirstElementOfType(ArrayHasPredicate.class);
		List<Element> children = element.getChildrenStream().collect(Collectors.toList());
		assertEquals(Arrays.asList(new ReplaceableBox<ColumnReference>(element.getLeftHandSide()),
				element.getInPredicateValue()), children);
	}

}
