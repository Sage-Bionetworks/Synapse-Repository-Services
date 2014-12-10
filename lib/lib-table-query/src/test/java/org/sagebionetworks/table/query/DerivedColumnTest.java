package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class DerivedColumnTest {
	
	@Test
	public void testDerivedColumnToSQL() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("james");
		assertEquals("james", element.toString());
	}
	
	@Test
	public void testDerivedColumnWithASToSQL() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("james as bond");
		assertEquals("james AS bond", element.toString());
	}
	
	@Test
	public void testDerivedColumnWithFunctionToSQL() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("min(bar)");
		assertEquals("MIN(bar)", element.toString());
	}

}
