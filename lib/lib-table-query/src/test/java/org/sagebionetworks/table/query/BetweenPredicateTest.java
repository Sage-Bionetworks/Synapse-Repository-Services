package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.BetweenPredicate;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.RowValueConstructor;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class BetweenPredicateTest {
	
	@Test
	public void testBetweenPredicateToSQL() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("bar");
		Boolean not = null;
		RowValueConstructor betweenRowValueConstructor = SqlElementUntils.createRowValueConstructor("1.0");
		RowValueConstructor andRowValueConstructorRHS = SqlElementUntils.createRowValueConstructor("2.0");
		BetweenPredicate element = new BetweenPredicate(columnReferenceLHS, not, betweenRowValueConstructor, andRowValueConstructorRHS);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("bar BETWEEN 1.0 AND 2.0", builder.toString());
	}
	
	@Test
	public void testBetweenPredicateToSQLNot() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("bar");
		Boolean not = Boolean.TRUE;
		RowValueConstructor betweenRowValueConstructor = SqlElementUntils.createRowValueConstructor("1.0");
		RowValueConstructor andRowValueConstructorRHS = SqlElementUntils.createRowValueConstructor("2.0");
		BetweenPredicate element = new BetweenPredicate(columnReferenceLHS, not, betweenRowValueConstructor, andRowValueConstructorRHS);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("bar NOT BETWEEN 1.0 AND 2.0", builder.toString());
	}

}
