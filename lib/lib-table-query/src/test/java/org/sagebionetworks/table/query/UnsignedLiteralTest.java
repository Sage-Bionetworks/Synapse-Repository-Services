package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.UnsignedLiteral;

public class UnsignedLiteralTest {

	@Test (expected=IllegalArgumentException.class)
	public void testOr(){
		// an UnsignedLiteral cannot be both UnsignedNumericLiteral or a GeneralLiteral
		new UnsignedLiteral("123", "joe's");
	}
	
	@Test
	public void testUnsignedToSQL(){
		StringBuilder builder = new StringBuilder();
		UnsignedLiteral ai = new UnsignedLiteral("123", null);
		ai.toSQL(builder);
		assertEquals("123", builder.toString());
	}
	
	@Test
	public void testGeneralToSQL(){
		StringBuilder builder = new StringBuilder();
		UnsignedLiteral ai = new UnsignedLiteral(null, "has'quote");
		ai.toSQL(builder);
		assertEquals("'has''quote'", builder.toString());
	}

}
