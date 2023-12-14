package org.sagebionetworks.openapi.datamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.openapi.datamodel.pathinfo.EndpointInfo;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONArrayAdapterImpl;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.translator.ControllerModelDoclet;

import com.google.gson.Gson;

public class OpenAPISpecModelTest {
	@Test
	public void testInitializeFromJSONObject() {
		assertThrows(UnsupportedOperationException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new OpenAPISpecModel().initializeFromJSONObject(adapter);
		});
	}

	@Test
	public void testWriteToJSONObject() throws JSONObjectAdapterException {
		JSONObjectAdapter writeTo = Mockito.mock(JSONObjectAdapter.class);
		JSONObjectAdapterImpl apiInfoImpl = new JSONObjectAdapterImpl();
		JSONObjectAdapterImpl pathsImpl = new JSONObjectAdapterImpl();
		JSONObjectAdapterImpl componentsImpl = new JSONObjectAdapterImpl();
		JSONArrayAdapterImpl serversImpl = new JSONArrayAdapterImpl();
		JSONArrayAdapterImpl tagsImpl = new JSONArrayAdapterImpl();
		Mockito.doReturn(apiInfoImpl, pathsImpl, componentsImpl).when(writeTo).createNew();
		Mockito.doReturn(serversImpl, tagsImpl).when(writeTo).createNewArray();

		Map<String, Map<String, EndpointInfo>> paths = new LinkedHashMap<>();
		paths.put("PATH", new LinkedHashMap<>());

		ApiInfo apiInfo = Mockito.mock(ApiInfo.class);
		JSONObjectAdapterImpl apiInfoResult = new JSONObjectAdapterImpl();
		Mockito.doReturn(apiInfoResult).when(apiInfo).writeToJSONObject(any());

		JsonSchema jsonSchema = new JsonSchema();
		jsonSchema.setType(Type.integer);
		Map<String, JsonSchema> schemas = new LinkedHashMap<>();
		schemas.put("COMPONENT_TYPE_1", jsonSchema);
		Map<String, SecurityScheme> securitySchemes = new HashMap<>();
		securitySchemes.put("bearerAuth", new SecurityScheme().withType("http").withScheme("bearer"));
		Components components = new Components().withSchemas(schemas).withSecuritySchemes(securitySchemes);


		OpenAPISpecModel model = Mockito.spy(new OpenAPISpecModel().withOpenapi("V3").withInfo(apiInfo).withPaths(paths)
				.withServers(new ArrayList<>()).withTags(new ArrayList<>()).withComponents(components));
		Mockito.doNothing().when(model).populateServers(any());
		Mockito.doNothing().when(model).populateTags(any());
		Mockito.doNothing().when(model).populatePaths(any());

		// call under test
		model.writeToJSONObject(writeTo);
		Mockito.verify(writeTo).put("openapi", "V3");
		Mockito.verify(writeTo).put("info", apiInfoResult);
		Mockito.verify(apiInfo).writeToJSONObject(apiInfoImpl);
		Mockito.verify(model).populateServers(serversImpl);
		Mockito.verify(writeTo).put("servers", serversImpl);
		Mockito.verify(model).populateTags(tagsImpl);
		Mockito.verify(writeTo).put("tags", tagsImpl);
		Mockito.verify(model).populatePaths(pathsImpl);
		Mockito.verify(writeTo).put("paths", pathsImpl);
		Mockito.verify(writeTo).put("components", componentsImpl);
	}

	@Test
	public void testPopulatePaths() throws JSONObjectAdapterException {
		JSONObjectAdapter pathsAdapter = Mockito.mock(JSONObjectAdapter.class);
		JSONObjectAdapterImpl pathAdapterImpl1 = new JSONObjectAdapterImpl();
		JSONObjectAdapterImpl pathAdapterImpl2 = new JSONObjectAdapterImpl();
		Mockito.doReturn(pathAdapterImpl1, pathAdapterImpl2).when(pathsAdapter).createNew();
		Map<String, Map<String, EndpointInfo>> paths = new LinkedHashMap<>();
		Map<String, EndpointInfo> path1 = new LinkedHashMap<>();
		Map<String, EndpointInfo> path2 = new LinkedHashMap<>();
		paths.put("PATH_1", path1);
		paths.put("PATH_2", path2);

		OpenAPISpecModel model = Mockito.spy(new OpenAPISpecModel().withPaths(paths));
		// call under test
		model.populatePaths(pathsAdapter);
		Mockito.verify(pathsAdapter).put("PATH_1", pathAdapterImpl1);
		Mockito.verify(pathsAdapter).put("PATH_2", pathAdapterImpl2);
		Mockito.verify(model).populateCurrentPath(pathAdapterImpl1, "PATH_1");
		Mockito.verify(model).populateCurrentPath(pathAdapterImpl2, "PATH_2");
	}

	@Test
	public void testPopulateCurrentPath() throws JSONObjectAdapterException {
		JSONObjectAdapter currentPathAdapter = Mockito.mock(JSONObjectAdapter.class);
		JSONObjectAdapterImpl currentPathAdapterImpl = new JSONObjectAdapterImpl();
		Mockito.doReturn(currentPathAdapterImpl).when(currentPathAdapter).createNew();

		Map<String, Map<String, EndpointInfo>> paths = new LinkedHashMap<>();
		paths.put("PATH_1", new LinkedHashMap<>());
		EndpointInfo endpoint1 = Mockito.mock(EndpointInfo.class);
		EndpointInfo endpoint2 = Mockito.mock(EndpointInfo.class);
		JSONObjectAdapterImpl endpoint1Impl = new JSONObjectAdapterImpl();
		JSONObjectAdapterImpl endpoint2Impl = new JSONObjectAdapterImpl();
		Mockito.doReturn(endpoint1Impl).when(endpoint1).writeToJSONObject(any());
		Mockito.doReturn(endpoint2Impl).when(endpoint2).writeToJSONObject(any());
		paths.get("PATH_1").put("OPERATION_1", endpoint1);
		paths.get("PATH_1").put("OPERATION_2", endpoint2);

		// call under test
		new OpenAPISpecModel().withPaths(paths).populateCurrentPath(currentPathAdapter, "PATH_1");
		Mockito.verify(currentPathAdapter).put("OPERATION_1", endpoint1Impl);
		Mockito.verify(currentPathAdapter).put("OPERATION_2", endpoint2Impl);
		Mockito.verify(endpoint1).writeToJSONObject(currentPathAdapterImpl);
		Mockito.verify(endpoint2).writeToJSONObject(currentPathAdapterImpl);
	}

	@Test
	public void testPopulateTags() throws JSONObjectAdapterException {
		JSONArrayAdapter tags = Mockito.mock(JSONArrayAdapter.class);
		JSONObjectAdapterImpl tagsImpl = new JSONObjectAdapterImpl();
		Mockito.doReturn(tagsImpl).when(tags).createNew();
		TagInfo tag1 = Mockito.mock(TagInfo.class);
		TagInfo tag2 = Mockito.mock(TagInfo.class);
		JSONObjectAdapterImpl tag1Impl = new JSONObjectAdapterImpl();
		JSONObjectAdapterImpl tag2Impl = new JSONObjectAdapterImpl();
		Mockito.doReturn(tag1Impl).when(tag1).writeToJSONObject(any());
		Mockito.doReturn(tag2Impl).when(tag2).writeToJSONObject(any());
		List<TagInfo> tagInfoList = new ArrayList<>(Arrays.asList(tag1, tag2));

		// call under test
		new OpenAPISpecModel().withTags(tagInfoList).populateTags(tags);
		Mockito.verify(tags).put(0, tag1Impl);
		Mockito.verify(tags).put(1, tag2Impl);
		Mockito.verify(tag1).writeToJSONObject(tagsImpl);
		Mockito.verify(tag2).writeToJSONObject(tagsImpl);
	}

	@Test
	public void testPopulateServers() throws JSONObjectAdapterException {
		JSONArrayAdapter servers = Mockito.mock(JSONArrayAdapter.class);
		JSONObjectAdapterImpl serversImpl = new JSONObjectAdapterImpl();
		Mockito.doReturn(serversImpl).when(servers).createNew();
		ServerInfo server1 = Mockito.mock(ServerInfo.class);
		ServerInfo server2 = Mockito.mock(ServerInfo.class);
		JSONObjectAdapterImpl server1Impl = new JSONObjectAdapterImpl();
		JSONObjectAdapterImpl server2Impl = new JSONObjectAdapterImpl();
		Mockito.doReturn(server1Impl).when(server1).writeToJSONObject(any());
		Mockito.doReturn(server2Impl).when(server2).writeToJSONObject(any());
		List<ServerInfo> serverInfoList = new ArrayList<>(Arrays.asList(server1, server2));

		// call under test
		new OpenAPISpecModel().withServers(serverInfoList).populateServers(servers);
		Mockito.verify(servers).put(0, server1Impl);
		Mockito.verify(servers).put(1, server2Impl);
		Mockito.verify(server1).writeToJSONObject(serversImpl);
		Mockito.verify(server2).writeToJSONObject(serversImpl);
	}

	@Test
	public void testWriteToJSONObjectWithOnlyRequiredProperties() throws JSONObjectAdapterException {
		Map<String, Map<String, EndpointInfo>> paths = new LinkedHashMap<>();
		paths.put("PATH", new LinkedHashMap<>());
		ApiInfo apiInfo = Mockito.mock(ApiInfo.class);
		Mockito.doReturn(new JSONObjectAdapterImpl()).when(apiInfo).writeToJSONObject(any());
		OpenAPISpecModel model = Mockito
				.spy(new OpenAPISpecModel().withOpenapi("V3").withInfo(apiInfo).withPaths(paths));
		Mockito.doNothing().when(model).populatePaths(any());
		JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);

		// call under test
		model.writeToJSONObject(adapter);
		Mockito.verify(model, Mockito.times(0)).populateServers(any());
		Mockito.verify(adapter, Mockito.times(0)).put(eq("servers"), any(JSONArrayAdapter.class));
		Mockito.verify(model, Mockito.times(0)).populateTags(any());
		Mockito.verify(adapter, Mockito.times(0)).put(eq("tags"), any(JSONArrayAdapter.class));
		Mockito.verify(adapter, Mockito.times(0)).put(eq("components"), any(JSONObjectAdapter.class));
	}

	@Test
	public void testWriteToJSONObjectWithEmptyPaths() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new OpenAPISpecModel().withOpenapi("V3").withInfo(new ApiInfo()).withPaths(new LinkedHashMap<>())
					.writeToJSONObject(adapter);
		});
		assertEquals("The 'paths' field should not be empty.", exception.getMessage());
	}

	@Test
	public void testWriteToJSONObjectWithNullPaths() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new OpenAPISpecModel().withOpenapi("V3").withInfo(new ApiInfo()).withPaths(null).writeToJSONObject(adapter);
		});
		assertEquals("The 'paths' field should not be null.", exception.getMessage());
	}

	@Test
	public void testWriteToJSONObjectWithNullInfo() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new OpenAPISpecModel().withOpenapi("V3").withInfo(null).writeToJSONObject(adapter);
		});
		assertEquals("The 'info' field should not be null.", exception.getMessage());
	}

	@Test
	public void testWriteToJSONObjectWithNullOpenapi() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			JSONObjectAdapter adapter = Mockito.mock(JSONObjectAdapter.class);
			// call under test
			new OpenAPISpecModel().withOpenapi(null).writeToJSONObject(adapter);
		});
		assertEquals("The 'openapi' field should not be null.", exception.getMessage());
	}
}
