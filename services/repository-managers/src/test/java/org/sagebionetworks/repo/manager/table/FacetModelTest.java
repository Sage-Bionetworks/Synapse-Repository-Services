package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.JsonSubColumnModel;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.TranslationDependencies;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.QueryExpression;
import org.sagebionetworks.table.query.util.FacetRequestColumnModel;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class FacetModelTest {
	FacetModel facetModel;
	
	private Set<String> supportedFacetColumns;

	private String tableId;
	private ColumnModel facetColumnModel;
	private ColumnModel facetColumnModel2;
	private List<FacetRequestColumnModel> validatedQueryFacetColumns;
	private String facetColumnName;
	private String facetColumnName2;
	private String facetColumnId;
	private String facetColumnId2;
	private List<ColumnModel> facetSchema;
	private Long min;
	private Long max;
	private String selectedValue;
	private FacetColumnRangeRequest rangeRequest;
	private FacetColumnValuesRequest valuesRequest;
	private ArrayList<FacetColumnRequest> selectedFacets;
	private QueryExpression originalQuery;
	private TranslationDependencies dependencies;

	private Long userId;
	
	@BeforeEach
	public void setUp() throws Exception {
		tableId = "syn123";
		supportedFacetColumns = new HashSet<>();
		validatedQueryFacetColumns = new ArrayList<>();

		facetColumnId = "890";
		facetColumnName = "asdf";
	
		facetColumnModel = new ColumnModel();
		facetColumnModel.setName(facetColumnName);
		facetColumnModel.setId(facetColumnId);
		facetColumnModel.setColumnType(ColumnType.INTEGER);
		facetColumnModel.setMaximumSize(50L);
		facetColumnModel.setFacetType(FacetType.range);
		
		facetColumnId2 = "098";
		facetColumnName2 = "qwerty";
		facetColumnModel2 = new ColumnModel();
		facetColumnModel2.setName(facetColumnName2);
		facetColumnModel2.setId(facetColumnId2);
		facetColumnModel2.setColumnType(ColumnType.STRING);
		facetColumnModel2.setMaximumSize(50L);
		facetColumnModel2.setFacetType(FacetType.enumeration);
		
		ColumnModel jsonColumn = new ColumnModel();
		
		jsonColumn.setName("jsonColumn");
		jsonColumn.setId("099");
		jsonColumn.setColumnType(ColumnType.JSON);
		jsonColumn.setJsonSubColumns(List.of(
			new JsonSubColumnModel().setName("a").setJsonPath("$.a").setFacetType(FacetType.enumeration).setColumnType(ColumnType.STRING),
			new JsonSubColumnModel().setName("b").setJsonPath("$.b").setFacetType(FacetType.range).setColumnType(ColumnType.INTEGER)
		));
		
		ColumnModel jsonColumnWithoutFacets = new ColumnModel();
		
		jsonColumnWithoutFacets.setName("jsonColumnNoFacets");
		jsonColumnWithoutFacets.setId("097");
		jsonColumnWithoutFacets.setColumnType(ColumnType.JSON);
		jsonColumnWithoutFacets.setJsonSubColumns(List.of(
			new JsonSubColumnModel().setName("a").setJsonPath("$.a").setColumnType(ColumnType.STRING)
		));
		
		ColumnModel cm = new ColumnModel();
		cm.setName("ayy");
		cm.setId("099");
		cm.setColumnType(ColumnType.STRING);
		cm.setMaximumSize(50L);
		
		facetSchema = Lists.newArrayList(facetColumnModel, facetColumnModel2, jsonColumn, jsonColumnWithoutFacets, cm);
		
		selectedValue = "someValue";
		
		min = 23L;
		max = 56L;
		
		rangeRequest = new FacetColumnRangeRequest();
		rangeRequest.setColumnName(facetColumnName);
		rangeRequest.setMax(max.toString());
		rangeRequest.setMin(min.toString());
		
		valuesRequest = new FacetColumnValuesRequest();
		valuesRequest.setColumnName(facetColumnName2);
		valuesRequest.setFacetValues(Sets.newHashSet(selectedValue));

		userId = 1L;


		selectedFacets = Lists.newArrayList((FacetColumnRequest)rangeRequest, (FacetColumnRequest)valuesRequest);

		dependencies = TranslationDependencies.builder().setSchemaProvider(schemaProvider(facetSchema))
				.setIndexDescription(new TableIndexDescription(IdAndVersion.parse(tableId))).setUserId(userId).build();
		
		originalQuery = new TableQueryParser("select * from " + tableId + " where asdf <> ayy and asdf < 'taco bell'").queryExpression();

	}
	/////////////////////
	// Constructor tests
	/////////////////////
	@Test
	public void testConstructor(){
		facetModel = new FacetModel(selectedFacets, originalQuery, dependencies, true);
		List<FacetTransformer> facetTransformers = facetModel.getFacetInformationQueries();
		assertNotNull(facetTransformers);
		for(FacetTransformer transformer : facetTransformers){
			assertNotNull(transformer);
		}
	}
	
	
	///////////////////////////////
	// createValidatedFacetsList()
	///////////////////////////////
	@Test
	public void testCreateValidatedFacetsListNullSchema(){
		boolean returnFacets = true;
		assertThrows(IllegalArgumentException.class, ()->{
			FacetModel.createValidatedFacetsList(selectedFacets , null, returnFacets);
		});
	}
	
	@Test
	public void testCreateValidatedFacetsListUnsupportedColumnName(){
		boolean returnFacets = true;
		//remove one column from schema
		facetSchema.remove(0);
		
		assertEquals(4, facetSchema.size()); //only 4 columns in schema now
		assertEquals(2, selectedFacets.size()); //but fil158r on 2 facet columns
		
		assertThrows(InvalidTableQueryFacetColumnRequestException.class, ()->{
			FacetModel.createValidatedFacetsList(selectedFacets , facetSchema, returnFacets);
		});		
	}
	
	@Test
	public void testCreateValidatedFacetsList(){
		boolean returnFacets = true;
		
		
		List<FacetRequestColumnModel> result = FacetModel.createValidatedFacetsList(selectedFacets , facetSchema, returnFacets);
		
		//check that we got nonEmptyList back
		//processFacetColumnRequest tests handles case where some columns don't get added
		assertEquals(4, result.size());
		
		assertEquals(Arrays.asList("asdf", "qwerty", "jsonColumn", "jsonColumn"), result.stream().map(FacetRequestColumnModel::getColumnName).collect(Collectors.toList()));
		assertEquals(Arrays.asList(null, null, "$.a", "$.b"), result.stream().map(FacetRequestColumnModel::getJsonPath).collect(Collectors.toList()));
	}
	
	

	/////////////////////////////////////////////
	// createColumnNameToFacetColumnMap() Tests
	/////////////////////////////////////////////

	@Test
	public void testCreateColumnNameToFacetColumnMapNullList() {
		Map<String, FacetColumnRequest> map = FacetModel.createColumnNameToFacetColumnMap(null);
		assertNotNull(map);
		assertEquals(0, map.size());
	}

	@Test
	public void testCreateColumnNameToFacetColumnMapDuplicateName() {
		// setup
		FacetColumnRequest facetRequest1 = new FacetColumnRangeRequest();
		String sameName = facetColumnName;
		facetRequest1.setColumnName(sameName);
		FacetColumnRequest facetRequest2 = new FacetColumnRangeRequest();
		facetRequest2.setColumnName(sameName);
		assertThrows(IllegalArgumentException.class, ()->{
			FacetModel.createColumnNameToFacetColumnMap(Lists.newArrayList(facetRequest1, facetRequest2));
		});
	}

	@Test
	public void testCreateColumnNameToFacetColumnMap() {
		Map<String, FacetColumnRequest> map = FacetModel
				.createColumnNameToFacetColumnMap(selectedFacets);
		assertNotNull(map);
		assertEquals(2, map.size());
		assertEquals(rangeRequest, map.get(facetColumnName));
		assertEquals(valuesRequest, map.get(facetColumnName2));
	}

	////////////////////////////////////////////
	// processFacetColumnRequest() tests
	////////////////////////////////////////////
	@Test
	public void testProcessFacetColumnRequestColumnModelFacetTypeIsNull() {
		facetColumnModel.setFacetType(null);

		FacetModel.processFacetColumnRequest(validatedQueryFacetColumns, supportedFacetColumns, facetColumnModel, null, new FacetColumnRangeRequest(), true);
		
		assertTrue(validatedQueryFacetColumns.isEmpty());
		assertTrue(supportedFacetColumns.isEmpty());
	}

	@Test
	public void testProcessFacetColumnRequestFacetParamsNullReturnFacetsFalse() {
		facetColumnModel.setFacetType(FacetType.range);

		FacetModel.processFacetColumnRequest(validatedQueryFacetColumns, supportedFacetColumns, facetColumnModel, null, null, false);
		
		assertTrue(validatedQueryFacetColumns.isEmpty());
		assertEquals(1, supportedFacetColumns.size());

	}

	@Test
	public void testProcessFacetColumnRequestFacetParamsNullReturnFacetsTrue() {
		facetColumnModel.setFacetType(FacetType.range);

		FacetModel.processFacetColumnRequest(validatedQueryFacetColumns, supportedFacetColumns, facetColumnModel, null, null, true);
		
		assertEquals(1, validatedQueryFacetColumns.size());
		FacetRequestColumnModel validatedQueryFacetColumn = validatedQueryFacetColumns.get(0);
		assertNull(validatedQueryFacetColumn.getFacetColumnRequest());
		assertEquals(facetColumnName, validatedQueryFacetColumn.getColumnName());
		assertEquals(FacetType.range, validatedQueryFacetColumn.getFacetType());
		assertEquals(1, supportedFacetColumns.size());

	}

	@Test
	public void testProcessFacetColumnRequestFacetParamsNotNull() {
		// setup
		facetColumnModel.setFacetType(FacetType.range);

		FacetColumnRangeRequest facetRange = new FacetColumnRangeRequest();
		facetRange.setMin("123");
		facetRange.setMax("456");
		facetRange.setColumnName(facetColumnName);

		FacetModel.processFacetColumnRequest(validatedQueryFacetColumns, supportedFacetColumns, facetColumnModel, null, facetRange, false);

		assertEquals(1, validatedQueryFacetColumns.size());
		FacetRequestColumnModel validatedQueryFacetColumn = validatedQueryFacetColumns.get(0);
		assertEquals(facetRange, validatedQueryFacetColumn.getFacetColumnRequest());
		assertEquals(facetColumnName, validatedQueryFacetColumn.getColumnName());
		assertEquals(FacetType.range, validatedQueryFacetColumn.getFacetType());
		assertEquals(1, supportedFacetColumns.size());
	}
	
	@Test
	public void testProcessFacetColumnRequestFacetWithJsonSubColumn() {
		// json column
		ColumnModel column = facetSchema.get(2);
		// range subcolumn
		JsonSubColumnModel subColumn = column.getJsonSubColumns().get(1);

		FacetColumnRangeRequest facetRange = new FacetColumnRangeRequest();
		facetRange.setJsonPath("$.b");
		facetRange.setMin("123");
		facetRange.setMax("456");
		facetRange.setColumnName(facetColumnName);

		FacetModel.processFacetColumnRequest(validatedQueryFacetColumns, supportedFacetColumns, column, subColumn, facetRange, false);

		assertEquals(1, validatedQueryFacetColumns.size());
		FacetRequestColumnModel validatedQueryFacetColumn = validatedQueryFacetColumns.get(0);
		assertEquals(facetRange, validatedQueryFacetColumn.getFacetColumnRequest());
		assertEquals(column.getName(), validatedQueryFacetColumn.getColumnName());
		assertEquals(FacetType.range, validatedQueryFacetColumn.getFacetType());
		assertEquals(1, supportedFacetColumns.size());
	}

	
	///////////////////////////////////////////
	// generateFacetQueryTransformers() tests
	///////////////////////////////////////////
	@Test 
	public void testGenerateFacetQueryTransformersNullQuery() {
		originalQuery = null;
		assertThrows(IllegalArgumentException.class, ()->{
			FacetModel.generateFacetQueryTransformers(originalQuery, dependencies, validatedQueryFacetColumns);
		});
	}
	
	@Test 
	public void testGenerateFacetQueryTransformersWithNullDependencies() {
		dependencies = null;
		assertThrows(IllegalArgumentException.class, ()->{
			FacetModel.generateFacetQueryTransformers(originalQuery, dependencies, validatedQueryFacetColumns);
		});
	}
	
	@Test
	public void testGenerateFacetQueryTransformersNullList() {
		validatedQueryFacetColumns = null;
		assertThrows(IllegalArgumentException.class, ()->{
			FacetModel.generateFacetQueryTransformers(originalQuery, dependencies, validatedQueryFacetColumns);
		});
	}
	
	@Test
	public void testGenerateFacetQueryTransformers(){
		validatedQueryFacetColumns.add(new FacetRequestColumnModel(facetColumnModel, rangeRequest));
		validatedQueryFacetColumns.add(new FacetRequestColumnModel(facetColumnModel2, valuesRequest));
		
		List<FacetTransformer> result = FacetModel.generateFacetQueryTransformers(originalQuery, dependencies, validatedQueryFacetColumns);
		//just check for the correct item types.  
		//the transformers' unit tests already check that fields are set correctly
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof FacetTransformerRange);
		assertTrue(result.get(1) instanceof FacetTransformerValueCounts);
	}
	
	@Test
	public void testGenerateFacetQueryTransformersWithJsonSubColumns(){
		ColumnModel jsonColumn = new ColumnModel()
			.setName("jsonColumn")
			.setColumnType(ColumnType.JSON)
			.setJsonSubColumns(List.of(
				new JsonSubColumnModel().setName("a").setJsonPath("$.a").setFacetType(FacetType.range).setColumnType(ColumnType.INTEGER),
				new JsonSubColumnModel().setName("b").setJsonPath("$.b").setFacetType(FacetType.enumeration).setColumnType(ColumnType.INTEGER)
			));
		
		jsonColumn.getJsonSubColumns().forEach(jsonSubColumn -> {
			validatedQueryFacetColumns.add(new FacetRequestColumnModel(jsonColumn.getName(), jsonSubColumn, null));	
		});
		
		List<FacetTransformer> result = FacetModel.generateFacetQueryTransformers(originalQuery, dependencies, validatedQueryFacetColumns);
		
		//just check for the correct item types.  
		//the transformers' unit tests already check that fields are set correctly
		assertEquals(2, result.size());
		assertTrue(result.get(0) instanceof FacetTransformerRange);
		assertTrue(result.get(1) instanceof FacetTransformerValueCounts);
	}
	
	/**
	 * Helper to create a schema provider for the given schema.
	 * @param schema
	 * @return
	 */
	SchemaProvider schemaProvider(List<ColumnModel> schema) {
		SchemaProvider mockProvider = Mockito.mock(SchemaProvider.class);
		when(mockProvider.getTableSchema(any())).thenReturn(schema);
		return mockProvider;
	}
	
}
