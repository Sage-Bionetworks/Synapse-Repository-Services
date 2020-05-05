package org.sagebionetworks.repo.model.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.LinkedHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import nl.jqno.equalsverifier.EqualsVerifier;

public class NormalizedJsonSchemaTest {
	
	JsonSchema schema;
	
	@BeforeEach
	public void before() {

		JsonSchema subA = new JsonSchema();
		subA.setType(Type.string);
		subA.setDescription("sub-a");
		
		JsonSchema subB = new JsonSchema();
		subB.setType(Type.number);
		subB.setDescription("sub-b");
		
		JsonSchema subZ = new JsonSchema();
		subZ.setType(Type.array);
		JsonSchema subZType = new JsonSchema();
		subZType.setType(Type.string);
		subZ.setItems(subZType);
		subZ.setDescription("sub-z");
		
		schema = new JsonSchema();
		schema.setDescription("Schema with multiple properties in a fixed order");
		schema.setType(Type.object);
		schema.setProperties(new LinkedHashMap<String, JsonSchema>(3));
		schema.getProperties().put("a", subA);
		schema.getProperties().put("z", subZ);
		schema.getProperties().put("b", subB);
	}

	@Test
	public void testStableRoundTrip() throws JSONObjectAdapterException {
		// call under test
		NormalizedJsonSchema normalized = new NormalizedJsonSchema(schema);
		assertNotNull(normalized);
		assertNotNull(normalized.getNormalizedSchemaJson());
		assertNotNull(normalized.getSha256Hex());
		// Use the resulting normal to create a new schema
		JsonSchema fromNormalJson = EntityFactory.createEntityFromJSONString(normalized.getNormalizedSchemaJson(), JsonSchema.class);
		assertEquals(schema, fromNormalJson);
		// Call under test
		NormalizedJsonSchema result = new NormalizedJsonSchema(fromNormalJson);
		// the hash must not change when marshaling to/from JSON.
		assertEquals(normalized.getSha256Hex(), result.getSha256Hex());
	}
	
	@Test
	public void testHashAndEquals() {
		EqualsVerifier.forClass(NormalizedJsonSchema.class).verify();
	}
}
