package org.sagebionetworks.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SerializationUtilsTest {

	@Test
	public void testRoundTrip() {
		JSONEntitySample entity = new JSONEntitySample();
		entity.setStringField("foo");
		String ser = SerializationUtils.serializeAndHexEncode(entity);
		JSONEntitySample copy = SerializationUtils.hexDecodeAndDeserialize(ser, JSONEntitySample.class);
		assertEquals(entity, copy);
	}

}
