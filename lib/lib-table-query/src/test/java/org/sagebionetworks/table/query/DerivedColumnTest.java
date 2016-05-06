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
	
	@Test
	public void testDerivedColumnGetNameFunction() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("min(bar)");
		assertEquals("MIN(bar)", element.getColumnName());
	}
	
	@Test
	public void testDerivedColumnGetNameFunctionQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("count('has space')");
		assertEquals("COUNT('has space')", element.getColumnName());
	}

	@Test
	public void testDerivedColumnGetNameQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("'has space'");
		assertEquals("has space", element.getColumnName());
	}
	
	@Test
	public void testDerivedColumnGetNameNoQuotes() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("no_space");
		assertEquals("no_space", element.getColumnName());
	}
	
	@Test
	public void testDerivedColumnGetNameDouble() throws ParseException{
		DerivedColumn element = SqlElementUntils.createDerivedColumn("1.23");
		assertEquals("1.23", element.getColumnName());
	}
}
