package org.sagebionetworks.repo.manager.grid.internal.replica.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;

public class JsonObjectSubjectTest {


	@Test
	public void testToJson() {
		String jsonStr = "{\"a\":\"aval\",\"b\":\"bval\",\"c\":\"cval\"}";
		JSONObject object = new JSONObject(new JSONTokener(jsonStr));

		// call under test
		JsonObjectSubject sub = new JsonObjectSubject(object);
		assertEquals(object, sub.toJson());
	}
}
