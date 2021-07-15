package org.sagebionetworks.table.query;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ApproximateNumericLiteral;

public class ApproximateNumericLiteralTest {
	
	@Test
	public void testNoSign() throws ParseException{
		ApproximateNumericLiteral element = new TableQueryParser("123.4e123").approximateNumericLiteral();
		assertEquals("1.234E125", element.toSql());
	}
	
	@Test
	public void testPlusSign() throws ParseException{
		ApproximateNumericLiteral element = new TableQueryParser("1.3456e+15").approximateNumericLiteral();
		assertEquals("1.3456E15", element.toSql());
	}
	
	@Test
	public void testMinusSign() throws ParseException{
		ApproximateNumericLiteral element = new TableQueryParser("1.3456e-15").approximateNumericLiteral();
		assertEquals("1.3456E-15", element.toSql());
	}
	
	@Test
	public void testInteger() throws ParseException{
		ApproximateNumericLiteral element = new TableQueryParser("1234e-15").approximateNumericLiteral();
		assertEquals("1.234E-12", element.toSql());
	}
	
	@Test
	public void testGetChidren() throws ParseException {
		ApproximateNumericLiteral element = new TableQueryParser("1234e-15").approximateNumericLiteral();
		assertEquals(Collections.emptyList(), element.getChildren());
	}

}
