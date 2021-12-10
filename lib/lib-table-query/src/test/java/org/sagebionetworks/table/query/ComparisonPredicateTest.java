package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.CompOp;
import org.sagebionetworks.table.query.model.ComparisonPredicate;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.ReplaceableBox;
import org.sagebionetworks.table.query.model.RowValueConstructor;
import org.sagebionetworks.table.query.model.UnsignedLiteral;
import org.sagebionetworks.table.query.util.SqlElementUtils;

import com.google.common.collect.Lists;

public class ComparisonPredicateTest {

	@Test
	public void testComparisonPredicateToSQL() throws ParseException {
		ColumnReference columnReferenceLHS = SqlElementUtils.createColumnReference("foo");
		CompOp compOp = CompOp.NOT_EQUALS_OPERATOR;
		RowValueConstructor rowValueConstructorRHS = SqlElementUtils.createRowValueConstructor("1");
		ComparisonPredicate element = new ComparisonPredicate(columnReferenceLHS, compOp, rowValueConstructorRHS);
		assertEquals("foo <> 1", element.toString());
	}

	@Test
	public void testHasPredicate() throws ParseException {
		Predicate predicate = new TableQueryParser("foo > 12").predicate();
		ComparisonPredicate element = predicate.getFirstElementOfType(ComparisonPredicate.class);
		assertEquals("foo", element.getLeftHandSide().toSql());
		List<UnsignedLiteral> values = Lists.newArrayList(element.getRightHandSideValues());
		assertNotNull(values);
		assertEquals(1, values.size());
		assertEquals("12", values.get(0).toSqlWithoutQuotes());
	}

	@Test
	public void testGetChidren() throws ParseException {
		Predicate predicate = new TableQueryParser("foo > 12").predicate();
		ComparisonPredicate element = predicate.getFirstElementOfType(ComparisonPredicate.class);
		List<Element> children = element.getChildrenStream().collect(Collectors.toList());
		assertEquals(Arrays.asList(new ReplaceableBox<ColumnReference>(element.getLeftHandSide()),
				element.getRowValueConstructorRHS()), children);
	}
}
