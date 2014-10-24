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
		StringBuilder builder = new StringBuilder();
		ref.toSQL(builder, null);
		assertEquals("lhs.rhs", builder.toString());
	}
	
	@Test
	public void testToSQLNoRHS() throws ParseException{
		ColumnReference ref = SqlElementUntils.createColumnReference("lhs");
		StringBuilder builder = new StringBuilder();
		ref.toSQL(builder, null);
		assertEquals("lhs", builder.toString());
	}
	
	@Test
	public void testToSQLDelimited() throws ParseException{
		ColumnReference ref = SqlElementUntils.createColumnReference("\"has space\".\"has\"\"quote\"");
		StringBuilder builder = new StringBuilder();
		ref.toSQL(builder, null);
		assertEquals("\"has space\".\"has\"\"quote\"", builder.toString());
	}
	
}
