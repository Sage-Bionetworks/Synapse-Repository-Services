package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.table.query.model.RowValueConstructorElement;
import org.sagebionetworks.table.query.model.RowValueConstructorList;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class RowValueConstructorListTest {

	
	@Test
	public void testRowValueConstructorListToSQLOne() throws ParseException{
		List<RowValueConstructorElement> list = SqlElementUntils.createRowValueConstructorElements("one");
		RowValueConstructorList element = new RowValueConstructorList(list);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("one", builder.toString());
	}
	
	@Test
	public void testRowValueConstructorListToSQLMore() throws ParseException{
		List<RowValueConstructorElement> list = SqlElementUntils.createRowValueConstructorElements("one", "two");
		RowValueConstructorList element = new RowValueConstructorList(list);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("one, two", builder.toString());
	}
}
