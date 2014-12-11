package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.NullPredicate;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class NullPredicateTest {

	@Test
	public void testNullPredicateToSQL() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("foo");
		NullPredicate element = new NullPredicate(columnReferenceLHS, null);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo IS NULL", builder.toString());
	}
	
	@Test
	public void testNullPredicateToSQLNot() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("foo");
		NullPredicate element = new NullPredicate(columnReferenceLHS, Boolean.TRUE);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo IS NOT NULL", builder.toString());
	}
}
