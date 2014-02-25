package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.util.SQLExample;
import org.sagebionetworks.table.query.util.SQLExampleProvider;

public class TableQueryParserTest {
	
	/**
	 * We must be able to pares all of the example SQL.
	 */
	@Test
	public void testAllExamples(){
		List<SQLExample> exampleList = SQLExampleProvider.getSQLExamples();
		assertNotNull(exampleList);
		assertTrue(exampleList.size() > 2);
		// Test each example
		for(SQLExample example: exampleList){

			try{
				// Make sure we can parse the SQL
				System.out.println("Parsing: "+example.getSql());
				TableQueryParser.parserQuery(example.getSql());
			} catch(ParseException e){
				e.printStackTrace();
				fail("Failed to parse: '"+example.getSql()+"' Error: "+e.getMessage());
			}
		}
	}
	
	@Test
	public void testSelectStar() throws ParseException{
		// Parse the query into a basic model object
		QuerySpecification sq = TableQueryParser.parserQuery("select * from syn123");
		assertNotNull(sq);
		assertEquals(null,  sq.getSetQuantifier());
		assertNotNull(sq.getSelectList());
		assertEquals("Simple select * was missing the asterisk", "*",  sq.getSelectList().getAsterisk());
		assertEquals("Select * should not have any columns", null,  sq.getSelectList().getColumns());
		assertNotNull(sq.getTableExpression());
		assertNotNull(sq.getTableExpression().getFromClause());
		assertNotNull(sq.getTableExpression().getFromClause().getTableReference());
		assertNotNull(sq.getTableExpression().getFromClause().getTableReference().getTableName());
		assertEquals("syn123", sq.getTableExpression().getFromClause().getTableReference().getTableName());
	}
	
	@Test
	public void testSelectTableNameUnsigned() throws ParseException{
		// Parse the query into a basic model object
		QuerySpecification sq = TableQueryParser.parserQuery("select * from 123");
		assertNotNull(sq);
		assertNotNull(sq.getTableExpression());
		assertNotNull(sq.getTableExpression().getFromClause());
		assertNotNull(sq.getTableExpression().getFromClause().getTableReference());
		assertNotNull(sq.getTableExpression().getFromClause().getTableReference().getTableName());
		assertEquals("123", sq.getTableExpression().getFromClause().getTableReference().getTableName());
	}
	
	@Test
	public void testSelectMultipleColumns() throws ParseException{
		QuerySpecification sq = TableQueryParser.parserQuery("select foo, bar, foobar from syn123");
		assertNotNull(sq);
		assertNotNull(sq.getSelectList());
		assertEquals("Asterisk should be null when we have columns", null, sq.getSelectList().getAsterisk());
		List<DerivedColumn> columns = sq.getSelectList().getColumns();
		assertNotNull(columns);
		assertEquals(3, columns.size());
		// foo
		DerivedColumn dc = columns.get(0);
		assertEquals(null, dc.getAsClause());
		assertNotNull(dc.getValueExpression());
		assertNotNull(dc.getValueExpression());
		assertEquals(null, dc.getValueExpression().getSetFunction());
		ColumnReference cr = dc.getValueExpression().getColumnReference();
		assertNotNull(cr);
		assertEquals(null, cr.getQualifier());
		assertEquals("foo", cr.getColumnName());
		// bar
		dc = columns.get(1);
		assertEquals(null, dc.getAsClause());
		assertNotNull(dc.getValueExpression());
		assertNotNull(dc.getValueExpression());
		assertEquals(null, dc.getValueExpression().getSetFunction());
		cr = dc.getValueExpression().getColumnReference();
		assertNotNull(cr);
		assertEquals(null, cr.getQualifier());
		assertEquals("bar", cr.getColumnName());
	}
	
	@Test
	public void testSelectQuotedColumnName() throws ParseException{
		QuerySpecification sq = TableQueryParser.parserQuery("select \"foo & Bar\" from syn123");
		assertNotNull(sq);
		assertNotNull(sq.getSelectList());
		assertEquals("Asterisk should be null when we have columns", null, sq.getSelectList().getAsterisk());
		List<DerivedColumn> columns = sq.getSelectList().getColumns();
		assertNotNull(columns);
		assertEquals(3, columns.size());
		// foo
		DerivedColumn dc = columns.get(0);
		assertEquals(null, dc.getAsClause());
		assertNotNull(dc.getValueExpression());
		assertNotNull(dc.getValueExpression());
		assertEquals(null, dc.getValueExpression().getSetFunction());
		ColumnReference cr = dc.getValueExpression().getColumnReference();
		assertNotNull(cr);
		assertEquals(null, cr.getQualifier());
		assertEquals("foo", cr.getColumnName());
		// bar
		dc = columns.get(1);
		assertEquals(null, dc.getAsClause());
		assertNotNull(dc.getValueExpression());
		assertNotNull(dc.getValueExpression());
		assertEquals(null, dc.getValueExpression().getSetFunction());
		cr = dc.getValueExpression().getColumnReference();
		assertNotNull(cr);
		assertEquals(null, cr.getQualifier());
		assertEquals("bar", cr.getColumnName());
	}

}
