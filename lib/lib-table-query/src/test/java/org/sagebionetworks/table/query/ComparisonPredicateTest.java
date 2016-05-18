package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.CompOp;
import org.sagebionetworks.table.query.model.ComparisonPredicate;
import org.sagebionetworks.table.query.model.HasQuoteValue;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.RowValueConstructor;
import org.sagebionetworks.table.query.util.SqlElementUntils;

import com.google.common.collect.Lists;

public class ComparisonPredicateTest {

	@Test
	public void testComparisonPredicateToSQL() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("foo");
		CompOp compOp = CompOp.NOT_EQUALS_OPERATOR;
		RowValueConstructor rowValueConstructorRHS = SqlElementUntils.createRowValueConstructor("1");
		ComparisonPredicate element = new ComparisonPredicate(columnReferenceLHS, compOp, rowValueConstructorRHS);
		assertEquals("foo <> 1", element.toString());
	}
	
	@Test
	public void testHasPredicate() throws ParseException{
		Predicate predicate = new TableQueryParser("foo > bar").predicate();
		ComparisonPredicate element = predicate.getComparisonPredicate();
		assertEquals("foo", element.getLeftHandSide().toSql());
		List<HasQuoteValue> values = Lists.newArrayList(element.getRightHandSideValues());
		assertNotNull(values);
		assertEquals(1, values.size());
		assertEquals("bar", values.get(0).getValueWithoutQuotes());
	}
}
