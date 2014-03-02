package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ActualIdentifier;

public class ActualIdentifierTest {
	
	@Test (expected=IllegalArgumentException.class)
	public void testOr(){
		// an actual-identifier can be a regular-identifier or delimited-identifier but not both.
		new ActualIdentifier("one", "two");
	}
	
	@Test
	public void testRegularToSQL(){
		StringBuilder builder = new StringBuilder();
		ActualIdentifier ai = new ActualIdentifier("C123", null);
		ai.toSQL(builder);
		assertEquals("C123", builder.toString());
	}
	
	@Test
	public void testDelimitedToSQL(){
		StringBuilder builder = new StringBuilder();
		ActualIdentifier ai = new ActualIdentifier(null, "has\"quote");
		ai.toSQL(builder);
		assertEquals("\"has\"\"quote\"", builder.toString());
	}

}
