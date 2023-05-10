package org.sagebionetworks.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.controller.model.ControllerModel;
import org.sagebionetworks.controller.model.MethodModel;
import org.sagebionetworks.controller.model.Operation;
import org.sagebionetworks.controller.model.ParameterLocation;
import org.sagebionetworks.controller.model.ParameterModel;
import org.sagebionetworks.controller.model.RequestBodyModel;
import org.sagebionetworks.controller.model.ResponseModel;
import org.sagebionetworks.openapi.datamodel.ApiInfo;
import org.sagebionetworks.openapi.datamodel.OpenAPISpecModel;
import org.sagebionetworks.openapi.datamodel.ServerInfo;
import org.sagebionetworks.openapi.datamodel.TagInfo;
import org.sagebionetworks.openapi.datamodel.pathinfo.EndpointInfo;
import org.sagebionetworks.openapi.datamodel.pathinfo.ParameterInfo;
import org.sagebionetworks.openapi.datamodel.pathinfo.RequestBodyInfo;
import org.sagebionetworks.openapi.datamodel.pathinfo.ResponseInfo;
import org.sagebionetworks.openapi.datamodel.pathinfo.Schema;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;

public class ControllerModelsToOpenAPIModelTranslatorTest {
	ControllerModelsToOpenAPIModelTranslator translator;

	private static final String DESCRIPTION = "DESCRIPTION";

	@BeforeEach
	private void setUp() {
		this.translator = Mockito.spy(new ControllerModelsToOpenAPIModelTranslator());
	}
	
	@Test
	public void testTranslate() {
		String displayName = "DISPLAY_NAME";
		String basePath = "/BASE_PATH";
		List<MethodModel> methods = new ArrayList<>();
		ControllerModel controllerModel = new ControllerModel().withDisplayName(displayName).withPath(basePath)
				.withMethods(methods).withDescription(DESCRIPTION);
		ApiInfo apiInfo = new ApiInfo();
		List<ServerInfo> servers = new ArrayList<>();

		Mockito.doNothing().when(translator).insertPaths(any(List.class), any(String.class), any(String.class),
				any(Map.class));
		Mockito.doReturn(apiInfo).when(translator).getApiInfo();
		Mockito.doReturn(servers).when(translator).getServers();

		OpenAPISpecModel result = translator.translate(Arrays.asList(controllerModel));
		List<TagInfo> tags = new ArrayList<>();
		tags.add(new TagInfo().withDescription(DESCRIPTION).withName(displayName));
		OpenAPISpecModel expected = new OpenAPISpecModel().withInfo(apiInfo).withOpenapi("3.0.1").withServers(servers)
				.withComponents(null).withPaths(new LinkedHashMap<>()).withTags(tags);
		assertEquals(expected, result);

		Mockito.verify(translator).insertPaths(methods, basePath, displayName, result.getPaths());
		Mockito.verify(translator).getApiInfo();
		Mockito.verify(translator).getServers();
	}

	@Test
	public void testTranslateWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.translate(null);
		});
		assertEquals("controllerModels is required.", exception.getMessage());
	}

	@Test
	public void testGetApiInfo() {
		ApiInfo expectedApiInfo = new ApiInfo().withTitle("Sample OpenAPI definition").withVersion("v1");
		assertEquals(expectedApiInfo, translator.getApiInfo());
	}

	@Test
	public void testGetServers() {
		ServerInfo server = new ServerInfo().withUrl("https://repo-prod.prod.sagebase.org")
				.withDescription("This is the generated server URL");
		assertEquals(new ArrayList<>(Arrays.asList(server)), translator.getServers());
	}

	@Test
	public void testInsertPaths() {
		List<MethodModel> methods = new ArrayList<>();
		String basePath = "/BASE_PATH";
		String displayName = "DISPLAY_NAME";
		Map<String, Map<String, EndpointInfo>> paths = new LinkedHashMap<>();

		String methodPath = "/METHOD_PATH";
		String observedMethodPath = "\"" + methodPath + "\"";
		MethodModel method = new MethodModel().withPath(observedMethodPath).withOperation(Operation.get);
		methods.add(method);

		EndpointInfo endpointInfo = new EndpointInfo();
		Mockito.doReturn(endpointInfo).when(translator).getEndpointInfo(any(MethodModel.class), any(String.class));

		// call under test.
		translator.insertPaths(methods, basePath, displayName, paths);

		Map<String, Map<String, EndpointInfo>> expectedPaths = new LinkedHashMap<>();
		Map<String, EndpointInfo> operationToEndpoint = new LinkedHashMap<>();
		operationToEndpoint.put("get", endpointInfo);
		expectedPaths.put(basePath + methodPath, operationToEndpoint);

		assertEquals(expectedPaths, paths);
		Mockito.verify(translator).getEndpointInfo(method, displayName);
	}

	@Test
	public void testInsertPathsWithNullPaths() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.insertPaths(new ArrayList<>(), "", "", null);
		});
		assertEquals("paths is required.", exception.getMessage());
	}

	@Test
	public void testInsertPathsWithNullDisplayName() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.insertPaths(new ArrayList<>(), "", null, new LinkedHashMap<>());
		});
		assertEquals("displayName is required.", exception.getMessage());
	}

	@Test
	public void testInsertPathsWithNullBasePath() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.insertPaths(new ArrayList<>(), null, "", new LinkedHashMap<>());
		});
		assertEquals("basePath is required.", exception.getMessage());
	}

	@Test
	public void testInsertPathsWithNullMethods() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.insertPaths(null, "", "", new LinkedHashMap<>());
		});
		assertEquals("methods is required.", exception.getMessage());
	}

	@Test
	public void testInsertOperationAndEndpointInfoWithDuplicateOperation() {
		Map<String, EndpointInfo> operationToEndpointInfo = new LinkedHashMap<>();
		MethodModel method1 = new MethodModel().withOperation(Operation.get);
		String displayName = "DISPLAY_NAME";
		EndpointInfo endpointInfo = new EndpointInfo();
		Mockito.doReturn(endpointInfo).when(translator).getEndpointInfo(any(MethodModel.class), any(String.class));
		translator.insertOperationAndEndpointInfo(operationToEndpointInfo, method1, displayName);

		MethodModel method2 = new MethodModel().withOperation(Operation.get);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.insertOperationAndEndpointInfo(operationToEndpointInfo, method2, displayName);
		});
		assertEquals("OperationToEndpoint already contains operation get", exception.getMessage());
		Mockito.verify(translator).getEndpointInfo(method1, displayName);
	}

	@Test
	public void testInsertOperationAndEndpointInfo() {
		Map<String, EndpointInfo> operationToEndpointInfo = new LinkedHashMap<>();
		MethodModel method = new MethodModel().withOperation(Operation.get);
		String displayName = "DISPLAY_NAME";
		EndpointInfo endpointInfo = new EndpointInfo();
		Mockito.doReturn(endpointInfo).when(translator).getEndpointInfo(any(MethodModel.class), any(String.class));

		// call under test.
		translator.insertOperationAndEndpointInfo(operationToEndpointInfo, method, displayName);
		Mockito.verify(translator).getEndpointInfo(method, displayName);
		assertTrue(operationToEndpointInfo.containsKey("get"));
		assertEquals(endpointInfo, operationToEndpointInfo.get("get"));
	}

	@Test
	public void testInsertOperationAndEndpointInfoWithNullDisplayName() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.insertOperationAndEndpointInfo(new HashMap<>(), new MethodModel(), null);
		});
		assertEquals("displayName is required.", exception.getMessage());
	}

	@Test
	public void testInsertOperationAndEndpointInfoWithNullMethod() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.insertOperationAndEndpointInfo(new HashMap<>(), null, "");
		});
		assertEquals("method is required.", exception.getMessage());
	}

	@Test
	public void testInsertOperationAndEndpointInfoWithNullOperationToEndpoints() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.insertOperationAndEndpointInfo(null, new MethodModel(), "");
		});
		assertEquals("operationToEndpoint is required.", exception.getMessage());
	}

	@Test
	public void testGetEndpointInfo() {
		String methodName = "METHOD_NAME";
		String displayName = "DISPLAY_NAME";
		List<ParameterModel> parameters = new ArrayList<>();
		RequestBodyModel requestBodyModel = new RequestBodyModel();
		ResponseModel responses = new ResponseModel();
		List<String> tags = new ArrayList<>(Arrays.asList(displayName));
		MethodModel method = new MethodModel().withName(methodName).withRequestBody(requestBodyModel)
				.withParameters(parameters).withResponse(responses);

		List<ParameterInfo> expectedParameters = new ArrayList<>();
		RequestBodyInfo requestBodyInfo = new RequestBodyInfo();
		Map<String, ResponseInfo> respones = new LinkedHashMap<>();
		Mockito.doReturn(expectedParameters).when(translator).getParameters(any(List.class));
		Mockito.doReturn(requestBodyInfo).when(translator).getRequestBodyInfo(any(RequestBodyModel.class));
		Mockito.doReturn(respones).when(translator).getResponses(any(ResponseModel.class));

		EndpointInfo expectedEndpointInfo = new EndpointInfo().withTags(tags).withOperationId(methodName)
				.withParameters(expectedParameters).withRequestBody(requestBodyInfo).withResponses(respones);

		// call under test.
		assertEquals(expectedEndpointInfo, translator.getEndpointInfo(method, displayName));
		Mockito.verify(translator).getParameters(parameters);
		Mockito.verify(translator).getRequestBodyInfo(requestBodyModel);
		Mockito.verify(translator).getResponses(responses);
	}
	
	@Test
	public void testGetEndpointInfoWithNullRequestBodyModel() {
		String methodName = "METHOD_NAME";
		String displayName = "DISPLAY_NAME";
		List<ParameterModel> parameters = new ArrayList<>();
		RequestBodyModel requestBodyModel = null;
		ResponseModel responses = new ResponseModel();
		List<String> tags = new ArrayList<>(Arrays.asList(displayName));
		MethodModel method = new MethodModel().withName(methodName).withRequestBody(requestBodyModel)
				.withParameters(parameters).withResponse(responses);

		List<ParameterInfo> expectedParameters = new ArrayList<>();
		Map<String, ResponseInfo> respones = new LinkedHashMap<>();
		Mockito.doReturn(expectedParameters).when(translator).getParameters(any(List.class));
		Mockito.doReturn(respones).when(translator).getResponses(any(ResponseModel.class));

		EndpointInfo expectedEndpointInfo = new EndpointInfo().withTags(tags).withOperationId(methodName)
				.withParameters(expectedParameters).withRequestBody(null).withResponses(respones);

		// call under test.
		assertEquals(expectedEndpointInfo, translator.getEndpointInfo(method, displayName));
		Mockito.verify(translator).getParameters(parameters);
		Mockito.verify(translator, Mockito.times(0)).getRequestBodyInfo(any());
		Mockito.verify(translator).getResponses(responses);
	}

	@Test
	public void testGetEndpointInfoWithNullDisplayName() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.getEndpointInfo(new MethodModel(), null);
		});
		assertEquals("displayName is required.", exception.getMessage());
	}

	@Test
	public void testGetEndpointInfoWithNullMethod() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.getEndpointInfo(null, "");
		});
		assertEquals("method is required.", exception.getMessage());
	}

	@Test
	public void testGetResponses() {
		JsonSchema js = new JsonSchema();
		js.setType(Type.integer);
		js.setFormat("int32");
		ResponseModel input = new ResponseModel().withDescription(DESCRIPTION).withSchema(js).withStatusCode(200);

		Map<String, ResponseInfo> expectedResponses = new LinkedHashMap<>();
		Map<String, Schema> contentTypeToSchema = new HashMap<>();
		contentTypeToSchema.put("application/json", new Schema().withSchema(js));
		ResponseInfo responseInfo = new ResponseInfo().withDescription(DESCRIPTION).withContent(contentTypeToSchema);
		String statusCode = "200";
		expectedResponses.put(statusCode, responseInfo);

		// call under test.
		assertEquals(expectedResponses, translator.getResponses(input));
	}

	@Test
	public void testGetResponsesWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.getResponses(null);
		});
		assertEquals("response is required.", exception.getMessage());
	}

	@Test
	public void testGetRequestBodyInfo() {
		JsonSchema js = new JsonSchema();
		js.setType(Type.integer);
		js.setFormat("int32");
		RequestBodyModel input = new RequestBodyModel().withDescription(DESCRIPTION).withSchema(js).withRequired(true);

		Map<String, Schema> contentTypeToSchema = new LinkedHashMap<>();
		contentTypeToSchema.put("application/json", new Schema().withSchema(js));
		RequestBodyInfo expected = new RequestBodyInfo().withRequired(true).withContent(contentTypeToSchema);

		// call under test.
		assertEquals(expected, translator.getRequestBodyInfo(input));
	}

	@Test
	public void testGetRequestBodyInfoWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.getRequestBodyInfo(null);
		});
		assertEquals("requestBody is required.", exception.getMessage());
	}

	@Test
	public void testGetParameters() {
		ParameterInfo paramInfo = new ParameterInfo();
		ParameterModel paramModel = new ParameterModel();
		Mockito.doReturn(paramInfo).when(translator).getParameterInfo(any(ParameterModel.class));
		// call under test.
		assertEquals(Arrays.asList(paramInfo), translator.getParameters(Arrays.asList(paramModel)));
		Mockito.verify(translator).getParameterInfo(paramModel);
	}

	@Test
	public void testGetParametersWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.getParameters(null);
		});
		assertEquals("params is required.", exception.getMessage());
	}

	@Test
	public void testGetParameterInfo() {
		ParameterModel input = new ParameterModel().withDescription(DESCRIPTION).withIn(ParameterLocation.query)
				.withRequired(false);
		ParameterInfo expected = new ParameterInfo().withDescription(DESCRIPTION).withIn("query").withRequired(false);

		// call under test.
		assertEquals(expected, translator.getParameterInfo(input));
	}

	@Test
	public void testGetParameterInfoWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.getParameterInfo(null);
		});
		assertEquals("parameter is required.", exception.getMessage());
	}
}
