package org.sagebionetworks.table.query;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.table.query.model.SignedLiteral;

public class SignedLiteralTest {

	@Test (expected=IllegalArgumentException.class)
	public void testOr(){
		// an SignedLiteral cannot be both SignedNumericLiteral or a GeneralLiteral
		new SignedLiteral("123", "joe's");
	}
	
	@Test
	public void testUnsignedToSQL(){
		SignedLiteral ai = new SignedLiteral("123", null);
		assertEquals("123", ai.toString());
	}
	
	@Test
	public void testSignedToSQL() {
		SignedLiteral ai = new SignedLiteral("-123", null);
		assertEquals("-123", ai.toString());
	}

	@Test
	public void testDoubleToSQL() {
		SignedLiteral ai = new SignedLiteral("-123.0", null);
		assertEquals("-123.0", ai.toString());
	}

	@Test
	public void testSmallDoubleToSQL() {
		SignedLiteral ai = new SignedLiteral("-.3", null);
		assertEquals("-.3", ai.toString());
	}

	@Test
	public void testGeneralToSQL(){
		SignedLiteral ai = new SignedLiteral(null, "has'quote");
		assertEquals("'has''quote'", ai.toString());
	}
	
	@Test
	public void testValueWithQuotes() throws ParseException{
		SignedLiteral element = new TableQueryParser("'foo'").signedLiteral();
		assertEquals("foo", element.getFirstUnquotedValue());
		assertTrue(element.isSurrounedeWithQuotes());		
	}
	
	@Test
	public void testValueWithoutQuotes() throws ParseException{
		SignedLiteral element = new TableQueryParser("123").signedLiteral();
		assertEquals("123", element.getFirstUnquotedValue());
		assertFalse(element.isSurrounedeWithQuotes());		
	}

}
