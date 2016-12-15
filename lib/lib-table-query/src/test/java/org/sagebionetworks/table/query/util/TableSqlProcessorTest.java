package org.sagebionetworks.table.query.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
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
		
		whereClause = new WhereClause(SqlElementUntils.createSearchCondition("water=wet AND sky=blue"));
		facetSearchConditionString = "(tabs > spaces)";
		stringBuilder = new StringBuilder();
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
	
	@Test (expected= IllegalArgumentException.class)
	public void testGenerateSqlWithFacetsNonBasicSqlSelect() throws ParseException{
		TableSqlProcessor.generateSqlWithFacets("SELECT asdf FROM " + tableId, selectedFacets, schema);
	}
	
	@Test (expected= IllegalArgumentException.class)
	public void testGenerateSqlWithFacetsNonBasicSqlWhere() throws ParseException{
		TableSqlProcessor.generateSqlWithFacets("SELECT * FROM " + tableId + " WHERE asdf = 123", selectedFacets, schema);
	}
	
	@Test (expected= IllegalArgumentException.class)
	public void testGenerateSqlWithFacetsNonBasicSqlGroupBy() throws ParseException{
		TableSqlProcessor.generateSqlWithFacets("SELECT * FROM " + tableId + " GROUP BY asdf", selectedFacets, schema);
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
		assertEquals("SELECT * FROM " + tableId + " WHERE ( " + intColumnName + " <= " + max +" ) AND ( " + stringColumnName + " = '" + val+ "' ) ORDER BY " + intColumnName + " DESC",
				result);
	}
	
	///////////////////////////////////////////
	// createFacetSearchConditionString() tests
	///////////////////////////////////////////

	@Test
	public void testCreateFacetSearchConditionStringNullFacet(){
		assertNull(TableSqlProcessor.createFacetSearchConditionString(null, ColumnType.STRING));
	}
		
	@Test
	public void testCreateFacetSearchConditionStringUsingFacetColumnValuesRequestClass(){
		stringFacet.setFacetValues(Sets.newHashSet("hello"));
		assertEquals(TableSqlProcessor.createEnumerationSearchCondition(stringFacet, ColumnType.STRING),
				TableSqlProcessor.createFacetSearchConditionString(stringFacet, ColumnType.STRING));
	}
	
	@Test
	public void testCreateFacetSearchConditionStringUsingFacetColumnRangeRequestClass(){
		intFacet.setMax("123");
		assertEquals(TableSqlProcessor.createRangeSearchCondition(intFacet, ColumnType.INTEGER),
				TableSqlProcessor.createFacetSearchConditionString(intFacet, ColumnType.INTEGER));
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testCreateFacetSearchConditionStringUsingUnknownFacetClass(){
		TableSqlProcessor.createFacetSearchConditionString(new FacetColumnRequest(){
			public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom)
					throws JSONObjectAdapterException {return null;}
			public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {return null;}
			public String getJSONSchema() {return null;}
			public String getConcreteType() {return null;}
			public void setConcreteType(String concreteType) {}
			public String getColumnName() {return null;}
			public void setColumnName(String columnName) {}
		}, ColumnType.STRING);
	}
	
	////////////////////////////////////////////
	// createEnumerationSearchCondition() tests
	///////////////////////////////////////////
	@Test
	public void testEnumerationSearchConditionStringNullFacet(){
		assertNull(TableSqlProcessor.createEnumerationSearchCondition(null, ColumnType.STRING));
	}
	
	@Test
	public void testEnumerationSearchConditionStringNullFacetValues(){
		stringFacet.setFacetValues(null);
		assertNull(TableSqlProcessor.createEnumerationSearchCondition(stringFacet, ColumnType.STRING));
	}
	
	@Test
	public void testEnumerationSearchConditionStringEmptyFacetValues(){
		stringFacet.setFacetValues(new HashSet<String>());
		assertNull(TableSqlProcessor.createEnumerationSearchCondition(stringFacet, ColumnType.STRING));
	}
	
	@Test
	public void testEnumerationSearchConditionStringOneValue(){
		String value = "hello";
		stringFacet.setFacetValues(Sets.newHashSet(value));
		String searchConditionString = TableSqlProcessor.createEnumerationSearchCondition(stringFacet, ColumnType.STRING);
		assertEquals("(" + stringColumnName + "='" + value + "')", searchConditionString);
	}
	
	@Test
	public void testEnumerationSearchConditionStringOneValueIsNullKeyword(){
		String value = TableConstants.NULL_VALUE_KEYWORD;
		stringFacet.setFacetValues(Sets.newHashSet(value));
		String searchConditionString = TableSqlProcessor.createEnumerationSearchCondition(stringFacet, ColumnType.STRING);
		assertEquals("(" + stringColumnName + " IS NULL)", searchConditionString);
	}
	
	@Test
	public void testEnumerationSearchConditionStringTwoValues(){
		String value1 = "hello";
		String value2 = "world";
		stringFacet.setFacetValues(Sets.newHashSet(value1, value2));


		String searchConditionString = TableSqlProcessor.createEnumerationSearchCondition(stringFacet, ColumnType.STRING);
		assertEquals("(" + stringColumnName + "='" + value1 + "' OR " + stringColumnName + "='" + value2 + "')", searchConditionString);
	}
	
	@Test
	public void testEnumerationSearchConditionStringTwoValuesWithOneBeingNullKeyword(){
		String value1 = TableConstants.NULL_VALUE_KEYWORD;
		String value2 = "world";
		stringFacet.setFacetValues(Sets.newHashSet(value1, value2));


		String searchConditionString = TableSqlProcessor.createEnumerationSearchCondition(stringFacet, ColumnType.STRING);
		assertEquals("(" + stringColumnName + " IS NULL OR " + stringColumnName + "='" + value2 + "')", searchConditionString);
	}
	
	@Test
	public void testEnumerationSearchConditionStringOneValueContainsSpace(){
		String value = "hello world";
		stringFacet.setFacetValues(Sets.newHashSet(value));
		String searchConditionString = TableSqlProcessor.createEnumerationSearchCondition(stringFacet, ColumnType.STRING);
		assertEquals("(" + stringColumnName + "='" + value + "')", searchConditionString);
	}
	
	//////////////////////////////////////
	// createRangeSearchCondition() tests
	//////////////////////////////////////
	@Test
	public void testRangeSearchConditionNullFacet(){
		assertNull(TableSqlProcessor.createRangeSearchCondition(null, ColumnType.INTEGER));
	}
	
	@Test
	public void testRangeSearchConditionNoMinAndNoMax(){
		intFacet.setMax(null);
		intFacet.setMin("");
		assertNull(TableSqlProcessor.createRangeSearchCondition(intFacet, ColumnType.INTEGER));
	}
	
	@Test
	public void testRangeSearchConditionStringMinOnly(){
		String min = "42";
		intFacet.setMin(min);
		String searchConditionString = TableSqlProcessor.createRangeSearchCondition(intFacet, ColumnType.INTEGER);
		assertEquals("(" + intColumnName + ">=" + min + ")", searchConditionString);
	}
	
	@Test
	public void testRangeSearchConditionStringMaxOnly(){
		String max = "42";
		intFacet.setMax(max);
		String searchConditionString = TableSqlProcessor.createRangeSearchCondition(intFacet, ColumnType.INTEGER);
		assertEquals("(" + intColumnName + "<=" + max + ")", searchConditionString);
	}
	
	@Test
	public void testRangeSearchConditionStringMinAndMax(){
		String min = "123";
		String max = "456";
		intFacet.setMin(min);
		intFacet.setMax(max);
		String searchConditionString = TableSqlProcessor.createRangeSearchCondition(intFacet, ColumnType.INTEGER);
		assertEquals("(" + intColumnName + " BETWEEN " + min + " AND " + max + ")", searchConditionString);
	}
	
	//////////////////////////////////////
	// appendValueToStringBuilder() Tests
	//////////////////////////////////////
	
	@Test
	public void testAppendValueToStringBuilderStringType(){
		String value = "value asdf 48109-8)(_*()*)(7^*&%$%W$%#%$^^%$%^=";
		String expectedResult = "'"+value+"'";
		TableSqlProcessor.appendValueToStringBuilder(stringBuilder, value, ColumnType.STRING);
		assertEquals(expectedResult, stringBuilder.toString());
	}
	
	@Test
	public void testAppendValueToStringBuilderNonStringType(){
		String value = "682349708";
		String expectedResult = value;
		TableSqlProcessor.appendValueToStringBuilder(stringBuilder, value, ColumnType.INTEGER);
		assertEquals(expectedResult, stringBuilder.toString());
	}
	
	////////////////////////////////////////////////////////////
	// appendFacetWhereClauseToStringBuilderIfNecessary() tests
	////////////////////////////////////////////////////////////
	@Test (expected = IllegalArgumentException.class)
	public void appendFacetWhereClauseToStringBuilderIfNecessaryNullBuilder(){
		TableSqlProcessor.appendFacetWhereClauseToStringBuilderIfNecessary(null, facetSearchConditionString, whereClause);
	}
	
	@Test
	public void appendFacetWhereClauseToStringBuilderIfNecessaryNullFacetSearchConditionStringNullWhereClause(){
		TableSqlProcessor.appendFacetWhereClauseToStringBuilderIfNecessary(stringBuilder, null, null);
		assertEquals(0, stringBuilder.length());
	}
	
	@Test
	public void appendFacetWhereClauseToStringBuilderIfNecessaryNullWhereClause(){
		TableSqlProcessor.appendFacetWhereClauseToStringBuilderIfNecessary(stringBuilder, facetSearchConditionString, null);
		assertEquals(" WHERE "+ facetSearchConditionString , stringBuilder.toString());
	}
	
	@Test
	public void appendFacetWhereClauseToStringBuilderIfNecessaryNullFacetSearchConditionString(){
		TableSqlProcessor.appendFacetWhereClauseToStringBuilderIfNecessary(stringBuilder, null, whereClause);
		assertEquals(" WHERE "+ whereClause.getSearchCondition().toSql(), stringBuilder.toString());
	}
	
	@Test
	public void appendFacetWhereClauseToStringBuilderIfNecessaryNoNulls(){
		TableSqlProcessor.appendFacetWhereClauseToStringBuilderIfNecessary(stringBuilder, facetSearchConditionString, whereClause);
		assertEquals(" WHERE ("+ whereClause.getSearchCondition().toSql() + ") AND (" + facetSearchConditionString + ")", stringBuilder.toString());
	}
	
}
