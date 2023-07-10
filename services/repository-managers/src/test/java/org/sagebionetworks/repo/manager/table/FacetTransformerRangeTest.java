package org.sagebionetworks.repo.manager.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnResultRange;
import org.sagebionetworks.repo.model.table.FacetType;
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

@ExtendWith(MockitoExtension.class)
public class FacetTransformerRangeTest {
	private FacetTransformerRange facetTransformer;
	private String columnName;
	private String selectedMin;
	private String selectedMax;
	private List<ColumnModel> schema;
	private String originalSearchCondition;
	private List<FacetRequestColumnModel> facets;
	private RowSet rowSet;
	private List<SelectColumn> correctSelectList;
	private Long userId;
	private QueryExpression originalQuery;
	private TranslationDependencies dependencies;
	
	@Mock
	private SchemaProvider mockSchemaProvider;
	@Mock
	private IndexDescriptionLookup mockLookup;
	
	@BeforeEach
	public void before() throws ParseException{
		schema = TableModelTestUtils.createOneOfEachType(true);
		assertFalse(schema.isEmpty());
		columnName = "i2";
		facets = new ArrayList<>();
		FacetColumnRangeRequest rangeRequest = new FacetColumnRangeRequest();
		rangeRequest.setColumnName(columnName);
		rangeRequest.setMin(selectedMin);
		rangeRequest.setMax(selectedMax);
		facets.add(new FacetRequestColumnModel(schema.get(2), rangeRequest)); //use column "i2"
		selectedMin = "12";
		selectedMax = "34";
		originalSearchCondition = "i0 LIKE 'asdf%'";
		userId = 1L;
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		
		dependencies = TranslationDependencies.builder().setSchemaProvider(mockSchemaProvider)
				.setIndexDescription(new TableIndexDescription(IdAndVersion.parse("syn123"))).setUserId(userId).build();
		
		originalQuery = new TableQueryParser("select * FROM syn123 WHERE " + originalSearchCondition).queryExpression();
		
		rowSet = new RowSet();
		
		SelectColumn col1 = new SelectColumn();
		col1.setName(FacetTransformerRange.MIN_ALIAS);
		SelectColumn col2 = new SelectColumn();
		col2.setName(FacetTransformerRange.MAX_ALIAS);
		correctSelectList = Lists.newArrayList(col1, col2);
		
		facetTransformer = new FacetTransformerRange(columnName, facets, originalQuery, dependencies, selectedMin, selectedMax);		

	}
	/////////////////////////////////
	// constructor tests()
	/////////////////////////////////
	@Test
	public void testConstructor() {
		assertEquals(columnName, ReflectionTestUtils.getField(facetTransformer, "columnName"));
		assertEquals(facets, ReflectionTestUtils.getField(facetTransformer, "facets"));
		assertEquals(selectedMin, ReflectionTestUtils.getField(facetTransformer, "selectedMin"));
		assertEquals(selectedMax, ReflectionTestUtils.getField(facetTransformer, "selectedMax"));		
		//this is tested in a separate test
		assertNotNull(ReflectionTestUtils.getField(facetTransformer, "generatedFacetSqlQuery"));
	}
	
	/////////////////////////////////
	// generateFacetSqlQuery tests()
	/////////////////////////////////
	
	@Test
	public void testGenerateFacetSqlQuery(){
		//check the non-transformed sql
		String expectedString = "SELECT MIN(_C2_) AS minimum, MAX(_C2_) AS maximum FROM T123 WHERE _C0_ LIKE :b0";
		assertEquals(expectedString, facetTransformer.getFacetSqlQuery().getOutputSQL());
		assertEquals("asdf%", facetTransformer.getFacetSqlQuery().getParameters().get("b0"));
	}
	
	@Test
	public void testGenerateFacetSqlQueryWihtCTE() throws ParseException{
		
		when(mockLookup.getIndexDescription(any())).thenReturn(new TableIndexDescription(IdAndVersion.parse("syn1")));
		VirtualTableIndexDescription vtid = new VirtualTableIndexDescription(IdAndVersion.parse("syn2"), "select * from syn1", mockLookup);
		dependencies = TranslationDependencies.builder().setSchemaProvider(mockSchemaProvider)
				.setIndexDescription(vtid).setUserId(userId).build();
		
		when(mockSchemaProvider.getColumnModel(any())).thenReturn(schema.get(0));
		
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(List.of(schema.get(0), schema.get(2)));
		originalQuery = new TableQueryParser("with syn2 as (select i0, i2 from syn1) select * from syn2 where i0 > 100 order by i2").queryExpression();
		
		// call under test
		facetTransformer = new FacetTransformerRange(columnName, facets, originalQuery, dependencies, selectedMin, selectedMax);
		//check the non-transformed sql
		String expectedString = "WITH T2 (_C0_, _C2_) AS (SELECT _C0_, _C2_ FROM T1)"
				+ " SELECT MIN(_C2_) AS minimum, MAX(_C2_) AS maximum FROM T2 WHERE _C0_ > :b0";
		assertEquals(expectedString, facetTransformer.getFacetSqlQuery().getOutputSQL());
		assertEquals("100", facetTransformer.getFacetSqlQuery().getParameters().get("b0"));
	}
	
	////////////////////////////
	// translateToResult() tests
	////////////////////////////
	public void testTranslateToResultNullRowSet(){
		assertThrows(IllegalArgumentException.class, ()->{
			facetTransformer.translateToResult(null);
		});
	}
	
	@Test
	public void testTranslateToResultWrongHeaders(){
		rowSet.setHeaders(new ArrayList<SelectColumn>());
		assertThrows(IllegalArgumentException.class, ()->{
			facetTransformer.translateToResult(rowSet);
		});
	}
	
	@Test
	public void testTranslateToResultCorrectHeadersWrongNumRows(){
		rowSet.setHeaders(correctSelectList);
		rowSet.setRows(new ArrayList<Row>());
		assertThrows(IllegalArgumentException.class, ()->{
			facetTransformer.translateToResult(rowSet);
		});
	}
	
	@Test 
	public void testTranslateToResultCorrectHeadersAndNumRows(){
		String colMin = "2";
		String colMax = "42";
		rowSet.setHeaders(correctSelectList);
		Row row = new Row();
		row.setValues(Lists.newArrayList(colMin, colMax));
		rowSet.setRows(Lists.newArrayList(row));
		FacetColumnResultRange result = (FacetColumnResultRange) facetTransformer.translateToResult(rowSet);

		assertEquals(columnName, result.getColumnName());
		assertEquals(FacetType.range, result.getFacetType());

		assertEquals(colMin, result.getColumnMin());
		assertEquals(colMax, result.getColumnMax());
		assertEquals(selectedMin, result.getSelectedMin());
		assertEquals(selectedMax, result.getSelectedMax());
	}
}
