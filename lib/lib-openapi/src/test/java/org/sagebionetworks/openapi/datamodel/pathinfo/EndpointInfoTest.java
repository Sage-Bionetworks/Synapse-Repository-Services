package org.sagebionetworks.openapi.datamodel.pathinfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONArrayAdapterImpl;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class EndpointInfoTest {
	@Test
	public void testInitializeFromJSONObject() {
		assertThrows(UnsupportedOperationException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new EndpointInfo().initializeFromJSONObject(adapter);
		});
	}

	@Test
	public void testWriteToJSONObject() throws JSONObjectAdapterException {
		Map<String, ResponseInfo> responses = new LinkedHashMap<>();
		responses.put("RESPONSE", new ResponseInfo());
		String operationId = "OPERATION_ID";
		RequestBodyInfo requestBody = Mockito.mock(RequestBodyInfo.class);
		JSONObjectAdapterImpl requestBodyObjectAdapter = new JSONObjectAdapterImpl();
		Mockito.doReturn(requestBodyObjectAdapter).when(requestBody).writeToJSONObject(any());

		EndpointInfo info = Mockito.spy(new EndpointInfo().withTags(new ArrayList<>()).withOperationId(operationId)
				.withParameters(new ArrayList<>()).withRequestBody(requestBody).withResponses(responses));
		Mockito.doReturn(responses).when(info).getResponses();
		Mockito.doNothing().when(info).populateResponses(any());
		Mockito.doNothing().when(info).populateTags(any());
		Mockito.doNothing().when(info).populateParameters(any());

		JSONObjectAdapter writeTo = Mockito.mock(JSONObjectAdapter.class);
		JSONObjectAdapterImpl writeToObjectAdapter = new JSONObjectAdapterImpl();
		Mockito.doReturn(writeToObjectAdapter).when(writeTo).createNew();
		JSONArrayAdapterImpl writeToArrayAdapter = new JSONArrayAdapterImpl();
		Mockito.doReturn(writeToArrayAdapter).when(writeTo).createNewArray();
		
		// call under test
		info.writeToJSONObject(writeTo);
		Mockito.verify(info).populateTags(writeToArrayAdapter);
		Mockito.verify(writeTo).put("tags", writeToArrayAdapter);
		Mockito.verify(writeTo).put("operationId", operationId);
		Mockito.verify(writeTo).put("parameters", writeToArrayAdapter);
		Mockito.verify(info).populateParameters(writeToArrayAdapter);
		Mockito.verify(writeTo).put("requestBody", requestBodyObjectAdapter);
		Mockito.verify(requestBody).writeToJSONObject(writeToObjectAdapter);
		Mockito.verify(info).populateResponses(writeToObjectAdapter);
		Mockito.verify(writeTo).put("responses", writeToObjectAdapter);
	}

	@Test
	public void testWriteToJSONObjectWithOnlyResponses() throws JSONObjectAdapterException {
		Map<String, ResponseInfo> responses = new LinkedHashMap<>();
		responses.put("RESPONSE", new ResponseInfo());
		EndpointInfo info = Mockito.spy(new EndpointInfo().withResponses(responses));
		Mockito.doReturn(responses).when(info).getResponses();
		Mockito.doNothing().when(info).populateResponses(any());

		JSONObjectAdapter writeTo = Mockito.mock(JSONObjectAdapter.class);
		JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl();
		Mockito.doReturn(adapter).when(writeTo).createNew();
		// call under test
		info.writeToJSONObject(writeTo);
		Mockito.verify(writeTo, Mockito.times(0)).put(eq("tags"), any(JSONArrayAdapter.class));
		Mockito.verify(writeTo, Mockito.times(0)).put(eq("operationId"), any(String.class));
		Mockito.verify(writeTo, Mockito.times(0)).put(eq("parameters"), any(JSONArrayAdapter.class));
		Mockito.verify(writeTo, Mockito.times(0)).put(eq("requestBody"), any(JSONObjectAdapter.class));
		Mockito.verify(info).populateResponses(any());
		Mockito.verify(writeTo).put("responses", adapter);
	}

	@Test
	public void testWriteToJSONObjectWithEmptyResponses() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new EndpointInfo().withResponses(new LinkedHashMap<>()).writeToJSONObject(adapter);
		});
		assertEquals("Responses must not be empty.", exception.getMessage());
	}

	@Test
	public void testWriteToJSONObjectWithNullResponses() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new EndpointInfo().writeToJSONObject(adapter);
		});
		assertEquals("Responses must not be null.", exception.getMessage());
	}

	@Test
	public void testPopulateResponses() throws JSONObjectAdapterException {
		Map<String, ResponseInfo> responses = new LinkedHashMap<>();
		ResponseInfo response1 = Mockito.mock(ResponseInfo.class);
		ResponseInfo response2 = Mockito.mock(ResponseInfo.class);
		JSONObjectAdapterImpl impl1 = new JSONObjectAdapterImpl();
		JSONObjectAdapterImpl impl2 = new JSONObjectAdapterImpl();
		Mockito.doReturn(impl1).when(response1).writeToJSONObject(any());
		Mockito.doReturn(impl2).when(response2).writeToJSONObject(any());
		responses.put("RESPONSE_1", response1);
		responses.put("RESPONSE_2", response2);

		EndpointInfo info = new EndpointInfo();
		info.withResponses(responses);
		JSONObjectAdapter responsesJson = Mockito.mock(JSONObjectAdapter.class);

		// call under test
		info.populateResponses(responsesJson);
		Mockito.verify(responsesJson, Mockito.times(2)).put(anyString(), any(JSONObjectAdapter.class));
		InOrder inOrder = Mockito.inOrder(responsesJson);
		inOrder.verify(responsesJson).put("RESPONSE_1", impl1);
		inOrder.verify(responsesJson).put("RESPONSE_2", impl2);
	}

	@Test
	public void testPopulateParameters() throws JSONObjectAdapterException {
		ParameterInfo param1 = Mockito.mock(ParameterInfo.class);
		ParameterInfo param2 = Mockito.mock(ParameterInfo.class);
		JSONObjectAdapterImpl impl1 = new JSONObjectAdapterImpl();
		JSONObjectAdapterImpl impl2 = new JSONObjectAdapterImpl();
		Mockito.doReturn(impl1).when(param1).writeToJSONObject(any());
		Mockito.doReturn(impl2).when(param2).writeToJSONObject(any());
		EndpointInfo info = new EndpointInfo();
		info.withParameters(new ArrayList<>(Arrays.asList(param1, param2)));
		JSONArrayAdapter parameters = Mockito.mock(JSONArrayAdapter.class);

		// call under test
		info.populateParameters(parameters);
		Mockito.verify(parameters, Mockito.times(2)).put(anyInt(), any(JSONObjectAdapter.class));
		InOrder inOrder = Mockito.inOrder(parameters);
		inOrder.verify(parameters).put(0, impl1);
		inOrder.verify(parameters).put(1, impl2);
	}

	@Test
	public void testPopulateTags() throws JSONObjectAdapterException {
		EndpointInfo info = new EndpointInfo();
		info.withTags(new ArrayList<>(Arrays.asList("TAG_1", "TAG_2")));
		JSONArrayAdapter tags = Mockito.mock(JSONArrayAdapter.class);

		// call under test
		info.populateTags(tags);
		Mockito.verify(tags, Mockito.times(2)).put(anyInt(), anyString());
		InOrder inOrder = Mockito.inOrder(tags);
		inOrder.verify(tags).put(0, "TAG_1");
		inOrder.verify(tags).put(1, "TAG_2");
	}
}
