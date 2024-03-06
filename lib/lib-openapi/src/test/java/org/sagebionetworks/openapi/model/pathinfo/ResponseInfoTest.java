package org.sagebionetworks.openapi.model.pathinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sagebionetworks.openapi.model.pathinfo.ResponseInfo;
import org.sagebionetworks.openapi.model.pathinfo.Schema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class ResponseInfoTest {
	@Test
	public void testInitializeFromJSONObject() {
		assertThrows(UnsupportedOperationException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new ResponseInfo().initializeFromJSONObject(adapter);
		});
	}
	
	@Test
	public void testWriteToJSONObject() throws JSONObjectAdapterException {
		Map<String, Schema> content = new LinkedHashMap<>();
		Schema schema = Mockito.mock(Schema.class);
		content.put("CONTENT", schema);
		ResponseInfo info = Mockito.spy(new ResponseInfo().withContent(content).withDescription("DESCRIPTION"));
		Mockito.doNothing().when(info).populateContent(any());
		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
		JSONObjectAdapterImpl impl = Mockito.mock(JSONObjectAdapterImpl.class);
		Mockito.doReturn(impl).when(adapter).createNew();
		
		// call under test
		info.writeToJSONObject(adapter);
		Mockito.verify(adapter).put("description", "DESCRIPTION");
		Mockito.verify(adapter).put("content", impl);
		Mockito.verify(info).populateContent(impl);
	}
	
	@Test
	public void testPopulateContent() throws JSONObjectAdapterException {
		Map<String, Schema> content = new LinkedHashMap<>();
		Schema schema1 = Mockito.mock(Schema.class);
		Schema schema2 = Mockito.mock(Schema.class);
		JSONObjectAdapterImpl resultSchema1 = Mockito.mock(JSONObjectAdapterImpl.class);
		JSONObjectAdapterImpl resultSchema2 = Mockito.mock(JSONObjectAdapterImpl.class);
		Mockito.doReturn(resultSchema1).when(schema1).writeToJSONObject(any());
		Mockito.doReturn(resultSchema2).when(schema2).writeToJSONObject(any());
		content.put("CONTENT_1", schema1);
		content.put("CONTENT_2", schema2);
		ResponseInfo info = new ResponseInfo().withContent(content);
		JSONObjectAdapter contentJson = Mockito.mock(JSONObjectAdapter.class);
		JSONObjectAdapterImpl impl = Mockito.mock(JSONObjectAdapterImpl.class);
		Mockito.doReturn(impl).when(contentJson).createNew();
		
		// call under test
		info.populateContent(contentJson);
		Mockito.verify(schema1).writeToJSONObject(impl);
		Mockito.verify(schema2).writeToJSONObject(impl);
		Mockito.verify(contentJson, Mockito.times(2)).put(anyString(), any(JSONObjectAdapter.class));
		InOrder inOrder = Mockito.inOrder(contentJson);
		inOrder.verify(contentJson).put("CONTENT_1", resultSchema1);
		inOrder.verify(contentJson).put("CONTENT_2", resultSchema2);
	}
	
	@Test
	public void testWriteToJSONObjectWithEmptyContent() throws JSONObjectAdapterException {
		ResponseInfo info = new ResponseInfo().withDescription("DESCRIPTION").withContent(new LinkedHashMap<>());
		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
		
		// call under test
		info.writeToJSONObject(adapter);
		Mockito.verify(adapter, Mockito.times(0)).put(eq("content"), any(JSONObjectAdapter.class));
	}
	
	@Test
	public void testWriteToJSONObjectWithNullContent() throws JSONObjectAdapterException {
		ResponseInfo info = new ResponseInfo().withDescription("DESCRIPTION").withContent(null);
		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
		
		// call under test
		info.writeToJSONObject(adapter);
		Mockito.verify(adapter, Mockito.times(0)).put(eq("content"), any(JSONObjectAdapter.class));
	}
	
	@Test
	public void testWriteToJSONObjectWithMissingDescription() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new ResponseInfo().withDescription(null).writeToJSONObject(adapter);
		});
		assertEquals("The 'description' field is required.", exception.getMessage());
	}
}
