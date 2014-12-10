package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.BooleanPrimary;
import org.sagebionetworks.table.query.model.BooleanTest;
import org.sagebionetworks.table.query.model.TruthValue;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class BooleanTestTest {

	@Test
	public void testBooleanTestSQLPrimaryOnly() throws ParseException{
		BooleanPrimary booleanPrimary = SqlElementUntils.createBooleanPrimary("foo <> 'bar'");
		Boolean is = null;
		Boolean not = null;
		TruthValue truthValue = null;
		
		BooleanTest element = new BooleanTest(booleanPrimary, is, not, truthValue);
		assertEquals("foo <> 'bar'", element.toString());
	}
	
	@Test
	public void testBooleanTestSQLPrimaryIsTrue() throws ParseException{
		BooleanPrimary booleanPrimary = SqlElementUntils.createBooleanPrimary("foo is null");
		Boolean is = Boolean.TRUE;
		Boolean not = null;
		TruthValue truthValue = TruthValue.TRUE;
		
		BooleanTest element = new BooleanTest(booleanPrimary, is, not, truthValue);
		assertEquals("foo IS NULL IS TRUE", element.toString());
	}
	
	@Test
	public void testBooleanTestSQLPrimaryIsNotKnown() throws ParseException{
		BooleanPrimary booleanPrimary = SqlElementUntils.createBooleanPrimary("foo > 1");
		Boolean is = Boolean.TRUE;
		Boolean not = Boolean.TRUE;
		TruthValue truthValue = TruthValue.UNKNOWN;
		
		BooleanTest element = new BooleanTest(booleanPrimary, is, not, truthValue);
		assertEquals("foo > 1 IS NOT UNKNOWN", element.toString());
	}
}
