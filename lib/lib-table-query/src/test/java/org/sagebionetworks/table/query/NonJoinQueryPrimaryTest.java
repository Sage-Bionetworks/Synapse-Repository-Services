package org.sagebionetworks.table.query;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.NonJoinQueryPrimary;

public class NonJoinQueryPrimaryTest {

	@Test
	public void testWithoutParentheses() throws ParseException {
		NonJoinQueryPrimary element = new TableQueryParser("select * from syn123").nonJoinQueryPrimary();
		assertNotNull(element);
		assertEquals("SELECT * FROM syn123", element.toSql());
	}
	
	@Test
	public void testWithParentheses() throws ParseException {
		NonJoinQueryPrimary element = new TableQueryParser("(select * from syn123)").nonJoinQueryPrimary();
		assertNotNull(element);
		assertEquals("(SELECT * FROM syn123)", element.toSql());
	}
}
