/**
 * 
 */
package org.sagebionetworks.repo.queryparser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.Expression;
import org.sagebionetworks.repo.web.query.QueryStatement;

/**
 * @author deflaux
 * 
 */
public class QueryParserTest {

	private static final Logger log = Logger.getLogger(QueryParserTest.class
			.getName());

	/************************************************************************
	 * Happy case tests
	 */

	/**
	 * @throws Exception
	 */
	@Test
	public void testSelectStar() throws Exception {

		QueryStatement stmt = new QueryStatement("select * from dataset");
		assertEquals("dataset", stmt.getTableName());
		assertEquals(null, stmt.getSelect());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testWhereEqualsString() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from layer where type == \"C\"");
		assertEquals("layer", stmt.getTableName());
		assertNotNull(stmt.getSearchCondition());
		assertEquals(1, stmt.getSearchCondition().size());
		assertTrue(stmt.getSearchCondition().get(0) instanceof Expression);
		Expression expression = (Expression) stmt.getSearchCondition().get(0);
		assertEquals("type", expression.getId().getFieldName());
		assertEquals("C", expression.getValue());
	}
	
	@Test
	public void testWhereGreaterThanLong() throws Exception {
		QueryStatement stmt = new QueryStatement(
				"select * from datasets where datasets.Number_of_Samples > 101");
		assertEquals("datasets", stmt.getTableName());
		assertNotNull(stmt.getSearchCondition());
		assertEquals(1, stmt.getSearchCondition().size());
		assertTrue(stmt.getSearchCondition().get(0) instanceof Expression);
		Expression expression = (Expression) stmt.getSearchCondition().get(0);
		assertEquals("Number_of_Samples", expression.getId().getFieldName());
		assertTrue(expression.getValue() instanceof Long);
		assertEquals(101, ((Long)expression.getValue()).longValue());
		assertEquals(Comparator.GREATER_THAN, expression.getCompare());
	}
	
	@Test
	public void testWhereGreaterThanOrEqualsLong() throws Exception {
		QueryStatement stmt = new QueryStatement(
				"select * from datasets where datasets.Number_of_Samples >= 101");
		assertEquals("datasets", stmt.getTableName());
		assertNotNull(stmt.getSearchCondition());
		assertEquals(1, stmt.getSearchCondition().size());
		assertTrue(stmt.getSearchCondition().get(0) instanceof Expression);
		Expression expression = (Expression) stmt.getSearchCondition().get(0);
		assertEquals("Number_of_Samples", expression.getId().getFieldName());
		assertTrue(expression.getValue() instanceof Long);
		assertEquals(101, ((Long)expression.getValue()).longValue());
		assertEquals(Comparator.GREATER_THAN_OR_EQUALS, expression.getCompare());
	}
	
	@Test
	public void testLessThanLong() throws Exception {
		QueryStatement stmt = new QueryStatement(
				"select * from datasets where datasets.Number_of_Samples < 101");
		assertEquals("datasets", stmt.getTableName());
		assertNotNull(stmt.getSearchCondition());
		assertEquals(1, stmt.getSearchCondition().size());
		assertTrue(stmt.getSearchCondition().get(0) instanceof Expression);
		Expression expression = (Expression) stmt.getSearchCondition().get(0);
		assertEquals("Number_of_Samples", expression.getId().getFieldName());
		assertTrue(expression.getValue() instanceof Long);
		assertEquals(101, ((Long)expression.getValue()).longValue());
		assertEquals(Comparator.LESS_THAN, expression.getCompare());
	}
	
	@Test
	public void testLessThanOrEqualsLong() throws Exception {
		QueryStatement stmt = new QueryStatement(
				"select * from datasets where datasets.Number_of_Samples <= 101");
		assertEquals("datasets", stmt.getTableName());
		assertNotNull(stmt.getSearchCondition());
		assertEquals(1, stmt.getSearchCondition().size());
		assertTrue(stmt.getSearchCondition().get(0) instanceof Expression);
		Expression expression = (Expression) stmt.getSearchCondition().get(0);
		assertEquals("Number_of_Samples", expression.getId().getFieldName());
		assertTrue(expression.getValue() instanceof Long);
		assertEquals(101, ((Long)expression.getValue()).longValue());
		assertEquals(Comparator.LESS_THAN_OR_EQUALS, expression.getCompare());
	}
	
	@Test
	public void testNotEqualsString() throws Exception {
		QueryStatement stmt = new QueryStatement(
				"select * from datasets where datasets.SomeString != \"Value String\"");
		assertEquals("datasets", stmt.getTableName());
		assertNotNull(stmt.getSearchCondition());
		assertEquals(1, stmt.getSearchCondition().size());
		assertTrue(stmt.getSearchCondition().get(0) instanceof Expression);
		Expression expression = (Expression) stmt.getSearchCondition().get(0);
		assertEquals("SomeString", expression.getId().getFieldName());
		assertTrue(expression.getValue() instanceof String);
		assertEquals("Value String", expression.getValue());
		assertEquals(Comparator.NOT_EQUALS, expression.getCompare());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testWhereEqualsNumber() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from dataset where \"Number of Samples\" == 100");
		assertEquals("dataset", stmt.getTableName());
		assertNotNull(stmt.getSearchCondition());
		assertEquals(1, stmt.getSearchCondition().size());
		assertTrue(stmt.getSearchCondition().get(0) instanceof Expression);
		Expression expression = (Expression) stmt.getSearchCondition().get(0);
		assertEquals("Number of Samples", expression.getId().getFieldName());
		assertEquals(new Long(100), expression.getValue());
	}

	@Test
	public void testMultipleWhere() throws Exception {
		QueryStatement stmt = new QueryStatement(
				"select * from dataset where dataset.Species == \"Human\" and dataset.Disease == \"Cancer\"");
		assertEquals("dataset", stmt.getTableName());
		assertNotNull(stmt.getSearchCondition());
		assertEquals(2, stmt.getSearchCondition().size());
		// The results should be in postfix form
		// Next should be an expression.
		Expression expression = stmt.getSearchCondition().get(1);
		assertEquals("dataset", expression.getId().getTableName());
		assertEquals("Disease", expression.getId().getFieldName());
		assertEquals(Comparator.EQUALS, expression.getCompare());
		assertEquals("Cancer", expression.getValue());
		
		// Next should be an expression.
		expression = stmt.getSearchCondition().get(0);
		assertEquals("dataset", expression.getId().getTableName());
		assertEquals("Species", expression.getId().getFieldName());
		assertEquals(Comparator.EQUALS, expression.getCompare());
		assertEquals("Human", expression.getValue());
		
	}
	/**
	 * @throws Exception
	 */
	@Test
	public void testCompoundIdWithSpaces() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from dataset where dataset.\"Number of Samples\" == 100");
		assertEquals("dataset", stmt.getTableName());
		assertNotNull(stmt.getSearchCondition());
		assertEquals(1, stmt.getSearchCondition().size());
		assertTrue(stmt.getSearchCondition().get(0) instanceof Expression);
		Expression expression = (Expression) stmt.getSearchCondition().get(0);
		assertEquals("dataset", expression.getId().getTableName());
		assertEquals("Number of Samples", expression.getId().getFieldName());
		assertEquals(new Long(100), expression.getValue());
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testWhereEqualsDate() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from layer where creationDate == \"2011-01-31\"");
		assertEquals("layer", stmt.getTableName());
		assertNotNull(stmt.getSearchCondition());
		assertEquals(1, stmt.getSearchCondition().size());
		assertTrue(stmt.getSearchCondition().get(0) instanceof Expression);
		Expression expression = (Expression) stmt.getSearchCondition().get(0);
		assertEquals("creationDate", expression.getId().getFieldName());
		assertEquals("2011-01-31", expression.getValue());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testLimit() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from dataset limit 13");
		assertEquals("dataset", stmt.getTableName());
		assertEquals(new Long(13), stmt.getLimit());
	}
	
	@Test
	public void testDefaultLimit() throws Exception {
		QueryStatement stmt = new QueryStatement(
				"select * from dataset");
		assertEquals((Long) 1000L, stmt.getLimit());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOffset() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from dataset offset 13");
		assertEquals("dataset", stmt.getTableName());
		assertEquals(new Long(13), stmt.getOffset());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDefaultOffset() throws Exception {
		QueryStatement stmt = new QueryStatement(
				"select * from dataset");
		assertEquals("dataset", stmt.getTableName());
		assertEquals(new Long(0), stmt.getOffset());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOrderBy() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from dataset order by name");
		assertEquals("dataset", stmt.getTableName());
		assertNull(stmt.getSortTable());
		assertEquals("name", stmt.getSortField());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOrderByAscending() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from dataset order by dataset.name asc");
		assertEquals("dataset", stmt.getTableName());
		assertEquals("dataset", stmt.getSortTable());
		assertEquals("name", stmt.getSortField());
		assertTrue(stmt.getSortAcending());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOrderByDescending() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from dataset order by name desc");
		assertEquals("dataset", stmt.getTableName());
		assertNull(stmt.getSortTable());
		assertEquals("name", stmt.getSortField());
		assertFalse(stmt.getSortAcending());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOrderByWithLimit() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from dataset order by dataset.\"name\" limit 30");
		assertEquals("dataset", stmt.getTableName());
		assertEquals("dataset", stmt.getSortTable());
		assertEquals("name", stmt.getSortField());
		assertEquals(new Long(30), stmt.getLimit());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testLayerQuery() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from layer where dataset.id == \"123\"");
		assertEquals("layer", stmt.getTableName());
		assertNotNull(stmt.getSearchCondition());
		assertEquals(1, stmt.getSearchCondition().size());
		assertTrue(stmt.getSearchCondition().get(0) instanceof Expression);
		Expression expression = (Expression) stmt.getSearchCondition().get(0);
		assertEquals("dataset", expression.getId().getTableName());
		assertEquals("id", expression.getId().getFieldName());
		assertEquals("123", expression.getValue());
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testSingleQuoteStringLiteral() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from layer where dataset.id == '123'");
		assertEquals("layer", stmt.getTableName());
		assertNotNull(stmt.getSearchCondition());
		assertEquals(1, stmt.getSearchCondition().size());
		assertTrue(stmt.getSearchCondition().get(0) instanceof Expression);
		Expression expression = (Expression) stmt.getSearchCondition().get(0);
		assertEquals("dataset", expression.getId().getTableName());
		assertEquals("id", expression.getId().getFieldName());
		assertEquals("123", expression.getValue());
	}

	@Test
	public void testFilterByProjectId() throws Exception {
		QueryStatement stmt = new QueryStatement("select * from entity where projectId == 'syn123'");
		assertEquals("entity", stmt.getTableName());
		assertNotNull(stmt.getSearchCondition());
		assertEquals(1, stmt.getSearchCondition().size());
		assertTrue(stmt.getSearchCondition().get(0) instanceof Expression);
		Expression expression = (Expression) stmt.getSearchCondition().get(0);
		assertEquals("projectId", expression.getId().getFieldName());
		assertEquals("syn123", expression.getValue());
	}
	
	/************************************************************************
	 * Error cases, make sure the messages we return are useful to humans.
	 */

	/**
	 * @throws Exception
	 */
	@Test(expected = ParseException.class)
	public void testMissingTable() throws Exception {
		queryShouldFail("select * from order by creationDate desc", null);
	}

	/**
	 * @throws Exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testBadLimit() throws Exception {
		queryShouldFail("select * from dataset limit 0",
				"pagination limit must be 1 or greater");
	}

	/**
	 * @throws Exception
	 */
	@Test(expected = ParseException.class)
	public void testMissingStringTerminator() throws Exception {

		queryShouldFail("select * from layer where type == \"C", 
				"TokenMgrError: Lexical error at line 1, column 37.  Encountered: <EOF> after : \"\\\"C\"");
	}

	private void queryShouldFail(String query, String expectedErrorMessage)
			throws Exception {
		try {
			new QueryStatement(query);
		} catch (Exception e) {
			log.info("Got error \"" + e.getMessage() + "\" for query \""
					+ query + "\"");
			if (null != expectedErrorMessage) {
				assertEquals(expectedErrorMessage, e.getMessage());
			}
			throw e;
		}
	}
	
	@Test
	public void testNullValue() throws Exception {
		QueryStatement stmt = new QueryStatement("select * from enity where parentId == null");
		assertNotNull(stmt.getSearchCondition());
		assertEquals(1, stmt.getSearchCondition().size());
		Expression expr = stmt.getSearchCondition().get(0);
		assertNotNull(expr);
		assertEquals("parentId", expr.getId().getFieldName());
		assertEquals(null, expr.getValue());
	}
	
	@Test
	public void testSelectOneValue() throws Exception{
		QueryStatement stmt = new QueryStatement("select id from enity where parentId == null");
		// This is what we expect the parser to find.
		List<String> expectedSelect = new ArrayList<String>();
		expectedSelect.add("id");
		assertNotNull(stmt);
		assertNotNull(stmt.getSelect());
		assertEquals(expectedSelect, stmt.getSelect());
	}
	
	@Test
	public void testSelectMultiple() throws Exception{
		QueryStatement stmt = new QueryStatement("select etag, name, id from enity where parentId == null");
		// This is what we expect the parser to find.
		List<String> expectedSelect = new ArrayList<String>();
		expectedSelect.add("etag");
		expectedSelect.add("name");
		expectedSelect.add("id");
		assertNotNull(stmt);
		assertNotNull(stmt.getSelect());
		assertEquals(expectedSelect, stmt.getSelect());
	}

}
