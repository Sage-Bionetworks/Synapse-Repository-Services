package org.sagebionetworks.util.json.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IdentityTranslatorTest {

	@Test
	public void testCanTranslate() {
		// call under test
		assertTrue(new IdentityTranslator(String.class).canTranslate(String.class));
		assertFalse(new IdentityTranslator(String.class).canTranslate(Long.class));
	}

	@Test
	public void testCanTranslateWithPrimitive() {
		// call under test
		assertTrue(new IdentityTranslator(Long.class, long.class).canTranslate(Long.class));
		assertTrue(new IdentityTranslator(Long.class, long.class).canTranslate(long.class));
		assertFalse(new IdentityTranslator(Long.class, long.class).canTranslate(String.class));
	}

	@Test
	public void testGetJSONClass() {
		// call under test
		assertEquals(Long.class, new IdentityTranslator(Long.class).getJSONClass());
	}

	@Test
	public void testTranslateFromJavaToJSON() {
		Long field = 123L;
		// call under test
		assertEquals(123L, new IdentityTranslator(Long.class).translateFromJavaToJSON(field));
	}

	@Test
	public void testTranslateFromJSONToJava() {
		Long value = 123L;
		// call under test
		assertEquals(123L, new IdentityTranslator(Long.class).translateFromJSONToJava(Long.class, value));
	}
}
