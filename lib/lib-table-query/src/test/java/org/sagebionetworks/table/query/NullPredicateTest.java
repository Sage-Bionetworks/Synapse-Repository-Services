package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.BooleanPredicate;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.NullPredicate;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class NullPredicateTest {

	@Test
	public void testNullPredicateToSQL() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("foo");
		NullPredicate element = new NullPredicate(columnReferenceLHS, null);
		assertEquals("foo IS NULL", element.toString());
	}
	
	@Test
	public void testNullPredicateToSQLNot() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("foo");
		NullPredicate element = new NullPredicate(columnReferenceLHS, Boolean.TRUE);
		assertEquals("foo IS NOT NULL", element.toString());
	}
	
	@Test
	public void testHasPredicate() throws ParseException{
		Predicate predicate = new TableQueryParser("foo is null").predicate();
		NullPredicate element = predicate.getFirstElementOfType(NullPredicate.class);
		assertEquals("foo", element.getLeftHandSide().toSql());
		assertEquals(null, element.getRightHandSideValues());
	}
	
	@Test
	public void testHasPredicateBoolean() throws ParseException{
		Predicate predicate = new TableQueryParser("foo is true").predicate();
		BooleanPredicate element = predicate.getFirstElementOfType(BooleanPredicate.class);
		assertEquals("foo", element.getLeftHandSide().toSql());
		assertEquals(null, element.getRightHandSideValues());
	}
	
}
