package org.sagebionetworks.repo.manager.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.sagebionetworks.table.cluster.description.IndexDescriptionLookup;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;
import org.sagebionetworks.table.cluster.description.VirtualTableIndexDescription;
import org.sagebionetworks.table.query.ParseException;

@ExtendWith(MockitoExtension.class)
public class CountQueryTest {

	private List<ColumnModel> schema;
	
	@Mock
	private SchemaProvider schemaProvider;
	private IdAndVersion tableId;
	private Long userId;
	private IndexDescription indexDescription;
	private QueryContext.Builder builder;
	private String startingSql;

	@Mock
	private IndexDescriptionLookup mockIndexDescriptionLookup;

	@BeforeEach
	public void before() {

		schema = List.of(
				TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING).setFacetType(FacetType.enumeration),
				TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER).setFacetType(FacetType.range),
				TableModelTestUtils.createColumn(3L, "three", ColumnType.STRING));

		userId = 789L;
		tableId = IdAndVersion.parse("syn123.4");

		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);

		// The starting sql will have an authorization filter applied.
		startingSql = "select * from " + tableId + " where ROW_BENEFACTOR IN (11,22)";

		builder = QueryContext.builder().setIndexDescription(indexDescription).setSchemaProvider(schemaProvider)
				.setUserId(userId).setStartingSql(startingSql).setMaxRowsPerCall(100L);
	}

	@Test
	public void testCountQueryWithAllParts() {
		when(schemaProvider.getTableSchema(any())).thenReturn(schema);
		// use a query with all parts set.
		builder.setAdditionalFilters(List.of(new ColumnSingleValueQueryFilter().setColumnName("three")
				.setOperator(ColumnSingleValueFilterOperator.EQUAL).setValues(List.of("a", "b"))));
		builder.setSelectedFacets(
				List.of(new FacetColumnValuesRequest().setColumnName("one").setFacetValues(Set.of("cat")),
						new FacetColumnRangeRequest().setColumnName("two").setMax("38").setMin("15")));

		QueryContext expantion = builder.build();
		// call under test
		CountQuery count = new CountQuery(expantion);

		String sql = "SELECT COUNT(*) FROM T123_4 WHERE"
				// facets
				+ " ( ( ( _C1_ = :b0 ) AND ( _C2_ BETWEEN :b1 AND :b2 ) ) )"
				// additional
				+ " AND ( ( ( _C3_ = :b3 OR _C3_ = :b4 ) )"
				// auth filter
				+ " AND ( ROW_BENEFACTOR IN ( :b5, :b6 ) ) )";
		
		Map<String, Object> expectedParmeters = new HashMap<>();
		
		expectedParmeters.put("b0", "cat");
		expectedParmeters.put("b1", 15L);
		expectedParmeters.put("b2", 38L);
		expectedParmeters.put("b3", "a");
		expectedParmeters.put("b4", "b");
		expectedParmeters.put("b5", 11L);
		expectedParmeters.put("b6", 22L);

		assertEquals(Optional.of(new BasicQuery(sql, expectedParmeters)), count.getCountQuery());
		assertNull(count.getOriginalPagination());
		assertEquals("syn123.4", count.getSingleTableId());
		assertEquals("d41d8cd98f00b204e9800998ecf8427e", count.getTableHash());
	}

	@Test
	public void testCountQueryWithOriginalPaginations() {
		when(schemaProvider.getTableSchema(any())).thenReturn(schema);
		builder.setStartingSql("select * from " + tableId + " limit 10 offset 2");

		QueryContext expantion = builder.build();
		// call under test
		CountQuery count = new CountQuery(expantion);
		
		String sql = "SELECT COUNT(*) FROM T123_4";
		Map<String, Object> expectedParmeters = new HashMap<>();
		assertEquals(Optional.of(new BasicQuery(sql, expectedParmeters)), count.getCountQuery());

		assertNotNull(count.getOriginalPagination());
		assertEquals("LIMIT 10 OFFSET 2", count.getOriginalPagination().toSql());
		assertEquals("syn123.4", count.getSingleTableId());
		assertEquals("d41d8cd98f00b204e9800998ecf8427e", count.getTableHash());

	}

	@Test
	public void testCountQueryWithCurrentUser() {
		when(schemaProvider.getTableSchema(any())).thenReturn(schema);
		builder.setStartingSql("select * from " + tableId + " where two = CURRENT_USER()");

		QueryContext expantion = builder.build();
		// call under test
		CountQuery count = new CountQuery(expantion);
		
		String sql = "SELECT COUNT(*) FROM T123_4 WHERE _C2_ = :b0";
		Map<String, Object> expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", userId);
		assertEquals(Optional.of(new BasicQuery(sql, expectedParmeters)), count.getCountQuery());

		assertNull(count.getOriginalPagination());
		assertEquals("syn123.4", count.getSingleTableId());
		assertEquals("d41d8cd98f00b204e9800998ecf8427e", count.getTableHash());

	}

	@Test
	public void testCountQueryWithNullParts() {
		when(schemaProvider.getTableSchema(any())).thenReturn(schema);
		builder.setAdditionalFilters(null);
		builder.setSelectedFacets(null);

		QueryContext expantion = builder.build();
		// call under test
		CountQuery count = new CountQuery(expantion);
		
		String sql = "SELECT COUNT(*) FROM T123_4 WHERE ROW_BENEFACTOR IN ( :b0, :b1 )";
		Map<String, Object> expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", 11L);
		expectedParmeters.put("b1", 22L);
		assertEquals(Optional.of(new BasicQuery(sql, expectedParmeters)), count.getCountQuery());
		
		assertNull(count.getOriginalPagination());
		assertEquals("syn123.4", count.getSingleTableId());
		assertEquals("d41d8cd98f00b204e9800998ecf8427e", count.getTableHash());

	}

	@Test
	public void testCountQueryNullExpansion() {
		builder.setStartingSql(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new CountQuery(null);
		}).getMessage();
		assertEquals("expansion is required.", message);
	}

	@Test
	public void testCountQueryWithInvalidSql() {
		builder.setStartingSql("this is not sql!");
		Throwable cause = assertThrows(IllegalArgumentException.class, () -> {
			new CountQuery(builder.build());
		}).getCause();
		assertTrue(cause instanceof ParseException);
	}

	@Test
	public void testCountQueryWithAggregateQuery() {
		when(schemaProvider.getTableSchema(any())).thenReturn(schema);
		builder.setAdditionalFilters(null);
		builder.setSelectedFacets(null);
		builder.setStartingSql("select count(*) from " + tableId);

		// call under test
		QueryContext expantion = builder.build();
		// call under test
		CountQuery count = new CountQuery(expantion);
		
		assertEquals(Optional.empty(), count.getCountQuery());
		assertNull(count.getOriginalPagination());
		assertNull(count.getSingleTableId());
		assertNull(count.getTableHash());
	}

	@Test
	public void testCountQueryWithVirtualTable() {
		tableId = IdAndVersion.parse("syn1");
		// This affects the hash of the virtual table
		Long lastTableChangeNumber = 2L;
		TableIndexDescription sourceTableDescription = new TableIndexDescription(tableId, lastTableChangeNumber);
		when(mockIndexDescriptionLookup.getIndexDescription(any())).thenReturn(sourceTableDescription);
		IdAndVersion virtualId = IdAndVersion.parse("syn2");
		String definingSql = "select one, cast(sum(two) as 33) from syn1 where two > 12 group by one";
		indexDescription = new VirtualTableIndexDescription(virtualId, definingSql, mockIndexDescriptionLookup);
		ColumnModel one = schema.get(0);
		ColumnModel sum = new ColumnModel().setName("sum").setId("33").setColumnType(ColumnType.INTEGER);
		when(schemaProvider.getTableSchema(tableId)).thenReturn(schema);
		when(schemaProvider.getTableSchema(virtualId)).thenReturn(List.of(one, sum));
		when(schemaProvider.getColumnModel(any())).thenReturn(sum);

		String sql = indexDescription.preprocessQuery("select * from syn2");
		QueryContext expantion = QueryContext.builder().setIndexDescription(indexDescription)
				.setSchemaProvider(schemaProvider).setUserId(userId).setStartingSql(sql)
				.setMaxRowsPerCall(100L).build();

		// call under test
		CountQuery count = new CountQuery(expantion);

		String expectedSql = "WITH T2 (_C1_, _C33_) AS"
				+ " (SELECT _C1_, CAST(SUM(_C2_) AS SIGNED) FROM T1 WHERE _C2_ > :b0 GROUP BY _C1_)"
				+ " SELECT COUNT(*) FROM T2";
		Map<String, Object> expectedParmeters = new HashMap<>();
		expectedParmeters.put("b0", 12L);
		assertEquals(Optional.of(new BasicQuery(expectedSql, expectedParmeters)), count.getCountQuery());
		assertNull(count.getOriginalPagination());
		assertEquals("syn2", count.getSingleTableId());
		assertEquals("38b522e72524bc4e58f29fbc0493fbf4", count.getTableHash());
		
		verify(mockIndexDescriptionLookup).getIndexDescription(tableId);
		verify(schemaProvider, times(2)).getTableSchema(any());
		verify(schemaProvider).getTableSchema(tableId);
		verify(schemaProvider).getTableSchema(virtualId);
		verify(schemaProvider).getColumnModel("33");
		verify(schemaProvider, times(2)).getColumnModel(any());
	}
}
