package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.BetweenPredicate;
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
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder);
		assertEquals("foo >= 123.4", builder.toString());
	}
	
	@Test
	public void testPredicateToSQLBetweenPredicate() throws ParseException{
		BetweenPredicate betweenPredicate = SqlElementUntils.createBetweenPredicate("bar between 0.0 and 1.0");
		Predicate element = new Predicate(betweenPredicate);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder);
		assertEquals("bar BETWEEN 0.0 AND 1.0", builder.toString());
	}
	
	@Test
	public void testPredicateToSQLInPredicate() throws ParseException{
		InPredicate inPredicate = SqlElementUntils.createInPredicate("bar in (2,3,5)");
		Predicate element = new Predicate(inPredicate);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder);
		assertEquals("bar IN ( 2, 3, 5 )", builder.toString());
	}
	
	@Test
	public void testPredicateToSQLLikePredicate() throws ParseException{
		LikePredicate likePredicate = SqlElementUntils.createLikePredicate("bar like '%suffix'");
		Predicate element = new Predicate(likePredicate);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder);
		assertEquals("bar LIKE '%suffix'", builder.toString());
	}
	
	@Test
	public void testPredicateToSQLNullPredicate() throws ParseException{
		NullPredicate nullPredicate = SqlElementUntils.createNullPredicate("foo is not null");
		Predicate element = new Predicate(nullPredicate);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder);
		assertEquals("foo IS NOT NULL", builder.toString());
	}
	
}
