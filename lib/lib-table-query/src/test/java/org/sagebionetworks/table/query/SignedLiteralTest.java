package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

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
		StringBuilder builder = new StringBuilder();
		SignedLiteral ai = new SignedLiteral("123", null);
		ai.toSQL(builder, null);
		assertEquals("123", builder.toString());
	}
	
	@Test
	public void testSignedToSQL() {
		StringBuilder builder = new StringBuilder();
		SignedLiteral ai = new SignedLiteral("-123", null);
		ai.toSQL(builder, null);
		assertEquals("-123", builder.toString());
	}

	@Test
	public void testDoubleToSQL() {
		StringBuilder builder = new StringBuilder();
		SignedLiteral ai = new SignedLiteral("-123.0", null);
		ai.toSQL(builder, null);
		assertEquals("-123.0", builder.toString());
	}

	@Test
	public void testSmallDoubleToSQL() {
		StringBuilder builder = new StringBuilder();
		SignedLiteral ai = new SignedLiteral("-.3", null);
		ai.toSQL(builder, null);
		assertEquals("-.3", builder.toString());
	}

	@Test
	public void testGeneralToSQL(){
		StringBuilder builder = new StringBuilder();
		SignedLiteral ai = new SignedLiteral(null, "has'quote");
		ai.toSQL(builder, null);
		assertEquals("'has''quote'", builder.toString());
	}

}
