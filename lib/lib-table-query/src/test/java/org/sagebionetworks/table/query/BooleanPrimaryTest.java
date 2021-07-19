package org.sagebionetworks.table.query;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.BooleanPrimary;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.SearchCondition;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class BooleanPrimaryTest {

	@Test
	public void testBooleanPrimaryToSQLPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo in(1,2)");
		BooleanPrimary element = new BooleanPrimary(predicate);
		assertEquals("foo IN ( 1, 2 )", element.toString());
	}
	
	@Test
	public void testBooleanPrimaryToSQLSearchCondition() throws ParseException{
		SearchCondition searchCondition = SqlElementUntils.createSearchCondition("bar = 1");
		BooleanPrimary element = new BooleanPrimary(searchCondition);
		assertEquals("( bar = 1 )", element.toString());
	}
	
	@Test
	public void testGetChidrenWithPredicate() throws ParseException {
		BooleanPrimary element = new TableQueryParser(" foo = 12").booleanPrimary();
		assertEquals(Arrays.asList(element.getPredicate()), element.getChildren());
	}
	
	@Test
	public void testGetChidrenWithSearchCondition() throws ParseException {
		BooleanPrimary element = new TableQueryParser("( bar = 1)").booleanPrimary();
		assertEquals(Arrays.asList(element.getSearchCondition()), element.getChildren());
	}
}
