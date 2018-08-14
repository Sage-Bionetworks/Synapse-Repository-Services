package org.sagebionetworks.table.query.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.google.common.collect.Sets;


public class FacetRequestColumnModelTest {
	String columnId;
	String columnName;
	ColumnModel columnModel;
	FacetColumnValuesRequest facetValues;
	FacetColumnRangeRequest facetRange;
	StringBuilder stringBuilder;

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
		stringBuilder = new StringBuilder();
	}

	@Test (expected = IllegalArgumentException.class)
	public void testConstructorNullColumnModel() {
		new FacetRequestColumnModel(null, facetValues);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testConstructorNullName() {
		columnModel.setName(null);
		new FacetRequestColumnModel(columnModel, facetValues);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testConstructorNullFacetType() {
		columnModel.setFacetType(null);
		new FacetRequestColumnModel(columnModel, facetValues);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorNullColumnType(){
		columnModel.setColumnType(null);
		new FacetRequestColumnModel(columnModel, facetValues);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorNonmatchingColumnNames(){
		columnModel.setName("wrongName");
		new FacetRequestColumnModel(columnModel, facetValues);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testConstructorWrongParameterForEnumeration(){
		columnModel.setFacetType(FacetType.enumeration);
		new FacetRequestColumnModel(columnModel, facetRange);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testConstructorWrongParameterForRange(){
		columnModel.setFacetType(FacetType.range);
		new FacetRequestColumnModel(columnModel, facetValues);
	}

	@Test
	public void testConstructorNullFacetRequest(){
		FacetRequestColumnModel validatedQueryFacetColumn = new FacetRequestColumnModel(columnModel, null);
		assertEquals(columnName, validatedQueryFacetColumn.getColumnName());
		assertNull(validatedQueryFacetColumn.getFacetColumnRequest());
		assertNull(validatedQueryFacetColumn.getSearchConditionString());
	}

	@Test
	public void testConstructorForEnumerationNoSearchCondition(){
		columnModel.setFacetType(FacetType.enumeration);
		FacetRequestColumnModel validatedQueryFacetColumn = new FacetRequestColumnModel(columnModel, facetValues);
		assertEquals(columnName, validatedQueryFacetColumn.getColumnName());

		assertFalse(validatedQueryFacetColumn.getFacetColumnRequest() instanceof FacetColumnRangeRequest);
		assertEquals(facetValues, validatedQueryFacetColumn.getFacetColumnRequest());

		assertNull(validatedQueryFacetColumn.getSearchConditionString());
	}

	@Test
	public void testConstructorForRangeNoSearchCondition(){
		columnModel.setFacetType(FacetType.range);
		FacetRequestColumnModel validatedQueryFacetColumn = new FacetRequestColumnModel(columnModel, facetRange);
		assertEquals(columnName, validatedQueryFacetColumn.getColumnName());

		assertFalse(validatedQueryFacetColumn.getFacetColumnRequest() instanceof FacetColumnValuesRequest);

		assertEquals(facetRange, validatedQueryFacetColumn.getFacetColumnRequest());
		assertNull(validatedQueryFacetColumn.getSearchConditionString());
	}


	///////////////////////////////////////////
	// createFacetSearchConditionString() tests
	///////////////////////////////////////////

	@Test
	public void testCreateFacetSearchConditionStringNullFacet(){
		assertNull(FacetRequestColumnModel.createFacetSearchConditionString(null));
	}

	@Test
	public void testCreateFacetSearchConditionStringUsingFacetColumnValuesRequestClass(){
		facetValues.setFacetValues(Sets.newHashSet("hello"));
		assertEquals(FacetRequestColumnModel.createEnumerationSearchCondition(facetValues),
				FacetRequestColumnModel.createFacetSearchConditionString(facetValues));
	}

	@Test
	public void testCreateFacetSearchConditionStringUsingFacetColumnRangeRequestClass(){
		facetRange.setMax("123");
		assertEquals(FacetRequestColumnModel.createRangeSearchCondition(facetRange),
				FacetRequestColumnModel.createFacetSearchConditionString(facetRange));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateFacetSearchConditionStringUsingUnknownFacetClass(){
		FacetRequestColumnModel.createFacetSearchConditionString(new FacetColumnRequest(){
			public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom)
					throws JSONObjectAdapterException {return null;}
			public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {return null;}
			public String getJSONSchema() {return null;}
			public String getConcreteType() {return null;}
			public void setConcreteType(String concreteType) {}
			public String getColumnName() {return null;}
			public void setColumnName(String columnName) {}
		});
	}

	////////////////////////////////////////////
	// createEnumerationSearchCondition() tests
	///////////////////////////////////////////
	@Test
	public void testEnumerationSearchConditionStringNullFacet(){
		assertNull(FacetRequestColumnModel.createEnumerationSearchCondition(null));
	}

	@Test
	public void testEnumerationSearchConditionStringNullFacetValues(){
		facetValues.setFacetValues(null);
		assertNull(FacetRequestColumnModel.createEnumerationSearchCondition(facetValues));
	}

	@Test
	public void testEnumerationSearchConditionStringEmptyFacetValues(){
		facetValues.setFacetValues(new HashSet<String>());
		assertNull(FacetRequestColumnModel.createEnumerationSearchCondition(facetValues));
	}

	@Test
	public void testEnumerationSearchConditionStringOneValue(){
		String value = "hello";
		facetValues.setFacetValues(Sets.newHashSet(value));
		String searchConditionString = FacetRequestColumnModel.createEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\"='hello')", searchConditionString);
	}

	@Test
	public void testEnumerationSearchConditionStringOneValueIsNullKeyword(){
		String value = TableConstants.NULL_VALUE_KEYWORD;
		facetValues.setFacetValues(Sets.newHashSet(value));
		String searchConditionString = FacetRequestColumnModel.createEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" IS NULL)", searchConditionString);
	}

	@Test
	public void testEnumerationSearchConditionStringTwoValues(){
		String value1 = "hello";
		String value2 = "world";
		facetValues.setFacetValues(Sets.newHashSet(value1, value2));


		String searchConditionString = FacetRequestColumnModel.createEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\"='hello' OR \"someColumn\"='world')", searchConditionString);
	}

	@Test
	public void testEnumerationSearchConditionStringTwoValuesWithOneBeingNullKeyword(){
		String value1 = TableConstants.NULL_VALUE_KEYWORD;
		String value2 = "world";
		facetValues.setFacetValues(Sets.newHashSet(value1, value2));


		String searchConditionString = FacetRequestColumnModel.createEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" IS NULL OR \"someColumn\"='world')", searchConditionString);
	}

	@Test
	public void testEnumerationSearchConditionStringOneValueContainsSpace(){
		String value = "hello world";
		facetValues.setFacetValues(Sets.newHashSet(value));
		String searchConditionString = FacetRequestColumnModel.createEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\"='hello world')", searchConditionString);
	}

	//////////////////////////////////////
	// createRangeSearchCondition() tests
	//////////////////////////////////////
	@Test
	public void testRangeSearchConditionNullFacet(){
		assertNull(FacetRequestColumnModel.createRangeSearchCondition(null));
	}

	@Test
	public void testRangeSearchConditionNoMinAndNoMax(){
		facetRange.setMax(null);
		facetRange.setMin("");
		assertNull(FacetRequestColumnModel.createRangeSearchCondition(facetRange));
	}

	@Test
	public void testRangeSearchConditionStringMinOnly(){
		String min = "42";
		facetRange.setMin(min);
		String searchConditionString = FacetRequestColumnModel.createRangeSearchCondition(facetRange);
		assertEquals("(\"someColumn\">='42')", searchConditionString);
	}

	@Test
	public void testRangeSearchConditionStringMaxOnly(){
		String max = "42";
		facetRange.setMax(max);
		String searchConditionString = FacetRequestColumnModel.createRangeSearchCondition(facetRange);
		assertEquals("(\"someColumn\"<='42')", searchConditionString);
	}

	@Test
	public void testRangeSearchConditionStringMinAndMax(){
		String min = "123";
		String max = "456";
		facetRange.setMin(min);
		facetRange.setMax(max);
		String searchConditionString = FacetRequestColumnModel.createRangeSearchCondition(facetRange);
		assertEquals("(\"someColumn\" BETWEEN '123' AND '456')", searchConditionString);
	}

	//////////////////////////////////////
	// appendValueToStringBuilder() Tests
	//////////////////////////////////////

	@Test
	public void testAppendValueToStringBuilderStringType(){
		String value = "value asdf 48109-8)(_*()*)(7^*&%$%W$%#%$^^%$%^=";
		String expectedResult = "'"+value+"'";
		FacetRequestColumnModel.appendValueToStringBuilder(stringBuilder, value);
		assertEquals(expectedResult, stringBuilder.toString());
	}

	@Test
	public void testAppendValueToStringBuilderNonStringType(){
		String value = "682349708";
		String expectedResult = "'"+value+"'";
		FacetRequestColumnModel.appendValueToStringBuilder(stringBuilder, value);
		assertEquals(expectedResult, stringBuilder.toString());
	}

	@Test
	public void testAppendValueToStringBuilderEscapeSingleQuotes(){
		String value = "whomst'd've";
		String expectedResult = "'whomst''d''ve'";
		FacetRequestColumnModel.appendValueToStringBuilder(stringBuilder, value);
		assertEquals(expectedResult, stringBuilder.toString());
	}
}
