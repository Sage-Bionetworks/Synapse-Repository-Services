package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.BetweenPredicate;
import org.sagebionetworks.table.query.model.BooleanFunctionPredicate;
import org.sagebionetworks.table.query.model.BooleanPrimary;
import org.sagebionetworks.table.query.model.ComparisonPredicate;
import org.sagebionetworks.table.query.model.InPredicate;
import org.sagebionetworks.table.query.model.LikePredicate;
import org.sagebionetworks.table.query.model.NullPredicate;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class PredicateTest {
	
	@Test
	public void testPredicateToSQLComparisonPredicate() throws ParseException{
		ComparisonPredicate comparisonPredicate = SqlElementUntils.createComparisonPredicate("foo >= 123.4");
		Predicate element = new Predicate(comparisonPredicate);
		assertEquals("foo >= 123.4", element.toString());
	}
	
	@Test
	public void testPredicateToSQLBetweenPredicate() throws ParseException{
		BetweenPredicate betweenPredicate = SqlElementUntils.createBetweenPredicate("bar between 0.0 and 1.0");
		Predicate element = new Predicate(betweenPredicate);
		assertEquals("bar BETWEEN 0.0 AND 1.0", element.toString());
	}
	
	@Test
	public void testPredicateToSQLInPredicate() throws ParseException{
		InPredicate inPredicate = SqlElementUntils.createInPredicate("bar in (2,3,5)");
		Predicate element = new Predicate(inPredicate);
		assertEquals("bar IN ( 2, 3, 5 )", element.toString());
	}
	
	@Test
	public void testPredicateToSQLLikePredicate() throws ParseException{
		LikePredicate likePredicate = SqlElementUntils.createLikePredicate("bar like '%suffix'");
		Predicate element = new Predicate(likePredicate);
		assertEquals("bar LIKE '%suffix'", element.toString());
	}
	
	@Test
	public void testPredicateToSQLisInfinityBooleanFunction() throws ParseException {
		BooleanFunctionPredicate booleanFunctionPredicate = new TableQueryParser("isInfinity(col5)").predicate()
				.getFirstElementOfType(BooleanFunctionPredicate.class);
		Predicate element = new Predicate(booleanFunctionPredicate);
		assertEquals("ISINFINITY(col5)", element.toString());
	}

	@Test
	public void testPredicateToSQLIsNanBooleanFunction() throws ParseException {
		BooleanFunctionPredicate booleanFunctionPredicate = new TableQueryParser("isNaN(col5)").predicate().getFirstElementOfType(BooleanFunctionPredicate.class);
		Predicate element = new Predicate(booleanFunctionPredicate);
		assertEquals("ISNAN(col5)", element.toString());
	}

	@Test
	public void testPredicateToSQLNullPredicate() throws ParseException{
		NullPredicate nullPredicate = SqlElementUntils.createNullPredicate("foo is null");
		Predicate element = new Predicate(nullPredicate);
		assertEquals("foo IS NULL", element.toString());
	}

	@Test
	public void testPredicateToSQLNotNullPredicate() throws ParseException {
		NullPredicate nullPredicate = SqlElementUntils.createNullPredicate("foo is not null");
		Predicate element = new Predicate(nullPredicate);
		assertEquals("foo IS NOT NULL", element.toString());
	}
	
	@Test
	public void testBooleanTestSQLPrimaryIsBooleanTrue() throws ParseException {
		BooleanPrimary booleanPrimary = SqlElementUntils.createBooleanPrimary("foo is true");
		assertEquals("foo IS TRUE", booleanPrimary.toString());
	}

	@Test
	public void testBooleanTestSQLPrimaryIsBooleanFalse() throws ParseException {
		BooleanPrimary booleanPrimary = SqlElementUntils.createBooleanPrimary("foo is fAlse");
		assertEquals("foo IS FALSE", booleanPrimary.toString());
	}

	@Test
	public void testBooleanTestSQLPrimaryIsBooleanNotTrue() throws ParseException {
		BooleanPrimary booleanPrimary = SqlElementUntils.createBooleanPrimary("foo is not true");
		assertEquals("foo IS NOT TRUE", booleanPrimary.toString());
	}

	@Test
	public void testBooleanTestSQLPrimaryIsBooleanNotFalse() throws ParseException {
		BooleanPrimary booleanPrimary = SqlElementUntils.createBooleanPrimary("foo is not false");
		assertEquals("foo IS NOT FALSE", booleanPrimary.toString());
	}

	@Test
	public void testBooleanTestSQLPrimaryIsBooleanEqualsTrue() throws ParseException {
		BooleanPrimary booleanPrimary = SqlElementUntils.createBooleanPrimary("foo = true");
		assertEquals("foo = TRUE", booleanPrimary.toString());
	}

	@Test
	public void testBooleanTestSQLPrimaryIsBooleanEqualsFalse() throws ParseException {
		BooleanPrimary booleanPrimary = SqlElementUntils.createBooleanPrimary("foo <> fAlse");
		assertEquals("foo <> FALSE", booleanPrimary.toString());
	}
}
