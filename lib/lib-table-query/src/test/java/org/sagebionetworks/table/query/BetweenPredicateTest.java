package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.table.query.model.BetweenPredicate;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.RowValueConstructor;
import org.sagebionetworks.table.query.model.UnsignedLiteral;
import org.sagebionetworks.table.query.util.SqlElementUntils;

import com.google.common.collect.Lists;

public class BetweenPredicateTest {
	
	@Test
	public void testBetweenPredicateToSQL() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("bar");
		Boolean not = null;
		RowValueConstructor betweenRowValueConstructor = SqlElementUntils.createRowValueConstructor("1.0");
		RowValueConstructor andRowValueConstructorRHS = SqlElementUntils.createRowValueConstructor("2.0");
		BetweenPredicate element = new BetweenPredicate(columnReferenceLHS, not, betweenRowValueConstructor, andRowValueConstructorRHS);
		assertEquals("bar BETWEEN 1.0 AND 2.0", element.toString());
	}
	
	@Test
	public void testBetweenPredicateToSQLNot() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("bar");
		Boolean not = Boolean.TRUE;
		RowValueConstructor betweenRowValueConstructor = SqlElementUntils.createRowValueConstructor("1.0");
		RowValueConstructor andRowValueConstructorRHS = SqlElementUntils.createRowValueConstructor("2.0");
		BetweenPredicate element = new BetweenPredicate(columnReferenceLHS, not, betweenRowValueConstructor, andRowValueConstructorRHS);
		assertEquals("bar NOT BETWEEN 1.0 AND 2.0", element.toString());
	}
	
	@Test
	public void testHasPredicate() throws ParseException{
		Predicate predicate = new TableQueryParser("foo between 1.2 and 2.2").predicate();
		BetweenPredicate element = predicate.getFirstElementOfType(BetweenPredicate.class);
		assertEquals("foo", element.getLeftHandSide().toSql());
		List<UnsignedLiteral> values = Lists.newArrayList(element.getRightHandSideValues());
		assertNotNull(values);
		assertEquals(2, values.size());
		assertEquals("1.2", values.get(0).toSqlWithoutQuotes());
		assertEquals("2.2", values.get(1).toSqlWithoutQuotes());
	}

}
