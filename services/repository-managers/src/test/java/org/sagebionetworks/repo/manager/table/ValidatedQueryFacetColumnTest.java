package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.table.FacetRange;
import org.sagebionetworks.repo.model.table.FacetType;

public class ValidatedQueryFacetColumnTest {
	String columnName;
	Set<String> columnValues;
	FacetRange facetRange;
	@Before
	public void setUp(){
		columnName = "someColumn";
		columnValues = new HashSet<>();
		facetRange = new FacetRange();
	}

	@Test (expected = IllegalArgumentException.class)
	public void testConstructorNullColumnName() {
		new ValidatedQueryFacetColumn(null, FacetType.enumeration, columnValues, null);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testConstructorNullFacetType() {
		new ValidatedQueryFacetColumn(columnName, null, columnValues, facetRange);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testConstructorWrongParameterForEnumeration(){
		new ValidatedQueryFacetColumn(columnName, FacetType.enumeration, null, facetRange);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testConstructorWrongParameterForRange(){
		new ValidatedQueryFacetColumn(columnName, FacetType.range, columnValues, null);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testConstructorUnexpectedFacetType(){
		new ValidatedQueryFacetColumn(columnName, FacetType.none, null, null);
	}
	
	@Test
	public void testConstructorForEnumerationNoSearchCondition(){
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.enumeration, columnValues, null);
		assertEquals(columnName, validatedQueryFacetColumn.getColumnName());
		
		//the columnValues set should have been copied
		assertTrue(columnValues != validatedQueryFacetColumn.getColumnValues());
		assertEquals(columnValues, validatedQueryFacetColumn.getColumnValues());
		
		assertNull(validatedQueryFacetColumn.getFacetRange());
		assertNull(validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testConstructorForRangeNoSearchCondition(){
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.range, null, facetRange);
		assertEquals(columnName, validatedQueryFacetColumn.getColumnName());
		
		assertNull(validatedQueryFacetColumn.getColumnValues());
		
		//the facetRange should have been copied
		assertTrue(facetRange != validatedQueryFacetColumn.getFacetRange());
		assertEquals(facetRange, validatedQueryFacetColumn.getFacetRange());
		assertNull(validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testEnumerationSearchConditionStringOneValue(){
		String value = "hello";
		columnValues.add(value);
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.enumeration, columnValues, null);
		assertEquals("(" + columnName + "=" + value + ")", validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testEnumerationSearchConditionStringTwoValues(){
		String value1 = "hello";
		String value2 = "world";
		columnValues.add(value1);
		columnValues.add(value2);

		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.enumeration, columnValues, null);
		assertEquals("(" + columnName + "=" + value1 + " OR " + columnName + "=" + value2 + ")", validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testEnumerationSearchConditionStringOneValueContainsSpace(){
		String value = "hello world";
		columnValues.add(value);
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.enumeration, columnValues, null);
		assertEquals("(" + columnName + "='" + value + "')", validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testRangeSearchConditionStringMinOnly(){
		String min = "42";
		facetRange.setMin(min);
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.range, null, facetRange);
		assertEquals("(" + columnName + ">=" + min + ")", validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testRangeSearchConditionStringMaxOnly(){
		String max = "42";
		facetRange.setMax(max);
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.range, null, facetRange);
		assertEquals("(" + columnName + "<=" + max + ")", validatedQueryFacetColumn.getSearchConditionString());
	}
	
	@Test
	public void testRangeSearchConditionStringMinAndMax(){
		String min = "123";
		String max = "456";
		facetRange.setMin(min);
		facetRange.setMax(max);
		ValidatedQueryFacetColumn validatedQueryFacetColumn = new ValidatedQueryFacetColumn(columnName, FacetType.range, null, facetRange);
		assertEquals("(" + columnName + " BETWEEN " + min + " AND " + max + ")", validatedQueryFacetColumn.getSearchConditionString());
	}
}
