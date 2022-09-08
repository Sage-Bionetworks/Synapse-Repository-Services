package org.sagebionetworks.table.query.util;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnMultiValueFunction;
import org.sagebionetworks.repo.model.table.ColumnMultiValueFunctionQueryFilter;
import org.sagebionetworks.repo.model.table.ColumnSingleValueFilterOperator;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.TextMatchesQueryFilter;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.WhereClause;

@ExtendWith(MockitoExtension.class)
public class FacetUtilsTest {

	@Mock
	FacetRequestColumnModel mockFacetColumn;
	
	List<FacetRequestColumnModel> validatedQueryFacetColumns;

	String tableId;
	String facetColumnName;
	Optional<String> ignoredFacetColumn;

	
	WhereClause whereClause;
	String facetSearchConditionString;
	StringBuilder stringBuilder;
	QuerySpecification simpleModel;
	String columnName;
	
	
	String facetColumnId;
	ColumnModel facetColumnModel;
	List<ColumnModel> facetSchema;
	String min1;
	String max1;
	FacetColumnRangeRequest facetRange1;
	
	String searchCondition1;
	
	Set<String> supportedFacetColumns;
	Set<String> requestedFacetColumns;
	
	@Captor
	ArgumentCaptor<QuerySpecification> queryCaptor;
	

	
	@BeforeEach
	public void setUp() throws ParseException {
		facetColumnName = "asdf";
		ignoredFacetColumn = Optional.of(facetColumnName);

		simpleModel = TableQueryParser.parserQuery("select * from syn123 where i like 'trains'");
		columnName = "burrito";
		
		//facet setup
		facetColumnId = "890";
		validatedQueryFacetColumns = new ArrayList<>();
		facetColumnModel = new ColumnModel();
		facetColumnModel.setName(facetColumnName);
		facetColumnModel.setId(facetColumnId);
		facetColumnModel.setColumnType(ColumnType.STRING);
		facetColumnModel.setMaximumSize(50L);
		facetSchema = new ArrayList<>();
		facetSchema.add(facetColumnModel);
		
		min1 = "23";
		max1 = "56";
		facetRange1 = new FacetColumnRangeRequest();
		facetRange1.setMax(max1);
		facetRange1.setMin(min1);
		
		tableId = "syn123";
		searchCondition1 = "(searchCondition=asdf)";
				
		supportedFacetColumns = new HashSet<>();
		requestedFacetColumns = new HashSet<>();

		whereClause = new WhereClause(SqlElementUtils.createSearchCondition("water=wet AND sky=blue"));
		facetSearchConditionString = "(tabs > spaces)";
		stringBuilder = new StringBuilder();
	}

	/////////////////////////////////////////////
	// concatFacetSearchConditionStrings() Tests
	/////////////////////////////////////////////
	
	@Test
	public void testConcatFacetSearchConditionStringsNullFacetColumnsList(){
		assertThrows(IllegalArgumentException.class,
				() -> FacetUtils.concatFacetSearchConditionStrings(null, ignoredFacetColumn),
				"validatedFacets is required");
	}
	
	@Test
	public void testConcatFacetSearchConditionStringsNullColumnNameToIgnore(){
		ignoredFacetColumn = Optional.empty();
		when(mockFacetColumn.getSearchConditionString()).thenReturn(searchCondition1);

		validatedQueryFacetColumns.add(mockFacetColumn);
		assertEquals(1, validatedQueryFacetColumns.size());
		
		String result = FacetUtils.concatFacetSearchConditionStrings(validatedQueryFacetColumns, ignoredFacetColumn);
		assertEquals("(" + searchCondition1 + ")", result);
	}
	
	@Test 
	public void testConcatFacetSearchConditionStringsOnlyFacetInListIsIgnored(){
		when(mockFacetColumn.getColumnName()).thenReturn(facetColumnName);

		validatedQueryFacetColumns.add(mockFacetColumn);
		assertEquals(1, validatedQueryFacetColumns.size());
		
		String result = FacetUtils.concatFacetSearchConditionStrings(validatedQueryFacetColumns, ignoredFacetColumn);
		assertNull(result);
	}
	
	@Test
	public void testConcatFacetSearchConditionStringSearchConditionStringIsNull(){
		ignoredFacetColumn = Optional.empty();
		when(mockFacetColumn.getSearchConditionString()).thenReturn(null);
		validatedQueryFacetColumns.add(mockFacetColumn);
		assertEquals(1, validatedQueryFacetColumns.size());
		
		String result = FacetUtils.concatFacetSearchConditionStrings(validatedQueryFacetColumns, ignoredFacetColumn);
		assertNull(result);
	}
	
	@Test
	public void testConcatFacetSearchConditionStringMultipleFacetColumns(){
		ignoredFacetColumn = Optional.empty();
		when(mockFacetColumn.getSearchConditionString()).thenReturn(searchCondition1);

		validatedQueryFacetColumns.add(mockFacetColumn);
		String searchCondition2 = "(searchCondition2)";
		FacetRequestColumnModel mockFacetColumn2 = Mockito.mock(FacetRequestColumnModel.class);
		when(mockFacetColumn2.getSearchConditionString()).thenReturn("(searchCondition2)");
		validatedQueryFacetColumns.add(mockFacetColumn2);

		String result = FacetUtils.concatFacetSearchConditionStrings(validatedQueryFacetColumns, ignoredFacetColumn);
		assertEquals("(" + searchCondition1 + " AND " + searchCondition2 + ")", result);
	}
	
	/////////////////////////////////////////////////////////
	// appendFacetSearchConditionToQuerySpecification() test
	/////////////////////////////////////////////////////////
	
	@Test
	public void testAppendFacetSearchConditionToQuerySpecification() throws ParseException{
		when(mockFacetColumn.getSearchConditionString()).thenReturn(searchCondition1);

		validatedQueryFacetColumns.add(mockFacetColumn);
		QuerySpecification modifiedSql = FacetUtils.appendFacetSearchConditionToQuerySpecification(simpleModel, validatedQueryFacetColumns);
		assertEquals("SELECT * FROM syn123 WHERE ( i LIKE 'trains' ) AND ( ( ( searchCondition = asdf ) ) )", modifiedSql.toSql());
	}

	/////////////////////////////////////////////////////////
	// getSearchConditionString(QueryFilter) tests
	/////////////////////////////////////////////////////////
	@Test
	public void getSearchConditionStringForColumnSingleValueQueryFilterOneValue() {
		ColumnSingleValueQueryFilter filter = new ColumnSingleValueQueryFilter();
		filter.setColumnName(facetColumnName);
		filter.setOperator(ColumnSingleValueFilterOperator.LIKE);
		filter.setValues(Arrays.asList("value1"));
		String result = FacetUtils.getSearchConditionString(filter);
		assertEquals("((\"asdf\" LIKE 'value1'))", result);
	}

	@Test
	public void getSearchConditionStringForColumnSingleValueQueryFilterMultiValue() {
		ColumnSingleValueQueryFilter filter = new ColumnSingleValueQueryFilter();
		filter.setColumnName(facetColumnName);
		filter.setOperator(ColumnSingleValueFilterOperator.LIKE);
		filter.setValues(Arrays.asList("value1", "%value2%"));
		String result = FacetUtils.getSearchConditionString(filter);
		assertEquals("((\"asdf\" LIKE 'value1') OR (\"asdf\" LIKE '%value2%'))", result);
	}

	@Test
	public void getSearchConditionStringForColumnMultiValueFunctionQueryFilterOneValue() {
		ColumnMultiValueFunctionQueryFilter filter = new ColumnMultiValueFunctionQueryFilter();
		filter.setColumnName(facetColumnName);
		filter.setFunction(ColumnMultiValueFunction.HAS);
		filter.setValues(Arrays.asList("value1"));
		String result = FacetUtils.getSearchConditionString(filter);
		assertEquals("\"asdf\" HAS('value1')", result);
	}

	@Test
	public void getSearchConditionStringForColumnMultiValueFunctionQueryFilterMultiValue() {
		ColumnMultiValueFunctionQueryFilter filter = new ColumnMultiValueFunctionQueryFilter();
		filter.setColumnName(facetColumnName);
		filter.setFunction(ColumnMultiValueFunction.HAS_LIKE);
		filter.setValues(Arrays.asList("value1", "%value2%"));
		String result = FacetUtils.getSearchConditionString(filter);
		assertEquals("\"asdf\" HAS_LIKE('value1', '%value2%')", result);
	}

	@Test
	public void getSearchConditionStringFoTextMatchesQueryFilter() {
		TextMatchesQueryFilter filter = new TextMatchesQueryFilter();
		filter.setSearchExpression("value1");
		String result = FacetUtils.getSearchConditionString(filter);
		assertEquals("TEXT_MATCHES('value1')", result);
	}

	/////////////////////////////////////////////
	// concatQueryFilterConditionStrings() Tests
	/////////////////////////////////////////////

	@Test
	public void testConcatQueryFilterConditionStringsNull(){
		String result = FacetUtils.concatQueryFilterConditionStrings(null);
		assertNull(result);
	}

	@Test
	public void testConcatQueryFilterConditionStringsEmpty(){
		String result = FacetUtils.concatQueryFilterConditionStrings(Collections.emptyList());
		assertNull(result);
	}

	@Test
	public void testConcatQueryFilterConditionStringsMultipleFilters(){
		ColumnSingleValueQueryFilter singleValueQueryFilter = new ColumnSingleValueQueryFilter();
		singleValueQueryFilter.setColumnName(facetColumnName);
		singleValueQueryFilter.setOperator(ColumnSingleValueFilterOperator.LIKE);
		singleValueQueryFilter.setValues(Arrays.asList("a", "%b%", "c%"));

		ColumnMultiValueFunctionQueryFilter multiValueFunctionQueryFilter = new ColumnMultiValueFunctionQueryFilter();
		multiValueFunctionQueryFilter.setColumnName("qwerty");
		multiValueFunctionQueryFilter.setFunction(ColumnMultiValueFunction.HAS_LIKE);
		multiValueFunctionQueryFilter.setValues(Arrays.asList("%uiop"));

		TextMatchesQueryFilter textMatchesQueryFilter = new TextMatchesQueryFilter();
		textMatchesQueryFilter.setSearchExpression("search terms");


		String result = FacetUtils.concatQueryFilterConditionStrings(Arrays.asList(singleValueQueryFilter, multiValueFunctionQueryFilter, textMatchesQueryFilter));
		assertEquals("(((\"asdf\" LIKE 'a') OR (\"asdf\" LIKE '%b%') OR (\"asdf\" LIKE 'c%')) AND \"qwerty\" HAS_LIKE('%uiop') AND TEXT_MATCHES('search terms'))", result);
	}
}
