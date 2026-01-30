package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.table.TableConstants.NULL_VALUE_KEYWORD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnResultValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultValues;
import org.sagebionetworks.repo.model.table.FacetColumnSortConfig;
import org.sagebionetworks.repo.model.table.FacetColumnSortDirection;
import org.sagebionetworks.repo.model.table.FacetColumnSortProperty;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.JsonSubColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.TranslationDependencies;
import org.sagebionetworks.table.cluster.description.IndexDescriptionLookup;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.cluster.description.VirtualTableIndexDescription;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.util.FacetRequestColumnModel;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class FacetTransformerValueCountsTest {
	private String selectedValue;
	private String notSelectedValue;
	private List<ColumnModel> schema;
	private QueryExpression originalQuery;
	private TranslationDependencies dependencies;
	private String originalSearchCondition;
	private List<FacetRequestColumnModel> facets;
	private RowSet rowSet;
	private List<SelectColumn> correctSelectList;
	private Set<String> selectedValuesSet;
	private ColumnModel stringModel;
	private ColumnModel stringListModel;
	private ColumnModel jsonColumnModel;
	private FacetColumnSortConfig facetSortConfig;
	private Long userId;
	
	
	@Mock
	private SchemaProvider mockSchemaProvider;
	@Mock
	private IndexDescriptionLookup mockLookup;

	
	@BeforeEach
	public void before() throws ParseException{
		stringModel = new ColumnModel();
		stringModel.setName("stringColumn");
		stringModel.setColumnType(ColumnType.STRING);
		stringModel.setId("1");
		stringModel.setFacetType(FacetType.enumeration);
		stringModel.setMaximumSize(50L);

		stringListModel = new ColumnModel();
		stringListModel.setName("stringListColumn");
		stringListModel.setColumnType(ColumnType.STRING_LIST);
		stringListModel.setId("2");
		stringListModel.setFacetType(FacetType.enumeration);
		stringListModel.setMaximumSize(50L);
		stringListModel.setMaximumListLength(24L);
		
		jsonColumnModel = new ColumnModel();
		jsonColumnModel.setName("jsonColumn");
		jsonColumnModel.setColumnType(ColumnType.JSON);
		jsonColumnModel.setId("3");
		jsonColumnModel.setJsonSubColumns(List.of(
			new JsonSubColumnModel().setName("a").setJsonPath("$.a").setFacetType(FacetType.enumeration).setColumnType(ColumnType.INTEGER)
		));

		schema = Arrays.asList(stringModel, stringListModel, jsonColumnModel);

		facets = new ArrayList<>();
		selectedValue = "selectedValue";
		notSelectedValue = "notSelectedValue";
		FacetColumnValuesRequest valuesRequest = new FacetColumnValuesRequest();
		valuesRequest.setColumnName(stringModel.getName());
		selectedValuesSet = Sets.newHashSet(selectedValue);
		valuesRequest.setFacetValues(selectedValuesSet);
		facets.add(new FacetRequestColumnModel(schema.get(0), valuesRequest));//use column "i0"

		userId = 1L;

		originalSearchCondition = "\"stringColumn\" LIKE 'asdf%'";
		
		dependencies = TranslationDependencies.builder().setSchemaProvider(mockSchemaProvider)
				.setIndexDescription(new TableIndexDescription(IdAndVersion.parse("syn123"))).setUserId(userId).build();
		
		originalQuery = new TableQueryParser("select * FROM syn123 WHERE " + originalSearchCondition).queryExpression();
		
		rowSet = new RowSet();
		
		SelectColumn col1 = new SelectColumn();
		col1.setName(FacetTransformerValueCounts.VALUE_ALIAS);
		SelectColumn col2 = new SelectColumn();
		col2.setName(FacetTransformerValueCounts.COUNT_ALIAS);
		correctSelectList = Lists.newArrayList(col1, col2);
		
		facetSortConfig = FacetTransformerValueCounts.DEFAULT_SORT_CONFIG;
	}
	
	static Stream<FacetColumnSortConfig> facetColumnSortConfigFactory(){
		return Arrays.stream(FacetColumnSortProperty.values())
			.flatMap(property -> Arrays.stream(FacetColumnSortDirection.values())
				.map(new FacetColumnSortConfig().setProperty(property)::setDirection)
			);
	}
	
	@ParameterizedTest
	@MethodSource("facetColumnSortConfigFactory")
	public void testGetOrderBy(FacetColumnSortConfig sortConfig) {
		String expected = null;
		
		if (sortConfig.getProperty() == FacetColumnSortProperty.FREQUENCY) {
			expected = "frequency " + sortConfig.getDirection() + ", value ASC";
		} else if (sortConfig.getProperty() == FacetColumnSortProperty.VALUE) {
			expected = "value " + sortConfig.getDirection();
		} else {
			throw new IllegalArgumentException("Unknown FacetColumnSortProperty: " + sortConfig.getProperty());
		}
		
		assertEquals(expected, FacetTransformerValueCounts.getOrderBy(sortConfig));
	}
	
	/////////////////////////////////
	// constructor tests()
	/////////////////////////////////
	
	@Test
	public void testConstructor() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		
		FacetTransformerValueCounts facetTransformer = new FacetTransformerValueCounts(stringModel.getName(), facetSortConfig, null, null, false, facets, originalQuery, dependencies, selectedValuesSet);

		assertEquals(stringModel.getName(), ReflectionTestUtils.getField(facetTransformer, "columnName"));
		assertEquals(facets, ReflectionTestUtils.getField(facetTransformer, "facets"));
		assertEquals(selectedValuesSet, ReflectionTestUtils.getField(facetTransformer, "selectedValues"));
		
		
		//this is tested in a separate test
		assertNotNull(ReflectionTestUtils.getField(facetTransformer, "generatedFacetSqlQuery"));
	}
	
	@Test
	public void testConstructorWithDefaultSortConfig() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		
		facetSortConfig = null;
		
		FacetTransformerValueCounts facetTransformer = new FacetTransformerValueCounts(stringModel.getName(), facetSortConfig, null, null, false, facets, originalQuery, dependencies, selectedValuesSet);
		
		assertEquals(FacetTransformerValueCounts.DEFAULT_SORT_CONFIG, ReflectionTestUtils.getField(facetTransformer, "sortConfig"));
	}
	
	/////////////////////////////////
	// generateFacetSqlQuery tests()
	/////////////////////////////////
	@ParameterizedTest
	@MethodSource("facetColumnSortConfigFactory")
	public void testGenerateFacetSqlQuery(FacetColumnSortConfig facetSortConfig) {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		
		FacetTransformerValueCounts facetTransformer = new FacetTransformerValueCounts(stringModel.getName(), facetSortConfig, null, null, false, facets, originalQuery, dependencies, selectedValuesSet);

		//check the non-transformed sql
		String expectedString = "SELECT \"stringColumn\" AS value, COUNT(*) AS frequency"
				+ " FROM syn123"
				+ " WHERE \"stringColumn\" LIKE 'asdf%'"
				+ " GROUP BY \"stringColumn\""
				+ " ORDER BY " + FacetTransformerValueCounts.getOrderBy(facetSortConfig)
				+ " LIMIT 500";
		assertEquals(expectedString, facetTransformer.getFacetSqlQuery().getInputSql());
		
		//transformed model will be correct if schema and non-transformed query are correct
		//because it is handled by SqlQuery Constructor
	}
	
	@ParameterizedTest
	@MethodSource("facetColumnSortConfigFactory")
	public void testGenerateFacetSqlQueryWithCTE(FacetColumnSortConfig facetSortConfig) throws ParseException{
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		when(mockLookup.getIndexDescription(any())).thenReturn(new TableIndexDescription(IdAndVersion.parse("syn1")));
		VirtualTableIndexDescription vtid = new VirtualTableIndexDescription(IdAndVersion.parse("syn2"), "select * from syn1", mockLookup);
		dependencies = TranslationDependencies.builder().setSchemaProvider(mockSchemaProvider)
				.setIndexDescription(vtid).setUserId(userId).build();
		
		when(mockSchemaProvider.getColumnModel(any())).thenReturn(stringModel);
		
		originalQuery = new TableQueryParser("with syn2 as (select * from syn1 where stringColumn like 'foo%') select * from syn2").queryExpression();
		// call under test
		FacetTransformerValueCounts facetTransformer = new FacetTransformerValueCounts(stringModel.getName(), facetSortConfig, null, null, false, facets, originalQuery, dependencies, selectedValuesSet);

		String expectedString = "WITH T2 (_C1_, _C2_, _C3_) AS (SELECT _C1_, _C2_, _C3_ FROM T1 WHERE _C1_ LIKE :b1)"
				+ " SELECT _C1_ AS value, COUNT(*) AS frequency FROM T2"
				+ " GROUP BY _C1_ ORDER BY "
				+ FacetTransformerValueCounts.getOrderBy(facetSortConfig)
				+ " LIMIT :b0";
		assertEquals(expectedString, facetTransformer.getFacetSqlQuery().getOutputSQL());
		assertEquals(500L, facetTransformer.getFacetSqlQuery().getParameters().get("b0"));
		assertEquals("foo%", facetTransformer.getFacetSqlQuery().getParameters().get("b1"));
	}
	
	@ParameterizedTest
	@MethodSource("facetColumnSortConfigFactory")
	public void testGenerateFacetSqlQueryWithDefiningWhere(FacetColumnSortConfig facetSortConfig) throws ParseException{
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		when(mockLookup.getIndexDescription(any())).thenReturn(new TableIndexDescription(IdAndVersion.parse("syn1")));
		VirtualTableIndexDescription vtid = new VirtualTableIndexDescription(IdAndVersion.parse("syn2"), "select * from syn1", mockLookup);
		dependencies = TranslationDependencies.builder().setSchemaProvider(mockSchemaProvider)
				.setIndexDescription(vtid).setUserId(userId).build();
		
		when(mockSchemaProvider.getColumnModel(any())).thenReturn(stringModel);
		
		originalQuery = new TableQueryParser("with syn2 as (select * from syn1) select * from syn2 defining_where stringColumn like 'foo%'").queryExpression();
		// call under test
		FacetTransformerValueCounts facetTransformer = new FacetTransformerValueCounts(stringModel.getName(), facetSortConfig, null, null, false, facets, originalQuery, dependencies, selectedValuesSet);

		String expectedString = "WITH T2 (_C1_, _C2_, _C3_) AS (SELECT _C1_, _C2_, _C3_ FROM T1 WHERE _C1_ LIKE :b1)"
				+ " SELECT _C1_ AS value, COUNT(*) AS frequency FROM T2"
				+ " GROUP BY _C1_ ORDER BY "
				+ FacetTransformerValueCounts.getOrderBy(facetSortConfig)
				+ " LIMIT :b0";
		assertEquals(expectedString, facetTransformer.getFacetSqlQuery().getOutputSQL());
		assertEquals(500L, facetTransformer.getFacetSqlQuery().getParameters().get("b0"));
		assertEquals("foo%", facetTransformer.getFacetSqlQuery().getParameters().get("b1"));
	}
	
	@ParameterizedTest
	@MethodSource("facetColumnSortConfigFactory")
	public void testGenerateFacetSqlQueryWithCTEAndSelectedFacet(FacetColumnSortConfig facetSortConfig) throws ParseException{
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		when(mockLookup.getIndexDescription(any())).thenReturn(new TableIndexDescription(IdAndVersion.parse("syn1")));
		VirtualTableIndexDescription vtid = new VirtualTableIndexDescription(IdAndVersion.parse("syn2"), "select * from syn1", mockLookup);
		dependencies = TranslationDependencies.builder().setSchemaProvider(mockSchemaProvider)
				.setIndexDescription(vtid).setUserId(userId).build();
		
		when(mockSchemaProvider.getColumnModel(any())).thenReturn(stringModel);
		
		originalQuery = new TableQueryParser("with syn2 as (select * from syn1) select * from syn2").queryExpression();
		// call under test
		FacetTransformerValueCounts facetTransformer = new FacetTransformerValueCounts(stringListModel.getName(), facetSortConfig, null, null, false, facets, originalQuery, dependencies, selectedValuesSet);

		String expectedString = "WITH T2 (_C1_, _C2_, _C3_) AS (SELECT _C1_, _C2_, _C3_ FROM T1)"
				+ " SELECT _C2_ AS value, COUNT(*) AS frequency FROM T2 WHERE ( ( _C1_ = :b0 ) )"
				+ " GROUP BY _C2_ ORDER BY "
				+ FacetTransformerValueCounts.getOrderBy(facetSortConfig)
				+ " LIMIT :b1";
		assertEquals(expectedString, facetTransformer.getFacetSqlQuery().getOutputSQL());
		assertEquals("selectedValue", facetTransformer.getFacetSqlQuery().getParameters().get("b0"));
		assertEquals(500L, facetTransformer.getFacetSqlQuery().getParameters().get("b1"));
	}

	@ParameterizedTest
	@MethodSource("facetColumnSortConfigFactory")
	public void testGenerateFacetSqlQueryWithListTypes(FacetColumnSortConfig facetSortConfig){
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		FacetTransformerValueCounts facetTransformer = new FacetTransformerValueCounts(stringListModel.getName(), facetSortConfig, null, null, true, facets, originalQuery, dependencies, selectedValuesSet);

		//check the non-transformed sql
		String expectedString = "SELECT T123_INDEX_C2_._C2__UNNEST AS value, COUNT(*) AS frequency "
				+ "FROM T123 LEFT JOIN JSON_TABLE(T123._C2_, '$[*]' COLUMNS(_C2__UNNEST VARCHAR(50) PATH '$' ERROR ON ERROR)) AS T123_INDEX_C2_ ON TRUE "
				+ "WHERE ( _C1_ LIKE :b0 ) AND ( ( ( _C1_ = :b1 ) ) ) "
				+ "GROUP BY T123_INDEX_C2_._C2__UNNEST ORDER BY "
				+ FacetTransformerValueCounts.getOrderBy(facetSortConfig)
				+ " LIMIT :b2";
		assertEquals(expectedString, facetTransformer.getFacetSqlQuery().getOutputSQL());
		assertEquals("asdf%", facetTransformer.getFacetSqlQuery().getParameters().get("b0"));
		assertEquals("selectedValue", facetTransformer.getFacetSqlQuery().getParameters().get("b1"));
		assertEquals(500L, facetTransformer.getFacetSqlQuery().getParameters().get("b2"));
	}

	@ParameterizedTest
	@MethodSource("facetColumnSortConfigFactory")
	public void testGenerateFacetSqlQueryWithJsonPath(FacetColumnSortConfig facetSortConfig){
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		JsonSubColumnModel subColumn = jsonColumnModel.getJsonSubColumns().get(0);
		
		FacetTransformerValueCounts facetTransformer = new FacetTransformerValueCounts(jsonColumnModel.getName(), facetSortConfig, subColumn.getJsonPath(), subColumn.getColumnType(), false, facets, originalQuery, dependencies, selectedValuesSet);

		//check the non-transformed sql
		String expectedString = "SELECT JSON_EXTRACT(\"jsonColumn\",'$.a') AS value, COUNT(*) AS frequency"
				+ " FROM syn123"
				+ " WHERE ( \"stringColumn\" LIKE 'asdf%' ) AND ( ( ( \"stringColumn\" = 'selectedValue' ) ) )"
				+ " GROUP BY JSON_EXTRACT(\"jsonColumn\",'$.a')"
				+ " ORDER BY "
				+ FacetTransformerValueCounts.getOrderBy(facetSortConfig)
				+ " LIMIT 500";
		
		assertEquals(expectedString, facetTransformer.getFacetSqlQuery().getInputSql());
	}

	////////////////////////////
	// translateToResult() tests
	////////////////////////////
	@Test
	public void testTranslateToResultNullRowSet(){
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		FacetTransformerValueCounts facetTransformer = new FacetTransformerValueCounts(stringModel.getName(), facetSortConfig, null, null, false, facets, originalQuery, dependencies, selectedValuesSet);
		assertThrows(IllegalArgumentException.class, ()->{
			facetTransformer.translateToResult(null);
		});
	}
	
	@Test
	public void testTranslateToResultWrongHeaders(){
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		FacetTransformerValueCounts facetTransformer = new FacetTransformerValueCounts(stringModel.getName(), facetSortConfig, null, null, false, facets, originalQuery, dependencies, selectedValuesSet);

		rowSet.setHeaders(Collections.emptyList());

		assertThrows(IllegalArgumentException.class, ()->{
			facetTransformer.translateToResult(rowSet);
		});
	}
	
	@Test 
	public void testTranslateToResultNullValueColumn(){
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		FacetTransformerValueCounts facetTransformer = new FacetTransformerValueCounts(stringModel.getName(), facetSortConfig, null, null, false, facets, originalQuery, dependencies, selectedValuesSet);

		Long row1Count = 42L;
		rowSet.setHeaders(correctSelectList);
		Row row1 = new Row();
		//the value column is null
		row1.setValues(Lists.newArrayList(null, row1Count.toString()));
	
		rowSet.setRows(Lists.newArrayList(row1));

		FacetColumnResultValues expected = new FacetColumnResultValues()
			.setColumnName(stringModel.getName())
			.setFacetType(FacetType.enumeration)
			.setFacetValues(List.of(
				new FacetColumnResultValueCount().setCount(42L).setValue(NULL_VALUE_KEYWORD).setIsSelected(false)
			));
			
		FacetColumnResultValues result = (FacetColumnResultValues) facetTransformer.translateToResult(rowSet);
		
		assertEquals(expected, result);

	}
	
	@Test 
	public void testTranslateToResultCorrectHeaders(){
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		FacetTransformerValueCounts facetTransformer = new FacetTransformerValueCounts(stringModel.getName(), facetSortConfig, null, null, false, facets, originalQuery, dependencies, selectedValuesSet);

		Long row1Count = 42L;
		Long row2Count = 23L;
		rowSet.setHeaders(correctSelectList);
		Row row1 = new Row();
		row1.setValues(Lists.newArrayList(selectedValue, row1Count.toString()));
		Row row2 = new Row();
		row2.setValues(Lists.newArrayList(notSelectedValue, row2Count.toString()));
		rowSet.setRows(Lists.newArrayList(row1, row2));
		
		FacetColumnResultValues expected = new FacetColumnResultValues()
			.setColumnName(stringModel.getName())
			.setFacetType(FacetType.enumeration)
			.setFacetValues(List.of(
				new FacetColumnResultValueCount().setCount(42L).setValue(selectedValue).setIsSelected(true),
				new FacetColumnResultValueCount().setCount(23L).setValue(notSelectedValue).setIsSelected(false)
			));
			
		FacetColumnResultValues result = (FacetColumnResultValues) facetTransformer.translateToResult(rowSet);
		
		assertEquals(expected, result);
	}
	
	@Test 
	public void testTranslateToResultWithJsonPath(){
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		JsonSubColumnModel subColumn = jsonColumnModel.getJsonSubColumns().get(0);
		
		FacetTransformerValueCounts facetTransformer = new FacetTransformerValueCounts(jsonColumnModel.getName(), facetSortConfig, subColumn.getJsonPath(), subColumn.getColumnType(), false, facets, originalQuery, dependencies, selectedValuesSet);

		Long row1Count = 42L;
		Long row2Count = 23L;
		rowSet.setHeaders(correctSelectList);
		Row row1 = new Row();
		row1.setValues(Lists.newArrayList(selectedValue, row1Count.toString()));
		Row row2 = new Row();
		row2.setValues(Lists.newArrayList(notSelectedValue, row2Count.toString()));
		rowSet.setRows(Lists.newArrayList(row1, row2));
		
		FacetColumnResultValues expected = new FacetColumnResultValues()
			.setColumnName(jsonColumnModel.getName())
			.setJsonPath(jsonColumnModel.getJsonSubColumns().get(0).getJsonPath())
			.setFacetType(FacetType.enumeration)
			.setFacetValues(List.of(
				new FacetColumnResultValueCount().setCount(42L).setValue(selectedValue).setIsSelected(true),
				new FacetColumnResultValueCount().setCount(23L).setValue(notSelectedValue).setIsSelected(false)
			));
		
		FacetColumnResultValues result = (FacetColumnResultValues) facetTransformer.translateToResult(rowSet);
		
		assertEquals(expected, result);

	}
	
	
	
}
