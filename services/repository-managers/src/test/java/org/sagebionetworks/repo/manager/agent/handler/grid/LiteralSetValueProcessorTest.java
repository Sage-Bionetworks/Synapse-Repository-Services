package org.sagebionetworks.repo.manager.agent.handler.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.update.LiteralSetValue;

public class LiteralSetValueProcessorTest {

	private LiteralSetValueProcessor handler;
	private RowView row;

	@BeforeEach
	public void before() {
		handler = new LiteralSetValueProcessor();
		row = new RowView();
	}

	@Test
	public void testCreateConValueWithString() {
		LiteralSetValue sv = new LiteralSetValue().setColumnName("a").setValue("a string");
		JSONObject svRaw = new JSONObject("{\"columnName\":\"a\", \"value\":\"a string\"}");

		// call under test
		ConValue result = handler.createConValue(row, sv, svRaw).get();

		assertEquals(ConType.STRING, result.getType());
		assertEquals("a string", result.getValue());
	}

	@Test
	public void testCreateConValueWithNumber() {
		LiteralSetValue sv = new LiteralSetValue().setColumnName("a").setValue(2L);
		JSONObject svRaw = new JSONObject("{\"columnName\":\"a\", \"value\": 2 }");

		// call under test
		ConValue result = handler.createConValue(row, sv, svRaw).get();

		assertEquals(ConType.LONG, result.getType());
		assertEquals(2L, result.getValue());
	}

	@Test
	public void testCreateConValueWithJSONObject() {
		LiteralSetValue sv = new LiteralSetValue().setColumnName("a").setValue("{\"foo\":\"bar\"}");
		JSONObject svRaw = new JSONObject("{\"columnName\":\"a\", \"value\": {\"foo\":\"bar\"}}");

		// call under test
		ConValue result = handler.createConValue(row, sv, svRaw).get();

		assertEquals(ConType.JSON_OBJECT, result.getType());
		assertTrue(new JSONObject("{\"foo\":\"bar\"}").similar(result.getValue()));
	}

	@Test
	public void testCreateConValueWithJSONArray() {
		LiteralSetValue sv = new LiteralSetValue().setColumnName("a").setValue("[\"a string\"]");
		JSONObject svRaw = new JSONObject("{\"columnName\":\"a\", \"value\":[\"a string\"]}");

		// call under test
		ConValue result = handler.createConValue(row, sv, svRaw).get();

		assertEquals(ConType.JSON_ARRAY, result.getType());
		assertTrue(new JSONArray("[\"a string\"]").similar(result.getValue()));
	}


	@Test
	public void testCreateConValueWithNull() {
		LiteralSetValue sv = new LiteralSetValue().setColumnName("a").setValue(null);
		JSONObject svRaw = new JSONObject("{\"columnName\":\"a\", \"value\":null}");

		// call under test
		ConValue result = handler.createConValue(row, sv, svRaw).get();

		assertEquals(ConType.NULL, result.getType());
		assertEquals(JSONObject.NULL, result.getValue());
	}

	@Test
	public void testCreateConValueWithUndefined() {
		LiteralSetValue sv = new LiteralSetValue().setColumnName("a").setValue(null);
		JSONObject svRaw = new JSONObject("{\"columnName\":\"a\"}");

		// call under test
		ConValue result = handler.createConValue(row, sv, svRaw).get();

		assertEquals(ConType.UNDEFINED, result.getType());
		assertEquals(null, result.getValue());
	}

}
