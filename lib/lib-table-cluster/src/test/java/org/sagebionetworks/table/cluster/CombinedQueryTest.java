package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnSingleValueFilterOperator;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.JsonSubColumnModel;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;

@ExtendWith(MockitoExtension.class)
public class CombinedQueryTest {

	@Mock
	SchemaProvider mockSchemaProvider;

	private List<QueryFilter> additionalFilters;
	private List<FacetColumnRequest> selectedFacets;
	private List<ColumnModel> schema;
	private List<SortItem> sortList;

	private Long overrideLimit;
	private Long overrideOffset;

	@BeforeEach
	public void before() {
		schema = List.of(
			TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING).setFacetType(FacetType.enumeration),
			TableModelTestUtils.createColumn(222L, "bar", ColumnType.INTEGER_LIST).setFacetType(FacetType.enumeration),
			TableModelTestUtils.createColumn(333L, "aBool", ColumnType.BOOLEAN),
			TableModelTestUtils.createColumn(444L, "aJson", ColumnType.JSON).setJsonSubColumns(List.of(
				new JsonSubColumnModel().setName("a").setJsonPath("$.a").setColumnType(ColumnType.INTEGER).setFacetType(FacetType.enumeration)
			))
		);

		additionalFilters = List.of(new ColumnSingleValueQueryFilter().setColumnName("foo")
				.setOperator(ColumnSingleValueFilterOperator.LIKE).setValues(List.of("one", "two")));

		Set<String> ids = new LinkedHashSet<>();
		ids.add("1");
		ids.add("2");
		ids.add("3");
		selectedFacets = List.of(new FacetColumnValuesRequest().setColumnName("bar").setFacetValues(ids));

		sortList = List.of(new SortItem().setColumn("foo").setDirection(SortDirection.DESC));

		overrideLimit = 99L;
		overrideOffset = 2L;
	}

	@Test
	public void testGetCombinedSqlWithAllOverrides() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
				.setQuery("select * from syn123").setAdditionalFilters(additionalFilters).setSortList(sortList)
				.setOverrideLimit(overrideLimit).setOverrideOffset(overrideOffset).setSelectedFacets(selectedFacets)
				.build();

		assertEquals(
				"SELECT * FROM syn123 WHERE " + "( ( \"foo\" LIKE 'one' OR \"foo\" LIKE 'two' ) ) "
						+ "AND ( ( ( \"bar\" HAS ( '1', '2', '3' ) ) ) ) " + "ORDER BY \"foo\" DESC LIMIT 99 OFFSET 2",
				combined.getCombinedSql());
	}

	@Test
	public void testGetCombinedSqlWithAllOverridesExistingWhere() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
				.setQuery("select aBool from syn123 where aBool is true").setAdditionalFilters(additionalFilters)
				.setSortList(sortList).setOverrideLimit(overrideLimit).setOverrideOffset(overrideOffset)
				.setSelectedFacets(selectedFacets).build();

		assertEquals(
				"SELECT aBool FROM syn123 WHERE ( ( aBool IS TRUE ) "
						+ "AND ( ( \"foo\" LIKE 'one' OR \"foo\" LIKE 'two' ) ) )"
						+ " AND ( ( ( \"bar\" HAS ( '1', '2', '3' ) ) ) ) ORDER BY \"foo\" DESC LIMIT 99 OFFSET 2",
				combined.getCombinedSql());
	}

	@Test
	public void testGetCombinedSqlWithAllOverridesExistingPagination() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		overrideLimit = 19L;
		overrideOffset = 4L;
		additionalFilters = null;
		sortList = null;
		selectedFacets = null;
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
				.setQuery("select * from syn123 limit 10 offset 3").setAdditionalFilters(additionalFilters)
				.setSortList(sortList).setOverrideLimit(overrideLimit).setOverrideOffset(overrideOffset)
				.setSelectedFacets(selectedFacets).build();

		assertEquals("SELECT * FROM syn123 LIMIT 6 OFFSET 7", combined.getCombinedSql());
	}

	@Test
	public void testGetCombinedSqlWithExistingSort() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		overrideLimit = null;
		overrideOffset = null;
		additionalFilters = null;
		sortList = List.of(new SortItem().setColumn("foo").setDirection(SortDirection.DESC));
		selectedFacets = null;
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
				.setQuery("select * from syn123 order by aBool ASC").setAdditionalFilters(additionalFilters)
				.setSortList(sortList).setOverrideLimit(overrideLimit).setOverrideOffset(overrideOffset)
				.setSelectedFacets(selectedFacets).build();

		assertEquals("SELECT * FROM syn123 ORDER BY \"foo\" DESC, aBool ASC", combined.getCombinedSql());
	}

	@Test
	public void testGetCombinedSqlWithEmptyAdditionalFilters() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		overrideLimit = null;
		overrideOffset = null;
		additionalFilters = Collections.emptyList();
		sortList = null;
		selectedFacets = null;
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
				.setQuery("select * from syn123").setAdditionalFilters(additionalFilters).setSortList(sortList)
				.setOverrideLimit(overrideLimit).setOverrideOffset(overrideOffset).setSelectedFacets(selectedFacets)
				.build();

		assertEquals("SELECT * FROM syn123", combined.getCombinedSql());
	}
	
	@Test
	public void testGetCombinedSqlWithSelectedFacetWithSubColumns() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		overrideLimit = null;
		overrideOffset = null;
		additionalFilters = null;
		sortList = null;
		selectedFacets = List.of(new FacetColumnValuesRequest().setColumnName("aJson").setJsonPath("$.a").setFacetValues(Set.of("b")));
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
				.setQuery("select * from syn123").setAdditionalFilters(additionalFilters).setSortList(sortList)
				.setOverrideLimit(overrideLimit).setOverrideOffset(overrideOffset).setSelectedFacets(selectedFacets)
				.build();

		assertEquals("SELECT * FROM syn123 WHERE ( ( JSON_UNQUOTE(JSON_EXTRACT(\"aJson\",'$.a')) = CAST('b' AS INTEGER) ) )", combined.getCombinedSql());
	}
	
	@Test
	public void testGetCombinedSqlWithSelectedFacetWithSubColumnsAndNoMatch() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		overrideLimit = null;
		overrideOffset = null;
		additionalFilters = null;
		sortList = null;
		selectedFacets = List.of(new FacetColumnValuesRequest().setColumnName("aJson").setJsonPath("$.b").setFacetValues(Set.of("b")));
		
		String result = assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
			.setQuery("select * from syn123").setAdditionalFilters(additionalFilters).setSortList(sortList)
			.setOverrideLimit(overrideLimit).setOverrideOffset(overrideOffset).setSelectedFacets(selectedFacets)
			.build();
		}).getMessage();
		
		assertEquals("Could not find a subColumn with jsonPath '$.b' for column 'aJson'", result);
	}

	@Test
	public void testGetCombinedSqlWithUnknownFacet() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);

		selectedFacets = List
				.of(new FacetColumnValuesRequest().setColumnName("doesNotExist").setFacetValues(Set.of("1")));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
					.setQuery("select aBool from syn123 where aBool is true").setAdditionalFilters(additionalFilters)
					.setSortList(sortList).setOverrideLimit(overrideLimit).setOverrideOffset(overrideOffset)
					.setSelectedFacets(selectedFacets).build();
		}).getMessage();
		assertEquals("Facet selection: 'doesNotExist' does not match any column name of the schema", message);
	}

	@Test
	public void testGetCombinedSqlWithAllNull() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		additionalFilters = null;
		selectedFacets = null;
		sortList = null;
		overrideLimit = null;
		overrideOffset = null;

		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
				.setQuery("select * from syn123").setAdditionalFilters(additionalFilters).setSortList(sortList)
				.setOverrideLimit(overrideLimit).setOverrideOffset(overrideOffset).setSelectedFacets(selectedFacets)
				.build();

		assertEquals("SELECT * FROM syn123", combined.getCombinedSql());
	}

	@Test
	public void testGetCombinedSqlWithInvalidSql() {
		additionalFilters = null;
		selectedFacets = null;
		sortList = null;
		overrideLimit = null;
		overrideOffset = null;

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CombinedQuery.builder().setSchemaProvider(mockSchemaProvider).setQuery("this is not valid sql")
					.setAdditionalFilters(additionalFilters).setSortList(sortList).setOverrideLimit(overrideLimit)
					.setOverrideOffset(overrideOffset).setSelectedFacets(selectedFacets).build();
		});
	}

	@Test
	public void testGetCombinedSqlWithCTEAndAllOverrides() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
				.setQuery("with syn2 as (select * from syn1) select * from syn2").setAdditionalFilters(additionalFilters).setSortList(sortList)
				.setOverrideLimit(overrideLimit).setOverrideOffset(overrideOffset).setSelectedFacets(selectedFacets)
				.build();

		assertEquals(
				"WITH syn2 AS (SELECT * FROM syn1) "
				+ "SELECT * FROM syn2 WHERE ( ( \"foo\" LIKE 'one' OR \"foo\" LIKE 'two' ) )"
				+ " AND ( ( ( \"bar\" HAS ( '1', '2', '3' ) ) ) ) "
				+ "ORDER BY \"foo\" DESC LIMIT 99 OFFSET 2",
				combined.getCombinedSql());
	}

	@Test
	public void testGetCombinedSqlWithCTEAndDefiningConditions() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(schema);
		
		additionalFilters = List.of(
				new ColumnSingleValueQueryFilter().setColumnName("foo")
				.setOperator(ColumnSingleValueFilterOperator.LIKE).setValues(List.of("one", "two"))
				,new ColumnSingleValueQueryFilter().setColumnName("aBool")
				.setOperator(ColumnSingleValueFilterOperator.IN).setValues(List.of("false")).setIsDefiningCondition(true));
		
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
				.setQuery("with syn2 as (select * from syn1) select * from syn2").setAdditionalFilters(additionalFilters).setSortList(sortList)
				.setOverrideLimit(overrideLimit).setOverrideOffset(overrideOffset).setSelectedFacets(selectedFacets)
				.build();

		assertEquals(
				"WITH syn2 AS (SELECT * FROM syn1) "
				+ "SELECT * FROM syn2 DEFINING_WHERE ( \"aBool\" IN ( 'false' ) ) "
				+ "WHERE ( ( \"foo\" LIKE 'one' OR \"foo\" LIKE 'two' ) ) AND ( ( ( \"bar\" HAS ( '1', '2', '3' ) ) ) )"
				+ " ORDER BY \"foo\" DESC LIMIT 99 OFFSET 2",
				combined.getCombinedSql());
	}

}
