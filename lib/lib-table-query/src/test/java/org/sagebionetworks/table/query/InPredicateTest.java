package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.InPredicate;
import org.sagebionetworks.table.query.model.InPredicateValue;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class InPredicateTest {

	@Test
	public void testInPredicateToSQL() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("bar");
		Boolean not = null;
		InPredicateValue inPredicateValue =  SqlElementUntils.createInPredicateValue("(1)");
		InPredicate element = new InPredicate(columnReferenceLHS, not, inPredicateValue);
		assertEquals("bar IN ( 1 )", element.toString());
	}
	
	@Test
	public void testInPredicateToSQLNot() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("bar");
		Boolean not = Boolean.TRUE;
		InPredicateValue inPredicateValue =  SqlElementUntils.createInPredicateValue("(1, 2)");
		InPredicate element = new InPredicate(columnReferenceLHS, not, inPredicateValue);
		assertEquals("bar NOT IN ( 1, 2 )", element.toString());
	}
}
