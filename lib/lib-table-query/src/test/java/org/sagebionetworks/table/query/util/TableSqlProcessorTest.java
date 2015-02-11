package org.sagebionetworks.table.query.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.Identifier;
import org.sagebionetworks.table.query.model.SortKey;
import org.sagebionetworks.table.query.model.ValueExpressionPrimary;

public class TableSqlProcessorTest {
	
	@Test
	public void testGetName() throws ParseException{
		Identifier a = new TableQueryParser("foo").identifier();
		Identifier b = new TableQueryParser("\"foo\"").identifier();
		// They should have the same string value even though one is in quotes.
		assertEquals(TableSqlProcessor.getName(a), TableSqlProcessor.getName(b));
	}
	
	@Test
	public void testGetNameFunction() throws ParseException{
		ValueExpressionPrimary a = new TableQueryParser("count(*)").valueExpressionPrimary();
		ValueExpressionPrimary b = new TableQueryParser("COUNT(*)").valueExpressionPrimary();
		// They should have the same string value even though one is in quotes.
		assertEquals(TableSqlProcessor.getName(a), TableSqlProcessor.getName(b));
	}
	
	@Test
	public void testGetNameFunction2() throws ParseException{
		ValueExpressionPrimary a = new TableQueryParser("foo").valueExpressionPrimary();
		ValueExpressionPrimary b = new TableQueryParser("\"foo\"").valueExpressionPrimary();
		// They should have the same string value even though one is in quotes.
		assertEquals(TableSqlProcessor.getName(a), TableSqlProcessor.getName(b));
	}
	
	@Test
	public void testNameEqualsSortKeyTrue() throws ParseException{
		SortKey a = new TableQueryParser("foo").sortKey();
		SortKey b = new TableQueryParser("\"foo\"").sortKey();
		assertTrue(TableSqlProcessor.nameEquals(a, b));
	}
	
	@Test
	public void testNameEqualsDerivedColumn() throws ParseException{
		DerivedColumn a = new TableQueryParser("count(foo)").derivedColumn();
		DerivedColumn b = new TableQueryParser("COUNT( \"foo\" )").derivedColumn();
		assertTrue(TableSqlProcessor.nameEquals(a, b));
	}
	@Test
	public void testNameEqualsSortKeyFalse() throws ParseException{
		SortKey a = new TableQueryParser("bar").sortKey();
		SortKey b = new TableQueryParser("\"foo\"").sortKey();
		assertFalse(TableSqlProcessor.nameEquals(a, b));
	}
	
	@Test
	public void testNameEqualsSortSpecification() throws ParseException{
		SortKey a = new TableQueryParser("foo asc").sortKey();
		SortKey b = new TableQueryParser("\"foo\" DESC").sortKey();
		assertTrue(TableSqlProcessor.nameEquals(a, b));
	}
	
	@Test
	public void testToggleSortNoSort() throws ParseException{
		String sql = "select * from syn123";
		String expected = "SELECT * FROM syn123 ORDER BY \"foo\" ASC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "foo");
		assertEquals(expected, results);
	}
	
	@Test
	public void testToggleSortAlreadySorting() throws ParseException{
		String sql = "select * from syn123 order by foo ASC";
		String expected = "SELECT * FROM syn123 ORDER BY \"foo\" DESC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "foo");
		assertEquals(expected, results);
	}
	
	@Test
	public void testToggleSortAlreadySortingMultiple() throws ParseException{
		String sql = "select * from syn123 order by bar desc, foo ASC";
		String expected = "SELECT * FROM syn123 ORDER BY \"foo\" DESC, bar DESC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "foo");
		assertEquals(expected, results);
	}
	
	@Test
	public void testToggleSortAlreadySortingMultipleWithQuotes() throws ParseException{
		String sql = "select * from syn123 order by bar desc, \"foo\" ASC";
		String expected = "SELECT * FROM syn123 ORDER BY \"foo\" DESC, bar DESC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "foo");
		assertEquals(expected, results);
	}
	
	@Test
	public void testToggleSortAlreadySortingMultipleAddNew() throws ParseException{
		String sql = "select * from syn123 order by bar desc, foofoo ASC";
		String expected = "SELECT * FROM syn123 ORDER BY \"foo not\" ASC, bar DESC, foofoo ASC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "foo not");
		assertEquals(expected, results);
	}
	
	@Test
	public void testToggleSortAlreadySortingMultipleAddNew2() throws ParseException{
		String sql = "select * from syn123 order by bar desc, foofoo ASC";
		String expected = "SELECT * FROM syn123 ORDER BY \"foo\" ASC, bar DESC, foofoo ASC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "foo");
		assertEquals(expected, results);
	}
	
	/**
	 * In order to sort on an aggregate function we must have a column alias.
	 * @throws ParseException
	 */
	@Test
	public void testToggleSortAggregate() throws ParseException{
		String sql = "select bar, count(foo) from syn123 group by bar";
		String expected = "SELECT bar, COUNT(foo) FROM syn123 GROUP BY bar ORDER BY COUNT(foo) ASC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "count(foo)");
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetSortingInfo() throws ParseException{
		SortItem foo = new SortItem();
		foo.setColumn("foo");
		foo.setDirection(SortDirection.ASC);
		SortItem barbar = new SortItem();
		barbar.setColumn("bar bar");
		barbar.setDirection(SortDirection.DESC);
		List<SortItem> expected = Arrays.asList(foo, barbar);
		List<SortItem> results = TableSqlProcessor.getSortingInfo("select * from syn123 order by foo asc, \"bar bar\" DESC");
		assertEquals(expected, results);
	}
}
