package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ArrayHasLikeSpec;
import org.sagebionetworks.table.query.model.ArrayHasPredicate;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.EscapeCharacter;
import org.sagebionetworks.table.query.model.InPredicateValue;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class ArrayHasPredicateTest {
	

	@Test
	public void testArrayHasPredicateToSQL() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("bar");
		Boolean not = null;
		InPredicateValue ArrayHasPredicateValue =  SqlElementUntils.createInPredicateValue("(1)");
		ArrayHasPredicate element = new ArrayHasPredicate(columnReferenceLHS, not, ArrayHasPredicateValue);
		assertEquals("bar HAS ( 1 )", element.toString());
	}
	
	@Test
	public void testArrayHasPredicateToSQLWithLike() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("bar");
		Boolean not = null;
		InPredicateValue ArrayHasPredicateValue =  SqlElementUntils.createInPredicateValue("(1)");
		ArrayHasPredicate element = new ArrayHasPredicate(columnReferenceLHS, not, ArrayHasPredicateValue, new ArrayHasLikeSpec());
		assertEquals("bar HAS_LIKE ( 1 )", element.toString());
	}
	
	@Test
	public void testArrayHasPredicateToSQLWithLikeAndEscape() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("bar");
		Boolean not = null;
		InPredicateValue ArrayHasPredicateValue = SqlElementUntils.createInPredicateValue("(1)");
		EscapeCharacter escape = SqlElementUntils.createEscapeCharacter("'_'");
		ArrayHasPredicate element = new ArrayHasPredicate(columnReferenceLHS, not, ArrayHasPredicateValue, new ArrayHasLikeSpec(escape));
		assertEquals("bar HAS_LIKE ( 1 ) ESCAPE '_'", element.toString());
	}

	@Test
	public void testArrayHasPredicateToSQLNot() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("bar");
		Boolean not = Boolean.TRUE;
		InPredicateValue ArrayHasPredicateValue =  SqlElementUntils.createInPredicateValue("(1, 2)");
		ArrayHasPredicate element = new ArrayHasPredicate(columnReferenceLHS, not, ArrayHasPredicateValue);
		assertEquals("bar NOT HAS ( 1, 2 )", element.toString());
	}

	@Test
	public void testHasPredicate() throws ParseException{
		Predicate predicate = new TableQueryParser("foo has (1,'2',3)").predicate();
		ArrayHasPredicate element = predicate.getFirstElementOfType(ArrayHasPredicate.class);
		assertEquals("foo", element.getLeftHandSide().toSql());

		List<String> expectedValues = Arrays.asList(
				"1", "2", "3"
		);
				
		List<String> values = StreamSupport.stream(element.getRightHandSideValues().spliterator(), false)
				.map( l -> l.toSqlWithoutQuotes())
				.collect(Collectors.toList());
		
		assertEquals(expectedValues, values);
	}
	
	@Test
	public void testHasPredicateWithLike() throws ParseException{
		Predicate predicate = new TableQueryParser("foo has_like (1,'2',3)").predicate();
		ArrayHasPredicate element = predicate.getFirstElementOfType(ArrayHasPredicate.class);
		assertEquals("foo", element.getLeftHandSide().toSql());
		assertNotNull(element.getHasLikeSpec());
		assertNull(element.getHasLikeSpec().getEscapeCharacter());
		
		List<String> expectedValues = Arrays.asList(
				"1", "2", "3"
		);
				
		List<String> values = StreamSupport.stream(element.getRightHandSideValues().spliterator(), false)
				.map( l -> l.toSqlWithoutQuotes())
				.collect(Collectors.toList());
		
		assertEquals(expectedValues, values);
	}
	
	@Test
	public void testHasPredicateWithLikeAndEscape() throws ParseException{
		Predicate predicate = new TableQueryParser("foo has_like (1,'2',3) escape '_'").predicate();
		ArrayHasPredicate element = predicate.getFirstElementOfType(ArrayHasPredicate.class);
		assertEquals("foo", element.getLeftHandSide().toSql());
		assertNotNull(element.getHasLikeSpec());
		assertEquals("'_'", element.getHasLikeSpec().getEscapeCharacter().toSql());
		
		List<String> expectedValues = Arrays.asList(
				"1", "2", "3", "_"
		);
				
		List<String> values = StreamSupport.stream(element.getRightHandSideValues().spliterator(), false)
				.map( l -> l.toSqlWithoutQuotes())
				.collect(Collectors.toList());
		
		assertEquals(expectedValues, values);
	}
}
