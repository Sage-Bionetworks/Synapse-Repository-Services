package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;


public class ValidatedQueryFacetColumnTest {
	String columnName;
	FacetColumnValuesRequest facetValues;
	FacetColumnRangeRequest facetRange;
	@Before
	public void setUp(){
		columnName = "someColumn";
		facetValues = new FacetColumnValuesRequest();
		facetValues.setColumnName(columnName);
		facetRange = new FacetColumnRangeRequest();
		facetRange.setColumnName(columnName);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testConstructorNullColumnName() {
		new ValidatedQueryFacetColumn(null, FacetType.enumeration, facetValues);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testConstructorNullFacetType() {
		new ValidatedQueryFacetColumn(columnName, null, facetValues);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testConstructorWrongParameterForEnumeration(){
		new ValidatedQueryFacetColumn(columnName, FacetType.enumeration, facetRange);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testConstructorWrongParameterForRange(){
		new ValidatedQueryFacetColumn(columnName, FacetType.range, facetValues);
	}
	
	@Test
	public void testConstructorNullFacetRequest(){
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.range, null);
		assertEquals(columnName, validatedQueryFacetColumn.getColumnName());
		assertNull(validatedQueryFacetColumn.getFacetColumnRequest());
		assertNull(validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testConstructorForEnumerationNoSearchCondition(){
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.enumeration, facetValues);
		assertEquals(columnName, validatedQueryFacetColumn.getColumnName());
		
		assertFalse(validatedQueryFacetColumn.getFacetColumnRequest() instanceof FacetColumnRangeRequest);
		assertEquals(facetValues, validatedQueryFacetColumn.getFacetColumnRequest());
		
		assertNull(validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testConstructorForRangeNoSearchCondition(){
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.range, facetRange);
		assertEquals(columnName, validatedQueryFacetColumn.getColumnName());
		
		assertFalse(validatedQueryFacetColumn.getFacetColumnRequest() instanceof FacetColumnValuesRequest);

		assertEquals(facetRange, validatedQueryFacetColumn.getFacetColumnRequest());
		assertNull(validatedQueryFacetColumn.getSearchConditionString());
	}
}
