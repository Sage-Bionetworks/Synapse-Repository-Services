package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ColumnList;

public class ColumnListTest {

	@Test
	public void testColumnList() throws ParseException {
		ColumnList element = new TableQueryParser("(a,b,c)").columnList();
		assertEquals("(a, b, c)", element.toSql());
	}
	
	@Test
	public void testColumnListWithOneElement() throws ParseException {
		ColumnList element = new TableQueryParser("(a)").columnList();
		assertEquals("(a)", element.toSql());
	}
}
