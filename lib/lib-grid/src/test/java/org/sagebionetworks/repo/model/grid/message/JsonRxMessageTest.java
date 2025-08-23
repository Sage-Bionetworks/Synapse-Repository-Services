package org.sagebionetworks.repo.model.grid.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class JsonRxMessageTest {

	@ParameterizedTest
	@EnumSource(JsonRxMessageType.class)
	public void testConstructorWithArrayBody(JsonRxMessageType type) {
		String json = "[" + type.code + ",2,\"some-method\",[10,11]]";
		// call under test
		JsonRxMessage message = new JsonRxMessage(json);
		JsonRxMessage expected = new JsonRxMessage(type).setId(2).setMethod("some-method")
				.setBody(new JSONArray("[10,11]"));
		assertEquals(expected, message);
		assertEquals(json, message.toString());
	}

	@ParameterizedTest
	@EnumSource(JsonRxMessageType.class)
	public void testConstructorWithObjectBody(JsonRxMessageType type) {
		String json = "[" + type.code + ",2,\"some-method\",{\"someKey\":101}]";
		// call under test
		JsonRxMessage message = new JsonRxMessage(json);
		JsonRxMessage expected = new JsonRxMessage(type).setId(2).setMethod("some-method")
				.setBody(new JSONObject("{\"someKey\":101}"));
		assertEquals(expected, message);
		assertEquals(json, message.toString());
	}

	@ParameterizedTest
	@EnumSource(JsonRxMessageType.class)
	public void testConstructorWithNullBody(JsonRxMessageType type) {
		String json = "[" + type.code + ",2,\"some-method\"]";
		// call under test
		JsonRxMessage message = new JsonRxMessage(json);
		JsonRxMessage expected = new JsonRxMessage(type).setId(2).setMethod("some-method");
		assertEquals(expected, message);
		assertEquals(json, message.toString());
	}

	@ParameterizedTest
	@EnumSource(JsonRxMessageType.class)
	public void testConstructorWithArrayBodyAndNullMethod(JsonRxMessageType type) {
		String json = "[" + type.code + ",2,[10,11]]";
		// call under test
		JsonRxMessage message = new JsonRxMessage(json);
		JsonRxMessage expected = new JsonRxMessage(type).setId(2).setBody(new JSONArray("[10,11]"));
		assertEquals(expected, message);
		assertEquals(json, message.toString());
	}

	@ParameterizedTest
	@EnumSource(JsonRxMessageType.class)
	public void testConstructorWithObjectBodyAndNullMethod(JsonRxMessageType type) {
		String json = "[" + type.code + ",2,{\"someKey\":101}]";
		// call under test
		JsonRxMessage message = new JsonRxMessage(json);
		JsonRxMessage expected = new JsonRxMessage(type).setId(2).setBody(new JSONObject("{\"someKey\":101}"));
		assertEquals(expected, message);
		assertEquals(json, message.toString());
	}

	@ParameterizedTest
	@EnumSource(JsonRxMessageType.class)
	public void testConstructorWithNullId(JsonRxMessageType type) {
		String json = "[" + type.code + ",\"some-method\",[10,11]]";
		// call under test
		JsonRxMessage message = new JsonRxMessage(json);
		JsonRxMessage expected = new JsonRxMessage(type).setMethod("some-method").setBody(new JSONArray("[10,11]"));
		assertEquals(expected, message);
		assertEquals(json, message.toString());
	}

	@ParameterizedTest
	@EnumSource(JsonRxMessageType.class)
	public void testConstructorWithAllNullable(JsonRxMessageType type) {
		String json = "[" + type.code + "]";
		// call under test
		JsonRxMessage message = new JsonRxMessage(json);
		JsonRxMessage expected = new JsonRxMessage(type);
		assertEquals(expected, message);
		assertEquals(json, message.toString());
	}

	@Test
	public void testEmpty() {
		String message = assertThrowsExactly(IllegalArgumentException.class, () -> {
			// call under test
			new JsonRxMessage("[]");
		}).getMessage();
		assertEquals("Expected the fist element of the array to be a message code.", message);
	}

	@Test
	public void testGetId() {
		// call under test
		assertEquals(Optional.of(2), new JsonRxMessage(JsonRxMessageType.Notification).setId(2).getId());
	}

	@Test
	public void testGetIdWithNull() {
		// call under test
		assertEquals(Optional.empty(), new JsonRxMessage(JsonRxMessageType.Notification).setId(null).getId());
	}

	@Test
	public void testGetMethod() {
		// call under test
		assertEquals(Optional.of("method"),
				new JsonRxMessage(JsonRxMessageType.Notification).setMethod("method").getMethod());
	}

	@Test
	public void testGetMethodWithNull() {
		// call under test
		assertEquals(Optional.empty(), new JsonRxMessage(JsonRxMessageType.Notification).setMethod(null).getMethod());
	}

	@Test
	public void testGetBody() {
		// call under test
		assertEquals(Optional.of(new JSONArray("[1]")).get().toString(),
				new JsonRxMessage(JsonRxMessageType.Notification).setBody(new JSONArray("[1]")).getBody().get()
						.toString());
	}

	@Test
	public void testGetBodyWithNull() {
		// call under test
		assertEquals(Optional.empty(),
				new JsonRxMessage(JsonRxMessageType.Notification).setBody((JSONArray) null).getBody());
	}

	@Test
	public void testConstructorNullType() {
		String message = assertThrowsExactly(IllegalArgumentException.class, () -> {
			// call under test
			new JsonRxMessage((JsonRxMessageType) null);
		}).getMessage();
		assertEquals("type is required.", message);
	}
}
