package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class ColumnReferenceTest {

	@Test
	public void testToSQL() throws ParseException {
		ColumnReference ref = SqlElementUntils.createColumnReference("lhs.rhs");
		assertEquals("lhs.rhs", ref.toString());
	}

	@Test
	public void testToSQLNoRHS() throws ParseException {
		ColumnReference ref = SqlElementUntils.createColumnReference("lhs");
		assertEquals("lhs", ref.toString());
	}

	@Test
	public void testToSQLDelimited() throws ParseException {
		ColumnReference ref = SqlElementUntils.createColumnReference("\"has space\".\"has\"\"quote\"");
		assertEquals("\"has space\".\"has\"\"quote\"", ref.toString());
	}

	@Test
	public void testGetChidren() throws ParseException {
		ColumnReference element = new TableQueryParser("\"has space\".\"has\"\"quote\"").columnReference();
		assertEquals(Arrays.asList(element.getNameLHS(), element.getNameRHS()), element.getChildren());
	}
}
