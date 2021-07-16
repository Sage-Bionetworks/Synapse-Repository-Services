package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.BooleanTerm;
import org.sagebionetworks.table.query.model.SearchCondition;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class SearchConditionTest {

	@Test
	public void testSearchConditionToSQLSingle() throws ParseException {
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
	public void testSearchConditionToSQLSingleIsTrue() throws ParseException {
		SearchCondition element = new TableQueryParser("foo is True").searchCondition();
		assertEquals("foo IS TRUE", element.toString());
	}

	@Test
	public void testSearchConditionToSQLSingleTrue() throws ParseException {
		SearchCondition element = new TableQueryParser("foo = True").searchCondition();
		assertEquals("foo = TRUE", element.toString());
	}

	@Test
	public void testSearchConditionToSQLMultiple() throws ParseException {
		List<BooleanTerm> terms = SqlElementUntils.createBooleanTerms("foo=1", "bar=2");
		SearchCondition element = new SearchCondition(terms);
		assertEquals("foo = 1 OR bar = 2", element.toString());
	}

	@Test
	public void testGetChildren() throws ParseException {
		SearchCondition element = new TableQueryParser("foo is True or bar > 1").searchCondition();
		assertEquals(new LinkedList<>(element.getOrBooleanTerms()), element.getChildren());
	}
}
