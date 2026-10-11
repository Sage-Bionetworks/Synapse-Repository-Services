package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.InPredicate;
import org.sagebionetworks.table.query.model.InPredicateValue;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.PredicateLeftHandSide;
import org.sagebionetworks.table.query.model.UnsignedLiteral;
import org.sagebionetworks.table.query.util.SqlElementUtils;

import com.google.common.collect.Lists;

public class InPredicateTest {

	@Test
	public void testInPredicateToSQL() throws ParseException {
		ColumnReference columnReferenceLHS = SqlElementUtils.createColumnReference("bar");
		Boolean not = null;
		InPredicateValue inPredicateValue = SqlElementUtils.createInPredicateValue("(1)");
		InPredicate element = new InPredicate(new PredicateLeftHandSide(columnReferenceLHS), not, inPredicateValue);
		assertEquals("bar IN ( 1 )", element.toString());
	}

	@Test
	public void testInPredicateToSQLNot() throws ParseException {
		ColumnReference columnReferenceLHS = SqlElementUtils.createColumnReference("bar");
		Boolean not = Boolean.TRUE;
		InPredicateValue inPredicateValue = SqlElementUtils.createInPredicateValue("(1, 2)");
		InPredicate element = new InPredicate(new PredicateLeftHandSide(columnReferenceLHS), not, inPredicateValue);
		assertEquals("bar NOT IN ( 1, 2 )", element.toString());
	}

	@Test
	public void testInPredicateToSQL_NotSetToFalse() throws ParseException {
		ColumnReference columnReferenceLHS = SqlElementUtils.createColumnReference("bar");
		Boolean not = Boolean.FALSE;
		InPredicateValue inPredicateValue = SqlElementUtils.createInPredicateValue("(1, 2)");
		InPredicate element = new InPredicate(new PredicateLeftHandSide(columnReferenceLHS), not, inPredicateValue);
		assertEquals("bar IN ( 1, 2 )", element.toString());
	}

	@Test
	public void testHasPredicate() throws ParseException {
		Predicate predicate = new TableQueryParser("foo in (1,'2',3)").predicate();
		InPredicate element = predicate.getFirstElementOfType(InPredicate.class);
		assertEquals("foo", element.getLeftHandSide().toSql());
		List<UnsignedLiteral> values = Lists.newArrayList(element.getRightHandSideValues());
		assertNotNull(values);
		assertEquals(3, values.size());
		assertEquals("2", values.get(0).toSqlWithoutQuotes());
		assertEquals("1", values.get(1).toSqlWithoutQuotes());
		assertEquals("3", values.get(2).toSqlWithoutQuotes());
	}

	@Test
	public void testGetChildren() throws ParseException {
		Predicate predicate = new TableQueryParser("foo in (1,'2',3)").predicate();
		InPredicate element = predicate.getFirstElementOfType(InPredicate.class);
		List<Element> children = element.getChildrenStream().collect(Collectors.toList());
		assertEquals(Arrays.asList(element.getLeftHandSide(), element.getInPredicateValue()), children);
	}

	@Test
	public void testInPredicateWithCohort() throws ParseException {
		// call under test
		Predicate predicate = new TableQueryParser("participant_id in cohort(cohort_1)").predicate();
		InPredicate element = predicate.getFirstElementOfType(InPredicate.class);
		assertEquals("participant_id IN COHORT(cohort_1)", element.toSql());
		assertEquals("cohort_1", element.getInPredicateValue().getCohortReference().get().getName());
		assertEquals(Collections.emptyList(), Lists.newArrayList(element.getRightHandSideValues()));
	}

	@Test
	public void testInPredicateWithCohortNot() throws ParseException {
		// call under test
		Predicate predicate = new TableQueryParser("participant_id NOT IN COHORT(c1)").predicate();
		assertEquals("participant_id NOT IN COHORT(c1)", predicate.toSql());
	}

	@Test
	public void testInPredicateWithCohortRoundTrip() throws ParseException {
		String sql = new TableQueryParser("select * from syn123 where a IN COHORT(c1) and b in (1,2)").queryExpression().toSql();
		// call under test
		String reparsed = new TableQueryParser(sql).queryExpression().toSql();
		assertEquals("SELECT * FROM syn123 WHERE a IN COHORT(c1) AND b IN ( 1, 2 )", reparsed);
		assertEquals(sql, reparsed);
	}

	@Test
	public void testInPredicateWithValueList() throws ParseException {
		Predicate predicate = new TableQueryParser("foo in (1)").predicate();
		// call under test
		assertEquals(Optional.empty(), predicate.getFirstElementOfType(InPredicate.class).getInPredicateValue().getCohortReference());
	}

	@Test
	public void testInPredicateWithCohortMissingName() {
		assertThrows(ParseException.class, () -> {
			// call under test
			new TableQueryParser("foo in cohort()").predicate();
		});
	}

	@Test
	public void testInPredicateWithCohortQuotedName() {
		assertThrows(ParseException.class, () -> {
			// call under test
			new TableQueryParser("foo in cohort('c1')").predicate();
		});
	}

	@Test
	public void testInPredicateWithBareIdentifier() {
		assertThrows(ParseException.class, () -> {
			// call under test
			new TableQueryParser("foo in c1").predicate();
		});
	}

	@Test
	public void testHasPredicateWithCohort() {
		assertThrows(ParseException.class, () -> {
			// call under test
			new TableQueryParser("foo has cohort(c1)").predicate();
		});
	}

	@Test
	public void testHasLikePredicateWithCohort() {
		assertThrows(ParseException.class, () -> {
			// call under test
			new TableQueryParser("foo has_like cohort(c1)").predicate();
		});
	}

	@Test
	public void testQuotedColumnNamedCohort() throws ParseException {
		// call under test
		Predicate predicate = new TableQueryParser("\"cohort\" in cohort(c1)").predicate();
		assertEquals("\"cohort\" IN COHORT(c1)", predicate.toSql());
	}
}
