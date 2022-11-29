package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnSingleValueFilterOperator;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;

@ExtendWith(MockitoExtension.class)
public class CombinedQueryTest {

	private List<QueryFilter> additionalFilters;
	private List<FacetColumnRequest> selectedFacets;
	private List<ColumnModel> schema;
	private List<SortItem> sortList;
	private SchemaProvider schemaProvider;
	private Long overrideLimit;
	private Long overrideOffset;

	@BeforeEach
	public void before() {
		schema = List.of(
				TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING).setFacetType(FacetType.enumeration),
				TableModelTestUtils.createColumn(222L, "bar", ColumnType.INTEGER_LIST).setFacetType(
						FacetType.enumeration),
				TableModelTestUtils.createColumn(333L, "aBool", ColumnType.BOOLEAN));

		additionalFilters = List.of(new ColumnSingleValueQueryFilter().setColumnName("foo")
				.setOperator(ColumnSingleValueFilterOperator.LIKE).setValues(List.of("one", "two")));

		Set<String> ids = new LinkedHashSet<>();
		ids.add("1");
		ids.add("2");
		ids.add("3");
		selectedFacets = List.of(new FacetColumnValuesRequest().setColumnName("bar").setFacetValues(ids));

		sortList = List.of(new SortItem().setColumn("foo").setDirection(SortDirection.DESC));

		schemaProvider = schemaProvider(schema);

		overrideLimit = 99L;
		overrideOffset = 2L;
	}

	@Test
	public void testGetCombinedSqlWithAllOverrides() {
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(schemaProvider)
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
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(schemaProvider)
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
		overrideLimit = 19L;
		overrideOffset = 4L;
		additionalFilters = null;
		sortList = null;
		selectedFacets = null;
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(schemaProvider)
				.setQuery("select * from syn123 limit 10 offset 3").setAdditionalFilters(additionalFilters)
				.setSortList(sortList).setOverrideLimit(overrideLimit).setOverrideOffset(overrideOffset)
				.setSelectedFacets(selectedFacets).build();

		assertEquals("SELECT * FROM syn123 LIMIT 6 OFFSET 7", combined.getCombinedSql());
	}
	
	@Test
	public void testGetCombinedSqlWithExistingSort() {
		overrideLimit = null;
		overrideOffset = null;
		additionalFilters = null;
		sortList = List.of(new SortItem().setColumn("foo").setDirection(SortDirection.DESC));
		selectedFacets = null;
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(schemaProvider)
				.setQuery("select * from syn123 order by aBool ASC").setAdditionalFilters(additionalFilters)
				.setSortList(sortList).setOverrideLimit(overrideLimit).setOverrideOffset(overrideOffset)
				.setSelectedFacets(selectedFacets).build();

		assertEquals("SELECT * FROM syn123 ORDER BY \"foo\" DESC, aBool ASC", combined.getCombinedSql());
	}

	@Test
	public void testGetCombinedSqlWithEmptyAdditionalFilters() {
		overrideLimit = null;
		overrideOffset = null;
		additionalFilters = Collections.emptyList();
		sortList = null;
		selectedFacets = null;
		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(schemaProvider)
				.setQuery("select * from syn123").setAdditionalFilters(additionalFilters).setSortList(sortList)
				.setOverrideLimit(overrideLimit).setOverrideOffset(overrideOffset).setSelectedFacets(selectedFacets)
				.build();

		assertEquals("SELECT * FROM syn123", combined.getCombinedSql());
	}

	@Test
	public void testGetCombinedSqlWithUnknownFacet() {

		selectedFacets = List
				.of(new FacetColumnValuesRequest().setColumnName("doesNotExist").setFacetValues(Set.of("1")));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			CombinedQuery.builder().setSchemaProvider(schemaProvider)
					.setQuery("select aBool from syn123 where aBool is true").setAdditionalFilters(additionalFilters)
					.setSortList(sortList).setOverrideLimit(overrideLimit).setOverrideOffset(overrideOffset)
					.setSelectedFacets(selectedFacets).build();
		}).getMessage();
		assertEquals("Facet selection: 'doesNotExist' does not match any column name of the schema", message);
	}

	@Test
	public void testGetCombinedSqlWithAllNull() {
		additionalFilters = null;
		selectedFacets = null;
		sortList = null;
		overrideLimit = null;
		overrideOffset = null;

		// call under test
		CombinedQuery combined = CombinedQuery.builder().setSchemaProvider(schemaProvider)
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
			CombinedQuery.builder().setSchemaProvider(schemaProvider).setQuery("this is not valid sql")
					.setAdditionalFilters(additionalFilters).setSortList(sortList).setOverrideLimit(overrideLimit)
					.setOverrideOffset(overrideOffset).setSelectedFacets(selectedFacets).build();
		});
	}

	/**
	 * Helper to create a schema provider for the given schema.
	 * 
	 * @param schema
	 * @return
	 */
	SchemaProvider schemaProvider(List<ColumnModel> schema) {
		return (IdAndVersion tableId) -> {
			return schema;
		};
	}
}
