package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;

import com.google.common.collect.Sets;


public class ValidatedQueryFacetColumnTest {
	String columnName;
	FacetColumnValuesRequest facetValues;
	FacetColumnRangeRequest facetRange;
	@Before
	public void setUp(){
		columnName = "someColumn";
		facetValues = new FacetColumnValuesRequest();
		facetRange = new FacetColumnRangeRequest();
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
	
	@Test
	public void testEnumerationSearchConditionStringOneValue(){
		String value = "hello";
		facetValues.setFacetValues(Sets.newHashSet(value));
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.enumeration, facetValues);
		assertEquals("(" + columnName + "=" + value + ")", validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testEnumerationSearchConditionStringOneValueIsNullKeyword(){
		String value = ValidatedQueryFacetColumn.NULL_VALUE_KEYWORD;
		facetValues.setFacetValues(Sets.newHashSet(value));
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.enumeration, facetValues);
		assertEquals("(" + columnName + " IS NULL)", validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testEnumerationSearchConditionStringTwoValues(){
		String value1 = "hello";
		String value2 = "world";
		facetValues.setFacetValues(Sets.newHashSet(value1, value2));


		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.enumeration, facetValues);
		assertEquals("(" + columnName + "=" + value1 + " OR " + columnName + "=" + value2 + ")", validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testEnumerationSearchConditionStringTwoValuesWithOneBeingNullKeyword(){
		String value1 = ValidatedQueryFacetColumn.NULL_VALUE_KEYWORD;
		String value2 = "world";
		facetValues.setFacetValues(Sets.newHashSet(value1, value2));


		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.enumeration, facetValues);
		assertEquals("(" + columnName + " IS NULL OR " + columnName + "=" + value2 + ")", validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testEnumerationSearchConditionStringOneValueContainsSpace(){
		String value = "hello world";
		facetValues.setFacetValues(Sets.newHashSet(value));
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.enumeration, facetValues);
		assertEquals("(" + columnName + "='" + value + "')", validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testRangeSearchConditionStringMinOnly(){
		String min = "42";
		facetRange.setMin(min);
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.range, facetRange);
		assertEquals("(" + columnName + ">=" + min + ")", validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testRangeSearchConditionStringMaxOnly(){
		String max = "42";
		facetRange.setMax(max);
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.range, facetRange);
		assertEquals("(" + columnName + "<=" + max + ")", validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testRangeSearchConditionStringMinAndMax(){
		String min = "123";
		String max = "456";
		facetRange.setMin(min);
		facetRange.setMax(max);
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.range, facetRange);
		assertEquals("(" + columnName + " BETWEEN " + min + " AND " + max + ")", validatedQueryFacetColumn.getSearchConditionString());
	}
}
