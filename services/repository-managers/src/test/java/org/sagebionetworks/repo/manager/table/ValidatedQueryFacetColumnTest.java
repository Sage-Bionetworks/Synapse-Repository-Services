package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;


public class ValidatedQueryFacetColumnTest {
	String columnId;
	String columnName;
	ColumnModel columnModel;
	FacetColumnValuesRequest facetValues;
	FacetColumnRangeRequest facetRange;
	@Before
	public void setUp(){
		columnName = "someColumn";
		columnId = "123";
		facetValues = new FacetColumnValuesRequest();
		facetValues.setColumnName(columnName);
		facetRange = new FacetColumnRangeRequest();
		facetRange.setColumnName(columnName);
		columnModel = new ColumnModel();
		columnModel.setId(columnId);
		columnModel.setName(columnName);
		columnModel.setColumnType(ColumnType.STRING);
		columnModel.setFacetType(FacetType.enumeration);
		
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testConstructorNullColumnModel() {
		new ValidatedQueryFacetColumn(null, facetValues);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testConstructorNullName() {
		columnModel.setName(null);
		new ValidatedQueryFacetColumn(columnModel, facetValues);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testConstructorNullFacetType() {
		columnModel.setFacetType(null);
		new ValidatedQueryFacetColumn(columnModel, facetValues);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorNullColumnType(){
		columnModel.setColumnType(null);
		new ValidatedQueryFacetColumn(columnModel, facetValues);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorNonmatchingColumnNames(){
		columnModel.setName("wrongName");
		new ValidatedQueryFacetColumn(columnModel, facetValues);
	}	
	
	@Test (expected = IllegalArgumentException.class)
	public void testConstructorWrongParameterForEnumeration(){
		columnModel.setFacetType(FacetType.enumeration);
		new ValidatedQueryFacetColumn(columnModel, facetRange);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testConstructorWrongParameterForRange(){
		columnModel.setFacetType(FacetType.range);
		new ValidatedQueryFacetColumn(columnModel, facetValues);
	}
	
	@Test
	public void testConstructorNullFacetRequest(){
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnModel, null);
		assertEquals(columnName, validatedQueryFacetColumn.getColumnName());
		assertNull(validatedQueryFacetColumn.getFacetColumnRequest());
		assertNull(validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testConstructorForEnumerationNoSearchCondition(){
		columnModel.setFacetType(FacetType.enumeration);
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnModel, facetValues);
		assertEquals(columnName, validatedQueryFacetColumn.getColumnName());
		
		assertFalse(validatedQueryFacetColumn.getFacetColumnRequest() instanceof FacetColumnRangeRequest);
		assertEquals(facetValues, validatedQueryFacetColumn.getFacetColumnRequest());
		
		assertNull(validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testConstructorForRangeNoSearchCondition(){
		columnModel.setFacetType(FacetType.range);
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnModel, facetRange);
		assertEquals(columnName, validatedQueryFacetColumn.getColumnName());
		
		assertFalse(validatedQueryFacetColumn.getFacetColumnRequest() instanceof FacetColumnValuesRequest);

		assertEquals(facetRange, validatedQueryFacetColumn.getFacetColumnRequest());
		assertNull(validatedQueryFacetColumn.getSearchConditionString());
	}
}
