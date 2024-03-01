package org.sagebionetworks.repo.manager.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.table.FacetTransformer;
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
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;

public class FacetQueriesTest {

	private List<ColumnModel> schema;
	private SchemaProvider schemaProvider;
	private IndexDescription indexDescription;
	private IdAndVersion tableId;
	private Long userId;
	private QueryContext.Builder builder;
	private String startingSql;

	@BeforeEach
	public void before() {

		schema = List.of(
				TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING).setFacetType(FacetType.enumeration),
				TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER).setFacetType(FacetType.range),
				TableModelTestUtils.createColumn(3L, "three", ColumnType.STRING));

		schemaProvider = Mockito.mock(SchemaProvider.class);
		when(schemaProvider.getTableSchema(any())).thenReturn(schema);

		userId = 789L;
		tableId = IdAndVersion.parse("syn123.4");

		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);

		// The starting sql will have an authorization filter applied.
		startingSql = "select * from " + tableId + " where ROW_BENEFACTOR IN (11,22)";

		builder = QueryContext.builder().setIndexDescription(indexDescription).setSchemaProvider(schemaProvider)
				.setUserId(userId).setStartingSql(startingSql).setMaxRowsPerCall(100L);
	}

	@Test
	public void testFacetQueriesWithAllParts() {
		// use a query with all parts set.
		builder.setAdditionalFilters(List.of(new ColumnSingleValueQueryFilter().setColumnName("three")
				.setOperator(ColumnSingleValueFilterOperator.EQUAL).setValues(List.of("a", "b"))));
		builder.setSelectedFacets(
				List.of(new FacetColumnValuesRequest().setColumnName("one").setFacetValues(Set.of("cat")),
						new FacetColumnRangeRequest().setColumnName("two").setMax("38").setMin("15")));

		// call under test
		FacetQueries facet = new FacetQueries(builder.build());
		assertNotNull(facet.getFacetInformationQueries());
		assertEquals(2, facet.getFacetInformationQueries().size());
		// one
		FacetTransformer transformer = facet.getFacetInformationQueries().get(0);
		assertEquals("SELECT _C1_ AS value, COUNT(*) AS frequency FROM T123_4 WHERE"
				// additional filter
				+ " ( ( ( _C3_ = :b0 OR _C3_ = :b1 ) )"
				// authentication filter
				+ " AND ( ROW_BENEFACTOR IN ( :b2, :b3 ) ) )"
				// selected facet on other row
				+ " AND ( ( ( _C2_ BETWEEN :b4 AND :b5 ) ) )"
				+ " GROUP BY _C1_ ORDER BY frequency DESC, value ASC LIMIT :b6",
				transformer.getFacetSqlQuery().getOutputSQL());
		
		Map<String, Object> expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", "a");
		expectedParmeters.put("b1", "b");		
		expectedParmeters.put("b2", 11L);
		expectedParmeters.put("b3", 22L);
		expectedParmeters.put("b4", 15L);
		expectedParmeters.put("b5", 38L);
		expectedParmeters.put("b6", 100L);
		assertEquals(expectedParmeters, transformer.getFacetSqlQuery().getParameters());

		// two
		transformer = facet.getFacetInformationQueries().get(1);
		assertEquals("SELECT MIN(_C2_) AS minimum, MAX(_C2_) AS maximum FROM T123_4 WHERE"
			// selected facet on other column
			+ " ( ( ( _C1_ = :b0 ) ) )"
			// additional filter
			+ " AND ( ( ( _C3_ = :b1 OR _C3_ = :b2 ) )"
			// auth filter
			+ " AND ( ROW_BENEFACTOR IN ( :b3, :b4 ) ) )", 
			transformer.getFacetSqlQuery().getOutputSQL());
		
		expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", "cat");
		expectedParmeters.put("b1", "a");
		expectedParmeters.put("b2", "b");
		expectedParmeters.put("b3", 11L);
		expectedParmeters.put("b4", 22L);
		
		assertEquals(expectedParmeters, transformer.getFacetSqlQuery().getParameters());
	}

	@Test
	public void testFacetQueriesWithNoSelectedAndNoAdditional() {
		// use a query with all parts set.
		builder.setAdditionalFilters(null);
		builder.setSelectedFacets(null);

		// call under test
		FacetQueries facet = new FacetQueries(builder.build());
		assertNotNull(facet.getFacetInformationQueries());
		assertEquals(2, facet.getFacetInformationQueries().size());

		// one
		FacetTransformer transformer = facet.getFacetInformationQueries().get(0);
		assertEquals("SELECT _C1_ AS value, COUNT(*) AS frequency FROM T123_4 WHERE"
				// auth filter
				+ " ROW_BENEFACTOR IN ( :b0, :b1 )"
				// stats group by
				+ " GROUP BY _C1_ ORDER BY frequency DESC, value ASC LIMIT :b2",
				transformer.getFacetSqlQuery().getOutputSQL());
		Map<String, Object> expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", 11L);
		expectedParmeters.put("b1", 22L);
		expectedParmeters.put("b2", 100L);
		assertEquals(expectedParmeters, transformer.getFacetSqlQuery().getParameters());

		// two
		transformer = facet.getFacetInformationQueries().get(1);
		assertEquals("SELECT MIN(_C2_) AS minimum, MAX(_C2_) AS maximum FROM T123_4 WHERE"
				+ " ROW_BENEFACTOR IN ( :b0, :b1 )", transformer.getFacetSqlQuery().getOutputSQL());
		expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", 11L);
		expectedParmeters.put("b1", 22L);
		assertEquals(expectedParmeters, transformer.getFacetSqlQuery().getParameters());
	}

	@Test
	public void testFacetQueriesWithCurrentUser() {
		builder.setStartingSql("select * from " + tableId + " where two = CURRENT_USER()");

		// call under test
		FacetQueries facet = new FacetQueries(builder.build());
		assertNotNull(facet.getFacetInformationQueries());
		assertEquals(2, facet.getFacetInformationQueries().size());

		// one
		FacetTransformer transformer = facet.getFacetInformationQueries().get(0);
		assertEquals(
				"SELECT _C1_ AS value, COUNT(*) AS frequency FROM T123_4 WHERE _C2_ = :b0 GROUP BY _C1_ ORDER BY frequency DESC, value ASC LIMIT :b1",
				transformer.getFacetSqlQuery().getOutputSQL());
		Map<String, Object> expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", userId);
		expectedParmeters.put("b1", 100L);
		assertEquals(expectedParmeters, transformer.getFacetSqlQuery().getParameters());
	}

	@Test
	public void testFacetQueriesWithBadSql() {
		// use a query with all parts set.
		builder.setAdditionalFilters(null);
		builder.setSelectedFacets(null);
		builder.setStartingSql("this is not sql!");

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new FacetQueries(builder.build());
		});

	}

	@Test
	public void testCountQueryWithNullExpansion() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new FacetQueries(null);
		}).getMessage();
		assertEquals("expansion is required.", message);
	}

}
