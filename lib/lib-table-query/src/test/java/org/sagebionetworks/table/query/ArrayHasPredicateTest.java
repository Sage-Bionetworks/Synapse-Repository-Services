package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ArrayHasPredicate;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.HasPredicate;
import org.sagebionetworks.table.query.model.ArrayHasPredicate;
import org.sagebionetworks.table.query.model.InPredicateValue;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.UnsignedLiteral;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class ArrayHasPredicateTest {
	

	@Test
	public void testArrayHasPredicateToSQL() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("bar");
		Boolean not = null;
		InPredicateValue ArrayHasPredicateValue =  SqlElementUntils.createInPredicateValue("(1)");
		ArrayHasPredicate element = new ArrayHasPredicate(columnReferenceLHS, not, ArrayHasPredicateValue);
		assertEquals("bar HAS ( 1 )", element.toString());
	}

	@Test
	public void testArrayHasPredicateToSQLNot() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("bar");
		Boolean not = Boolean.TRUE;
		InPredicateValue ArrayHasPredicateValue =  SqlElementUntils.createInPredicateValue("(1, 2)");
		ArrayHasPredicate element = new ArrayHasPredicate(columnReferenceLHS, not, ArrayHasPredicateValue);
		assertEquals("bar NOT HAS ( 1, 2 )", element.toString());
	}

	@Test
	public void testHasPredicate() throws ParseException{
		Predicate predicate = new TableQueryParser("foo has (1,'2',3)").predicate();
		ArrayHasPredicate element = predicate.getFirstElementOfType(ArrayHasPredicate.class);
		assertEquals("foo", element.getLeftHandSide().toSql());
		List<UnsignedLiteral> values = Lists.newArrayList(element.getRightHandSideValues());
		assertNotNull(values);
		assertEquals(3, values.size());
		assertEquals("1", values.get(0).toSqlWithoutQuotes());
		assertEquals("2", values.get(1).toSqlWithoutQuotes());
		assertEquals("3", values.get(2).toSqlWithoutQuotes());
	}
}
