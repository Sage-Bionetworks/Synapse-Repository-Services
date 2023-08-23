package org.sagebionetworks.table.query.util;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.JsonSubColumnModel;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;



public class FacetRequestColumnModelTest {
	String columnId;
	String columnName;
	ColumnModel columnModel;
	FacetColumnValuesRequest facetValues;
	FacetColumnRangeRequest facetRange;
	StringBuilder stringBuilder;

	@BeforeEach
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

	@Test
	public void testConstructorNullColumnModel() {
		assertThrows(IllegalArgumentException.class, () -> {			
			new FacetRequestColumnModel(null, facetValues);
		});
	}

	@Test
	public void testConstructorNullName() {
		columnModel.setName(null);
		assertThrows(IllegalArgumentException.class, () -> {
			new FacetRequestColumnModel(columnModel, facetValues);
		});
	}

	@Test
	public void testConstructorNullFacetType() {
		columnModel.setFacetType(null);
		assertThrows(IllegalArgumentException.class, () -> {
			new FacetRequestColumnModel(columnModel, facetValues);
		});
	}

	@Test
	public void testConstructorNullColumnType(){
		columnModel.setColumnType(null);
		assertThrows(IllegalArgumentException.class, () -> {
			new FacetRequestColumnModel(columnModel, facetValues);
		});
	}

	@Test
	public void testConstructorNonmatchingColumnNames(){
		columnModel.setName("wrongName");
		assertThrows(IllegalArgumentException.class, () -> {
			new FacetRequestColumnModel(columnModel, facetValues);
		});
	}

	@Test
	public void testConstructorWrongParameterForEnumeration(){
		columnModel.setFacetType(FacetType.enumeration);
		assertThrows(IllegalArgumentException.class, () -> {
			new FacetRequestColumnModel(columnModel, facetRange);
		});
	}

	@Test
	public void testConstructorWrongParameterForRange(){
		columnModel.setFacetType(FacetType.range);
		assertThrows(IllegalArgumentException.class, () -> {
			new FacetRequestColumnModel(columnModel, facetValues);
		});
	}
	
	@Test
	public void testConstructorWithJsonColumn(){
		JsonSubColumnModel subColumn = new JsonSubColumnModel().setName("foo").setJsonPath("$.bar").setFacetType(FacetType.enumeration).setColumnType(ColumnType.INTEGER);
		
		facetValues.setJsonPath("$.bar");
		facetValues.setFacetValues(Set.of("10"));
		
		FacetRequestColumnModel result = new FacetRequestColumnModel(columnModel.getName(), subColumn, facetValues);
		
		assertEquals("$.bar", result.getJsonPath());
		assertEquals(FacetType.enumeration, result.getFacetType());
		assertEquals("someColumn", result.getColumnName());
		assertEquals("(JSON_EXTRACT(\"someColumn\",'$.bar')=CAST('10' AS INTEGER))", result.getSearchConditionString());
	}
	
	@Test
	public void testConstructorWithJsonColumnAndMissingJsonPath(){
		JsonSubColumnModel subColumn = new JsonSubColumnModel().setName("foo").setJsonPath("$.bar").setFacetType(FacetType.enumeration).setColumnType(ColumnType.INTEGER);
		facetValues.setJsonPath(null);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {
			new FacetRequestColumnModel(columnModel.getName(), subColumn, facetValues);
		}).getMessage();
		
		assertEquals("Unexpected facet request jsonPath (Was 'null', Expected '$.bar')", result);
	}
	
	@Test
	public void testConstructorWithJsonColumnAndMissingFacetType(){
		JsonSubColumnModel subColumn = new JsonSubColumnModel().setName("foo").setJsonPath("$.bar").setFacetType(null).setColumnType(ColumnType.INTEGER);
		facetValues.setJsonPath(null);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {
			new FacetRequestColumnModel(columnModel.getName(), subColumn, facetValues);
		}).getMessage();
		
		assertEquals("subColumn.facetType is required.", result);
	}
	
	@Test
	public void testConstructorWithJsonColumnAndMissingColumnType(){
		JsonSubColumnModel subColumn = new JsonSubColumnModel().setName("foo").setJsonPath("$.bar").setFacetType(FacetType.enumeration).setColumnType(null);
		facetValues.setJsonPath(null);
		
		String result = assertThrows(IllegalArgumentException.class, () -> {
			new FacetRequestColumnModel(columnModel.getName(), subColumn, facetValues);
		}).getMessage();
		
		assertEquals("subColumn.columnType is required.", result);
	}
	
	@Test
	public void testConstructorWithJsonColumnAndJsonPathNotMatching(){
		JsonSubColumnModel subColumn = new JsonSubColumnModel().setName("foo").setJsonPath("$.bar").setFacetType(FacetType.enumeration).setColumnType(ColumnType.INTEGER);
		
		facetValues.setJsonPath("$.foo");
		
		String result = assertThrows(IllegalArgumentException.class, () -> {
			new FacetRequestColumnModel(columnModel.getName(), subColumn, facetValues);
		}).getMessage();
		
		assertEquals("Unexpected facet request jsonPath (Was '$.foo', Expected '$.bar')", result);
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
		assertNull(FacetRequestColumnModel.createFacetSearchConditionString(null, false, null));
	}

	@Test
	public void testCreateFacetSearchConditionString_UsingFacetColumnValuesRequestClass_SingleValueColumn(){
		facetValues.setFacetValues(Set.of("hello"));
		assertEquals(FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues, null),
				FacetRequestColumnModel.createFacetSearchConditionString(facetValues, false, null));
	}
	
	@Test
	public void testCreateFacetSearchConditionStringWithFacetColumnValuesAndJsonPath(){
		facetValues.setJsonPath("$.foo");
		facetValues.setFacetValues(Set.of("hello"));
		assertEquals(FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues, null),
				FacetRequestColumnModel.createFacetSearchConditionString(facetValues, false, null));
	}

	@Test
	public void testCreateFacetSearchConditionString_UsingFacetColumnValuesRequestClass_ListColumn(){
		facetValues.setFacetValues(Set.of("hello"));
		assertEquals(FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues),
				FacetRequestColumnModel.createFacetSearchConditionString(facetValues, true, null));
	}

	@Test
	public void testCreateFacetSearchConditionStringUsingFacetColumnRangeRequestClass(){
		facetRange.setMax("123");
		assertEquals(FacetRequestColumnModel.createRangeSearchCondition(facetRange, null),
				FacetRequestColumnModel.createFacetSearchConditionString(facetRange, false, null));
	}
	
	@Test
	public void testCreateFacetSearchConditionStringWithFacetColumnRangeRequestAndJsonPath(){
		facetRange.setJsonPath("$.foo");
		facetRange.setMax("123");
		assertEquals(FacetRequestColumnModel.createRangeSearchCondition(facetRange, null),
				FacetRequestColumnModel.createFacetSearchConditionString(facetRange, false, null));
	}

	@Test
	public void testCreateFacetSearchConditionStringUsingUnknownFacetClass(){
		assertThrows(IllegalArgumentException.class, () -> {
			FacetRequestColumnModel.createFacetSearchConditionString(new FacetColumnRequest(){
				public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter toInitFrom)
						throws JSONObjectAdapterException {return null;}
				public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter writeTo) throws JSONObjectAdapterException {return null;}
				public String getJSONSchema() {return null;}
				public String getConcreteType() {return null;}
				public FacetColumnRequest setConcreteType(String concreteType) { return this;}
				public String getColumnName() {return null;}
				public FacetColumnRequest setColumnName(String columnName) {return this;}
				public String getJsonPath() {return null;}
				public FacetColumnRequest setJsonPath(String jsonPath) {return null;}
			}, false, null);
		});
	}

	////////////////////////////////////////////
	// createSingleValueColumnEnumerationSearchCondition() tests
	///////////////////////////////////////////
	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_NullFacet(){
		assertNull(FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(null, null));
	}

	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_NullFacetValues(){
		facetValues.setFacetValues(null);
		assertNull(FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues, null));
	}

	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_EmptyFacetValues(){
		facetValues.setFacetValues(new HashSet<String>());
		assertNull(FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues, null));
	}

	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_OneValue(){
		String value = "hello";
		facetValues.setFacetValues(Set.of(value));
		String searchConditionString = FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues, null);
		assertEquals("(\"someColumn\"='hello')", searchConditionString);
	}

	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_OneValueIsNullKeyword(){
		String value = TableConstants.NULL_VALUE_KEYWORD;
		facetValues.setFacetValues(Set.of(value));
		String searchConditionString = FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues, null);
		assertEquals("(\"someColumn\" IS NULL)", searchConditionString);
	}

	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_TwoValues(){
		String value1 = "hello";
		String value2 = "world";
		facetValues.setFacetValues(new LinkedHashSet<>(List.of(value1, value2)));


		String searchConditionString = FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues, null);
		assertEquals("(\"someColumn\"='hello' OR \"someColumn\"='world')", searchConditionString);
	}

	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_TwoValuesWithOneBeingNullKeyword(){
		String value1 = TableConstants.NULL_VALUE_KEYWORD;
		String value2 = "world";
		facetValues.setFacetValues(new LinkedHashSet<>(List.of(value1, value2)));


		String searchConditionString = FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues, null);
		assertEquals("(\"someColumn\" IS NULL OR \"someColumn\"='world')", searchConditionString);
	}

	@Test
	public void testSingleValueColumnEnumerationSearchConditionString_OneValueContainsSpace(){
		String value = "hello world";
		facetValues.setFacetValues(Set.of(value));
		String searchConditionString = FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues, null);
		assertEquals("(\"someColumn\"='hello world')", searchConditionString);
	}
	
	@Test
	public void testSingleValueColumnEnumerationSearchConditionStringWithJsonPath(){
		String value = "hello world";
		facetValues.setFacetValues(Set.of(value));
		facetValues.setJsonPath("$.foo");
		String searchConditionString = FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues, null);
		assertEquals("(JSON_EXTRACT(\"someColumn\",'$.foo')='hello world')", searchConditionString);
	}
	
	@Test
	public void testSingleValueColumnEnumerationSearchConditionStringWithJsonPathAndTargetType(){
		String value = "hello world";
		facetValues.setFacetValues(Set.of(value));
		facetValues.setJsonPath("$.foo");
		String searchConditionString = FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues, ColumnType.STRING);
		assertEquals("(JSON_EXTRACT(\"someColumn\",'$.foo')=CAST('hello world' AS STRING))", searchConditionString);
	}

	@Test
	public void testEnumerationSearchConditionStringColumnNameWithQuotes() {
		String columnName = "\"quoted\"Column";
		facetValues.setColumnName(columnName);
		facetValues.setFacetValues(Collections.singleton("myValue"));
		String expectedResult = "(\"\"\"quoted\"\"Column\"='myValue')";
		String searchConditionString = FacetRequestColumnModel.createSingleValueColumnEnumerationSearchCondition(facetValues, null);
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
		facetValues.setFacetValues(Set.of(value));
		String searchConditionString = FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" HAS ('hello'))", searchConditionString);
	}

	@Test
	public void testListColumnEnumerationSearchConditionString_OneValueIsNullKeyword(){
		String value = TableConstants.NULL_VALUE_KEYWORD;
		facetValues.setFacetValues(Set.of(value));
		String searchConditionString = FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" IS NULL)", searchConditionString);
	}

	@Test
	public void testListColumnEnumerationSearchConditionString_TwoValues(){
		String value1 = "hello";
		String value2 = "world";
		facetValues.setFacetValues(new LinkedHashSet<>(List.of(value1, value2)));


		String searchConditionString = FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" HAS ('hello','world'))", searchConditionString);
	}

	@Test
	public void testListColumnEnumerationSearchConditionString_TwoValuesWithNull(){
		String value1 = "hello";
		String value2 = "world";
		facetValues.setFacetValues(new LinkedHashSet<>(List.of(value1, value2, TableConstants.NULL_VALUE_KEYWORD)));


		String searchConditionString = FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" HAS ('hello','world') OR \"someColumn\" IS NULL)", searchConditionString);
	}

	@Test
	public void testListColumnEnumerationSearchConditionString_TwoValuesWithOneBeingNullKeyword(){
		String value1 = TableConstants.NULL_VALUE_KEYWORD;
		String value2 = "world";
		facetValues.setFacetValues(new LinkedHashSet<>(List.of(value1, value2)));


		String searchConditionString = FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" HAS ('world') OR \"someColumn\" IS NULL)", searchConditionString);
	}

	@Test
	public void testListColumnEnumerationSearchConditionString_OneValueContainsSpace() {
		String value = "hello world";
		facetValues.setFacetValues(new LinkedHashSet<>(List.of(value)));
		String searchConditionString = FacetRequestColumnModel.createListColumnEnumerationSearchCondition(facetValues);
		assertEquals("(\"someColumn\" HAS ('hello world'))", searchConditionString);
	}

	//////////////////////////////////////
	// createRangeSearchCondition() tests
	//////////////////////////////////////
	@Test
	public void testRangeSearchConditionNullFacet(){
		assertNull(FacetRequestColumnModel.createRangeSearchCondition(null, null));
	}

	@Test
	public void testRangeSearchConditionNoMinAndNoMax(){
		facetRange.setMax(null);
		facetRange.setMin("");
		assertNull(FacetRequestColumnModel.createRangeSearchCondition(facetRange, null));
	}

	@Test
	public void testRangeSearchConditionStringMinOnly(){
		String min = "42";
		facetRange.setMin(min);
		String searchConditionString = FacetRequestColumnModel.createRangeSearchCondition(facetRange, null);
		assertEquals("(\"someColumn\">='42')", searchConditionString);
	}

	@Test
	public void testRangeSearchConditionStringMaxOnly(){
		String max = "42";
		facetRange.setMax(max);
		String searchConditionString = FacetRequestColumnModel.createRangeSearchCondition(facetRange, null);
		assertEquals("(\"someColumn\"<='42')", searchConditionString);
	}

	@Test
	public void testRangeSearchConditionStringMinAndMax(){
		String min = "123";
		String max = "456";
		facetRange.setMin(min);
		facetRange.setMax(max);
		String searchConditionString = FacetRequestColumnModel.createRangeSearchCondition(facetRange, null);
		assertEquals("(\"someColumn\" BETWEEN '123' AND '456')", searchConditionString);
	}

	@Test
	public void testRangeSearchConditionStringMinIsNullValueKeyword(){
		String min = TableConstants.NULL_VALUE_KEYWORD;
		String max = "456";
		facetRange.setMin(min);
		facetRange.setMax(max);
		String searchConditionString = FacetRequestColumnModel.createRangeSearchCondition(facetRange, null);
		assertEquals("(\"someColumn\" IS NULL)", searchConditionString);
	}

	@Test
	public void testRangeSearchConditionStringMaxIsNullValueKeyword() {
		String min = "123";
		String max = TableConstants.NULL_VALUE_KEYWORD;
		facetRange.setMin(min);
		facetRange.setMax(max);
		String searchConditionString = FacetRequestColumnModel.createRangeSearchCondition(facetRange, null);
		assertEquals("(\"someColumn\" IS NULL)", searchConditionString);
	}
	@Test
	public void testRangeSearchConditionStringColumnNameWithQuotes() {
		String columnName = "\"quoted\"Column";
		facetRange.setColumnName(columnName);
		facetRange.setMax("42");
		String expectedResult = "(\"\"\"quoted\"\"Column\"<='42')";
		String searchConditionString = FacetRequestColumnModel.createRangeSearchCondition(facetRange, null);
		assertEquals(expectedResult, searchConditionString);
	}
	
	@Test
	public void testRangeSearchConditionStringWithJsonPath(){
		String min = "123";
		String max = "456";
		facetRange.setJsonPath("$.foo");
		facetRange.setMin(min);
		facetRange.setMax(max);
		String searchConditionString = FacetRequestColumnModel.createRangeSearchCondition(facetRange, null);
		assertEquals("(JSON_EXTRACT(\"someColumn\",'$.foo') BETWEEN '123' AND '456')", searchConditionString);
	}
	
	@Test
	public void testRangeSearchConditionStringWithJsonPathAndTargetType(){
		String min = "123";
		String max = "456";
		facetRange.setJsonPath("$.foo");
		facetRange.setMin(min);
		facetRange.setMax(max);
		String searchConditionString = FacetRequestColumnModel.createRangeSearchCondition(facetRange, ColumnType.INTEGER);
		assertEquals("(JSON_EXTRACT(\"someColumn\",'$.foo') BETWEEN CAST('123' AS INTEGER) AND CAST('456' AS INTEGER))", searchConditionString);
	}

	//////////////////////////////////////
	// appendValueToStringBuilder() Tests
	//////////////////////////////////////

	@Test
	public void testAppendValueToStringBuilderStringType(){
		String value = "value asdf 48109-8)(_*()*)(7^*&%$%W$%#%$^^%$%^=";
		String expectedResult = "'"+value+"'";
		FacetRequestColumnModel.appendValueToStringBuilder(stringBuilder, value, null);
		assertEquals(expectedResult, stringBuilder.toString());
	}

	@Test
	public void testAppendValueToStringBuilderNonStringType(){
		String value = "682349708";
		String expectedResult = "'"+value+"'";
		FacetRequestColumnModel.appendValueToStringBuilder(stringBuilder, value, null);
		assertEquals(expectedResult, stringBuilder.toString());
	}

	@Test
	public void testAppendValueToStringBuilderEscapeSingleQuotes(){
		String value = "whomst'd've";
		String expectedResult = "'whomst''d''ve'";
		FacetRequestColumnModel.appendValueToStringBuilder(stringBuilder, value, null);
		assertEquals(expectedResult, stringBuilder.toString());
	}
	
	@Test
	public void testAppendValueToStringWithTargetType(){
		String value = "10";
		String expectedResult = "CAST('10' AS INTEGER)";
		FacetRequestColumnModel.appendValueToStringBuilder(stringBuilder, value, ColumnType.INTEGER);
		assertEquals(expectedResult, stringBuilder.toString());
	}
}
