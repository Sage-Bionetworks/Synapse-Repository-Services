package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.table.query.model.BooleanTerm;
import org.sagebionetworks.table.query.model.SearchCondition;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class SearchConditionTest {
	
	@Test
	public void testSearchConditionToSQLSingle() throws ParseException{
		List<BooleanTerm> terms = SqlElementUntils.createBooleanTerms("foo=1");
		SearchCondition element = new SearchCondition(terms);
		assertEquals("foo = 1", element.toString());
	}
	
	@Test
	public void testSearchConditionToSQLNegative() throws ParseException {
		List<BooleanTerm> terms = SqlElementUntils.createBooleanTerms("foo=-1");
		SearchCondition element = new SearchCondition(terms);
		assertEquals("foo = -1", element.toString());
	}

	@Test
	public void testSearchConditionToSQLSingleTrue() throws ParseException {
		List<BooleanTerm> terms = SqlElementUntils.createBooleanTerms("foo= True");
		SearchCondition element = new SearchCondition(terms);
		assertEquals("foo = TRUE", element.toString());
	}

	@Test
	public void testSearchConditionToSQLMultiple() throws ParseException{
		List<BooleanTerm> terms = SqlElementUntils.createBooleanTerms("foo=1", "bar=2");
		SearchCondition element = new SearchCondition(terms);
		assertEquals("foo = 1 OR bar = 2", element.toString());
	}

}
