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
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("bar", builder.toString());
	}
	
	@Test
	public void testRowValueConstructorElementToSQLNull() throws ParseException{
		RowValueConstructorElement element = new RowValueConstructorElement(Boolean.TRUE, null);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("NULL", builder.toString());
	}
	
	@Test
	public void testRowValueConstructorElementToSQLDefault() throws ParseException{
		RowValueConstructorElement element = new RowValueConstructorElement(null, Boolean.TRUE);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("DEFAULT", builder.toString());
	}
}
