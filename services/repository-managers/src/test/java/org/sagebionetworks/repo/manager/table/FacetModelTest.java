package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.query.ParseException;

import com.google.common.collect.Lists;

public class FacetModelTest {

	@Before
	public void setUp() throws Exception {
	}

	/////////////////////////////////////////////
	// createColumnNameToFacetColumnMap() Tests
	/////////////////////////////////////////////
	
	@Test
	public void testCreateColumnNameToFacetColumnMapNullList(){
		Map<String, FacetColumnRequest> map = FacetModel.createColumnNameToFacetColumnMap(null);
		assertNotNull(map);
		assertEquals(0, map.size());
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testCreateColumnNameToFacetColumnMapDuplicateName(){
		//setup
		FacetColumnRequest facetRequest1 = new FacetColumnRangeRequest();
		String sameName = "asdf";
		facetRequest1.setColumnName(sameName);
		FacetColumnRequest facetRequest2 = new FacetColumnRangeRequest();
		facetRequest2.setColumnName(sameName);
		
		FacetModel.createColumnNameToFacetColumnMap(Lists.newArrayList(facetRequest1, facetRequest2));
	}
	
	@Test 
	public void testCreateColumnNameToFacetColumnMap(){
		//setup
		FacetColumnRequest facetRequest1 = new FacetColumnRangeRequest();
		String name1 = "asdf";
		facetRequest1.setColumnName(name1);

		FacetColumnRequest facetRequest2 = new FacetColumnRangeRequest();
		String name2 = "qwerty";
		facetRequest2.setColumnName(name2);
		
		Map<String, FacetColumnRequest> map = FacetModel.createColumnNameToFacetColumnMap(Lists.newArrayList(facetRequest1, facetRequest2));
		assertNotNull(map);
		assertEquals(2, map.size());
		assertEquals(facetRequest1, map.get(name1));
		assertEquals(facetRequest2, map.get(name2));
	}
	
	

	///////////////////////////////////////
	// getFacetFilteredQuery() Tests
	///////////////////////////////////////
	
	@Test
	public void testAppendFacetSearchConditionEmptyFacetColumnsList() throws ParseException{
		SqlQuery query = new SqlQuery("select * from " + tableId, facetSchema);
		
		assertTrue(validatedQueryFacetColumns.isEmpty());
		SqlQuery copy = appendFacetSearchCondition(query, validatedQueryFacetColumns);
	
		assertTrue(query != copy);//not same reference, are different object instances
		assertEquals(query.getOutputSQL(), copy.getOutputSQL()); //but are essentially the same
		
	}
	
	@Test
	public void testAppendFacetSearchConditionNonEmptyFacetColumnsList() throws ParseException{
		SqlQuery query = new SqlQuery("select * from " + tableId + " where asdf <> ayy and asdf < 'taco bell'", facetSchema);
		
		validatedQueryFacetColumns.add(new ValidatedQueryFacetColumn(facetColumnName, FacetType.range, facetRange1));
		
		SqlQuery modifiedQuery = appendFacetSearchCondition(query, validatedQueryFacetColumns);
		assertEquals("SELECT _C"+facetColumnId+"_, ROW_ID, ROW_VERSION FROM T"+KeyFactory.stringToKey(tableId)+" WHERE ( _C"+facetColumnId+"_ <> :b0 AND _C"+facetColumnId+"_ < :b1 ) AND ( ( ( _C"+facetColumnId+"_ BETWEEN :b2 AND :b3 ) ) )", modifiedQuery.getOutputSQL());
		assertEquals(min1, modifiedQuery.getParameters().get("b2"));
		assertEquals(max1, modifiedQuery.getParameters().get("b3"));
	}
	
	/*
	 	@Test
	public void testDetermineAddToValidatedFacetListColumnModelFacetTypeIsNull(){
		facetColumnModel.setFacetType(null);
		
		determineAddToValidatedFacetList(validatedQueryFacetColumns, facetColumnModel, new FacetColumnRangeRequest(), true);
		assertTrue(validatedQueryFacetColumns.isEmpty());
	}
	
	@Test
	public void testDetermineAddToValidatedFacetListFacetParamsNullReturnFacetsFalse(){
		facetColumnModel.setFacetType(FacetType.range); 
		
		determineAddToValidatedFacetList(validatedQueryFacetColumns, facetColumnModel, null, false);
		assertTrue(validatedQueryFacetColumns.isEmpty());
	}
	
	@Test
	public void testDetermineAddToValidatedFacetListFacetParamsNullReturnFacetsTrue(){
		facetColumnModel.setFacetType(FacetType.range); 
		
		determineAddToValidatedFacetList(validatedQueryFacetColumns, facetColumnModel, null, true);
		assertEquals(1, validatedQueryFacetColumns.size());
		ValidatedQueryFacetColumn validatedQueryFacetColumn = validatedQueryFacetColumns.get(0);
		assertNull(validatedQueryFacetColumn.getFacetColumnRequest());
		assertEquals(facetColumnName, validatedQueryFacetColumn.getColumnName());
		assertEquals(FacetType.range, validatedQueryFacetColumn.getFacetType());
		
	}
	 
	 	@Test
	public void testDetermineAddToValidatedFacetListFacetParamsNotNull(){
		//setup
		facetColumnModel.setFacetType(FacetType.range); 
		
		FacetColumnRangeRequest facetRange = new FacetColumnRangeRequest();
		facetRange.setMin("123");
		facetRange.setMax("456");
		facetRange.setColumnName(facetColumnName);
		
		determineAddToValidatedFacetList(validatedQueryFacetColumns, facetColumnModel, facetRange, false);
		
		assertEquals(1, validatedQueryFacetColumns.size());
		ValidatedQueryFacetColumn validatedQueryFacetColumn = validatedQueryFacetColumns.get(0);
		assertEquals(facetRange, validatedQueryFacetColumn.getFacetColumnRequest());
		assertEquals(facetColumnName, validatedQueryFacetColumn.getColumnName());
		assertEquals(FacetType.range, validatedQueryFacetColumn.getFacetType());
	}
	 
	 
	 	@Test (expected = IllegalArgumentException.class)
	public void testCheckForUnfacetedColumnsInRequestNotAllSuported(){
		supportedFacetColumns.add("a");
		supportedFacetColumns.add("b");
		requestedFacetColumns.addAll(supportedFacetColumns);
		requestedFacetColumns.add("z");
		TableQueryManagerImpl.checkForUnfacetedColumnsInRequest(requestedFacetColumns, supportedFacetColumns);
	}
	
	public void testCheckForUnfacetedColumnsInRequestAllSuported(){
		supportedFacetColumns.add("a");
		supportedFacetColumns.add("b");
		requestedFacetColumns.addAll(supportedFacetColumns);
		TableQueryManagerImpl.checkForUnfacetedColumnsInRequest(requestedFacetColumns, supportedFacetColumns);
	}
	 
	 
	 
	 */

}
