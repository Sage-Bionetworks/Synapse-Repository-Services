package org.sagebionetworks.schema.semantic.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class NumericIdentifierTest {
	
	@Test
	public void testToString() {
		NumericIdentifier numeric = new NumericIdentifier(123L);
		assertEquals("123", numeric.toString());
	}
	
	@Test
	public void testNullValue() {
		String message = assertThrows(IllegalArgumentException.class,  ()->{
			 new NumericIdentifier(null);
		}).getMessage();
		assertEquals("Value cannot be null", message);
	}
	
	@Test
	public void testHashAndEquals() {
		EqualsVerifier.forClass(NumericIdentifier.class).verify();
	}

}
