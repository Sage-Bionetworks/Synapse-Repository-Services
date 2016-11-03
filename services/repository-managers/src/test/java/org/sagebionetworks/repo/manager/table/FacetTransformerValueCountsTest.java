package org.sagebionetworks.repo.manager.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.FacetColumnResultValueCount;
import org.sagebionetworks.repo.model.table.FacetColumnResultValues;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.query.ParseException;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class FacetTransformerValueCountsTest {
	private FacetTransformerValueCounts facetTransformer;
	private String columnName;
	String selectedValue;
	String notSelectedValue;
	private List<ColumnModel> schema;
	private SqlQuery originalQuery;
	private String originalSearchCondition;
	private List<ValidatedQueryFacetColumn> facets;
	private RowSet rowSet;
	private List<SelectColumn> correctSelectList;
	private Set<String> selectedValuesSet;

	
	@Before
	public void before() throws ParseException{
		schema = TableModelTestUtils.createOneOfEachType(true);
		assertFalse(schema.isEmpty());
		columnName = "i0";
		facets = new ArrayList<>();
		selectedValue = "selectedValue";
		notSelectedValue = "notSelectedValue";
		FacetColumnValuesRequest valuesRequest = new FacetColumnValuesRequest();
		valuesRequest.setColumnName(columnName);
		selectedValuesSet = Sets.newHashSet(selectedValue);
		valuesRequest.setFacetValues(selectedValuesSet);
		facets.add(new ValidatedQueryFacetColumn("i0", FacetType.enumeration, valuesRequest));

		originalSearchCondition = "i0 LIKE 'asdf%'";
		originalQuery = new SqlQuery("SELECT * FROM syn123 WHERE " + originalSearchCondition, schema);
		
		rowSet = new RowSet();
		
		SelectColumn col1 = new SelectColumn();
		col1.setName(FacetTransformerValueCounts.VALUE_ALIAS);
		SelectColumn col2 = new SelectColumn();
		col2.setName(FacetTransformerValueCounts.COUNT_ALIAS);
		correctSelectList = Lists.newArrayList(col1, col2);
		
		facetTransformer = new FacetTransformerValueCounts(columnName, facets, originalQuery, selectedValuesSet);		

	}
	/////////////////////////////////
	// constructor tests()
	/////////////////////////////////
	
	@Test
	public void testConstructor() {
		assertEquals(columnName, ReflectionTestUtils.getField(facetTransformer, "columnName"));
		assertEquals(facets, ReflectionTestUtils.getField(facetTransformer, "facets"));
		assertEquals(selectedValuesSet, ReflectionTestUtils.getField(facetTransformer, "selectedValues"));
		
		
		//this is tested in a separate test
		assertNotNull(ReflectionTestUtils.getField(facetTransformer, "generatedFacetSqlQuery"));
	}
	
	/////////////////////////////////
	// generateFacetSqlQuery tests()
	/////////////////////////////////
	@Test
	public void testGenerateFacetSqlQuery(){

		//check the non-transformed sql
		String expectedString = "SELECT " + columnName + " AS " 
		+ FacetTransformerValueCounts.VALUE_ALIAS
		+ ", COUNT(*) AS " 
		+ FacetTransformerValueCounts.COUNT_ALIAS 
		+ " FROM syn123 WHERE ( "+originalSearchCondition
		+ " ) GROUP BY " + columnName 
		+ " LIMIT " + FacetTransformerValueCounts.MAX_NUM_FACET_CATEGORIES;
		assertEquals(expectedString, facetTransformer.getFacetSqlQuery().getModel().toSql());
		
		//transformed model will be correct if schema and non-transformed query are correct
		//because it is handled by SqlQuery Constructor
	}
	

	////////////////////////////
	// translateToResult() tests
	////////////////////////////
	@Test (expected = IllegalArgumentException.class)
	public void testTranslateToResultNullRowSet(){
		facetTransformer.translateToResult(null);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testTranslateToResultWrongHeaders(){
		rowSet.setHeaders(new ArrayList<SelectColumn>());
		facetTransformer.translateToResult(rowSet);
	}
	
	@Test 
	public void testTranslateToResultCorrectHeaders(){
		Long row1Count = 42L;
		Long row2Count = 23L;
		rowSet.setHeaders(correctSelectList);
		Row row1 = new Row();
		row1.setValues(Lists.newArrayList(selectedValue, row1Count.toString()));
		Row row2 = new Row();
		row2.setValues(Lists.newArrayList(notSelectedValue, row2Count.toString()));
		rowSet.setRows(Lists.newArrayList(row1, row2));
		FacetColumnResultValues result = (FacetColumnResultValues) facetTransformer.translateToResult(rowSet);

		assertEquals(columnName, result.getColumnName());
		assertEquals(FacetType.enumeration, result.getFacetType());

		List<FacetColumnResultValueCount> valueCounts = result.getFacetValues();
		assertEquals(2, valueCounts.size());
		
		FacetColumnResultValueCount valueCount1 = valueCounts.get(0);
		assertEquals(selectedValue, valueCount1.getValue());
		assertEquals(row1Count, valueCount1.getCount());
		assertTrue(valueCount1.getIsSelected());
		
		FacetColumnResultValueCount valueCount2 = valueCounts.get(1);
		assertEquals(notSelectedValue, valueCount2.getValue());
		assertEquals(row2Count, valueCount2.getCount());
		assertFalse(valueCount2.getIsSelected());
	}
	
}
