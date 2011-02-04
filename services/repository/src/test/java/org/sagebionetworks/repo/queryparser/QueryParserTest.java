/**
 * 
 */
package org.sagebionetworks.repo.queryparser;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.queryparser.QueryNode;
import org.sagebionetworks.repo.queryparser.QueryParser;

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
		
		QueryParser parser = new QueryParser(
				new StringReader("select * from dataset"));
		/*
		 * Start parsing from the nonterminal "Start".
		 */
		QueryNode parseTree = (QueryNode) parser.Start();

		QueryNode tableId = (QueryNode) parseTree.jjtGetChild(0);
		System.out.println("The table is " + tableId.jjtGetValue() + " of type " + tableId);	
		
		assertEquals("dataset", tableId.jjtGetValue().toString());
		
		assertEquals("TableName", tableId.toString());

		/*
		 * If parsing completed without exceptions, print the resulting
		 * parse tree on standard output.
		 */
		parseTree.dump("");
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testWhereEqualsString() throws Exception {
		
		QueryParser parser = new QueryParser(
				new StringReader("select * from layer where foo == \"foobar\""));
		/*
		 * Start parsing from the nonterminal "Start".
		 */
		QueryNode parseTree = (QueryNode) parser.Start();

		String tableId = null;
		String whereField = null;
		Object whereValue = null;
		
		for(int i = 0; i < parseTree.jjtGetNumChildren(); i++) {
			QueryNode node = (QueryNode) parseTree.jjtGetChild(i);
			switch(node.getId()) {
			case QueryParser.JJTWHERE :
				whereField = (String) ((QueryNode) node.jjtGetChild(0)).jjtGetValue();
				whereValue = ((QueryNode) node.jjtGetChild(1).jjtGetChild(0)).jjtGetValue();
				break;
			case QueryParser.JJTTABLENAME :
				tableId = (String) node.jjtGetValue();
				break;
			}
		}
		
		assertEquals("layer", tableId);
		assertEquals("foo", whereField);
		assertEquals("\"foobar\"", whereValue);
		
		/*
		 * If parsing completed without exceptions, print the resulting
		 * parse tree on standard output.
		 */
		parseTree.dump("");
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testWhereEqualsNumber() throws Exception {
		
		QueryParser parser = new QueryParser(
				new StringReader("select * from layer where foo == 100"));
		/*
		 * Start parsing from the nonterminal "Start".
		 */
		QueryNode parseTree = (QueryNode) parser.Start();

		String tableId = null;
		String whereField = null;
		Object whereValue = null;
		
		for(int i = 0; i < parseTree.jjtGetNumChildren(); i++) {
			QueryNode node = (QueryNode) parseTree.jjtGetChild(i);
			switch(node.getId()) {
			case QueryParser.JJTWHERE :
				whereField = (String) ((QueryNode) node.jjtGetChild(0)).jjtGetValue();
				whereValue = ((QueryNode) node.jjtGetChild(1).jjtGetChild(0)).jjtGetValue();
				break;
			case QueryParser.JJTTABLENAME :
				tableId = (String) node.jjtGetValue();
				break;
			}
		}
		
		assertEquals("layer", tableId);
		assertEquals("foo", whereField);
		assertEquals(new Long(100), whereValue);
		
		/*
		 * If parsing completed without exceptions, print the resulting
		 * parse tree on standard output.
		 */
		parseTree.dump("");
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testWhereEqualsDate() throws Exception {
		
		QueryParser parser = new QueryParser(
				new StringReader("select * from layer where foo == \"2011-01-31\""));
		/*
		 * Start parsing from the nonterminal "Start".
		 */
		QueryNode parseTree = (QueryNode) parser.Start();

		String tableId = null;
		String whereField = null;
		Object whereValue = null;
		
		for(int i = 0; i < parseTree.jjtGetNumChildren(); i++) {
			QueryNode node = (QueryNode) parseTree.jjtGetChild(i);
			switch(node.getId()) {
			case QueryParser.JJTWHERE :
				whereField = (String) ((QueryNode) node.jjtGetChild(0)).jjtGetValue();
				whereValue = ((QueryNode) node.jjtGetChild(1).jjtGetChild(0)).jjtGetValue();
				break;
			case QueryParser.JJTTABLENAME :
				tableId = (String) node.jjtGetValue();
				break;
			}
		}
		
		assertEquals("layer", tableId);
		assertEquals("foo", whereField);
		assertEquals("\"2011-01-31\"", whereValue);
		
		/*
		 * If parsing completed without exceptions, print the resulting
		 * parse tree on standard output.
		 */
		parseTree.dump("");
	}

}
