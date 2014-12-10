package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.RowValueConstructorElement;
import org.sagebionetworks.table.query.model.ValueExpression;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class RowValueConstructorElementTest {

	@Test
	public void testRowValueConstructorElementToSQL() throws ParseException{
		ValueExpression valueExpression = SqlElementUntils.createValueExpression("bar");
		RowValueConstructorElement element = new RowValueConstructorElement(valueExpression);
		assertEquals("bar", element.toString());
	}
	
	@Test
	public void testRowValueConstructorElementToSQLNull() throws ParseException{
		RowValueConstructorElement element = new RowValueConstructorElement(Boolean.TRUE, null);
		assertEquals("NULL", element.toString());
	}
	
	@Test
	public void testRowValueConstructorElementToSQLDefault() throws ParseException{
		RowValueConstructorElement element = new RowValueConstructorElement(null, Boolean.TRUE);
		assertEquals("DEFAULT", element.toString());
	}
}
