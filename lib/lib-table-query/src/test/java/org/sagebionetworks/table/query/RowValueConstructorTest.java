package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.RowValueConstructor;

public class RowValueConstructorTest {
	
	@Test
	public void testRowValueConstructorToSQLRowValueConstructorElement() throws ParseException{
		RowValueConstructor element = new TableQueryParser("foo").rowValueConstructor();
		assertEquals("foo", element.toString());
	}
	
}
