package org.sagebionetworks.repo.manager.table.query;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AggregateDataConfiguration;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnSingleValueFilterOperator;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.FacetColumnRequest;
import org.sagebionetworks.repo.model.table.FacetColumnValuesRequest;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.description.IndexDescription;

@ExtendWith(MockitoExtension.class)
public class QueryContextTest {

	private static final String SALT = "6f1c4a9e-2b7d-4e3a-9c51-0d8e7f6a5b41";

	@Mock
	private SchemaProvider mockSchemaProvider;
	@Mock
	private IndexDescription mockIndexDescription;

	private IdAndVersion tableId;
	private Long userId;
	private Long maxBytesPerPage;
	private Long maxRowsPerCall;
	private ColumnSingleValueQueryFilter singleValueFilter;
	private QueryContext.Builder builder;
	private String startingSql;
	private List<QueryFilter> additionalFilters;
	private List<FacetColumnRequest> selectedFacets;
	private Long selectFileColumn;
	private Boolean includeEntityEtag;
	private Long offset;
	private Long limit;
	private List<SortItem> sort;

	@BeforeEach
	public void before() {

		maxBytesPerPage = 100_000_000L;

		maxRowsPerCall = 15L;
		userId = 789L;
		tableId = IdAndVersion.parse("syn123.4");

		startingSql = "select * from " + tableId;

		singleValueFilter = new ColumnSingleValueQueryFilter().setColumnName("two")
				.setOperator(ColumnSingleValueFilterOperator.EQUAL).setValues(List.of("99", "89"));
		additionalFilters = List.of(singleValueFilter);
		selectedFacets = List.of(new FacetColumnValuesRequest().setColumnName("one").setFacetValues(Set.of("cat")));
		selectFileColumn = 99L;

		includeEntityEtag = true;
		limit = 11L;
		offset = 3L;

		sort = List.of(new SortItem().setColumn("three").setDirection(SortDirection.DESC));

		builder = QueryContext.builder().setIndexDescription(mockIndexDescription).setSchemaProvider(mockSchemaProvider)
				.setUserId(userId).setMaxBytesPerPage(maxBytesPerPage).setStartingSql(startingSql)
				.setMaxRowsPerCall(maxRowsPerCall).setAdditionalFilters(additionalFilters)
				.setSelectedFacets(selectedFacets).setSelectFileColumn(selectFileColumn)
				.setIncludeEntityEtag(includeEntityEtag).setLimit(limit).setOffset(offset).setSort(sort);
	}

	@Test
	public void testBuildWithAllParts() {

		// call under test
		QueryContext expansion = builder.build();

		assertNotNull(expansion.getSchemaProvider());
		assertTrue(expansion.getSchemaProvider() instanceof CachingSchemaProvider);
		assertEquals(mockIndexDescription, expansion.getIndexDescription());
		assertEquals(userId, expansion.getUserId());
		assertEquals(maxBytesPerPage, expansion.getMaxBytesPerPage());
		assertEquals(maxRowsPerCall, expansion.getMaxRowsPerCall());
		assertEquals(startingSql, expansion.getStartingSql());
		assertEquals(additionalFilters, expansion.getAdditionalFilters());
		assertEquals(selectedFacets, expansion.getSelectedFacets());
		assertEquals(selectFileColumn, expansion.getSelectFileColumn());
		assertEquals(includeEntityEtag, expansion.getIncludeEntityEtag());
		assertEquals(limit, expansion.getLimit());
		assertEquals(offset, expansion.getOffset());
		assertEquals(sort, expansion.getSort());

		assertNotNull(expansion);
	}

	@Test
	public void testBuildWithNullSql() {

		builder.setStartingSql(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			builder.build();
		}).getMessage();
		assertEquals("startingSql is required.", message);

	}

	@Test
	public void testBuildWithNullProvider() {
		builder.setSchemaProvider(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			builder.build();
		}).getMessage();
		assertEquals("schemaProvider is required.", message);

	}

	@Test
	public void testBuildWithDescription() {
		builder.setIndexDescription(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			builder.build();
		}).getMessage();
		assertEquals("indexDescription is required.", message);
	}

	@Test
	public void testBuildWithUserid() {
		builder.setUserId(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			builder.build();
		}).getMessage();
		assertEquals("userId is required.", message);
	}

	@Test
	public void testBuildWithAggregateDataConfigurationAndSalt() {
		AggregateDataConfiguration configuration = new AggregateDataConfiguration().setSourceTableId("syn123")
				.setSuppressionThreshold(5L);
		builder.setAggregateDataConfiguration(configuration).setAggregateDataSalt(SALT);

		// call under test
		QueryContext context = builder.build();

		assertEquals(Optional.of(configuration), context.getAggregateDataConfiguration());
		assertEquals(Optional.of(SALT), context.getAggregateDataSalt());
	}

	@Test
	public void testBuildWithAggregateDataConfigurationAndNoSalt() {
		builder.setAggregateDataConfiguration(new AggregateDataConfiguration().setSourceTableId("syn123")
				.setSuppressionThreshold(5L));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			builder.build();
		}).getMessage();
		assertEquals("An aggregate-only query requires the aggregateDataSalt of its source.", message);
	}

	@Test
	public void testBuildWithNoAggregateDataConfiguration() {
		// call under test
		QueryContext context = builder.build();

		assertEquals(Optional.empty(), context.getAggregateDataConfiguration());
		assertEquals(Optional.empty(), context.getAggregateDataSalt());
	}

	@Test
	public void testMaximumNumberOfAdditionalFiltersValuesWithNullValues() {

		singleValueFilter = new ColumnSingleValueQueryFilter().setColumnName("two")
				.setOperator(ColumnSingleValueFilterOperator.LIKE).setValues(null);
		
		builder.setAdditionalFilters(List.of(singleValueFilter));
		// call under test
		QueryContext context = builder.build();
		assertNotNull(context.getAdditionalFilters());
	}
	
}
