package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class SelectListTest {
	
	@Test
	public void testStar(){
		StringBuilder builder = new StringBuilder();
		SelectList element = new SelectList(Boolean.TRUE);
		element.toSQL(builder, null);
		assertEquals("*", builder.toString());
	}

	@Test
	public void testDerivedList() throws ParseException{
		StringBuilder builder = new StringBuilder();
		List<DerivedColumn> columns = SqlElementUntils.createDerivedColumns("foo", "bar as \"new name\"");
		assertNotNull(columns);
		assertEquals(2, columns.size());
		SelectList element = new SelectList(columns);
		element.toSQL(builder, null);
		assertEquals("foo, bar AS \"new name\"", builder.toString());
	}
	
	@Test
	public void testDerivedListFunction() throws ParseException{
		StringBuilder builder = new StringBuilder();
		List<DerivedColumn> columns = SqlElementUntils.createDerivedColumns("max(foo)", "min(bar)");
		assertNotNull(columns);
		assertEquals(2, columns.size());
		SelectList element = new SelectList(columns);
		element.toSQL(builder, null);
		assertEquals("MAX(foo), MIN(bar)", builder.toString());
	}
	
}
