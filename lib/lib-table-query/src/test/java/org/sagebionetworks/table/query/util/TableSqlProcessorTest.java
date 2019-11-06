package org.sagebionetworks.table.query.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.model.WhereClause;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TableSqlProcessorTest {
	
	String stringColumnName;
	String intColumnName;
	String tableId;
	FacetColumnValuesRequest stringFacet;
	FacetColumnRangeRequest intFacet;
	String basicSql;
	List<FacetColumnRequest> selectedFacets;
	List<ColumnModel> schema;
	
	StringBuilder stringBuilder;
	
	WhereClause whereClause;
	String facetSearchConditionString;
	@Before
	public void setUp() throws ParseException{
		stringColumnName = "stringColumn";
		intColumnName = "integerColumn";
		tableId = "syn123";
		stringFacet = new FacetColumnValuesRequest();
		stringFacet.setColumnName(stringColumnName);
		intFacet = new FacetColumnRangeRequest();
		intFacet.setColumnName(intColumnName);
		
		
		ColumnModel stringColumnModel = new ColumnModel();
		stringColumnModel.setName(stringColumnName);
		stringColumnModel.setColumnType(ColumnType.STRING);
		stringColumnModel.setFacetType(FacetType.enumeration);
		
		ColumnModel intColumnModel = new ColumnModel();
		intColumnModel.setName(intColumnName);
		intColumnModel.setColumnType(ColumnType.INTEGER);
		intColumnModel.setFacetType(FacetType.range);
		
		
		basicSql = "SELECT * FROM " + tableId + " ORDER BY "+ intColumnName +" DESC";
		selectedFacets = new ArrayList<>();
		stringBuilder = new StringBuilder();
		schema = Lists.newArrayList(stringColumnModel, intColumnModel);
	}
	
	@Test
	public void testToggleSortNoSort() throws ParseException{
		String sql = "select * from " + tableId;
		String expected = "SELECT * FROM " + tableId + " ORDER BY \"foo\" ASC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "foo");
		assertEquals(expected, results);
	}
	
	@Test
	public void testToggleSortAlreadySorting() throws ParseException{
		String sql = "select * from " + tableId + " order by foo ASC";
		String expected = "SELECT * FROM " + tableId + " ORDER BY \"foo\" DESC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "foo");
		assertEquals(expected, results);
	}
	
	@Test
	public void testToggleSortAlreadySortingMultiple() throws ParseException{
		String sql = "select * from " + tableId + " order by bar desc, foo ASC";
		String expected = "SELECT * FROM " + tableId + " ORDER BY \"foo\" DESC, bar DESC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "foo");
		assertEquals(expected, results);
	}
	
	@Test
	public void testToggleSortAlreadySortingMultipleWithQuotes() throws ParseException{
		String sql = "select * from " + tableId + " order by bar desc, \"foo\" ASC";
		String expected = "SELECT * FROM " + tableId + " ORDER BY \"foo\" DESC, bar DESC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "foo");
		assertEquals(expected, results);
	}
	
	@Test
	public void testToggleSortAlreadySortingMultipleAddNew() throws ParseException{
		String sql = "select * from " + tableId + " order by bar desc, foofoo ASC";
		String expected = "SELECT * FROM " + tableId + " ORDER BY \"foo not\" ASC, bar DESC, foofoo ASC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "foo not");
		assertEquals(expected, results);
	}
	
	@Test
	public void testToggleSortAlreadySortingMultipleAddNew2() throws ParseException{
		String sql = "select * from " + tableId + " order by bar desc, foofoo ASC";
		String expected = "SELECT * FROM " + tableId + " ORDER BY \"foo\" ASC, bar DESC, foofoo ASC";
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
		String sql = "select bar, count(foo) from " + tableId + " group by bar";
		String expected = "SELECT bar, COUNT(foo) FROM " + tableId + " GROUP BY bar ORDER BY COUNT(foo) ASC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "count(foo)");
		assertEquals(expected, results);
	}
	
	@Test
	public void testToggleSortAggregateAlias() throws ParseException{
		String sql = "select bar, count(foo) as b from " + tableId + " group by bar";
		String expected = "SELECT bar, COUNT(foo) AS b FROM " + tableId + " GROUP BY bar ORDER BY \"b\" ASC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "b");
		assertEquals(expected, results);
	}
	
	@Test
	public void testToggleSortPLFM_4118() throws ParseException{
		String sql = "select * from " + tableId;
		String expected = "SELECT * FROM " + tableId + " ORDER BY \"Date Approved/Rejected\" ASC";
		// call under test.
		String results = TableSqlProcessor.toggleSort(sql, "Date Approved/Rejected");
		assertEquals(expected, results);
	}
	
	@Test
	public void testToggleSortPLFM_4118Count() throws ParseException{
		String sql = "select * from " + tableId;
		String expected = "SELECT * FROM " + tableId + " ORDER BY COUNT(\"Date Approved/Rejected\") ASC";
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
		List<SortItem> results = TableSqlProcessor.getSortingInfo("select * from " + tableId + " order by foo asc, \"bar bar\" DESC");
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetSortingInfoPLFM_4459() throws ParseException{
		SortItem foo = new SortItem();
		foo.setColumn("foo");
		foo.setDirection(SortDirection.ASC);
		List<SortItem> expected = Arrays.asList(foo);
		List<SortItem> results = TableSqlProcessor.getSortingInfo("select * from " + tableId + " order by foo");
		assertEquals(expected, results);
	}
	
	/////////////////////////////////
	// generateSqlWithFacets() tests
	/////////////////////////////////
	@Test (expected= IllegalArgumentException.class)
	public void testGenerateSqlWithFacetsNullSql() throws ParseException{
		TableSqlProcessor.generateSqlWithFacets(null, selectedFacets, schema);
	}
	
	@Test (expected= IllegalArgumentException.class)
	public void testGenerateSqlWithFacetsNullFacets() throws ParseException{
		TableSqlProcessor.generateSqlWithFacets(basicSql, null, schema);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGenerateSqlWithFacetsNullSchema() throws ParseException{
		TableSqlProcessor.generateSqlWithFacets(basicSql, selectedFacets, null);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGenerateSqlWithFacetsMoreFacetColumnsThanSchema() throws ParseException{
		schema.clear();
		selectedFacets.add(stringFacet);
		TableSqlProcessor.generateSqlWithFacets(basicSql, selectedFacets, schema);
	}
	
	@Test
	public void testGenerateSqlWithFacetsHappyCaseNoFacets() throws ParseException{
		String result = TableSqlProcessor.generateSqlWithFacets(basicSql, selectedFacets, schema);
		assertEquals(basicSql, result);
	}
	
	@Test
	public void testGenerateSqlWithFacetsHappyCase() throws ParseException{
		String max = "12345";
		String val = "testeroni";
		intFacet.setMax(max);
		selectedFacets.add(intFacet);
		stringFacet.setFacetValues(Sets.newHashSet(val));
		selectedFacets.add(stringFacet);
		
		String result = TableSqlProcessor.generateSqlWithFacets(basicSql, selectedFacets, schema);
		assertEquals("SELECT * FROM syn123"
				+ " WHERE ( ( \"integerColumn\" <= '12345' )"
				+ " AND ( \"stringColumn\" = 'testeroni' ) )"
				+ " ORDER BY integerColumn DESC",
				result);
	}
	
}
