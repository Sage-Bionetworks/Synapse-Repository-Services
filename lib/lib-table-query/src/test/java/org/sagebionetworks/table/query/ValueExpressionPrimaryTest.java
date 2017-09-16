package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ColumnNameReference;
import org.sagebionetworks.table.query.model.ValueExpressionPrimary;

public class ValueExpressionPrimaryTest {

	@Test
	public void testGetReferencedColumnCountStar() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("count(*)").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertEquals("There is no referenced column for count(*)",null, referencedColumn);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testIsReferenceInFunctioCountStar() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("count(*)").valueExpressionPrimary();
		assertTrue(element.isReferenceInFunction());
	}
	
	@Test
	public void testGetReferencedColumnFunctionColumn() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("count(foo)").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertTrue(element.isReferenceInFunction());
	}
	
	@Test
	public void testGetReferencedColumnFunctionSingleQuote() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("max('foo')").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertTrue(element.isReferenceInFunction());
	}
	
	@Test
	public void testGetReferencedColumnFunctionDoubleQuote() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("max(\"foo\")").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertTrue(element.isReferenceInFunction());
	}
	
	@Test
	public void testGetReferencedColumnSimple() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("foo").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertFalse(element.isReferenceInFunction());
	}
	
	@Test
	public void testGetReferencedColumnDoubleQuotese() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("\"foo\"").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertFalse(element.isReferenceInFunction());
	}
	
	@Test
	public void testGetReferencedColumnSingleQuotese() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("'foo'").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertFalse(element.isReferenceInFunction());
	}
	
	@Test
	public void testGetReferencedColumnAs() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("foo as bar").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertFalse(element.isReferenceInFunction());
	}
	
	@Test
	public void testGetReferencedColumnAsDoubleQuotes() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("\"foo\" as bar").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertFalse(element.isReferenceInFunction());
	}
	
	@Test
	public void testGetReferencedColumnAsSingleQuotes() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("'foo' as bar").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertFalse(element.isReferenceInFunction());
	}
	
	
}
