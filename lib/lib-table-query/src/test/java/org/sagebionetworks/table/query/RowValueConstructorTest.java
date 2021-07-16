package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.RowValueConstructor;

public class RowValueConstructorTest {
	
	@Test
	public void testRowValueConstructorToSQLRowValueConstructorElement() throws ParseException{
		RowValueConstructor element = new TableQueryParser("foo").rowValueConstructor();
		assertEquals("foo", element.toString());
	}
	
	@Test
	public void testGetChildren() throws ParseException {
		RowValueConstructor element = new TableQueryParser("foo").rowValueConstructor();
		assertEquals(Collections.singleton(element.getChild()), element.getChildren());
	}
}
