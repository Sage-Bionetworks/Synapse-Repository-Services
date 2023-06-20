package org.sagebionetworks.openapi.datamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class ApiInfoTest {
	@Test
	public void testInitializeFromJSONObject() {
		assertThrows(UnsupportedOperationException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new ApiInfo().initializeFromJSONObject(adapter);
		});
	}
	
	@Test
	public void testWriteToJSONObject() throws JSONObjectAdapterException {
		ApiInfo info = new ApiInfo().withTitle("TITLE").withVersion("V0").withSummary("SUMMARY");
		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
		info.writeToJSONObject(adapter);
		
		Mockito.verify(adapter).put("title", "TITLE");
		Mockito.verify(adapter).put("version", "V0");
		Mockito.verify(adapter).put("summary", "SUMMARY");
	}
	
	@Test
	public void testWriteToJSONObjectWithMissingTitle() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new ApiInfo().withSummary("SUMMARY").withVersion("V0").writeToJSONObject(adapter);
		});
		assertEquals("The 'title' is a required attribute of ApiInfo.", exception.getMessage());
	}
	
	@Test
	public void testWriteToJSONObjectWithMissingVersion() throws JSONObjectAdapterException {
		ApiInfo info = new ApiInfo().withTitle("TITLE").withSummary("SUMMARY");
		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
		info.writeToJSONObject(adapter);
		
		Mockito.verify(adapter).put("title", "TITLE");
		Mockito.verify(adapter).put("summary", "SUMMARY");
		Mockito.verify(adapter, Mockito.times(0)).put(eq("version"), any(String.class));
	}
	
	@Test
	public void testWriteToJSONObjectWithMissingSummary() throws JSONObjectAdapterException {
		ApiInfo info = new ApiInfo().withTitle("TITLE").withVersion("V0");
		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
		info.writeToJSONObject(adapter);
		
		Mockito.verify(adapter).put("title", "TITLE");
		Mockito.verify(adapter).put("version", "V0");
		Mockito.verify(adapter, Mockito.times(0)).put(eq("summary"), any(String.class));
	}
}
