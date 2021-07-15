package org.sagebionetworks.table.query;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ArrayHasLikePredicate;
import org.sagebionetworks.table.query.model.ArrayHasPredicate;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.EscapeCharacter;
import org.sagebionetworks.table.query.model.InPredicateValue;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class ArrayHasLikePredicateTest {

	@Test
	public void testArrayHasLikePredicateToSQL() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("bar");
		Boolean not = null;
		InPredicateValue ArrayHasPredicateValue =  SqlElementUntils.createInPredicateValue("(1)");
		ArrayHasPredicate element = new ArrayHasLikePredicate(columnReferenceLHS, not, ArrayHasPredicateValue, null);
		assertEquals("bar HAS_LIKE ( 1 )", element.toString());
	}
	
	@Test
	public void testArrayHasLikePredicateToSQLWithEscape() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("bar");
		Boolean not = null;
		InPredicateValue ArrayHasPredicateValue = SqlElementUntils.createInPredicateValue("(1)");
		EscapeCharacter escape = SqlElementUntils.createEscapeCharacter("'_'");
		ArrayHasPredicate element = new ArrayHasLikePredicate(columnReferenceLHS, not, ArrayHasPredicateValue, escape);
		assertEquals("bar HAS_LIKE ( 1 ) ESCAPE '_'", element.toString());
	}
	
	@Test
	public void testHasPredicate() throws ParseException{
		Predicate predicate = new TableQueryParser("foo has_like (1,'2',3)").predicate();
		ArrayHasLikePredicate element = predicate.getFirstElementOfType(ArrayHasLikePredicate.class);
		assertEquals("foo", element.getLeftHandSide().toSql());
		assertNull(element.getEscapeCharacter());
		
		List<String> expectedValues = Arrays.asList(
				"1", "2", "3"
		);
				
		List<String> values = StreamSupport.stream(element.getRightHandSideValues().spliterator(), false)
				.map( l -> l.toSqlWithoutQuotes())
				.collect(Collectors.toList());
		
		assertEquals(expectedValues, values);
	}
	
	@Test
	public void testHasPredicateWithEscape() throws ParseException{
		Predicate predicate = new TableQueryParser("foo has_like (1,'2',3) escape '_'").predicate();
		ArrayHasLikePredicate element = predicate.getFirstElementOfType(ArrayHasLikePredicate.class);
		assertEquals("foo", element.getLeftHandSide().toSql());
		assertNotNull(element.getEscapeCharacter());
		assertEquals("'_'", element.getEscapeCharacter().toSql());
		
		List<String> expectedValues = Arrays.asList(
				"1", "2", "3", "_"
		);
				
		List<String> values = StreamSupport.stream(element.getRightHandSideValues().spliterator(), false)
				.map( l -> l.toSqlWithoutQuotes())
				.collect(Collectors.toList());
		
		assertEquals(expectedValues, values);
	}
	
	@Test
	public void testGetChidren() throws ParseException {
		Predicate predicate = new TableQueryParser("foo has_like (1,'2',3) escape '_'").predicate();
		ArrayHasLikePredicate element = predicate.getFirstElementOfType(ArrayHasLikePredicate.class);
		assertEquals(
				Arrays.asList(element.getLeftHandSide(), element.getInPredicateValue(), element.getEscapeCharacter()),
				element.getChildren());
	}
}
