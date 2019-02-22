package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.UnsignedNumericLiteral;

public class UnsignedNumericLiteralTest {
	
	@Test
	public void testLong() throws ParseException{
		UnsignedNumericLiteral element = new TableQueryParser("12345678").unsignedNumericLiteral();
		assertEquals("12345678", element.toSql());
	}

	@Test
	public void testDouble() throws ParseException{
		UnsignedNumericLiteral element = new TableQueryParser("256.4456").unsignedNumericLiteral();
		assertEquals("256.4456", element.toSql());
	}
	
	@Test
	public void testDoubleExponent() throws ParseException{
		UnsignedNumericLiteral element = new TableQueryParser("256.4456e-34").unsignedNumericLiteral();
		assertEquals("2.564456E-32", element.toSql());
	}
	
	@Test (expected=ParseException.class)
	public void testSignedExponent() throws ParseException{
		// cannot start with a sign
		new TableQueryParser("+256.4456e-34").unsignedNumericLiteral();
	}
	
	@Test (expected=ParseException.class)
	public void testSignedInteger() throws ParseException{
		// cannot start with a sign
		new TableQueryParser("-123").unsignedNumericLiteral();
	}
}
