package org.sagebionetworks.repo.manager.table.query;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.sagebionetworks.repo.model.AggregateCountSuppressionStrategy;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnSingleValueFilterOperator;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.RowSuppressionReasonCode;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.web.RowSuppressionException;
import org.sagebionetworks.table.cluster.CountSuppressionSpec;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;
import org.sagebionetworks.table.cluster.description.VirtualTableIndexDescription;
import org.sagebionetworks.table.query.ParseException;

public class MainQueryTest {

	private List<ColumnModel> schema;
	private SchemaProvider schemaProvider;
	private IndexDescription indexDescription;
	private IdAndVersion tableId;
	private Long userId;
	private Long maxBytesPerPage;
	private QueryContext.Builder builder;
	private String startingSql;

	@BeforeEach
	public void before() {

		schema = List.of(
				TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING_LIST).setFacetType(FacetType.enumeration),
				TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER),
				TableModelTestUtils.createColumn(3L, "three", ColumnType.STRING));

		schemaProvider = Mockito.mock(SchemaProvider.class);
		when(schemaProvider.getTableSchema(any())).thenReturn(schema);
		when(schemaProvider.getColumnModel(any())).thenReturn(schema.get(0));
		maxBytesPerPage = 100_000_000L;

		userId = 789L;
		tableId = IdAndVersion.parse("syn123.4");

		indexDescription = new ViewIndexDescription(tableId, TableType.entityview, -1L);

		// The starting sql will have an authorization filter applied.
		startingSql = "select * from " + tableId + " where ROW_BENEFACTOR IN (11,22)";

		builder = QueryContext.builder().setIndexDescription(indexDescription).setSchemaProvider(schemaProvider)
				.setUserId(userId).setMaxBytesPerPage(maxBytesPerPage).setStartingSql(startingSql)
				.setMaxRowsPerCall(100L);
	}

	@Test
	public void testMainQueryWithAllParts() {
		// use a query with all parts set.
		builder.setAdditionalFilters(List.of(new ColumnSingleValueQueryFilter().setColumnName("two")
				.setOperator(ColumnSingleValueFilterOperator.EQUAL).setValues(List.of("99", "89"))));
		builder.setSelectedFacets(
				List.of(new FacetColumnValuesRequest().setColumnName("one").setFacetValues(Set.of("cat"))));
		builder.setSort(List.of(new SortItem().setColumn("three").setDirection(SortDirection.DESC)));
		builder.setLimit(12L);
		builder.setOffset(3L);

		// call under test
		MainQuery main = new MainQuery(builder.build());
		assertNotNull(main);
		assertNotNull(main.getTranslator());
		assertEquals("SELECT _C1_, _C2_, _C3_, ROW_ID, ROW_VERSION, ROW_BENEFACTOR FROM T123_4 WHERE"
			// selected facet
			+ " ( ( ( ( JSON_OVERLAPS(LOWER(_C1_),LOWER(JSON_ARRAY(:b0))) IS TRUE ) ) ) )"
			// additional filter
			+ " AND ( ( ( _C2_ = :b1 OR _C2_ = :b2 ) )"
			// original authorization filter
			+ " AND ( ROW_BENEFACTOR IN ( :b3, :b4 ) ) )"
			// additional sort
			+ " ORDER BY _C3_ DESC"
			// override pagination
			+ " LIMIT :b5 OFFSET :b6", main.getTranslator().getOutputSQL());
		
		Map<String, Object> expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", "cat");
		expectedParmeters.put("b1", 89L);
		expectedParmeters.put("b2", 99L);
		expectedParmeters.put("b3", 11L);
		expectedParmeters.put("b4", 22L);
		expectedParmeters.put("b5", 12L);
		expectedParmeters.put("b6", 3L);
		assertEquals(expectedParmeters, main.getTranslator().getParameters());
	}

	@Test
	public void testMainQueryWithOverMaxBytePerPage() {
		builder.setMaxBytesPerPage(5L);
		// call under test
		MainQuery main = new MainQuery(builder.build());
		assertNotNull(main);
		assertNotNull(main.getTranslator());
		assertEquals("SELECT _C1_, _C2_, _C3_, ROW_ID, ROW_VERSION, ROW_BENEFACTOR FROM T123_4 WHERE"
				+ " ROW_BENEFACTOR IN ( :b0, :b1 )" + " LIMIT :b2 OFFSET :b3", main.getTranslator().getOutputSQL());
		Map<String, Object> expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", 11L);
		expectedParmeters.put("b1", 22L);
		// with so few bytes per row only a single row can be selected.
		expectedParmeters.put("b2", 1L);
		expectedParmeters.put("b3", 0L);
		assertEquals(expectedParmeters, main.getTranslator().getParameters());
	}

	@Test
	public void testMainQueryWithUserId() {
		startingSql = "select two from " + tableId + " where two = CURRENT_USER()";
		builder.setStartingSql(startingSql);

		// call under test
		MainQuery main = new MainQuery(builder.build());
		assertNotNull(main);
		assertNotNull(main.getTranslator());
		assertEquals("SELECT _C2_, ROW_ID, ROW_VERSION, ROW_BENEFACTOR FROM T123_4 WHERE _C2_ = :b0 LIMIT :b1 OFFSET :b2",
				main.getTranslator().getOutputSQL());
		Map<String, Object> expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", userId);
		expectedParmeters.put("b1", 5000000L);
		expectedParmeters.put("b2", 0L);
		assertEquals(expectedParmeters, main.getTranslator().getParameters());
	}
	
	@Test
	public void testMainQueryWithIncludeEtag() {
		startingSql = "select two from " + tableId;
		builder.setStartingSql(startingSql);
		builder.setIncludeEntityEtag(true);

		// call under test
		MainQuery main = new MainQuery(builder.build());
		assertNotNull(main);
		assertNotNull(main.getTranslator());
		assertEquals("SELECT _C2_, ROW_ID, ROW_VERSION, ROW_ETAG, ROW_BENEFACTOR FROM T123_4 LIMIT :b0 OFFSET :b1",
				main.getTranslator().getOutputSQL());
		Map<String, Object> expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", 5000000L);
		expectedParmeters.put("b1", 0L);
		assertEquals(expectedParmeters, main.getTranslator().getParameters());
	}

	@Test
	public void testCreateCountSuppressionSpecWithExplicitStrategy() {
		AggregateDataConfiguration config = new AggregateDataConfiguration().setSuppressionThreshold(5L)
				.setQuasiIdentifierColumnNames(List.of("one"))
				.setCountSuppressionStrategy(AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD);

		// call under test
		CountSuppressionSpec spec = MainQuery.createCountSuppressionSpec(config, Set.of());
		assertEquals(new CountSuppressionSpec(AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, 5L, Set.of()),
				spec);
	}

	@Test
	public void testCreateCountSuppressionSpecWithDefiningCounts() {
		AggregateDataConfiguration config = new AggregateDataConfiguration().setSuppressionThreshold(5L)
				.setQuasiIdentifierColumnNames(List.of("one"))
				.setCountSuppressionStrategy(AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD);

		// call under test
		CountSuppressionSpec spec = MainQuery.createCountSuppressionSpec(config, Set.of("12", "13"));
		assertEquals(new CountSuppressionSpec(AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, 5L,
				Set.of("12", "13")), spec);
	}

	@Test
	public void testCreateCountSuppressionSpecWithNoStrategyDefaultsToExclude() {
		AggregateDataConfiguration config = new AggregateDataConfiguration().setSuppressionThreshold(3L)
				.setQuasiIdentifierColumnNames(List.of("one"));

		// call under test
		CountSuppressionSpec spec = MainQuery.createCountSuppressionSpec(config, Set.of("12"));
		assertEquals(new CountSuppressionSpec(AggregateCountSuppressionStrategy.EXCLUDE_ROW, 3L, Set.of("12")), spec);
	}

	@Test
	public void testCreateCountSuppressionSpecWithNoConfig() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			MainQuery.createCountSuppressionSpec(null, Set.of());
		}).getMessage();
		assertEquals("configuration is required.", message);
	}

	@Test
	public void testCreateCountSuppressionSpecWithNoThreshold() {
		AggregateDataConfiguration config = new AggregateDataConfiguration()
				.setQuasiIdentifierColumnNames(List.of("one"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			MainQuery.createCountSuppressionSpec(config, Set.of());
		}).getMessage();
		assertEquals("configuration.suppressionThreshold is required.", message);
	}

	/**
	 * Configure the context as an aggregate read that returns rows, where column 'two' carries a
	 * quasi-identifier.
	 */
	private void setupRowReturningAggregate(String sql) {
		AggregateDataConfiguration config = new AggregateDataConfiguration().setSuppressionThreshold(5L)
				.setQuasiIdentifierColumnNames(List.of("two"))
				.setCountSuppressionStrategy(AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD);
		builder.setStartingSql(sql).setAggregateDataConfiguration(config)
				.setQidColumns(new QidColumns(Set.of("2"), Set.of()));
	}

	private static final String MASKED_COUNT_BY_THREE = "SELECT _C3_, CASE WHEN COUNT(*) > 0 AND COUNT(*) < 5 THEN -1"
			+ " ELSE COUNT(*) END FROM T123_4";

	@Test
	public void testMainQueryWithRowReturningAggregate() {
		setupRowReturningAggregate("select three, count(*) from " + tableId + " group by three");

		// call under test
		MainQuery main = new MainQuery(builder.build());
		// the protected count is suppressed in the SQL
		assertEquals(MASKED_COUNT_BY_THREE + " GROUP BY _C3_ LIMIT :b0 OFFSET :b1", main.getTranslator().getOutputSQL());
	}

	@Test
	public void testMainQueryWithRowReturningAggregateAndNoQidColumns() {
		AggregateDataConfiguration config = new AggregateDataConfiguration().setSuppressionThreshold(5L)
				.setQuasiIdentifierColumnNames(List.of("two"));
		builder.setStartingSql("select three, count(*) from " + tableId + " group by three")
				.setAggregateDataConfiguration(config).setQidColumns(QidColumns.NONE);

		// call under test
		MainQuery main = new MainQuery(builder.build());
		// With no strategy set, below-threshold rows are excluded.
		assertEquals("SELECT _C3_, COUNT(*) FROM T123_4 GROUP BY _C3_ HAVING (COUNT(*) = 0 OR COUNT(*) >= 5)"
				+ " LIMIT :b0 OFFSET :b1", main.getTranslator().getOutputSQL());
	}

	@Test
	public void testMainQueryWithNoQidColumns() {
		// Without QID columns no row-level restriction applies, so nothing is validated or suppressed.
		builder.setStartingSql("select three, count(*) from " + tableId + " group by three")
				.setAggregateDataConfiguration(new AggregateDataConfiguration().setSuppressionThreshold(5L)
						.setQuasiIdentifierColumnNames(List.of("two")));

		// call under test
		MainQuery main = new MainQuery(builder.build());
		assertEquals("SELECT _C3_, COUNT(*) FROM T123_4 GROUP BY _C3_ LIMIT :b0 OFFSET :b1",
				main.getTranslator().getOutputSQL());
	}

	@Test
	public void testMainQueryWithQidColumnsAndNoConfiguration() {
		builder.setStartingSql("select three, count(*) from " + tableId + " group by three")
				.setQidColumns(new QidColumns(Set.of("2"), Set.of()));

		String message = assertThrows(IllegalStateException.class, () -> {
			// call under test
			new MainQuery(builder.build());
		}).getMessage();
		assertEquals("Quasi-identifier columns require an aggregate data configuration", message);
	}

	@Test
	public void testMainQueryWithPositionalSort() {
		// MySQL reads the position against the translated select list, which translation can widen.
		setupRowReturningAggregate("select three, count(*) from " + tableId + " group by three order by 1");

		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			new MainQuery(builder.build());
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testMainQueryWithSortItemNamingQid() {
		setupRowReturningAggregate("select three, count(two) from " + tableId + " group by three");
		builder.setSort(List.of(new SortItem().setColumn("two")));

		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			new MainQuery(builder.build());
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testMainQueryWithSortItemNamingCountAlias() {
		setupRowReturningAggregate("select three, count(*) as n from " + tableId + " group by three");
		builder.setSort(List.of(new SortItem().setColumn("n").setDirection(SortDirection.DESC)));

		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			new MainQuery(builder.build());
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testMainQueryWithSortItemNamingCount() {
		setupRowReturningAggregate("select three, count(*) from " + tableId + " group by three");
		builder.setSort(List.of(new SortItem().setColumn("count(*)")));

		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			new MainQuery(builder.build());
		});
		assertEquals(RowSuppressionReasonCode.QID_IN_ORDER_BY, ex.getReasonCode());
	}

	@Test
	public void testMainQueryWithSortItemNamingNonQid() {
		setupRowReturningAggregate("select three, count(*) from " + tableId + " group by three");
		builder.setSort(List.of(new SortItem().setColumn("three")));

		// call under test
		MainQuery main = new MainQuery(builder.build());
		assertEquals(MASKED_COUNT_BY_THREE + " GROUP BY _C3_ ORDER BY _C3_ ASC LIMIT :b0 OFFSET :b1",
				main.getTranslator().getOutputSQL());
	}

	@Test
	public void testMainQueryWithAdditionalFilterOnQid() {
		// A request-supplied filter only restricts the rows, so it may name a QID.
		setupRowReturningAggregate("select three, count(*) from " + tableId + " group by three");
		builder.setAdditionalFilters(List.of(new ColumnSingleValueQueryFilter().setColumnName("two")
				.setOperator(ColumnSingleValueFilterOperator.EQUAL).setValues(List.of("99"))));

		// call under test
		MainQuery main = new MainQuery(builder.build());
		assertEquals(MASKED_COUNT_BY_THREE + " WHERE ( _C2_ = :b0 ) GROUP BY _C3_ LIMIT :b1 OFFSET :b2",
				main.getTranslator().getOutputSQL());
	}

	/**
	 * Configure the context as an aggregate read that returns rows from the VirtualTable syn2, the file
	 * perspective over the participant x file source syn1. The source column individualId (10) is the
	 * quasi-identifier; the VirtualTable's individualCount (12) counts it, and its fileId reuses the
	 * source column (11).
	 */
	private void setupVirtualTable(String definingSql, String sql, AggregateCountSuppressionStrategy strategy,
			Set<String> qidColumnIds) {
		ColumnModel individualId = TableModelTestUtils.createColumn(10L, "individualId", ColumnType.INTEGER);
		ColumnModel fileId = TableModelTestUtils.createColumn(11L, "fileId", ColumnType.INTEGER);
		ColumnModel individualCount = TableModelTestUtils.createColumn(12L, "individualCount", ColumnType.INTEGER);
		IdAndVersion sourceId = IdAndVersion.parse("syn1");
		IdAndVersion virtualTableId = IdAndVersion.parse("syn2");
		when(schemaProvider.getTableSchema(sourceId)).thenReturn(List.of(individualId, fileId));
		when(schemaProvider.getTableSchema(virtualTableId)).thenReturn(List.of(fileId, individualCount));
		when(schemaProvider.getColumnModel("10")).thenReturn(individualId);
		when(schemaProvider.getColumnModel("11")).thenReturn(fileId);
		when(schemaProvider.getColumnModel("12")).thenReturn(individualCount);

		IndexDescription virtualTable = new VirtualTableIndexDescription(virtualTableId, definingSql,
				TableIndexDescription::new);
		AggregateDataConfiguration config = new AggregateDataConfiguration().setSuppressionThreshold(5L)
				.setQuasiIdentifierColumnNames(List.of("individualId")).setCountSuppressionStrategy(strategy);
		// individualCount is the only column whose lineage is an aggregate.
		Set<String> aggregateQidColumnIds = qidColumnIds.contains("12") ? Set.of("12") : Set.of();
		builder.setIndexDescription(virtualTable).setStartingSql(virtualTable.preprocessQuery(sql))
				.setAggregateDataConfiguration(config)
				.setQidColumns(new QidColumns(qidColumnIds, aggregateQidColumnIds));
	}

	private static final String FILE_PERSPECTIVE_SQL = "select fileId, count(distinct individualId) as individualCount"
			+ " from syn1 group by fileId";

	@Test
	public void testMainQueryWithVirtualTableCountAndMask() {
		setupVirtualTable(FILE_PERSPECTIVE_SQL, "select * from syn2 order by fileId",
				AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, Set.of("12"));

		// call under test
		MainQuery main = new MainQuery(builder.build());
		// the count is suppressed where the defining SQL computes it
		assertEquals("WITH T2 (_C11_, _C12_) AS (SELECT _C11_, CASE WHEN COUNT(DISTINCT _C10_) > 0 AND"
				+ " COUNT(DISTINCT _C10_) < 5 THEN -1 ELSE COUNT(DISTINCT _C10_) END AS individualCount FROM T1"
				+ " GROUP BY _C11_) SELECT _C11_, _C12_ FROM T2 ORDER BY _C11_ LIMIT :b0 OFFSET :b1",
				main.getTranslator().getOutputSQL());
	}

	@Test
	public void testMainQueryWithVirtualTableCountAndExclude() {
		setupVirtualTable(FILE_PERSPECTIVE_SQL, "select fileId, individualCount from syn2",
				AggregateCountSuppressionStrategy.EXCLUDE_ROW, Set.of("12"));

		// call under test
		MainQuery main = new MainQuery(builder.build());
		assertEquals("WITH T2 (_C11_, _C12_) AS (SELECT _C11_, COUNT(DISTINCT _C10_) AS individualCount FROM T1"
				+ " GROUP BY _C11_ HAVING (COUNT(DISTINCT _C10_) = 0 OR COUNT(DISTINCT _C10_) >= 5))"
				+ " SELECT _C11_, _C12_ FROM T2 LIMIT :b0 OFFSET :b1", main.getTranslator().getOutputSQL());
	}

	@Test
	public void testMainQueryWithVirtualTableCountInWhere() {
		// Filtering on the count would select exactly the files whose count the suppression hides.
		setupVirtualTable(FILE_PERSPECTIVE_SQL, "select fileId from syn2 where individualCount = 1",
				AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, Set.of("12"));

		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			new MainQuery(builder.build());
		});
		assertEquals(RowSuppressionReasonCode.QID_AGGREGATE_IN_FILTER, ex.getReasonCode());
	}

	@Test
	public void testMainQueryWithVirtualTableCountInAdditionalFilter() {
		setupVirtualTable(FILE_PERSPECTIVE_SQL, "select * from syn2",
				AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, Set.of("12"));
		builder.setAdditionalFilters(List.of(new ColumnSingleValueQueryFilter().setColumnName("individualCount")
				.setOperator(ColumnSingleValueFilterOperator.EQUAL).setValues(List.of("1"))));

		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			new MainQuery(builder.build());
		});
		assertEquals(RowSuppressionReasonCode.QID_AGGREGATE_IN_FILTER, ex.getReasonCode());
	}

	@Test
	public void testMainQueryWithStoredAggregateQidInWhere() {
		// A materialized object's stored count cannot be suppressed where it is computed, but filtering on
		// it is rejected all the same.
		setupRowReturningAggregate("select three, count(*) from " + tableId + " where two = 1 group by three");
		builder.setQidColumns(new QidColumns(Set.of("2"), Set.of("2")));

		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			new MainQuery(builder.build());
		});
		assertEquals(RowSuppressionReasonCode.QID_AGGREGATE_IN_FILTER, ex.getReasonCode());
	}

	@Test
	public void testMainQueryWithStoredAggregateQidCounted() {
		// Counting the rows that carry the aggregate does not filter on its value.
		setupRowReturningAggregate("select three, count(two) from " + tableId + " group by three");
		builder.setQidColumns(new QidColumns(Set.of("2"), Set.of("2")));

		// call under test
		MainQuery main = new MainQuery(builder.build());
		assertEquals("SELECT _C3_, CASE WHEN COUNT(_C2_) > 0 AND COUNT(_C2_) < 5 THEN -1 ELSE COUNT(_C2_) END"
				+ " FROM T123_4 GROUP BY _C3_ LIMIT :b0 OFFSET :b1", main.getTranslator().getOutputSQL());
	}

	@Test
	public void testMainQueryWithVirtualTableCountNotRead() {
		// Excluding the rows of a count the query never reads would drop rows the caller may see.
		setupVirtualTable(FILE_PERSPECTIVE_SQL, "select fileId from syn2",
				AggregateCountSuppressionStrategy.EXCLUDE_ROW, Set.of("12"));

		// call under test
		MainQuery main = new MainQuery(builder.build());
		assertEquals("WITH T2 (_C11_, _C12_) AS (SELECT _C11_, COUNT(DISTINCT _C10_) AS individualCount FROM T1"
				+ " GROUP BY _C11_) SELECT _C11_ FROM T2 LIMIT :b0 OFFSET :b1", main.getTranslator().getOutputSQL());
	}

	@Test
	public void testMainQueryWithVirtualTableAndUnsuppressedQid() {
		// Only the suppressed count stops being a quasi-identifier; any other QID column still is. Here the
		// configuration also names fileId a quasi-identifier, so the VirtualTable's fileId column is one.
		setupVirtualTable(FILE_PERSPECTIVE_SQL, "select * from syn2",
				AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, Set.of("11", "12"));
		builder.setAggregateDataConfiguration(new AggregateDataConfiguration().setSuppressionThreshold(5L)
				.setQuasiIdentifierColumnNames(List.of("individualId", "fileId"))
				.setCountSuppressionStrategy(AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD));

		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			new MainQuery(builder.build());
		});
		assertEquals(RowSuppressionReasonCode.QID_PROJECTED, ex.getReasonCode());
	}

	@Test
	public void testMainQueryWithVirtualTableCountInExpression() {
		// A count wrapped in an expression no longer equals the count the threshold is compared against.
		setupVirtualTable("select fileId, count(distinct individualId) + 1 as individualCount from syn1 group by fileId",
				"select * from syn2", AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, Set.of("12"));

		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			new MainQuery(builder.build());
		});
		assertEquals(RowSuppressionReasonCode.AGGREGATE_NOT_PERMITTED, ex.getReasonCode());
	}

	@Test
	public void testMainQueryWithVirtualTableCardinalityDependentAggregate() {
		// Anyone who can read the source can write a VirtualTable over it, so its defining SQL may compute only a
		// whole count or a distinct-value list: sum(1) is a count that would never be suppressed.
		setupVirtualTable("select fileId, sum(1) as individualCount from syn1 group by fileId",
				"select fileId, individualCount from syn2", AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD,
				Set.of());

		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			new MainQuery(builder.build());
		});
		assertEquals(RowSuppressionReasonCode.AGGREGATE_NOT_PERMITTED, ex.getReasonCode());
	}

	@Test
	public void testMainQueryWithVirtualTableCountInDefiningConditionFilter() {
		// A defining condition is merged into the defining SQL, where it would see the count before it is
		// suppressed.
		setupVirtualTable(FILE_PERSPECTIVE_SQL, "select fileId from syn2",
				AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, Set.of("12"));
		builder.setAdditionalFilters(List.of(new ColumnSingleValueQueryFilter().setColumnName("individualCount")
				.setIsDefiningCondition(true).setOperator(ColumnSingleValueFilterOperator.EQUAL)
				.setValues(List.of("1"))));

		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			new MainQuery(builder.build());
		});
		assertEquals(RowSuppressionReasonCode.QID_AGGREGATE_IN_FILTER, ex.getReasonCode());
	}

	@Test
	public void testMainQueryWithVirtualTableSourceAggregateInDefiningConditionFilter() {
		// Here the source stores fileId as an aggregate of a quasi-identifier, as a MaterializedView's count
		// would be. A defining condition filters the source, so it is checked against the source's columns.
		setupVirtualTable(FILE_PERSPECTIVE_SQL, "select fileId from syn2",
				AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, Set.of("12"));
		builder.setInlinedSourceQidColumns(new QidColumns(Set.of("11"), Set.of("11")));
		builder.setAdditionalFilters(List.of(new ColumnSingleValueQueryFilter().setColumnName("fileId")
				.setIsDefiningCondition(true).setOperator(ColumnSingleValueFilterOperator.EQUAL)
				.setValues(List.of("1"))));

		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			new MainQuery(builder.build());
		});
		assertEquals(RowSuppressionReasonCode.QID_AGGREGATE_IN_FILTER, ex.getReasonCode());
	}

	@Test
	public void testMainQueryWithVirtualTableSourceQidInDefiningConditionFilter() {
		// A quasi-identifier that is not an aggregate only restricts the rows, so a defining condition may
		// name it.
		setupVirtualTable(FILE_PERSPECTIVE_SQL, "select fileId from syn2",
				AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, Set.of("12"));
		builder.setInlinedSourceQidColumns(new QidColumns(Set.of("10"), Set.of()));
		builder.setAdditionalFilters(List.of(new ColumnSingleValueQueryFilter().setColumnName("individualId")
				.setIsDefiningCondition(true).setOperator(ColumnSingleValueFilterOperator.EQUAL)
				.setValues(List.of("1"))));

		// call under test
		MainQuery main = new MainQuery(builder.build());
		assertEquals("WITH T2 (_C11_, _C12_) AS (SELECT _C11_, COUNT(DISTINCT _C10_) AS individualCount FROM T1"
				+ " WHERE ( _C10_ = :b2 ) GROUP BY _C11_) SELECT _C11_ FROM T2 LIMIT :b0 OFFSET :b1",
				main.getTranslator().getOutputSQL());
	}

	@Test
	public void testMainQueryWithVirtualTableCountInSelectedFacet() {
		setupVirtualTable(FILE_PERSPECTIVE_SQL, "select fileId from syn2",
				AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, Set.of("12"));
		ColumnModel fileId = TableModelTestUtils.createColumn(11L, "fileId", ColumnType.INTEGER);
		ColumnModel individualCount = TableModelTestUtils.createColumn(12L, "individualCount", ColumnType.INTEGER)
				.setFacetType(FacetType.enumeration);
		when(schemaProvider.getTableSchema(IdAndVersion.parse("syn2"))).thenReturn(List.of(fileId, individualCount));
		builder.setSelectedFacets(List.of(
				new FacetColumnValuesRequest().setColumnName("individualCount").setFacetValues(Set.of("1"))));

		RowSuppressionException ex = assertThrows(RowSuppressionException.class, () -> {
			// call under test
			new MainQuery(builder.build());
		});
		assertEquals(RowSuppressionReasonCode.QID_AGGREGATE_IN_FILTER, ex.getReasonCode());
	}

	@Test
	public void testMainQueryWithVirtualTableCountAfterCastToDouble() {
		// The cast adds a NULL column to the translated defining SQL ahead of the count (PLFM-10108 review).
		ColumnModel score = TableModelTestUtils.createColumn(13L, "score", ColumnType.STRING);
		ColumnModel maxScore = TableModelTestUtils.createColumn(14L, "maxScore", ColumnType.DOUBLE);
		setupVirtualTable("select fileId, cast(score as 14), count(distinct individualId) as individualCount"
				+ " from syn1 group by fileId", "select individualCount from syn2",
				AggregateCountSuppressionStrategy.MASK_BELOW_THRESHOLD, Set.of("12"));
		ColumnModel individualId = TableModelTestUtils.createColumn(10L, "individualId", ColumnType.INTEGER);
		ColumnModel fileId = TableModelTestUtils.createColumn(11L, "fileId", ColumnType.INTEGER);
		ColumnModel individualCount = TableModelTestUtils.createColumn(12L, "individualCount", ColumnType.INTEGER);
		when(schemaProvider.getTableSchema(IdAndVersion.parse("syn1"))).thenReturn(List.of(individualId, fileId, score));
		when(schemaProvider.getTableSchema(IdAndVersion.parse("syn2")))
				.thenReturn(List.of(fileId, maxScore, individualCount));
		when(schemaProvider.getColumnModel("14")).thenReturn(maxScore);

		// call under test
		MainQuery main = new MainQuery(builder.build());
		assertEquals("WITH T2 (_C11_, _C14_, _DBL_C14_, _C12_) AS (SELECT _C11_, CAST(_C13_ AS DOUBLE), NULL,"
				+ " CASE WHEN COUNT(DISTINCT _C10_) > 0 AND COUNT(DISTINCT _C10_) < 5 THEN -1 ELSE COUNT(DISTINCT _C10_) END"
				+ " AS individualCount FROM T1 GROUP BY _C11_) SELECT _C12_ FROM T2 LIMIT :b0 OFFSET :b1",
				main.getTranslator().getOutputSQL());
	}

	@Test
	public void testCountQueryWithNullExpansion() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new MainQuery(null);
		}).getMessage();
		assertEquals("expansion is required.", message);
	}

	@Test
	public void testCountQueryWithInvalidSql() {
		builder.setStartingSql("this is not sql!");
		Throwable cause = assertThrows(IllegalArgumentException.class, () -> {
			new MainQuery(builder.build());
		}).getCause();
		assertTrue(cause instanceof ParseException);
	}
}
