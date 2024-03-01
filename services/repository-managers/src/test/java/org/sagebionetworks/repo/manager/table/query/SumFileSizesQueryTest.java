package org.sagebionetworks.repo.manager.table.query;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnSingleValueFilterOperator;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnRangeRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;
import org.sagebionetworks.table.query.ParseException;

public class SumFileSizesQueryTest {

	private List<ColumnModel> schema;
	private SchemaProvider schemaProvider;
	private IndexDescription indexDescription;
	private IdAndVersion tableId;
	private Long userId;
	private QueryContext.Builder builder;
	private String startingSql;
	private Long maxRowsPerCall;

	@BeforeEach
	public void before() {

		schema = List.of(
				TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING).setFacetType(FacetType.enumeration),
				TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER).setFacetType(FacetType.range),
				TableModelTestUtils.createColumn(3L, "three", ColumnType.STRING));

		schemaProvider = Mockito.mock(SchemaProvider.class);
		when(schemaProvider.getTableSchema(any())).thenReturn(schema);
		when(schemaProvider.getColumnModel(any())).thenReturn(schema.get(0));

		userId = 789L;
		tableId = IdAndVersion.parse("syn123.4");

		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);

		// The starting sql will have an authorization filter applied.
		startingSql = "select * from " + tableId + " where ROW_BENEFACTOR IN (11,22)";

		maxRowsPerCall = 18L;

		builder = QueryContext.builder().setIndexDescription(indexDescription).setSchemaProvider(schemaProvider)
				.setUserId(userId).setMaxBytesPerPage(10_000L).setStartingSql(startingSql)
				.setMaxRowsPerCall(maxRowsPerCall);
	}

	@Test
	public void testBuildWithAllParts() {
		builder.setAdditionalFilters(List.of(new ColumnSingleValueQueryFilter().setColumnName("three")
				.setOperator(ColumnSingleValueFilterOperator.EQUAL).setValues(List.of("a", "b"))));
		builder.setSelectedFacets(
				List.of(new FacetColumnValuesRequest().setColumnName("one").setFacetValues(Set.of("cat")),
						new FacetColumnRangeRequest().setColumnName("two").setMax("38").setMin("15")));

		// call under test
		SumFileSizesQuery size = new SumFileSizesQuery(builder.build());
		assertNotNull(size);
		String sql = "SELECT ROW_ID, ROW_VERSION FROM T123_4 WHERE"
				+ " ( ( ( _C1_ = :b0 ) AND ( _C2_ BETWEEN :b1 AND :b2 ) ) )"
				+ " AND ( ( ( _C3_ = :b3 OR _C3_ = :b4 ) )"
				+ " AND ( ROW_BENEFACTOR IN ( :b5, :b6 ) ) )"
				// maxRowsPerCall + 1
				+ " LIMIT 19";

		Map<String, Object> expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", "cat");
		expectedParmeters.put("b1", 15L);
		expectedParmeters.put("b2", 38L);
		expectedParmeters.put("b3", "a");
		expectedParmeters.put("b4", "b");
		expectedParmeters.put("b5", 11L);
		expectedParmeters.put("b6", 22L);
		assertEquals(Optional.of(new BasicQuery(sql, expectedParmeters)), size.getRowIdAndVersionQuery());
	}

	@Test
	public void testBuildWithNullParts() {
		builder.setAdditionalFilters(null);
		builder.setSelectedFacets(null);

		// call under test
		SumFileSizesQuery size = new SumFileSizesQuery(builder.build());
		assertNotNull(size);
		String sql = "SELECT ROW_ID, ROW_VERSION FROM T123_4 WHERE ROW_BENEFACTOR IN ( :b0, :b1 ) LIMIT 19";
		Map<String, Object> expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", 11L);
		expectedParmeters.put("b1", 22L);
		assertEquals(Optional.of(new BasicQuery(sql, expectedParmeters)), size.getRowIdAndVersionQuery());
	}

	@Test
	public void testBuildWithTypeDataset() {

		builder.setIndexDescription(new ViewIndexDescription(tableId, TableType.dataset));
		builder.setAdditionalFilters(null);
		builder.setSelectedFacets(null);

		// call under test
		SumFileSizesQuery size = new SumFileSizesQuery(builder.build());
		assertNotNull(size);
		String sql = "SELECT ROW_ID, ROW_VERSION FROM T123_4 WHERE ROW_BENEFACTOR IN ( :b0, :b1 ) LIMIT 19";
		Map<String, Object> expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", 11L);
		expectedParmeters.put("b1", 22L);
		assertEquals(Optional.of(new BasicQuery(sql, expectedParmeters)), size.getRowIdAndVersionQuery());
	}

	@Test
	public void testBuildWithNonFileView() {
		builder.setIndexDescription(new TableIndexDescription(tableId));
		builder.setAdditionalFilters(null);
		builder.setSelectedFacets(null);

		// call under test
		SumFileSizesQuery size = new SumFileSizesQuery(builder.build());
		assertNotNull(size);
		assertEquals(Optional.empty(), size.getRowIdAndVersionQuery());
	}

	@Test
	public void testBuildWithAggregateQuery() {

		builder.setAdditionalFilters(null);
		builder.setStartingSql("select count(*) from " + tableId);

		// call under test
		SumFileSizesQuery size = new SumFileSizesQuery(builder.build());
		assertNotNull(size);
		assertEquals(Optional.empty(), size.getRowIdAndVersionQuery());
	}
	
	@Test
	public void testCountQueryWithNullExpansion() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new SumFileSizesQuery(null);
		}).getMessage();
		assertEquals("expansion is required.", message);
	}

	@Test
	public void testCountQueryWithInvalidSql() {
		builder.setStartingSql("this is not sql!");
		Throwable cause = assertThrows(IllegalArgumentException.class, () -> {
			new SumFileSizesQuery(builder.build());
		}).getCause();
		assertTrue(cause instanceof ParseException);
	}
}
