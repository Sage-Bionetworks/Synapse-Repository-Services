package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ColumnNameReference;
import org.sagebionetworks.table.query.model.ValueExpressionPrimary;

public class ValueExpressionPrimaryTest {

	@Test
	public void testGetReferencedColumnCountStar() throws ParseException {
		ValueExpressionPrimary element = new TableQueryParser("count(*)").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertEquals(null, referencedColumn, "There is no referenced column for count(*)");
	}

	@Test
	public void testIsReferenceInFunctioCountStar() throws ParseException {
		ValueExpressionPrimary element = new TableQueryParser("count(*)").valueExpressionPrimary();
		assertThrows(IllegalArgumentException.class, () -> {
			element.isReferenceInFunction();
		});

	}

	@Test
	public void testGetReferencedColumnFunctionColumn() throws ParseException {
		ValueExpressionPrimary element = new TableQueryParser("count(foo)").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertTrue(element.isReferenceInFunction());
	}

	@Test
	public void testGetReferencedColumnFunctionSingleQuote() throws ParseException {
		ValueExpressionPrimary element = new TableQueryParser("max('foo')").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertTrue(element.isReferenceInFunction());
	}

	@Test
	public void testGetReferencedColumnFunctionDoubleQuote() throws ParseException {
		ValueExpressionPrimary element = new TableQueryParser("max(\"foo\")").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertTrue(element.isReferenceInFunction());
	}

	@Test
	public void testGetReferencedColumnSimple() throws ParseException {
		ValueExpressionPrimary element = new TableQueryParser("foo").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertFalse(element.isReferenceInFunction());
	}

	@Test
	public void testGetReferencedColumnDoubleQuotese() throws ParseException {
		ValueExpressionPrimary element = new TableQueryParser("\"foo\"").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertFalse(element.isReferenceInFunction());
	}

	@Test
	public void testGetReferencedColumnSingleQuotese() throws ParseException {
		ValueExpressionPrimary element = new TableQueryParser("'foo'").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertFalse(element.isReferenceInFunction());
	}

	@Test
	public void testGetReferencedColumnAs() throws ParseException {
		ValueExpressionPrimary element = new TableQueryParser("foo as bar").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertFalse(element.isReferenceInFunction());
	}

	@Test
	public void testGetReferencedColumnAsDoubleQuotes() throws ParseException {
		ValueExpressionPrimary element = new TableQueryParser("\"foo\" as bar").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertFalse(element.isReferenceInFunction());
	}

	@Test
	public void testGetReferencedColumnAsSingleQuotes() throws ParseException {
		ValueExpressionPrimary element = new TableQueryParser("'foo' as bar").valueExpressionPrimary();
		ColumnNameReference referencedColumn = element.getReferencedColumn();
		assertNotNull(referencedColumn);
		assertEquals("foo", referencedColumn.toSqlWithoutQuotes());
		assertFalse(element.isReferenceInFunction());
	}

	@Test
	public void testParens() throws ParseException {
		ValueExpressionPrimary element = new TableQueryParser("(foo)").valueExpressionPrimary();
		assertEquals("(foo)", element.toSql());
	}

	@Test
	public void testGetChildren() throws ParseException {
		ValueExpressionPrimary element = new TableQueryParser("'foo' as bar").valueExpressionPrimary();
		assertEquals(Collections.singleton(element.getChild()), element.getChildren());
	}

}
