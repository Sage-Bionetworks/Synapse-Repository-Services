package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.schema.ObjectType;

public class EntityObjectTranslatorTest {
	
	String id;
	String etag;
	JSONObject object;
	
	EntityObjectTranslator translator;
	
	@BeforeEach
	public void before() {
		id = "syn123";
		etag = "some-etag";
		object = new JSONObject();
		object.put("id", id);
		object.put("etag", etag);
		
		translator = new EntityObjectTranslator();
	}

	@Test
	public void testGetObjectId() {
		// call under test
		String result = translator.getObjectId(object);
		assertEquals(id, result);
	}
	
	@Test
	public void testGetObjectIdWithNullObject() {
		object = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			translator.getObjectId(object);
		});
	}
	
	@Test
	public void testGetEtag() {
		// call under test
		String result = translator.getObjectEtag(object);
		assertEquals(etag, result);
	}
	
	@Test
	public void testGetObjectType() {
		// call under test
		ObjectType result = translator.getObjectType(object);
		assertEquals(ObjectType.entity, result);
	}
	
	
	@Test
	public void testGetObjectTypeWithNullObject() {
		object = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			translator.getObjectEtag(object);
		});
	}
}
