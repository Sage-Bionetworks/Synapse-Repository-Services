package org.sagebionetworks.table.query.util;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
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
	public void setUp() throws ParseException{
		facetColumnName = "asdf";

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
		assertThrows(IllegalArgumentException.class, () -> {			
			FacetUtils.concatFacetSearchConditionStrings(null, facetColumnName);
		});
	}
	
	@Test
	public void testConcatFacetSearchConditionStringsNullColumnNameToIgnore(){
		Mockito.when(mockFacetColumn.getSearchConditionString()).thenReturn(searchCondition1);
		validatedQueryFacetColumns.add(mockFacetColumn);
		assertEquals(1, validatedQueryFacetColumns.size());
		
		String result = FacetUtils.concatFacetSearchConditionStrings(validatedQueryFacetColumns, null);
		assertEquals("(" + searchCondition1 + ")", result);
	}
	
	@Test 
	public void testConcatFacetSearchConditionStringsOnlyFacetInListIsIgnored(){
		Mockito.when(mockFacetColumn.getColumnName()).thenReturn(facetColumnName);
		validatedQueryFacetColumns.add(mockFacetColumn);
		assertEquals(1, validatedQueryFacetColumns.size());
		
		String columnNameExpressionToIgnore = FacetUtils.getColumnNameExpression(facetColumnName, null);
		String result = FacetUtils.concatFacetSearchConditionStrings(validatedQueryFacetColumns, columnNameExpressionToIgnore);
		assertNull(result);
	}
	
	@Test 
	public void testConcatFacetSearchConditionStringsOnlyFacetInListIsIgnoredWithJsonPath(){
		when(mockFacetColumn.getColumnName()).thenReturn(facetColumnName);
		when(mockFacetColumn.getJsonPath()).thenReturn("$.a");
		
		validatedQueryFacetColumns.add(mockFacetColumn);
		assertEquals(1, validatedQueryFacetColumns.size());
		
		String columnNameExpressionToIgnore = FacetUtils.getColumnNameExpression(facetColumnName, "$.a");
		String result = FacetUtils.concatFacetSearchConditionStrings(validatedQueryFacetColumns, columnNameExpressionToIgnore);
		assertNull(result);
	}
	
	@Test
	public void testConcatFacetSearchConditionStringSearchConditionStringIsNull(){
		when(mockFacetColumn.getSearchConditionString()).thenReturn(null);
		validatedQueryFacetColumns.add(mockFacetColumn);
		assertEquals(1, validatedQueryFacetColumns.size());
		
		String result = FacetUtils.concatFacetSearchConditionStrings(validatedQueryFacetColumns, null);
		assertNull(result);
	}
	
	@Test
	public void testConcatFacetSearchConditionStringMultipleFacetColumns(){
		when(mockFacetColumn.getSearchConditionString()).thenReturn(searchCondition1);
		validatedQueryFacetColumns.add(mockFacetColumn);
		String searchCondition2 = "(searchCondition2)";
		FacetRequestColumnModel mockFacetColumn2 = Mockito.mock(FacetRequestColumnModel.class);
		when(mockFacetColumn2.getSearchConditionString()).thenReturn("(searchCondition2)");
		validatedQueryFacetColumns.add(mockFacetColumn2);

		String result = FacetUtils.concatFacetSearchConditionStrings(validatedQueryFacetColumns, null);
		assertEquals("(" + searchCondition1 + " AND " + searchCondition2 + ")", result);
	}
	
	/////////////////////////////////////////////////////////
	// appendFacetSearchConditionToQuerySpecification() test
	/////////////////////////////////////////////////////////
	
	@Test
	public void testAppendFacetSearchConditionToQuerySpecification() throws ParseException {
		when(mockFacetColumn.getSearchConditionString()).thenReturn(searchCondition1);
		validatedQueryFacetColumns.add(mockFacetColumn);
		WhereClause modifiedSql = FacetUtils.appendFacetSearchConditionToQuerySpecification(
				simpleModel.getTableExpression().getWhereClause(), validatedQueryFacetColumns);
		assertEquals("WHERE ( i LIKE 'trains' ) AND ( ( ( searchCondition = asdf ) ) )",
				modifiedSql.toSql());
	}
	
	@Test
	public void testGetColumnNameExpressionWithNoJsonPath() {
		String jsonPath = null;
		
		assertEquals("\"foo bar\"", FacetUtils.getColumnNameExpression("foo bar", jsonPath));
	}
	
	@Test
	public void testGetColumnNameExpressionWithJsonPath() {
		String jsonPath = "$.a";
		
		assertEquals("JSON_UNQUOTE(JSON_EXTRACT(\"foo bar\",'$.a'))", FacetUtils.getColumnNameExpression("foo bar", jsonPath));
	}
}
