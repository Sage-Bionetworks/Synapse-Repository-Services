package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class SelectListTest {
	
	@Test
	public void testStar(){
		SelectList element = new SelectList(Boolean.TRUE);
		assertEquals("*", element.toString());
	}

	@Test
	public void testDerivedList() throws ParseException{
		List<DerivedColumn> columns = SqlElementUntils.createDerivedColumns("foo", "bar as \"new name\"");
		assertNotNull(columns);
		assertEquals(2, columns.size());
		SelectList element = new SelectList(columns);
		assertEquals("foo, bar AS \"new name\"", element.toString());
	}
	
	@Test
	public void testDerivedListFunction() throws ParseException{
		List<DerivedColumn> columns = SqlElementUntils.createDerivedColumns("max(foo)", "min(bar)");
		assertNotNull(columns);
		assertEquals(2, columns.size());
		SelectList element = new SelectList(columns);
		assertEquals("MAX(foo), MIN(bar)", element.toString());
	}
	
	@Test
	public void testHasAtLeastOneColumnReferenceStar() throws ParseException {
		SelectList sl = new TableQueryParser("*").selectList();
		assertTrue(sl.hasAtLeastOneColumnReference());
	}
	
	
	@Test
	public void testHasAtLeastOneColumnReferenceConstant() throws ParseException {
		SelectList sl = new TableQueryParser("'a constant', 'b constant'").selectList();
		assertFalse(sl.hasAtLeastOneColumnReference());
	}
	
	@Test
	public void testHasAtLeastOneColumnReferenceConstantWihtColumn() throws ParseException {
		SelectList sl = new TableQueryParser("'a constant', foo").selectList();
		assertTrue(sl.hasAtLeastOneColumnReference());
	}
	
	@Test
	public void testHasAtLeastOneColumnReferenceArithmetic() throws ParseException {
		SelectList sl = new TableQueryParser("(5 + 3) * 100").selectList();
		assertFalse(sl.hasAtLeastOneColumnReference());
	}
	
	@Test
	public void testHasAtLeastOneColumnReferenceArithmeticWithColumn() throws ParseException {
		SelectList sl = new TableQueryParser("(5 + foo) * 100").selectList();
		assertTrue(sl.hasAtLeastOneColumnReference());
	}	
}
