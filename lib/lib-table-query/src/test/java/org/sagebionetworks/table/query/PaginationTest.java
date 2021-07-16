package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.Pagination;

public class PaginationTest {

	@Test
	public void testToSQL() {
		Pagination element = new Pagination("123", "456");
		assertEquals("LIMIT 123 OFFSET 456", element.toString());
	}

	@Test
	public void testToSQLWithLongs() {
		Pagination element = new Pagination(123L, 456L);
		assertEquals("LIMIT 123 OFFSET 456", element.toString());
	}

	@Test
	public void testToSQLNoOffset() {
		Pagination element = new Pagination("123", null);
		assertEquals("LIMIT 123", element.toString());
	}

	@Test
	public void testGetChildren() throws ParseException {
		Pagination element = new TableQueryParser("LIMIT 123 OFFSET 456").pagination();
		assertEquals(Collections.emptyList(), element.getChildren());
	}
}
