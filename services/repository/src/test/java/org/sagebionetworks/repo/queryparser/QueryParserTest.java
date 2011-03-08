/**
 * 
 */
package org.sagebionetworks.repo.queryparser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.logging.Logger;

import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.web.QueryStatement;

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
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testWhereEqualsString() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from layer where type == \"C\"");
		assertEquals("layer", stmt.getTableName());
		assertNull(stmt.getWhereTable());
		assertEquals("type", stmt.getWhereField());
		assertEquals("C", stmt.getWhereValue());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testWhereEqualsNumber() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from dataset where \"Number of Samples\" == 100");
		assertEquals("dataset", stmt.getTableName());
		assertNull(stmt.getWhereTable());
		assertEquals("Number of Samples", stmt.getWhereField());
		assertEquals(new Long(100), stmt.getWhereValue());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testCompoundIdWithSpaces() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from dataset where dataset.\"Number of Samples\" == 100");
		assertEquals("dataset", stmt.getTableName());
		assertEquals("dataset", stmt.getWhereTable());
		assertEquals("Number of Samples", stmt.getWhereField());
		assertEquals(new Long(100), stmt.getWhereValue());
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testWhereEqualsDate() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from layer where creationDate == \"2011-01-31\"");
		assertEquals("layer", stmt.getTableName());
		assertNull(stmt.getWhereTable());
		assertEquals("creationDate", stmt.getWhereField());
		assertEquals("2011-01-31", stmt.getWhereValue());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testLimit() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from dataset limit 13");
		assertEquals("dataset", stmt.getTableName());
		assertEquals(new Integer(13), stmt.getLimit());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOffset() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from dataset offset 13");
		assertEquals("dataset", stmt.getTableName());
		assertEquals(new Integer(13), stmt.getOffset());
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
		assertEquals(new Integer(30), stmt.getLimit());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testLayerQuery() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from layer where dataset.id == \"123\"");
		assertEquals("layer", stmt.getTableName());
		assertEquals("dataset", stmt.getWhereTable());
		assertEquals("id", stmt.getWhereField());
		assertEquals("123", stmt.getWhereValue());
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
	@Ignore
	@Test(expected = ParseException.class)
	public void testMisspelledSortDirection() throws Exception {
		queryShouldFail("select * from dataset order by creationDate dsc", null);
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

	/**
	 * @throws Exception
	 */
	@Ignore
	@Test(expected = ParseException.class)
	public void testExtraStuffAtTheEnd() throws Exception {
		queryShouldFail(
				"select * from dataset where Species == \"Human\" order by name desc limit 10 offset 1 this is extra stuff",
				null);
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

	/************************************************************************
	 * Parser happy case tests
	 */

	/**
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testParserSelectStar() throws Exception {

		QueryParser parser = new QueryParser(new StringReader(
				"select * from dataset"));
		/*
		 * Start parsing from the nonterminal "Start".
		 */
		QueryNode parseTree = (QueryNode) parser.Start();

		QueryNode tableName = (QueryNode) parseTree.jjtGetChild(0);
		System.out.println("The table is " + tableName.jjtGetValue()
				+ " of type " + tableName);

		assertEquals("dataset", tableName.jjtGetValue().toString());

		assertEquals("TableName", tableName.toString());

		/*
		 * If parsing completed without exceptions, print the resulting parse
		 * tree on standard output.
		 */
		parseTree.dump("");
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testParserWhereEqualsString() throws Exception {

		QueryParser parser = new QueryParser(new StringReader(
				"select * from layer where type == \"C\""));
		/*
		 * Start parsing from the nonterminal "Start".
		 */
		QueryNode parseTree = (QueryNode) parser.Start();

		String tableId = null;
		String whereField = null;
		Object whereValue = null;

		for (int i = 0; i < parseTree.jjtGetNumChildren(); i++) {
			QueryNode node = (QueryNode) parseTree.jjtGetChild(i);
			switch (node.getId()) {
			case QueryParser.JJTWHERE:
				whereField = (String) ((QueryNode) node.jjtGetChild(0))
						.jjtGetValue();
				whereValue = ((QueryNode) node.jjtGetChild(1).jjtGetChild(0))
						.jjtGetValue();
				break;
			case QueryParser.JJTTABLENAME:
				tableId = (String) node.jjtGetValue();
				break;
			}
		}

		assertEquals("layer", tableId);
		assertEquals("type", whereField);
		assertEquals("C", whereValue);

		/*
		 * If parsing completed without exceptions, print the resulting parse
		 * tree on standard output.
		 */
		parseTree.dump("");
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testParserWhereEqualsNumber() throws Exception {

		QueryParser parser = new QueryParser(new StringReader(
				"select * from dataset where \"Number of Samples\" == 100"));
		/*
		 * Start parsing from the nonterminal "Start".
		 */
		QueryNode parseTree = (QueryNode) parser.Start();

		String tableId = null;
		String whereField = null;
		Object whereValue = null;

		for (int i = 0; i < parseTree.jjtGetNumChildren(); i++) {
			QueryNode node = (QueryNode) parseTree.jjtGetChild(i);
			switch (node.getId()) {
			case QueryParser.JJTWHERE:
				whereField = (String) ((QueryNode) node.jjtGetChild(0))
						.jjtGetValue();
				whereValue = ((QueryNode) node.jjtGetChild(1).jjtGetChild(0))
						.jjtGetValue();
				break;
			case QueryParser.JJTTABLENAME:
				tableId = (String) node.jjtGetValue();
				break;
			}
		}

		assertEquals("dataset", tableId);
		assertEquals("Number of Samples", whereField);
		assertEquals(new Long(100), whereValue);

		/*
		 * If parsing completed without exceptions, print the resulting parse
		 * tree on standard output.
		 */
		parseTree.dump("");
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testParserWhereEqualsDate() throws Exception {

		QueryParser parser = new QueryParser(new StringReader(
				"select * from layer where creationDate == \"2011-01-31\""));
		/*
		 * Start parsing from the nonterminal "Start".
		 */
		QueryNode parseTree = (QueryNode) parser.Start();

		String tableId = null;
		String whereField = null;
		Object whereValue = null;

		for (int i = 0; i < parseTree.jjtGetNumChildren(); i++) {
			QueryNode node = (QueryNode) parseTree.jjtGetChild(i);
			switch (node.getId()) {
			case QueryParser.JJTWHERE:
				whereField = (String) ((QueryNode) node.jjtGetChild(0))
						.jjtGetValue();
				whereValue = ((QueryNode) node.jjtGetChild(1).jjtGetChild(0))
						.jjtGetValue();
				break;
			case QueryParser.JJTTABLENAME:
				tableId = (String) node.jjtGetValue();
				break;
			}
		}

		assertEquals("layer", tableId);
		assertEquals("creationDate", whereField);
		assertEquals("2011-01-31", whereValue);

		/*
		 * If parsing completed without exceptions, print the resulting parse
		 * tree on standard output.
		 */
		parseTree.dump("");
	}

}
