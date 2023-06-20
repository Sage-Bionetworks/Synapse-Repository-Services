package org.sagebionetworks.openapi.datamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class TagInfoTest {
	@Test
	public void testInitializeFromJSONObject() {
		assertThrows(UnsupportedOperationException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new TagInfo().initializeFromJSONObject(adapter);
		});
	}
	
	@Test
	public void testWriteToJSONObject() throws JSONObjectAdapterException {
		TagInfo info = new TagInfo().withName("NAME").withDescription("DESCRIPTION");
		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
		info.writeToJSONObject(adapter);
		
		Mockito.verify(adapter).put("name", "NAME");
		Mockito.verify(adapter).put("description", "DESCRIPTION");
	}
	
	@Test
	public void testWriteToJSONObjectWithMissingName() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new TagInfo().withDescription("DESCRIPTION").writeToJSONObject(adapter);
		});
		assertEquals("The 'name' field is required.", exception.getMessage());
	}
	
	@Test
	public void testWriteToJSONObjectWithMissingDescription() throws JSONObjectAdapterException {
		TagInfo info = new TagInfo().withName("NAME");
		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
		info.writeToJSONObject(adapter);
		
		Mockito.verify(adapter).put("name", "NAME");
		Mockito.verify(adapter, Mockito.times(0)).put(eq("description"), any(String.class));
	}
}
