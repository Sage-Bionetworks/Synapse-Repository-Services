package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.UnsignedInteger;

public class UnsignedIntegerTest {
	
	@Test
	public void testUnsigned() throws ParseException{
		UnsignedInteger element = new TableQueryParser("123456").unsignedInteger();
		assertEquals("123456", element.toSql());
	}
	
	@Test (expected=ParseException.class)
	public void testSigned() throws ParseException{
		// signs are not allowed
		new TableQueryParser("-123456").unsignedInteger();
	}

}
