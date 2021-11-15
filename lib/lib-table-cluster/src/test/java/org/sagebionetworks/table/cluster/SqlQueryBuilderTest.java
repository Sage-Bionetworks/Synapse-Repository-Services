package org.sagebionetworks.table.cluster;

import com.google.common.collect.Lists;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.ParseException;

import java.util.List;

import static org.junit.Assert.assertEquals;

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
		SqlQuery result = new SqlQueryBuilder("select * from syn123", userId)
		.schemaProvider(schemaProvider)
		.build();
		assertEquals("SELECT _C12_, ROW_ID, ROW_VERSION FROM T123", result.getOutputSQL());
	}

}
