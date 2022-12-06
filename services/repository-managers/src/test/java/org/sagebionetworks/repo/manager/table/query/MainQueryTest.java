package org.sagebionetworks.repo.manager.table.query;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnSingleValueFilterOperator;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;
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

		schemaProvider = (IdAndVersion tableId) -> {
			return schema;
		};
		maxBytesPerPage = 100_000_000L;

		userId = 789L;
		tableId = IdAndVersion.parse("syn123.4");

		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);

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
		assertEquals("SELECT _C1_, _C2_, _C3_, ROW_ID, ROW_VERSION FROM T123_4 WHERE"
				// original authorization filter
				+ " ( ( ROW_BENEFACTOR IN ( :b0, :b1 ) )"
				// additional filter
				+ " AND ( ( _C2_ = :b2 OR _C2_ = :b3 ) ) )"
				// selected facet
				+ " AND ( ( ( ROW_ID IN ( SELECT ROW_ID_REF_C1_ FROM T123_4_INDEX_C1_ WHERE _C1__UNNEST IN ( :b4 ) ) ) ) )"
				// additional sort
				+ " ORDER BY _C3_ DESC"
				// override pagination
				+ " LIMIT :b5 OFFSET :b6", main.getTranslator().getOutputSQL());
		Map<String, Object> expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", 11L);
		expectedParmeters.put("b1", 22L);
		expectedParmeters.put("b2", 99L);
		expectedParmeters.put("b3", 89L);
		expectedParmeters.put("b4", "cat");
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
		assertEquals("SELECT _C1_, _C2_, _C3_, ROW_ID, ROW_VERSION FROM T123_4 WHERE"
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
		assertEquals("SELECT _C2_, ROW_ID, ROW_VERSION FROM T123_4 WHERE _C2_ = :b0 LIMIT :b1 OFFSET :b2",
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
		assertEquals("SELECT _C2_, ROW_ID, ROW_VERSION, ROW_ETAG FROM T123_4 LIMIT :b0 OFFSET :b1",
				main.getTranslator().getOutputSQL());
		Map<String, Object> expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", 5000000L);
		expectedParmeters.put("b1", 0L);
		assertEquals(expectedParmeters, main.getTranslator().getParameters());
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
