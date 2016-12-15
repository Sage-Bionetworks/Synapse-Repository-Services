package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.table.query.util.SqlElementUntils;

@RunWith(MockitoJUnitRunner.class)
public class FacetUtilsTest {

	@Mock
	ValidatedQueryFacetColumn mockFacetColumn;
	
	List<ValidatedQueryFacetColumn> validatedQueryFacetColumns;

	String tableId;
	String facetColumnName;

	
	WhereClause whereClause;
	String facetSearchConditionString;
	StringBuilder stringBuilder;
	QuerySpecification simpleModel;
	String columnName;
	
	
	SqlQuery simpleQuery;
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
	

	
	@Before
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
		Mockito.when(mockFacetColumn.getColumnName()).thenReturn(facetColumnName);
		searchCondition1 = "(searchCondition1)";
		
		simpleQuery = new SqlQuery("select * from " + tableId, facetSchema);
		
		supportedFacetColumns = new HashSet<>();
		requestedFacetColumns = new HashSet<>();
	}

	/////////////////////////////////////////////
	// concatFacetSearchConditionStrings() Tests
	/////////////////////////////////////////////
	
	@Test (expected = IllegalArgumentException.class)
	public void testConcatFacetSearchConditionStringsNullFacetColumnsList(){
		FacetUtils.concatFacetSearchConditionStrings(null, facetColumnName);
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
		
		Mockito.when(mockFacetColumn.getSearchConditionString()).thenReturn(searchCondition1);
		validatedQueryFacetColumns.add(mockFacetColumn);
		assertEquals(1, validatedQueryFacetColumns.size());
		
		String result = FacetUtils.concatFacetSearchConditionStrings(validatedQueryFacetColumns, facetColumnName);
		assertNull(result);
	}
	
	@Test
	public void testConcatFacetSearchConditionStringSearchConditionStringIsNull(){
		Mockito.when(mockFacetColumn.getSearchConditionString()).thenReturn(null);
		validatedQueryFacetColumns.add(mockFacetColumn);
		assertEquals(1, validatedQueryFacetColumns.size());
		
		String result = FacetUtils.concatFacetSearchConditionStrings(validatedQueryFacetColumns, null);
		assertNull(result);
	}
	
	@Test
	public void testConcatFacetSearchConditionStringMultipleFacetColumns(){
		Mockito.when(mockFacetColumn.getSearchConditionString()).thenReturn(searchCondition1);
		validatedQueryFacetColumns.add(mockFacetColumn);
		String searchCondition2 = "(searchCondition2)";
		ValidatedQueryFacetColumn mockFacetColumn2 = Mockito.mock(ValidatedQueryFacetColumn.class);
		Mockito.when(mockFacetColumn2.getSearchConditionString()).thenReturn("(searchCondition2)");
		validatedQueryFacetColumns.add(mockFacetColumn2);

		String result = FacetUtils.concatFacetSearchConditionStrings(validatedQueryFacetColumns, null);
		assertEquals("(" + searchCondition1 + " AND " + searchCondition2 + ")", result);
	}

}
