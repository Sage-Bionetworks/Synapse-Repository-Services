package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnCohortFilter;
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
				.setOperator(ColumnSingleValueFilterOperator.LIKE).setValues(List.of("two", "one")));

		Set<String> ids = new LinkedHashSet<>();
		ids.add("3");
		ids.add("2");
		ids.add("1");
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
				"SELECT aBool FROM syn123 WHERE"
					+ " ( ( ( \"bar\" HAS ( '1', '2', '3' ) ) ) )"
					+ " AND ( ( ( \"foo\" LIKE 'one' OR \"foo\" LIKE 'two' ) )"
					+ " AND ( aBool IS TRUE ) )"
					+ " ORDER BY \"foo\" DESC LIMIT 99 OFFSET 2",
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

		assertEquals("SELECT * FROM syn123 WHERE ( ( JSON_EXTRACT(\"aJson\",'$.a') = CAST('b' AS INTEGER) ) )", combined.getCombinedSql());
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
			new ColumnSingleValueQueryFilter().setColumnName("foo").setOperator(ColumnSingleValueFilterOperator.LIKE)
				.setValues(List.of("two", "one")),
			new ColumnSingleValueQueryFilter().setColumnName("aBool").setOperator(ColumnSingleValueFilterOperator.IN)
				.setValues(List.of("false")).setIsDefiningCondition(true)
		);
		
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

	private static final List<ColumnModel> COHORT_SCHEMA = List.of(
			TableModelTestUtils.createColumn(111L, "pid", ColumnType.INTEGER),
			TableModelTestUtils.createColumn(222L, "name", ColumnType.STRING),
			TableModelTestUtils.createColumn(333L, "tags", ColumnType.STRING_LIST));

	private static ResolvedCohort cohort(String name, ColumnType type, String... values) {
		return new ResolvedCohort(name, type, List.of(values), false);
	}

	@Test
	public void testGetCombinedSqlWithCohorts() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(COHORT_SCHEMA);
		Map<String, ResolvedCohort> cohorts = Map.of(
				"c1", cohort("c1", ColumnType.INTEGER, "3", "1", "2"),
				"c2", cohort("c2", ColumnType.STRING, "o'brien", "smith"));
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
				.setQuery("select * from syn123 where pid in cohort(c1) and name not in cohort(c2)")
				.setCohorts(cohorts).build();

		assertEquals("SELECT * FROM syn123 WHERE name NOT IN ( 'o''brien', 'smith' ) AND pid IN ( '1', '2', '3' )",
				combined.getCombinedSql());
	}

	@Test
	public void testGetCombinedSqlWithCohortFilter() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(COHORT_SCHEMA);
		List<QueryFilter> filters = List.of(new ColumnCohortFilter().setColumnName("pid").setCohortName("c1"),
				new ColumnCohortFilter().setColumnName("pid").setCohortName("c2").setIsDefiningCondition(true));
		Map<String, ResolvedCohort> cohorts = Map.of(
				"c1", cohort("c1", ColumnType.INTEGER, "1", "2"),
				"c2", cohort("c2", ColumnType.INTEGER, "4"));
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
				.setQuery("select * from syn123 where name = 'a'").setAdditionalFilters(filters).setCohorts(cohorts)
				.build();

		assertEquals("SELECT * FROM syn123 DEFINING_WHERE ( \"pid\" IN ( '4' ) )"
				+ " WHERE ( ( \"pid\" IN ( '1', '2' ) ) ) AND ( name = 'a' )", combined.getCombinedSql());
	}

	@Test
	public void testGetCombinedSqlWithCohortsAndNoResolution() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(COHORT_SCHEMA);
		List<QueryFilter> filters = List.of(new ColumnCohortFilter().setColumnName("pid").setCohortName("c2"));
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
				.setQuery("select * from syn123 where pid in cohort(c1)").setAdditionalFilters(filters).build();

		assertEquals("SELECT * FROM syn123 WHERE ( ( \"pid\" IN COHORT(c2) ) ) AND ( pid IN COHORT(c1) )",
				combined.getCombinedSql());
	}

	@Test
	public void testGetCombinedSqlWithEmptyCohort() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(COHORT_SCHEMA);
		Map<String, ResolvedCohort> cohorts = Map.of("c1", cohort("c1", ColumnType.INTEGER));
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
				.setQuery("select * from syn123 where pid in cohort(c1) or name = 'a'").setCohorts(cohorts).build();

		assertEquals("SELECT * FROM syn123 WHERE name = 'a' OR pid IN ( NULL )", combined.getCombinedSql());
	}

	@Test
	public void testGetCombinedSqlWithEmptyCohortNotIn() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(COHORT_SCHEMA);
		Map<String, ResolvedCohort> cohorts = Map.of("c1", cohort("c1", ColumnType.INTEGER));
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
				.setQuery("select * from syn123 where pid not in cohort(c1)").setCohorts(cohorts).build();

		assertEquals("SELECT * FROM syn123 WHERE pid NOT IN ( NULL )", combined.getCombinedSql());
	}

	@Test
	public void testGetCombinedSqlWithUnknownCohort() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(COHORT_SCHEMA);
		Map<String, ResolvedCohort> cohorts = Map.of("c1", cohort("c1", ColumnType.INTEGER, "1"));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
					.setQuery("select * from syn123 where pid in cohort(other)").setCohorts(cohorts).build();
		}).getMessage();
		assertEquals("Unknown cohort: other", message);
	}

	@Test
	public void testGetCombinedSqlWithCohortTypeMismatch() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(COHORT_SCHEMA);
		Map<String, ResolvedCohort> cohorts = Map.of("c1", cohort("c1", ColumnType.STRING, "secret"));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
					.setQuery("select * from syn123 where pid in cohort(c1)").setCohorts(cohorts).build();
		}).getMessage();
		assertEquals("Cohort 'c1' selects a column of type STRING, which is not compatible with column 'pid' of type INTEGER",
				message);
	}

	@Test
	public void testGetCombinedSqlWithCohortOnListColumn() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(COHORT_SCHEMA);
		Map<String, ResolvedCohort> cohorts = Map.of("c1", cohort("c1", ColumnType.STRING, "secret"));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
					.setQuery("select * from syn123 where tags in cohort(c1)").setCohorts(cohorts).build();
		}).getMessage();
		assertEquals("Cohort 'c1' selects a column of type STRING, which is not compatible with column 'tags' of type STRING_LIST",
				message);
	}

	@Test
	public void testGetCombinedSqlWithCohortUnknownColumn() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(COHORT_SCHEMA);
		Map<String, ResolvedCohort> cohorts = Map.of("c1", cohort("c1", ColumnType.INTEGER, "1"));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
					.setQuery("select * from syn123 where nope in cohort(c1)").setCohorts(cohorts).build();
		}).getMessage();
		assertEquals("Column does not exist: nope", message);
	}

	@Test
	public void testGetCombinedSqlWithCohortFunctionLeftHandSide() {
		when(mockSchemaProvider.getTableSchema(any())).thenReturn(COHORT_SCHEMA);
		Map<String, ResolvedCohort> cohorts = Map.of("c1", cohort("c1", ColumnType.INTEGER, "1"));
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CombinedQuery.builder().setSchemaProvider(mockSchemaProvider)
					.setQuery("select * from syn123 where cast(name as INTEGER) in cohort(c1)").setCohorts(cohorts).build();
		}).getMessage();
		assertEquals("The left-hand side of IN COHORT(c1) must be a column reference", message);
	}

	@Test
	public void testResolvedCohortToStringOmitsValues() {
		// call under test
		String string = cohort("c1", ColumnType.INTEGER, "12345").toString();
		assertEquals("ResolvedCohort[name=c1, columnType=INTEGER, aggregateOnly=false]", string);
	}

}
