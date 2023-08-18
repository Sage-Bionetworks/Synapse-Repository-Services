package org.sagebionetworks.repo.manager.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.FacetType;
import org.sagebionetworks.table.cluster.SchemaProvider;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.description.ViewIndexDescription;

@ExtendWith(MockitoExtension.class)

public class ActionsRequiredQueryTest {
	
	@Mock
	private SchemaProvider schemaProvider;

	private IdAndVersion tableId;
	private Long userId;
	private IndexDescription indexDescription;
	private QueryContext.Builder builder;
	private String startingSql;
	private List<ColumnModel> schema;

	@BeforeEach
	public void before() {

		schema = List.of(
			TableModelTestUtils.createColumn(1L, "one", ColumnType.STRING).setFacetType(FacetType.enumeration),
			TableModelTestUtils.createColumn(2L, "two", ColumnType.INTEGER).setFacetType(FacetType.range),
			TableModelTestUtils.createColumn(3L, "three", ColumnType.ENTITYID),
			TableModelTestUtils.createColumn(4L, "four with space", ColumnType.ENTITYID)
		);

		userId = 789L;
		tableId = IdAndVersion.parse("syn123.4");

		indexDescription = new ViewIndexDescription(tableId, TableType.entityview);

		// The starting sql will have an authorization filter applied.
		startingSql = "select one, two from " + tableId + " where ROW_BENEFACTOR IN (11,22) AND one = 'abc' ORDER BY two DESC";

		builder = QueryContext.builder()
				.setIndexDescription(indexDescription)
				.setSchemaProvider(schemaProvider)
				.setSelectFileColumn(3L)
				.setUserId(userId)
				.setStartingSql(startingSql)
				.setLimit(10L);
	}

	@Test
	public void testActionsRequiredQuery() {
		when(schemaProvider.getTableSchema(any())).thenReturn(schema);
		when(schemaProvider.getColumnModel(any())).thenReturn(schema.get(0));
		
		QueryContext queryContext = builder.build();
		
		// Call under test
		ActionsRequiredQuery query = new ActionsRequiredQuery(queryContext);
		
		BasicQuery result = query.getFileEntityQuery(10, 0);
		
		assertEquals("SELECT DISTINCT _C3_ FROM T123_4 WHERE ROW_BENEFACTOR IN ( :b0, :b1 ) AND _C1_ = :b2 ORDER BY _C3_ LIMIT :pLimit OFFSET :pOffset", result.getSql());
		assertEquals(Map.of("b0", 11L, "b1", 22L, "b2", "abc", "pLimit", 10L, "pOffset", 0L), result.getParameters());
		
		result = query.getFileEntityQuery(10, 10);
		
		assertEquals("SELECT DISTINCT _C3_ FROM T123_4 WHERE ROW_BENEFACTOR IN ( :b0, :b1 ) AND _C1_ = :b2 ORDER BY _C3_ LIMIT :pLimit OFFSET :pOffset", result.getSql());
		assertEquals(Map.of("b0", 11L, "b1", 22L, "b2", "abc", "pLimit", 10L, "pOffset", 10L), result.getParameters());
	}
		
	@Test
	public void testActionsRequiredQueryWithAggregateQuery() {
		when(schemaProvider.getTableSchema(any())).thenReturn(schema);
		
		builder.setStartingSql("select one, count(*) from " + tableId + " where ROW_BENEFACTOR IN (11,22) GROUP BY one");
		
		QueryContext queryContext = builder.build();
		
		String result = assertThrows(IllegalArgumentException.class, () -> {	
			// Call under test
			new ActionsRequiredQuery(queryContext);
		}).getMessage();
		
		assertEquals("Including the actions required is not supported for aggregate queries", result);
	}
}
