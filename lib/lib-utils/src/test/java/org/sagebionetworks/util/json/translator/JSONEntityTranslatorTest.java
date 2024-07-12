package org.sagebionetworks.util.json.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.util.json.ExampleJSONEntity;

public class JSONEntityTranslatorTest {

	@Test
	public void testCanTranslate() {
		// call under test
		assertTrue(new JSONEntityTranslator().canTranslate(ExampleJSONEntity.class));
		assertFalse(new JSONEntityTranslator().canTranslate(Long.class));
	}

	@Test
	public void testGetJSONClass() {
		// call under test
		assertEquals(JSONObject.class, new JSONEntityTranslator().getJSONClass());
	}

	@Test
	public void testTranslateFromJavaToJSON() {
		ExampleJSONEntity field = new ExampleJSONEntity().setAge(99L);
		// call under test
		assertEquals("{\"age\":99}", new JSONEntityTranslator().translateFromJavaToJSON(field).toString());
	}

	@Test
	public void testTranslateFromJSONToJava() {
		JSONObject value = new JSONObject("{ \"age\":99 }");
		// call under test
		assertEquals(new ExampleJSONEntity().setAge(99L),
				new JSONEntityTranslator().translateFromJSONToJava(ExampleJSONEntity.class, value));
	}
}
