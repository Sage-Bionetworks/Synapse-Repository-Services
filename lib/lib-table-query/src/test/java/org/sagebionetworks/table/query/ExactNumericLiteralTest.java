package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ExactNumericLiteral;

public class ExactNumericLiteralTest {
	
	@Test
	public void testLong() throws ParseException{
		ExactNumericLiteral element = new TableQueryParser("12345678912345").exactNumericLiteral();
		assertEquals("12345678912345", element.toSql());
	}
	
	@Test
	public void testFloat() throws ParseException{
		ExactNumericLiteral element = new TableQueryParser("123.456").exactNumericLiteral();
		assertEquals("123.456", element.toSql());
	}
	
	@Test
	public void testEndPeriod() throws ParseException{
		ExactNumericLiteral element = new TableQueryParser("123.").exactNumericLiteral();
		assertEquals("123.0", element.toSql());
	}
	
	@Test
	public void testEndZero() throws ParseException{
		ExactNumericLiteral element = new TableQueryParser("123.0").exactNumericLiteral();
		assertEquals("123.0", element.toSql());
	}

	@Test
	public void testStartPeriod() throws ParseException{
		ExactNumericLiteral element = new TableQueryParser(".123").exactNumericLiteral();
		assertEquals("0.123", element.toSql());
	}
}
