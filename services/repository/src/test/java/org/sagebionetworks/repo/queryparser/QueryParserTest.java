/**
 * 
 */
package org.sagebionetworks.repo.queryparser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.queryparser.QueryNode;
import org.sagebionetworks.repo.queryparser.QueryParser;
import org.sagebionetworks.repo.web.QueryStatement;

/**
 * @author deflaux
 * 
 */
public class QueryParserTest {

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

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
				"select * from layer where foo == \"foobar\"");
		assertEquals("layer", stmt.getTableName());
		assertEquals("foo", stmt.getWhereField());
		assertEquals("foobar", stmt.getWhereValue());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testWhereEqualsNumber() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from layer where foo == 100");
		assertEquals("layer", stmt.getTableName());
		assertEquals("foo", stmt.getWhereField());
		assertEquals(new Long(100), stmt.getWhereValue());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testWhereEqualsDate() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from layer where foo == \"2011-01-31\"");
		assertEquals("layer", stmt.getTableName());
		assertEquals("foo", stmt.getWhereField());
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
				"select * from dataset order by foo");
		assertEquals("dataset", stmt.getTableName());
		assertEquals("foo", stmt.getSortField());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOrderByAscending() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from dataset order by foo asc");
		assertEquals("dataset", stmt.getTableName());
		assertEquals("foo", stmt.getSortField());
		assertTrue(stmt.getSortAcending());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOrderByDescending() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from dataset order by foo desc");
		assertEquals("dataset", stmt.getTableName());
		assertEquals("foo", stmt.getSortField());
		assertFalse(stmt.getSortAcending());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testOrderByWithLimit() throws Exception {

		QueryStatement stmt = new QueryStatement(
				"select * from dataset order by \"name\" limit 30");
		assertEquals("dataset", stmt.getTableName());
		assertEquals("name", stmt.getSortField());
		assertEquals(new Integer(30), stmt.getLimit());
	}

	/**
	 * @throws Exception
	 */
	@Test(expected = ParseException.class)
	public void testMissingTable() throws Exception {
		try {
			new QueryStatement("select * from order by foo desc");
		} catch (ParseException e) {
			// TODO assert that we are delivering a useful error message to
			// users
			System.out.println(e);
			throw e;
		}
	}

	/**
	 * @throws Exception
	 */
	@Ignore
	@Test(expected = ParseException.class)
	public void testMisspelledSortDirection() throws Exception {
		try {
			new QueryStatement("select * from dataset order by foo dsc");
		} catch (ParseException e) {
			// TODO assert that we are delivering a useful error message to
			// users
			System.out.println(e);
			throw e;
		}
	}

	/**
	 * @throws Exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testBadLimit() throws Exception {
		try {
			new QueryStatement("select * from foo limit 0");
		} catch (IllegalArgumentException e) {
			// TODO assert that we are delivering a useful error message to
			// users
			System.out.println(e);
			throw e;
		}
	}

	/**
	 * @throws Exception
	 */
	@Test
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
	public void testParserWhereEqualsString() throws Exception {

		QueryParser parser = new QueryParser(new StringReader(
				"select * from layer where foo == \"foobar\""));
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
		assertEquals("foo", whereField);
		assertEquals("foobar", whereValue);

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
	public void testParserWhereEqualsNumber() throws Exception {

		QueryParser parser = new QueryParser(new StringReader(
				"select * from layer where foo == 100"));
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
		assertEquals("foo", whereField);
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
	public void testParserWhereEqualsDate() throws Exception {

		QueryParser parser = new QueryParser(new StringReader(
				"select * from layer where foo == \"2011-01-31\""));
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
		assertEquals("foo", whereField);
		assertEquals("2011-01-31", whereValue);

		/*
		 * If parsing completed without exceptions, print the resulting parse
		 * tree on standard output.
		 */
		parseTree.dump("");
	}

}
