package org.sagebionetworks.table.query.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JsonTableColumnTest {

	@Test
	public void testJsonTableColumn() {
		JsonTableColumn column = new JsonTableColumn("_C123_", "BIGINT");
		
		assertEquals("_C123_ BIGINT PATH '$' ERROR ON ERROR", column.toSql());
	}
}
