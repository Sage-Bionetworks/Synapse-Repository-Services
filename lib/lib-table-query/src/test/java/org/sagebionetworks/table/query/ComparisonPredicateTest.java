package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.CompOp;
import org.sagebionetworks.table.query.model.ComparisonPredicate;
import org.sagebionetworks.table.query.model.RowValueConstructor;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class ComparisonPredicateTest {

	@Test
	public void testComparisonPredicateToSQL() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("foo");
		CompOp compOp = CompOp.NOT_EQUALS_OPERATOR;
		RowValueConstructor rowValueConstructorRHS = SqlElementUntils.createRowValueConstructor("1");
		ComparisonPredicate element = new ComparisonPredicate(columnReferenceLHS, compOp, rowValueConstructorRHS);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo <> 1", builder.toString());
	}
}
