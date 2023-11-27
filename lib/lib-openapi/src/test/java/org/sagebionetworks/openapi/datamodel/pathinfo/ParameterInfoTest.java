package org.sagebionetworks.openapi.datamodel.pathinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class ParameterInfoTest {
	@Test
	public void testInitializeFromJSONObject() {
		assertThrows(UnsupportedOperationException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new ParameterInfo().initializeFromJSONObject(adapter);
		});
	}
	
	@Test
	public void testWriteToJSONObject() throws JSONObjectAdapterException {
		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
		JsonSchema schema = Mockito.mock(JsonSchema.class);
		JSONObjectAdapterImpl impl = new JSONObjectAdapterImpl();
		Mockito.doReturn(impl).when(schema).writeToJSONObject(any());
		Mockito.doReturn(impl).when(adapter).createNew();
		ParameterInfo info = new ParameterInfo().withName("NAME").withIn("IN").withDescription("DESCRIPTION").withRequired(true)
				.withSchema(schema);
		// call under test.
		info.writeToJSONObject(adapter);
		Mockito.verify(adapter).put("name", "NAME");
		Mockito.verify(adapter).put("in", "IN");
		Mockito.verify(adapter).put("description", "DESCRIPTION");
		Mockito.verify(adapter).put("required", true);
		Mockito.verify(adapter).put("schema", impl);
		Mockito.verify(schema).writeToJSONObject(impl);
	}

	@Test
	public void testWriteToJSONObjectWithOnlyNameAndInFields() throws JSONObjectAdapterException {
		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
		ParameterInfo info = new ParameterInfo().withName("NAME").withIn("IN").withDescription(null).withRequired(false)
				.withSchema(null);
		// call under test.
		info.writeToJSONObject(adapter);
		Mockito.verify(adapter).put("name", "NAME");
		Mockito.verify(adapter).put("in", "IN");
		Mockito.verify(adapter).put(eq("required"), eq(false));
		Mockito.verify(adapter, Mockito.times(0)).put(eq("description"), anyString());
		Mockito.verify(adapter, Mockito.times(0)).put(eq("schema"), any(JSONObjectAdapter.class));
	}

	@Test
	public void testWriteToJSONObjectWithoutIn() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new ParameterInfo().withName("Name").withIn(null).writeToJSONObject(adapter);
		});
		assertEquals("The 'in' field must not be null.", exception.getMessage());
	}

	@Test
	public void testWriteToJSONObjectWithoutName() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new ParameterInfo().withName(null).writeToJSONObject(adapter);
		});
		assertEquals("The 'name' field must not be null.", exception.getMessage());
	}
}
