package org.sagebionetworks.repo.manager.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;

public class QueryTranslationsTest {

	private List<ColumnModel> schema;
	private SchemaProvider schemaProvider;
	private IndexDescription indexDescription;
	private IdAndVersion tableId;
	private Long userId;
	private Long maxBytesPerPage;
	private Long maxRowsPerCall;
	private QueryOptions options;
	private QueryContext.Builder builder;

	private String startingSql;

	@BeforeEach
	public void before() {

		schema = List.of(
			TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING).setFacetType(FacetType.enumeration),
			TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER),
			TableModelTestUtils.createColumn(3L, "three", ColumnType.STRING),
			TableModelTestUtils.createColumn(4L, "four", ColumnType.ENTITYID)
		);

		schemaProvider = Mockito.mock(SchemaProvider.class);
		when(schemaProvider.getTableSchema(any())).thenReturn(schema);
		when(schemaProvider.getColumnModel(any())).thenReturn(schema.get(0));
		
		maxBytesPerPage = 100_000_000L;
		maxRowsPerCall = 72L;

		userId = 789L;
		tableId = IdAndVersion.parse("syn123.4");

		indexDescription = new ViewIndexDescription(tableId, TableType.entityview, -1L);

		options = new QueryOptions()
			.withRunQuery(true)
			.withReturnFacets(true)
			.withReturnMaxRowsPerPage(true)
			.withRunCount(true)
			.withRunSumFileSizes(true)
			.withReturnActionsRequired(true);

		startingSql = "select * from " + tableId;

		builder = QueryContext.builder()
			.setIndexDescription(indexDescription).setSchemaProvider(schemaProvider)
			.setUserId(userId)
			.setMaxBytesPerPage(maxBytesPerPage)
			.setStartingSql(startingSql)
			.setMaxRowsPerCall(maxRowsPerCall)
			.setSelectFileColumn(4L);
	}

	@Test
	public void testBuildWithAllOptions() {

		// call under test
		QueryTranslations queries = new QueryTranslations(builder.build(), options);

		assertNotNull(queries.getMainQuery());

		assertNotNull(queries.getCountQuery());
		assertTrue(queries.getCountQuery().isPresent());

		assertNotNull(queries.getFacetQueries());
		assertTrue(queries.getFacetQueries().isPresent());

		assertNotNull(queries.getSumFileSizesQuery());
		assertTrue(queries.getSumFileSizesQuery().isPresent());
		
		assertNotNull(queries.getActionsRequiredQuery());
		assertTrue(queries.getActionsRequiredQuery().isPresent());

	}

	@Test
	public void testBuildWithNoOptions() {
		options.withRunCount(false);
		options.withReturnFacets(false);
		options.withRunSumFileSizes(false);
		options.withReturnActionsRequired(false);

		// call under test
		QueryTranslations queries = new QueryTranslations(builder.build(), options);

		assertNotNull(queries.getMainQuery());

		assertNotNull(queries.getCountQuery());
		assertFalse(queries.getCountQuery().isPresent());

		assertNotNull(queries.getFacetQueries());
		assertFalse(queries.getFacetQueries().isPresent());

		assertNotNull(queries.getSumFileSizesQuery());
		assertFalse(queries.getSumFileSizesQuery().isPresent());
		
		assertNotNull(queries.getActionsRequiredQuery());
		assertFalse(queries.getActionsRequiredQuery().isPresent());

	}

	@Test
	public void testBuildWithNullExpansion() {

		String message = assertThrows(IllegalArgumentException.class, () -> {
			new QueryTranslations(null, options);
		}).getMessage();
		assertEquals("expansion is required.", message);

	}

	@Test
	public void testBuildWithNulOptions() {
		options = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new QueryTranslations(builder.build(), options);
		}).getMessage();
		assertEquals("options is required.", message);

	}
}
