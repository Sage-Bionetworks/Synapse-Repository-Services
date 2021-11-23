package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.InValueList;
import org.sagebionetworks.table.query.model.ValueExpression;
import org.sagebionetworks.table.query.util.SqlElementUtils;

public class InValueListTest {

	@Test
	public void testInValueListToSQLOne() throws ParseException{
		List<ValueExpression> list = SqlElementUtils.createValueExpressions("one");
		InValueList element = new InValueList(list);
		assertEquals("one", element.toString());
	}
	
	@Test
	public void testInValueListToSQLMultiple() throws ParseException{
		List<ValueExpression> list = SqlElementUtils.createValueExpressions("one", "three");
		InValueList element = new InValueList(list);
		assertEquals("one, three", element.toString());
	}
	
	@Test
	public void testGetChildren() throws ParseException {
		InValueList element = new TableQueryParser("one, two").inValueList();
		assertEquals(new LinkedList<>(element.getValueExpressions()), element.getChildren());
	}
}
