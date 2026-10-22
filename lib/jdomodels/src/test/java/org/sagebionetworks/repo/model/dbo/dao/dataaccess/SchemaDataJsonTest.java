package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class SchemaDataJsonTest {

	@Test
	public void testToJSONObjectWithJSONObject() {
		JSONObject schemaData = new JSONObject("{\"a\":1}");

		// call under test
		assertSame(schemaData, SchemaDataJson.toJSONObject(schemaData));
	}

	@Test
	public void testToJSONObjectWithAdapter() throws JSONObjectAdapterException {
		JSONObject expected = new JSONObject("{\"a\":{\"b\":[1,\"two\",true]}}");

		// call under test
		JSONObject result = SchemaDataJson.toJSONObject(new JSONObjectAdapterImpl(expected.toString()));

		assertTrue(expected.similar(result));
	}

	@Test
	public void testToJSONObjectWithNull() {
		// call under test
		assertNull(SchemaDataJson.toJSONObject(null));
		assertNull(SchemaDataJson.toJSONString(null));
		assertNull(SchemaDataJson.fromJSONString(null));
	}

	@Test
	public void testToJSONObjectWithOtherTypes() {
		for (Object notAnObject : new Object[] { Boolean.TRUE, "{\"a\":1}", new JSONArray("[1]") }) {
			String message = assertThrows(IllegalArgumentException.class, () -> {
				// call under test
				SchemaDataJson.toJSONObject(notAnObject);
			}).getMessage();
			assertEquals("schemaData must be a JSON object.", message);
		}
	}

	@Test
	public void testFromJSONStringWithNotAnObject() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			SchemaDataJson.fromJSONString("[1]");
		}).getMessage();

		assertEquals("schemaData must be a JSON object.", message);
	}

	@Test
	public void testJSONStringRoundTrip() {
		JSONObject schemaData = new JSONObject("{\"a\":{\"b\":[1,2.5,\"x\"]},\"c\":null}");

		// call under test
		assertTrue(schemaData.similar(SchemaDataJson.fromJSONString(SchemaDataJson.toJSONString(schemaData))));
	}
}
