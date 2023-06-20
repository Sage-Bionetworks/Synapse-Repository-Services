package org.sagebionetworks.openapi.datamodel.pathinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class SchemaTest {
	@Test
	public void testInitializeFromJSONObject() {
		assertThrows(UnsupportedOperationException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new Schema().initializeFromJSONObject(adapter);
		});
	}
	
	@Test
	public void testWriteToJSONObject() throws JSONObjectAdapterException {
		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
		JSONObjectAdapterImpl adapterImpl = Mockito.mock(JSONObjectAdapterImpl.class);
		Mockito.doReturn(adapterImpl).when(adapter).createNew();
		JsonSchema jsonSchema = Mockito.mock(JsonSchema.class);
		JSONObjectAdapterImpl jsonSchemaImpl = Mockito.mock(JSONObjectAdapterImpl.class);
		Mockito.doReturn(jsonSchemaImpl).when(jsonSchema).writeToJSONObject(any());
		Schema schema = new Schema().withSchema(jsonSchema);
		
		// call under test
		schema.writeToJSONObject(adapter);
		Mockito.verify(jsonSchema).writeToJSONObject(adapterImpl);
		Mockito.verify(adapter).put("schema", jsonSchemaImpl);
	}
	
	@Test
	public void testWriteToJSONObjectWithNullSchema() throws JSONObjectAdapterException {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new Schema().withSchema(null).writeToJSONObject(adapter);
		});
		assertEquals("The 'schema' field must not be null.", exception.getMessage());
	}
}
