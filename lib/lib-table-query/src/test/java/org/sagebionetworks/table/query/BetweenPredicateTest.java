package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.BetweenPredicate;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.PredicateLeftHandSide;
import org.sagebionetworks.table.query.model.RowValueConstructor;
import org.sagebionetworks.table.query.model.UnsignedLiteral;
import org.sagebionetworks.table.query.util.SqlElementUtils;

import com.google.common.collect.Lists;

public class BetweenPredicateTest {

	@Test
	public void testBetweenPredicateToSQL() throws ParseException {
		ColumnReference columnReferenceLHS = SqlElementUtils.createColumnReference("bar");
		Boolean not = null;
		RowValueConstructor betweenRowValueConstructor = SqlElementUtils.createRowValueConstructor("1.0");
		RowValueConstructor andRowValueConstructorRHS = SqlElementUtils.createRowValueConstructor("2.0");
		BetweenPredicate element = new BetweenPredicate(new PredicateLeftHandSide(columnReferenceLHS), not, betweenRowValueConstructor,
				andRowValueConstructorRHS);
		assertEquals("bar BETWEEN 1.0 AND 2.0", element.toString());
	}

	@Test
	public void testBetweenPredicateToSQLNot() throws ParseException {
		ColumnReference columnReferenceLHS = SqlElementUtils.createColumnReference("bar");
		Boolean not = Boolean.TRUE;
		RowValueConstructor betweenRowValueConstructor = SqlElementUtils.createRowValueConstructor("1.0");
		RowValueConstructor andRowValueConstructorRHS = SqlElementUtils.createRowValueConstructor("2.0");
		BetweenPredicate element = new BetweenPredicate(new PredicateLeftHandSide(columnReferenceLHS), not, betweenRowValueConstructor,
				andRowValueConstructorRHS);
		assertEquals("bar NOT BETWEEN 1.0 AND 2.0", element.toString());
	}

	@Test
	public void testHasPredicate() throws ParseException {
		Predicate predicate = new TableQueryParser("foo between 1.2 and 2.2").predicate();
		BetweenPredicate element = predicate.getFirstElementOfType(BetweenPredicate.class);
		assertEquals("foo", element.getLeftHandSide().toSql());
		List<UnsignedLiteral> values = Lists.newArrayList(element.getRightHandSideValues());
		assertNotNull(values);
		assertEquals(2, values.size());
		assertEquals("1.2", values.get(0).toSqlWithoutQuotes());
		assertEquals("2.2", values.get(1).toSqlWithoutQuotes());
	}

	@Test
	public void testGetChidren() throws ParseException {
		Predicate predicate = new TableQueryParser("foo between 1.2 and 2.2").predicate();
		BetweenPredicate element = predicate.getFirstElementOfType(BetweenPredicate.class);
		List<Element> children = element.getChildrenStream().collect(Collectors.toList());
		assertEquals(Arrays.asList(element.getLeftHandSide(), element.getBetweenRowValueConstructor(), element.getAndRowValueConstructorRHS()), children);
	}

}
