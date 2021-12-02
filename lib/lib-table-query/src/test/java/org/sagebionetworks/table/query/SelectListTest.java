package org.sagebionetworks.table.query;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.util.SqlElementUtils;

public class SelectListTest {
	
	@Test
	public void testStar(){
		SelectList element = new SelectList(Boolean.TRUE);
		assertEquals("*", element.toString());
	}

	@Test
	public void testDerivedList() throws ParseException{
		List<DerivedColumn> columns = SqlElementUtils.createDerivedColumns("foo", "bar as \"new name\"");
		assertNotNull(columns);
		assertEquals(2, columns.size());
		SelectList element = new SelectList(columns);
		assertEquals("foo, bar AS \"new name\"", element.toString());
	}
	
	@Test
	public void testDerivedListFunction() throws ParseException{
		List<DerivedColumn> columns = SqlElementUtils.createDerivedColumns("max(foo)", "min(bar)");
		assertNotNull(columns);
		assertEquals(2, columns.size());
		SelectList element = new SelectList(columns);
		assertEquals("MAX(foo), MIN(bar)", element.toString());
	}
	
	@Test
	public void testGetChildren() throws ParseException {
		SelectList element = new TableQueryParser("foo, bar as baz").selectList();
		assertEquals(new LinkedList<>(element.getColumns()), element.getChildren());
	}
	
	@Test
	public void testReplaceElement() throws ParseException {
		QuerySpecification model = new TableQueryParser("select foo, bar from syn123").querySpecification();
		SelectList old = model.getFirstElementOfType(SelectList.class);
		SelectList replacement = new TableQueryParser("_C111_, _C222_").selectList();
		// call under test
		old.replaceElement(replacement);
		assertEquals("SELECT _C111_, _C222_ FROM syn123", model.toSql());
		assertNotNull(replacement.getParent());
		assertNull(old.getParent());
	}
}
