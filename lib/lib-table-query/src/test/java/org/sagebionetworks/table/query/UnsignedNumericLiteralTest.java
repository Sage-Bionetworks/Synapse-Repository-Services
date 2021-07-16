package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.UnsignedNumericLiteral;

public class UnsignedNumericLiteralTest {

	@Test
	public void testLong() throws ParseException {
		UnsignedNumericLiteral element = new TableQueryParser("12345678").unsignedNumericLiteral();
		assertEquals("12345678", element.toSql());
	}

	@Test
	public void testDouble() throws ParseException {
		UnsignedNumericLiteral element = new TableQueryParser("256.4456").unsignedNumericLiteral();
		assertEquals("256.4456", element.toSql());
	}

	@Test
	public void testDoubleExponent() throws ParseException {
		UnsignedNumericLiteral element = new TableQueryParser("256.4456e-34").unsignedNumericLiteral();
		assertEquals("2.564456E-32", element.toSql());
	}

	@Test
	public void testSignedExponent() throws ParseException {
		assertThrows(ParseException.class, () -> {
			// cannot start with a sign
			new TableQueryParser("+256.4456e-34").unsignedNumericLiteral();
		});

	}

	@Test
	public void testSignedInteger() throws ParseException {
		assertThrows(ParseException.class, () -> {
			// cannot start with a sign
			new TableQueryParser("-123").unsignedNumericLiteral();
		});
	}

	@Test
	public void testGetChildren() throws ParseException {
		UnsignedNumericLiteral element = new TableQueryParser("256.4456e-34").unsignedNumericLiteral();
		assertEquals(Collections.singleton(element.getChild()), element.getChildren());
	}
}
