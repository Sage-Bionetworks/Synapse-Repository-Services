package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.RowValueConstructor;
import org.sagebionetworks.table.query.model.RowValueConstructorElement;
import org.sagebionetworks.table.query.model.RowValueConstructorList;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class RowValueConstructorTest {
	
	@Test
	public void testRowValueConstructorToSQLRowValueConstructorElement() throws ParseException{
		RowValueConstructorElement rowValueConstructorElement = SqlElementUntils.createRowValueConstructorElement("foo");
		RowValueConstructor element = new RowValueConstructor(rowValueConstructorElement);
		assertEquals("foo", element.toString());
	}

	@Test
	public void testRowValueConstructorToSQLRowValueConstructorList() throws ParseException{
		RowValueConstructorList rowValueConstructorList = SqlElementUntils.createRowValueConstructorList("one, two");
		RowValueConstructor element = new RowValueConstructor(rowValueConstructorList);
		assertEquals("( one, two )", element.toString());
	}
	
}
