package org.sagebionetworks.schema;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.schema.element.SimpleString;

import nl.jqno.equalsverifier.EqualsVerifier;

public class SimleStringTest {

	@Test
	public void testSimpleString() {
		SimpleString element = new SimpleString("123");
		assertEquals("123", element.toString());
	}
	
	@Test
	public void testSimpleStringNullValue() {
		String message = assertThrows(IllegalArgumentException.class, ()->{
			new SimpleString(null);
		}).getMessage();
		assertEquals("Value cannot be null", message);
	}
	
	@Test
	public void testSimpleHashEquals() {
		EqualsVerifier.forClass(SimpleString.class).verify();
	}
}
