package org.sagebionetworks.table.query.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.query.ParseException;


import com.google.common.collect.Sets;

public class TableSqlProcessorTest {
	
	String columnName;
	FacetColumnValuesRequest facetValues;
	FacetColumnRangeRequest facetRange;
	String basicSql;
	List<FacetColumnRequest> selectedFacets;
	@Before
	public void setUp(){
		columnName = "someColumn";
		facetValues = new FacetColumnValuesRequest();
		facetValues.setColumnName(columnName);
		facetRange = new FacetColumnRangeRequest();
		facetRange.setColumnName(columnName);
		
		basicSql = "SELECT * FROM syn123 ORDER BY asdf DESC";
		selectedFacets = new ArrayList<>();
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
	public void testToggleSortAggregateAlias() throws ParseException{
		String sql = "select bar, count(foo) as b from syn123 group by bar";
		String expected = "SELECT bar, COUNT(foo) AS b FROM syn123 GROUP BY bar ORDER BY \"b\" ASC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "b");
		assertEquals(expected, results);
	}
	
	@Test
	public void testToggleSortPLFM_4118() throws ParseException{
		String sql = "select * from syn123";
		String expected = "SELECT * FROM syn123 ORDER BY \"Date Approved/Rejected\" ASC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "Date Approved/Rejected");
		assertEquals(expected, results);
	}
	
	@Test
	public void testToggleSortPLFM_4118Count() throws ParseException{
		String sql = "select * from syn123";
		String expected = "SELECT * FROM syn123 ORDER BY COUNT(\"Date Approved/Rejected\") ASC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "count(\"Date Approved/Rejected\")");
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
	
	/////////////////////////////////
	// generateSqlWithFacets() tests
	/////////////////////////////////
	@Test (expected= IllegalArgumentException.class)
	public void testGenerateSqlWithFacetsNullSql() throws ParseException{
		TableSqlProcessor.generateSqlWithFacets(null, selectedFacets);
	}
	
	@Test (expected= IllegalArgumentException.class)
	public void testGenerateSqlWithFacetsNullFacets() throws ParseException{
		TableSqlProcessor.generateSqlWithFacets(basicSql, null);
	}
	
	@Test (expected= IllegalArgumentException.class)
	public void testGenerateSqlWithFacetsNonBasicSqlSelect() throws ParseException{
		TableSqlProcessor.generateSqlWithFacets("SELECT asdf FROM syn123", selectedFacets);
	}
	
	@Test (expected= IllegalArgumentException.class)
	public void testGenerateSqlWithFacetsNonBasicSqlWhere() throws ParseException{
		TableSqlProcessor.generateSqlWithFacets("SELECT * FROM syn123 WHERE asdf = 123", selectedFacets);
	}
	
	@Test (expected= IllegalArgumentException.class)
	public void testGenerateSqlWithFacetsNonBasicSqlGroupBy() throws ParseException{
		TableSqlProcessor.generateSqlWithFacets("SELECT * FROM syn123 GROUP BY asdf", selectedFacets);
	}
	
	@Test
	public void testGenerateSqlWithFacetsHappyCaseNoFacets() throws ParseException{
		String result = TableSqlProcessor.generateSqlWithFacets(basicSql, selectedFacets);
		assertEquals(basicSql, result);
	}
	
	@Test
	public void testGenerateSqlWithFacetsHappyCase() throws ParseException{
		String max = "12345";
		String val = "testeroni";
		facetRange.setMax(max);
		selectedFacets.add(facetRange);
		facetValues.setFacetValues(Sets.newHashSet(val));
		selectedFacets.add(facetValues);
		
		String result = TableSqlProcessor.generateSqlWithFacets(basicSql, selectedFacets);
		assertEquals("SELECT * FROM syn123 WHERE ( " + columnName + " <= " + max +" ) AND ( " + columnName + " = " + val+ " ) ORDER BY asdf DESC",
				result);
	}
	
	///////////////////////////////////////////
	// createFacetSearchConditionString() tests
	///////////////////////////////////////////
	@Test
	public void testEnumerationSearchConditionStringOneValue(){
		String value = "hello";
		facetValues.setFacetValues(Sets.newHashSet(value));
		String searchConditionString = TableSqlProcessor.createFacetSearchConditionString(facetValues);
		assertEquals("(" + columnName + "=" + value + ")", searchConditionString);
	}
	
	@Test
	public void testEnumerationSearchConditionStringOneValueIsNullKeyword(){
		String value = TableConstants.NULL_VALUE_KEYWORD;
		facetValues.setFacetValues(Sets.newHashSet(value));
		String searchConditionString = TableSqlProcessor.createFacetSearchConditionString(facetValues);
		assertEquals("(" + columnName + " IS NULL)", searchConditionString);
	}
	
	@Test
	public void testEnumerationSearchConditionStringTwoValues(){
		String value1 = "hello";
		String value2 = "world";
		facetValues.setFacetValues(Sets.newHashSet(value1, value2));


		String searchConditionString = TableSqlProcessor.createFacetSearchConditionString(facetValues);
		assertEquals("(" + columnName + "=" + value1 + " OR " + columnName + "=" + value2 + ")", searchConditionString);
	}
	
	@Test
	public void testEnumerationSearchConditionStringTwoValuesWithOneBeingNullKeyword(){
		String value1 = TableConstants.NULL_VALUE_KEYWORD;
		String value2 = "world";
		facetValues.setFacetValues(Sets.newHashSet(value1, value2));


		String searchConditionString = TableSqlProcessor.createFacetSearchConditionString(facetValues);
		assertEquals("(" + columnName + " IS NULL OR " + columnName + "=" + value2 + ")", searchConditionString);
	}
	
	@Test
	public void testEnumerationSearchConditionStringOneValueContainsSpace(){
		String value = "hello world";
		facetValues.setFacetValues(Sets.newHashSet(value));
		String searchConditionString = TableSqlProcessor.createFacetSearchConditionString(facetValues);
		assertEquals("(" + columnName + "='" + value + "')", searchConditionString);
	}
	
	@Test
	public void testRangeSearchConditionStringMinOnly(){
		String min = "42";
		facetRange.setMin(min);
		String searchConditionString = TableSqlProcessor.createFacetSearchConditionString(facetRange);
		assertEquals("(" + columnName + ">=" + min + ")", searchConditionString);
	}
	
	@Test
	public void testRangeSearchConditionStringMaxOnly(){
		String max = "42";
		facetRange.setMax(max);
		String searchConditionString = TableSqlProcessor.createFacetSearchConditionString(facetRange);
		assertEquals("(" + columnName + "<=" + max + ")", searchConditionString);
	}
	
	@Test
	public void testRangeSearchConditionStringMinAndMax(){
		String min = "123";
		String max = "456";
		facetRange.setMin(min);
		facetRange.setMax(max);
		String searchConditionString = TableSqlProcessor.createFacetSearchConditionString(facetRange);
		assertEquals("(" + columnName + " BETWEEN " + min + " AND " + max + ")", searchConditionString);
	}
}
