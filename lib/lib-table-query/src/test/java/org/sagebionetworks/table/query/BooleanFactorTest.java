package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.BooleanFactor;
import org.sagebionetworks.table.query.model.BooleanTest;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class BooleanFactorTest {
	
	@Test
	public void testBooleanFactorToSQLMissingNot() throws ParseException{
		BooleanTest booleanTest = SqlElementUntils.createBooleanTest("foo>1");
		BooleanFactor element = new  BooleanFactor(null , booleanTest);
		assertEquals("foo > 1", element.toString());
	}
	
	@Test
	public void testBooleanFactorToSQLNot() throws ParseException{
		BooleanTest booleanTest = SqlElementUntils.createBooleanTest("foo>1");
		BooleanFactor element = new  BooleanFactor(Boolean.TRUE , booleanTest);
		assertEquals("NOT foo > 1", element.toString());
	}
	
	@Test
	public void testGetChidren() throws ParseException {
		BooleanFactor element = new TableQueryParser("foo>1").booleanFactor();
		assertEquals(Collections.singleton(element.getBooleanTest()), element.getChildren());
	}
}
