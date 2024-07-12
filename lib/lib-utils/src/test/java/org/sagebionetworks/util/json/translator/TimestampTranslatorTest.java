package org.sagebionetworks.util.json.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.Date;

import org.junit.jupiter.api.Test;

public class TimestampTranslatorTest {

	@Test
	public void testCanTranslate() {
		// call under test
		assertTrue(new TimestampTranslator().canTranslate(Timestamp.class));
		assertFalse(new TimestampTranslator().canTranslate(Date.class));
	}

	@Test
	public void testGetJSONClass() {
		// call under test
		assertEquals(Long.class, new TimestampTranslator().getJSONClass());
	}

	@Test
	public void testTranslateFromJavaToJSON() {
		Timestamp field = new Timestamp(123L);
		// call under test
		assertEquals(123L, new TimestampTranslator().translateFromJavaToJSON(field));
	}

	@Test
	public void testTranslateFromJSONToJava() {
		Long value = 123L;
		// call under test
		assertEquals(new Timestamp(123L), new TimestampTranslator().translateFromJSONToJava(Timestamp.class, value));
	}
}
