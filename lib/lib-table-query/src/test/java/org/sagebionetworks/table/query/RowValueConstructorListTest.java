package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.RowValueConstructorElement;
import org.sagebionetworks.table.query.model.RowValueConstructorList;
import org.sagebionetworks.table.query.util.SqlElementUtils;

public class RowValueConstructorListTest {

	
	@Test
	public void testRowValueConstructorListToSQLOne() throws ParseException{
		List<RowValueConstructorElement> list = SqlElementUtils.createRowValueConstructorElements("one");
		RowValueConstructorList element = new RowValueConstructorList(list);
		assertEquals("one", element.toString());
	}
	
	@Test
	public void testRowValueConstructorListToSQLMore() throws ParseException{
		List<RowValueConstructorElement> list = SqlElementUtils.createRowValueConstructorElements("one", "two");
		RowValueConstructorList element = new RowValueConstructorList(list);
		assertEquals("one, two", element.toString());
	}
	
	@Test
	public void testGetChildren() throws ParseException {
		RowValueConstructorList element = new TableQueryParser("one, two").rowValueConstructorList();
		assertEquals(new LinkedList<>(element.getRowValueConstructorElements()), element.getChildren());
	}
}
