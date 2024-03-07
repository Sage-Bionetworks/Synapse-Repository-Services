package org.sagebionetworks.openapi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class ServerInfoTest {
	@Test
	public void testInitializeFromJSONObject() {
		assertThrows(UnsupportedOperationException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new ServerInfo().initializeFromJSONObject(adapter);
		});
	}
	
	@Test
	public void testWriteToJSONObject() throws JSONObjectAdapterException {
		ServerInfo info = new ServerInfo().withDescription("DESCRIPTION").withUrl("URL");
		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
		info.writeToJSONObject(adapter);
		
		Mockito.verify(adapter).put("url", "URL");
		Mockito.verify(adapter).put("description", "DESCRIPTION");
	}
	
	@Test
	public void testWriteToJSONObjectWithoutUrl() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			ServerInfo info = new ServerInfo().withDescription("DESCRIPTION");
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			info.writeToJSONObject(adapter);
		});
		assertEquals("The 'url' field is required.", exception.getMessage());
	}
	
	@Test
	public void testWriteToJSONObjectWithoutDescription() throws JSONObjectAdapterException {
		ServerInfo info = new ServerInfo().withUrl("URL");
		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
		info.writeToJSONObject(adapter);
		
		Mockito.verify(adapter).put("url", "URL");
		Mockito.verify(adapter, Mockito.times(0)).put(eq("description"), any(String.class));
	}
}
