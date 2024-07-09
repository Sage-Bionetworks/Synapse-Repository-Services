package org.sagebionetworks.util.json.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.util.json.SomeEnum;

public class EnumTranslatorTest {

	@Test
	public void testCanTranslate() {
		// call under test
		assertTrue(new EnumTranslator().canTranslate(SomeEnum.class));
		assertFalse(new EnumTranslator().canTranslate(Long.class));
	}

	@Test
	public void testGetJSONClass() {
		// call under test
		assertEquals(String.class, new EnumTranslator().getJSONClass());
	}

	@Test
	public void testTranslateFromJavaToJSON() {
		SomeEnum field = SomeEnum.b;
		// call under test
		assertEquals("b", new EnumTranslator().translateFromJavaToJSON(field));
	}

	@Test
	public void testTranslateFromJSONToJava() {
		String value = "b";
		// call under test
		assertEquals(SomeEnum.b, new EnumTranslator().translateFromJSONToJava(SomeEnum.class, value));
	}
	
	@Test
	public void testTranslateFromJSONToJavaWithWrongValue() {
		String value = "missing";
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			new EnumTranslator().translateFromJSONToJava(SomeEnum.class, value);
		}).getMessage();
		assertEquals("The value: 'missing' was not found in type: 'org.sagebionetworks.util.json.SomeEnum'",message);

	}
}
