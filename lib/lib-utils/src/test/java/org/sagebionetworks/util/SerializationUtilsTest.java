package org.sagebionetworks.util;

import static org.junit.Assert.*;

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
	
	@Test
	public void testClone(){
		JSONEntitySample entity = new JSONEntitySample();
		entity.setStringField("foo");
		
		JSONEntitySample clone = SerializationUtils.cloneJSONEntity(entity);
		assertNotNull(clone);
		assertFalse(clone == entity);
		assertEquals(entity, clone);
	}

}
