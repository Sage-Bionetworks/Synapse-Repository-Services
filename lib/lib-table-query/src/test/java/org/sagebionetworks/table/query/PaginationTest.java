package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.Pagination;

public class PaginationTest {
	
	@Test
	public void testToSQL(){
		Pagination element = new Pagination("123", "456");
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder);
		assertEquals("LIMIT 123 OFFSET 456", builder.toString());
	}
	
	@Test
	public void testToSQLWithLongs() {
		Pagination element = new Pagination(123L, 456L);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder);
		assertEquals("LIMIT 123 OFFSET 456", builder.toString());
	}

	@Test
	public void testToSQLNoOffset(){
		Pagination element = new Pagination("123", null);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder);
		assertEquals("LIMIT 123", builder.toString());
	}

	@Test
	public void testToSQLNoLimit() {
		Pagination element = new Pagination(null, "456");
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder);
		assertEquals("OFFSET 456", builder.toString());
	}
}
