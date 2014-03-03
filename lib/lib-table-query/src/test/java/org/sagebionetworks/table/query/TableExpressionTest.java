package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class TableExpressionTest {
	
	@Test
	public void testTableExpressionToSQLNoWhere() throws ParseException{
		TableExpression element = SqlElementUntils.createTableExpression("from syn123");
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder);
		assertEquals("FROM syn123", builder.toString());
	}
	
	@Test
	public void testTableExpressionToSQLWithWhere() throws ParseException{
		TableExpression element = SqlElementUntils.createTableExpression("from syn123 where bar = 123");
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder);
		assertEquals("FROM syn123 WHERE bar = 123", builder.toString());
	}

}
