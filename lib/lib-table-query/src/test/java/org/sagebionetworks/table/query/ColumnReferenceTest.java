package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ActualIdentifier;
import org.sagebionetworks.table.query.model.ColumnName;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Identifier;

/**
 * 
 * @author John
 *
 */
public class ColumnReferenceTest {

	@Test
	public void testToSQL(){
		ColumnReference ref = new ColumnReference(new ColumnName(new Identifier(new ActualIdentifier("lhs", null))), new ColumnName(new Identifier(new ActualIdentifier("rhs", null))));
		StringBuilder builder = new StringBuilder();
		ref.toSQL(builder);
		assertEquals("lhs.rhs", builder.toString());
	}
	
	@Test
	public void testToSQLNoRHS(){
		ColumnReference ref = new ColumnReference(new ColumnName(new Identifier(new ActualIdentifier("lhs", null))), null);
		StringBuilder builder = new StringBuilder();
		ref.toSQL(builder);
		assertEquals("lhs", builder.toString());
	}
	
	@Test
	public void testToSQLDelimited(){
		ColumnReference ref = new ColumnReference(new ColumnName(new Identifier(new ActualIdentifier(null, "has space"))), new ColumnName(new Identifier(new ActualIdentifier(null, "has\"quote"))));
		StringBuilder builder = new StringBuilder();
		ref.toSQL(builder);
		assertEquals("\"has space\".\"has\"\"quote\"", builder.toString());
	}
	
}
