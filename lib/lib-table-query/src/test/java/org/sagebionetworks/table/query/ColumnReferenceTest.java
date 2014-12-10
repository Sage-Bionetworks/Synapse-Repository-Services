package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.util.SqlElementUntils;

/**
 * 
 * @author John
 *
 */
public class ColumnReferenceTest {

	@Test
	public void testToSQL() throws ParseException{
		ColumnReference ref = SqlElementUntils.createColumnReference("lhs.rhs");
		assertEquals("lhs.rhs", ref.toString());
	}
	
	@Test
	public void testToSQLNoRHS() throws ParseException{
		ColumnReference ref = SqlElementUntils.createColumnReference("lhs");
		assertEquals("lhs", ref.toString());
	}
	
	@Test
	public void testToSQLDelimited() throws ParseException{
		ColumnReference ref = SqlElementUntils.createColumnReference("\"has space\".\"has\"\"quote\"");
		assertEquals("\"has space\".\"has\"\"quote\"", ref.toString());
	}
	
}
