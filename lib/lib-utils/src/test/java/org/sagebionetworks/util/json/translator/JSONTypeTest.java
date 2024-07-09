package org.sagebionetworks.util.json.translator;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class JSONTypeTest {
	
	@Test
	public void testEachType() {
		
		for(JSONType type: JSONType.values()) {
			assertNotNull(type.getJsonType());
			assertNotNull(type);
			assertEquals(type, JSONType.lookupType(type.getJsonType()));
			assertNotNull(type.getFunction());
		}
	}
	
	@Test
	public void testGetString() {
		JSONObject object = new JSONObject();
		object.put("foo", "aString");
		// call under test
		assertEquals("aString", JSONType.lookupType(String.class).getFromJSON("foo", object));
	}
	
	@Test
	public void testGetLong() {
		JSONObject object = new JSONObject();
		object.put("foo", 123L);
		// call under test
		assertEquals(123L, JSONType.lookupType(Long.class).getFromJSON("foo", object));
	}

	@Test
	public void testGetDouble() {
		JSONObject object = new JSONObject();
		object.put("foo", 3.14);
		// call under test
		assertEquals(3.14, JSONType.lookupType(Double.class).getFromJSON("foo", object));
	}
	
	@Test
	public void testGetBoolean() {
		JSONObject object = new JSONObject();
		object.put("foo", false);
		// call under test
		assertEquals(false, JSONType.lookupType(Boolean.class).getFromJSON("foo", object));
	}
	
	@Test
	public void testGetJSONObject() {
		JSONObject child = new JSONObject();
		child.put("name", "someName");
		JSONObject object = new JSONObject();
		object.put("foo", child);
		// call under test
		assertEquals(child, JSONType.lookupType(JSONObject.class).getFromJSON("foo", object));
	}
	
	@Test
	public void testLookupTypeWithUnknown() {
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			JSONType.lookupType(Object.class);
		}).getMessage();
		assertEquals("Unknown type for type: java.lang.Object", message);
	}
}
