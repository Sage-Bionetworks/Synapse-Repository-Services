package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.ParseException;

import com.google.common.collect.Lists;

public class SqlQueryBuilderTest {
	
	ColumnModel columnFoo;
	List<ColumnModel> schema;
	
	@Before
	public void before(){
		columnFoo = new ColumnModel();
		columnFoo.setName("foo");
		columnFoo.setColumnType(ColumnType.INTEGER);
		columnFoo.setId("12");
		schema = Lists.newArrayList(columnFoo);
	}

	@Test
	public void testBuildSqlString() throws ParseException{
		SqlQuery result = new SqlQueryBuilder("select * from syn123")
		.tableSchema(schema)
		.build();
		assertEquals("SELECT _C12_, ROW_ID, ROW_VERSION FROM T123", result.getOutputSQL());
	}

}
