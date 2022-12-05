package org.sagebionetworks.table.cluster;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.cluster.description.TableIndexDescription;
import org.sagebionetworks.table.query.ParseException;

import com.google.common.collect.Lists;

public class SqlQueryBuilderTest {
	
	ColumnModel columnFoo;
	List<ColumnModel> schema;
	Long userId;
	SchemaProvider schemaProvider;
	
	@BeforeEach
	public void before(){
		columnFoo = new ColumnModel();
		columnFoo.setName("foo");
		columnFoo.setColumnType(ColumnType.INTEGER);
		columnFoo.setId("12");
		schema = Lists.newArrayList(columnFoo);
		userId = 1L;
		schemaProvider = (IdAndVersion tableId) -> {
			return schema;
		};
	}

	@Test
	public void testBuildSqlString() throws ParseException{
		QueryTranslator result = QueryTranslator.builder("select * from syn123", userId)
		.schemaProvider(schemaProvider)
		.indexDescription(new TableIndexDescription(IdAndVersion.parse("syn123")))
		.build();
		assertEquals("SELECT _C12_, ROW_ID, ROW_VERSION FROM T123", result.getOutputSQL());
	}

}
