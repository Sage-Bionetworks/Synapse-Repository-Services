package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.table.query.model.InValueList;
import org.sagebionetworks.table.query.model.ValueExpression;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class InValueListTest {

	@Test
	public void testInValueListToSQLOne() throws ParseException{
		List<ValueExpression> list = SqlElementUntils.createValueExpressions("one");
		InValueList element = new InValueList(list);
		assertEquals("one", element.toString());
	}
	
	@Test
	public void testInValueListToSQLMultiple() throws ParseException{
		List<ValueExpression> list = SqlElementUntils.createValueExpressions("one", "three");
		InValueList element = new InValueList(list);
		assertEquals("one, three", element.toString());
	}
}
