package org.sagebionetworks.util;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class JsonEntityUtilsTest {

	@Test
	public void testRoundTrip() {
		JSONEntitySample entity = new JSONEntitySample();
		entity.setStringField("something");
		// call under test
		String json = JsonEntityUtils.toJsonString(entity);
		assertEquals("{\"concreteType\":\"org.sagebionetworks.util.JSONEntitySample\",\"stringField\":\"something\"}", json);
		// call under test
		JSONEntitySample clone = JsonEntityUtils.fromJsonString(json, JSONEntitySample.class);
		assertEquals(entity, clone);
	}
	
	@Test
	public void testRoundTripWithNull() {
		JSONEntitySample entity = null;
		// call under test
		String json = JsonEntityUtils.toJsonString(entity);
		assertNull(json);
		// call under test
		JSONEntitySample clone = JsonEntityUtils.fromJsonString(json, JSONEntitySample.class);
		assertNull(clone);
	}
}
