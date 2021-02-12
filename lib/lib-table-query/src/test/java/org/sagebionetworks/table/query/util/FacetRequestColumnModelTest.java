package org.sagebionetworks.table.query.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Collections;
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
		assertNull(FacetRequestColumnModel.createFacetSearchConditionString(null, false));
	}

	@Test
	public void testCreateFacetSearchConditionString_UsingFacetColumnValuesRequestClass_SingleValueColumn(){
		facetValues.setFacetValues(Sets.newHashSet("hello"));
		assertEquals(FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues),
				FacetRequestColumnModel.createFacetSearchConditionString(facetValues, false));
	}

	@Test
	public void testCreateFacetSearchConditionString_UsingFacetColumnValuesRequestClass_ListColumn(){
		facetValues.setFacetValues(Sets.newHashSet("hello"));
		assertEquals(FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues),
				FacetRequestColumnModel.createFacetSearchConditionString(facetValues, true));
	}

	@Test
	public void testCreateFacetSearchConditionStringUsingFacetColumnRangeRequestClass(){
		facetRange.setMax("123");
		assertEquals(FacetRequestColumnModel.createRangeSearchCondition(facetRange),
				FacetRequestColumnModel.createFacetSearchConditionString(facetRange, false));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateFacetSearchConditionStringUsingUnknownFacetClass(){
		FacetRequestColumnModel.createFacetSearchConditionString(new FacetColumnRequest(){
			public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom)
					throws JSONObjectAdapterException {return null;}
			public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {return null;}
			public String getJSONSchema() {return null;}
			public String getConcreteType() {return null;}
			public FacetColumnRequest setConcreteType(String concreteType) { return this;}
			public String getColumnName() {return null;}
			public FacetColumnRequest setColumnName(String columnName) {return this;}
		}, false);
	}

	////////////////////////////////////////////
	// createSingleValueColumnEnumerationSearchCondition() tests
	///////////////////////////////////////////
	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_NullFacet(){
		assertNull(FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(null));
	}

	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_NullFacetValues(){
		facetValues.setFacetValues(null);
		assertNull(FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues));
	}

	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_EmptyFacetValues(){
		facetValues.setFacetValues(new HashSet<String>());
		assertNull(FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues));
	}

	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_OneValue(){
		String value = "hello";
		facetValues.setFacetValues(Sets.newHashSet(value));
		String searchConditionString = FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\"='hello')", searchConditionString);
	}

	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_OneValueIsNullKeyword(){
		String value = TableConstants.NULL_VALUE_KEYWORD;
		facetValues.setFacetValues(Sets.newHashSet(value));
		String searchConditionString = FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" IS NULL)", searchConditionString);
	}

	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_TwoValues(){
		String value1 = "hello";
		String value2 = "world";
		facetValues.setFacetValues(Sets.newHashSet(value1, value2));


		String searchConditionString = FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\"='hello' OR \"someColumn\"='world')", searchConditionString);
	}

	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_TwoValuesWithOneBeingNullKeyword(){
		String value1 = TableConstants.NULL_VALUE_KEYWORD;
		String value2 = "world";
		facetValues.setFacetValues(Sets.newHashSet(value1, value2));


		String searchConditionString = FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" IS NULL OR \"someColumn\"='world')", searchConditionString);
	}

	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_OneValueContainsSpace(){
		String value = "hello world";
		facetValues.setFacetValues(Sets.newHashSet(value));
		String searchConditionString = FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\"='hello world')", searchConditionString);
	}

	@Test
	public void testEnumerationSearchConditionStringColumnNameWithQuotes() {
		String columnName = "\"quoted\"Column";
		facetValues.setColumnName(columnName);
		facetValues.setFacetValues(Collections.singleton("myValue"));
		String expectedResult = "(\"\"\"quoted\"\"Column\"='myValue')";
		String searchConditionString = FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues);
		assertEquals(expectedResult, searchConditionString);
	}

	////////////////////////////////////////////
	// createListColumnEnumerationSearchCondition() tests
	///////////////////////////////////////////
	@Test
	public void testListColumnEnumerationSearchConditionString_NullFacet(){
		assertNull(FacetRequestColumnModel.createListColumnEnumerationSearchCondition(null));
	}

	@Test
	public void testListColumnEnumerationSearchConditionString_NullFacetValues(){
		facetValues.setFacetValues(null);
		assertNull(FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues));
	}

	@Test
	public void testListColumnEnumerationSearchConditionString_EmptyFacetValues(){
		facetValues.setFacetValues(Collections.emptySet());
		assertNull(FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues));
	}

	@Test
	public void testListColumnEnumerationSearchConditionString_OneValue(){
		String value = "hello";
		facetValues.setFacetValues(Sets.newHashSet(value));
		String searchConditionString = FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" HAS ('hello'))", searchConditionString);
	}

	@Test
	public void testListColumnEnumerationSearchConditionString_OneValueIsNullKeyword(){
		String value = TableConstants.NULL_VALUE_KEYWORD;
		facetValues.setFacetValues(Sets.newHashSet(value));
		String searchConditionString = FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" IS NULL)", searchConditionString);
	}

	@Test
	public void testListColumnEnumerationSearchConditionString_TwoValues(){
		String value1 = "hello";
		String value2 = "world";
		facetValues.setFacetValues(Sets.newHashSet(value1, value2));


		String searchConditionString = FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" HAS ('hello','world'))", searchConditionString);
	}

	@Test
	public void testListColumnEnumerationSearchConditionString_TwoValuesWithNull(){
		String value1 = "hello";
		String value2 = "world";
		facetValues.setFacetValues(Sets.newHashSet(value1, value2, TableConstants.NULL_VALUE_KEYWORD));


		String searchConditionString = FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" HAS ('hello','world') OR \"someColumn\" IS NULL)", searchConditionString);
	}

	@Test
	public void testListColumnEnumerationSearchConditionString_TwoValuesWithOneBeingNullKeyword(){
		String value1 = TableConstants.NULL_VALUE_KEYWORD;
		String value2 = "world";
		facetValues.setFacetValues(Sets.newHashSet(value1, value2));


		String searchConditionString = FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" HAS ('world') OR \"someColumn\" IS NULL)", searchConditionString);
	}

	@Test
	public void testListColumnEnumerationSearchConditionString_OneValueContainsSpace() {
		String value = "hello world";
		facetValues.setFacetValues(Sets.newHashSet(value));
		String searchConditionString = FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" HAS ('hello world'))", searchConditionString);
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

	@Test
	public void testRangeSearchConditionStringMinIsNullValueKeyword(){
		String min = TableConstants.NULL_VALUE_KEYWORD;
		String max = "456";
		facetRange.setMin(min);
		facetRange.setMax(max);
		String searchConditionString = FacetRequestColumnModel.createRangeSearchCondition(facetRange);
		assertEquals("(\"someColumn\" IS NULL)", searchConditionString);
	}

	@Test
	public void testRangeSearchConditionStringMaxIsNullValueKeyword() {
		String min = "123";
		String max = TableConstants.NULL_VALUE_KEYWORD;
		facetRange.setMin(min);
		facetRange.setMax(max);
		String searchConditionString = FacetRequestColumnModel.createRangeSearchCondition(facetRange);
		assertEquals("(\"someColumn\" IS NULL)", searchConditionString);
	}
	@Test
	public void testRangeSearchConditionStringColumnNameWithQuotes() {
		String columnName = "\"quoted\"Column";
		facetRange.setColumnName(columnName);
		facetRange.setMax("42");
		String expectedResult = "(\"\"\"quoted\"\"Column\"<='42')";
		String searchConditionString = FacetRequestColumnModel.createRangeSearchCondition(facetRange);
		assertEquals(expectedResult, searchConditionString);
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
