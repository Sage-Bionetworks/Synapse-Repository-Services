package org.sagebionetworks.util.json.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class ByteArrayTranslatorTest {

	@Test
	public void testCanTranslate() {
		// call under test
		assertTrue(new ByteArrayTranslator().canTranslate(byte[].class));
		assertFalse(new ByteArrayTranslator().canTranslate(int[].class));
	}

	@Test
	public void testGetJSONClass() {
		// call under test
		assertEquals(String.class, new ByteArrayTranslator().getJSONClass());
	}

	@Test
	public void testTranslateFromJavaToJSON() {
		byte[] field = new byte[] { 1, 2, 3 };
		// call under test
		String result = new ByteArrayTranslator().translateFromJavaToJSON(field);
		assertEquals("AQID", result);
	}

	@Test
	public void testTranslateFromJSONToJava() {
		String value = "AQID";
		// call under test
		assertTrue(Arrays.equals(new byte[] { 1, 2, 3 },
				new ByteArrayTranslator().translateFromJSONToJava(byte[].class, value)));
	}
}
