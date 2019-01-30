package org.sagebionetworks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.schema.adapter.JSONEntity;

public class SerializationUtilsTest {

	@Test
	public void testRoundTrip() {
		JSONEntitySample entity = new JSONEntitySample();
		entity.setStringField("foo");
		String ser = SerializationUtils.serializeAndHexEncode(entity);
		JSONEntity copy = SerializationUtils.hexDecodeAndDeserialize(ser, JSONEntity.class);
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
