package org.sagebionetworks.openapi.model.pathinfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class RequestBodyInfoTest {
	@Test
	public void testInitializeFromJSONObject() {
		assertThrows(UnsupportedOperationException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new RequestBodyInfo().initializeFromJSONObject(adapter);
		});
	}
	
	@Test
	public void testWriteToJSONObject() throws JSONObjectAdapterException {
		Map<String, Schema> content = new LinkedHashMap<>();
		Schema schema = Mockito.mock(Schema.class);
		content.put("CONTENT", schema);
		RequestBodyInfo info = new RequestBodyInfo().withContent(content).withRequired(true);

		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
		JSONObjectAdapterImpl adapterCreateNewResult = Mockito.mock(JSONObjectAdapterImpl.class);
		Mockito.doReturn(adapterCreateNewResult).when(adapter).createNew();
		JSONObjectAdapterImpl resultFromSchema = new JSONObjectAdapterImpl();
		Mockito.doReturn(resultFromSchema).when(schema).writeToJSONObject(any());
		
		// call under test
		info.writeToJSONObject(adapter);
		Mockito.verify(adapterCreateNewResult).put("CONTENT", resultFromSchema);
		Mockito.verify(schema).writeToJSONObject(adapterCreateNewResult);
		Mockito.verify(adapter).put("required", true);
	}
	
	@Test
	public void testWriteToJSONObjectWithoutRequired() throws JSONObjectAdapterException {
		Map<String, Schema> content = new LinkedHashMap<>();
		Schema schema = Mockito.mock(Schema.class);
		content.put("CONTENT", schema);
		RequestBodyInfo info = new RequestBodyInfo().withContent(content).withRequired(false);
		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
		JSONObjectAdapterImpl adapterCreateNewResult = Mockito.mock(JSONObjectAdapterImpl.class);
		Mockito.doReturn(adapterCreateNewResult).when(adapter).createNew();

		// call under test
		info.writeToJSONObject(adapter);
		Mockito.verify(adapter).put(eq("required"), eq(false));
	}
	
	@Test
	public void testWriteToJSONObjectWithEmptyContent() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new RequestBodyInfo().withContent(new LinkedHashMap<>()).writeToJSONObject(adapter);
		});
		assertEquals("The 'content' field must not be empty.", exception.getMessage());
	}
	
	@Test
	public void testWriteToJSONObjectWithMissingContent() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new RequestBodyInfo().withContent(null).writeToJSONObject(adapter);
		});
		assertEquals("The 'content' field must not be null.", exception.getMessage());
	}
}
