package org.sagebionetworks.util.json.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;

public class DateTranslatorTest {

	@Test
	public void testCanTranslate() {
		// call under test
		assertTrue(new DateTranslator().canTranslate(Date.class));
		assertFalse(new DateTranslator().canTranslate(Long.class));
	}

	@Test
	public void testGetJSONClass() {
		// call under test
		assertEquals(Long.class, new DateTranslator().getJSONClass());
	}

	@Test
	public void testTranslateFromJavaToJSON() {
		Date field = new Date(123L);
		// call under test
		assertEquals(123L, new DateTranslator().translateFromJavaToJSON(field));
	}

	@Test
	public void testTranslateFromJSONToJava() {
		Long value = 123L;
		// call under test
		assertEquals(new Date(123L), new DateTranslator().translateFromJSONToJava(Date.class, value));
	}
}
