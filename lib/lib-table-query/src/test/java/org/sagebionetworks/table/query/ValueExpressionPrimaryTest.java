package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.HasQuoteValue;
import org.sagebionetworks.table.query.model.ValueExpressionPrimary;

public class ValueExpressionPrimaryTest {

	@Test
	public void testGetReferencedColumnCountStar() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("count(*)").valueExpressionPrimary();
		HasQuoteValue referencedColumn = element.getReferencedColumn();
		assertEquals("There is no rferenced column for count(*)",null, referencedColumn);
	}
	
	@Test
	public void testGetReferencedColumnFunctionColumn() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("count(foo)").valueExpressionPrimary();
		HasQuoteValue referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.getValueWithoutQuotes());
	}
	
	@Test
	public void testGetReferencedColumnFunctionSingleQuote() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("max('foo')").valueExpressionPrimary();
		HasQuoteValue referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.getValueWithoutQuotes());
	}
	
	@Test
	public void testGetReferencedColumnFunctionDoubleQuote() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("max(\"foo\")").valueExpressionPrimary();
		HasQuoteValue referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.getValueWithoutQuotes());
	}
	
	@Test
	public void testGetReferencedColumnSimple() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("foo").valueExpressionPrimary();
		HasQuoteValue referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.getValueWithoutQuotes());
	}
	
	@Test
	public void testGetReferencedColumnDoubleQuotese() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("\"foo\"").valueExpressionPrimary();
		HasQuoteValue referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.getValueWithoutQuotes());
	}
	
	@Test
	public void testGetReferencedColumnSingleQuotese() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("'foo'").valueExpressionPrimary();
		HasQuoteValue referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.getValueWithoutQuotes());
	}
	
	@Test
	public void testGetReferencedColumnAs() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("foo as bar").valueExpressionPrimary();
		HasQuoteValue referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.getValueWithoutQuotes());
	}
	
	@Test
	public void testGetReferencedColumnAsDoubleQuotes() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("\"foo\" as bar").valueExpressionPrimary();
		HasQuoteValue referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.getValueWithoutQuotes());
	}
	
	@Test
	public void testGetReferencedColumnAsSingleQuotes() throws ParseException{
		ValueExpressionPrimary element = new TableQueryParser("'foo' as bar").valueExpressionPrimary();
		HasQuoteValue referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.getValueWithoutQuotes());
	}
	
	
}
