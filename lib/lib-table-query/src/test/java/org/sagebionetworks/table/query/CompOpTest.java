package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.CompOp;

public class CompOpTest {
	
	@Test
	public void testToSQLEquals(){
		assertEquals("=", CompOp.EQUALS_OPERATOR.toSQL());
	}

	@Test
	public void testToSQLNotEquals(){
		assertEquals("<>", CompOp.NOT_EQUALS_OPERATOR.toSQL());
	}
	
	@Test
	public void testToSQLLessThan(){
		assertEquals("<", CompOp.LESS_THAN_OPERATOR.toSQL());
	}
	
	@Test
	public void testToSQLGreaterThan(){
		assertEquals(">", CompOp.GREATER_THAN_OPERATOR.toSQL());
	}
	
	@Test
	public void testToSQLLessThanOrEquals(){
		assertEquals("<=", CompOp.LESS_THAN_OR_EQUALS_OPERATOR.toSQL());
	}
	
	@Test
	public void testToSQLGreaterThanOrEquals(){
		assertEquals(">=", CompOp.GREATER_THAN_OR_EQUALS_OPERATOR.toSQL());
	}
}
