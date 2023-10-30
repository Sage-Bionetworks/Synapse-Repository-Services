package org.sagebionetworks.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
	private static final String MOCK_CLASS_NAME = "MOCK_CLASS_NAME";
	private Map<String, JsonSchema> schemaMap;
	
	@BeforeEach
	private void setUp() {
		Map<String, JsonSchema> schemaMap = new HashMap<>();
		JsonSchema js = new JsonSchema();
		js.setType(Type.integer);
		schemaMap.put(MOCK_CLASS_NAME, js);
		this.schemaMap = schemaMap;

		this.translator = spy(new ControllerModelsToOpenAPIModelTranslator(schemaMap));
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
		Map<String, Map<String, JsonSchema>> components = new LinkedHashMap<>();

		doNothing().when(translator).insertPaths(any(List.class), any(String.class), any(String.class),
				any(Map.class));
		doReturn(apiInfo).when(translator).getApiInfo();
		doReturn(servers).when(translator).getServers();
		doReturn(components).when(translator).getComponents();

		OpenAPISpecModel result = translator.translate(Arrays.asList(controllerModel));
		List<TagInfo> tags = new ArrayList<>();
		tags.add(new TagInfo().withDescription(DESCRIPTION).withName(displayName));
		OpenAPISpecModel expected = new OpenAPISpecModel().withInfo(apiInfo).withOpenapi("3.0.1").withServers(servers)
				.withComponents(components).withPaths(new LinkedHashMap<>()).withTags(tags);
		assertEquals(expected, result);

		verify(translator).insertPaths(methods, basePath, displayName, result.getPaths());
		verify(translator).getApiInfo();
		verify(translator).getServers();
		verify(translator).getComponents();
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
	public void testGetComponents() {
		Map<String, Map<String, JsonSchema>> expectedComponents = new LinkedHashMap<>();
		expectedComponents.put("schemas", schemaMap);
		
		// call under test
		assertEquals(expectedComponents, translator.getComponents());
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
		doReturn(endpointInfo).when(translator).getEndpointInfo(any(MethodModel.class), any(String.class), any(String.class));

		// call under test.
		translator.insertPaths(methods, basePath, displayName, paths);

		Map<String, Map<String, EndpointInfo>> expectedPaths = new LinkedHashMap<>();
		Map<String, EndpointInfo> operationToEndpoint = new LinkedHashMap<>();
		operationToEndpoint.put("get", endpointInfo);
		expectedPaths.put(basePath + methodPath, operationToEndpoint);

		assertEquals(expectedPaths, paths);
		verify(translator).getEndpointInfo(method, displayName, basePath + methodPath);
	}
	
	@Test
	public void testInsertPathsWithoutStartingForwardSlash() {
		List<MethodModel> methods = new ArrayList<>();
		// missing "/" at the start of the base path
		String basePath = "BASE_PATH";
		String displayName = "DISPLAY_NAME";
		Map<String, Map<String, EndpointInfo>> paths = new LinkedHashMap<>();

		String methodPath = "/METHOD_PATH";
		String observedMethodPath = "\"" + methodPath + "\"";
		MethodModel method = new MethodModel().withPath(observedMethodPath).withOperation(Operation.get);
		methods.add(method);

		EndpointInfo endpointInfo = new EndpointInfo();
		doReturn(endpointInfo).when(translator).getEndpointInfo(any(MethodModel.class), any(String.class), any(String.class));

		// call under test.
		translator.insertPaths(methods, basePath, displayName, paths);

		Map<String, Map<String, EndpointInfo>> expectedPaths = new LinkedHashMap<>();
		Map<String, EndpointInfo> operationToEndpoint = new LinkedHashMap<>();
		operationToEndpoint.put("get", endpointInfo);
		// "/" should be added automatically
		String fullPath = "/" + basePath + methodPath;
		expectedPaths.put(fullPath, operationToEndpoint);

		assertEquals(expectedPaths, paths);
		verify(translator).getEndpointInfo(method, displayName, fullPath);
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
		String fullPath = "/test/path";
		EndpointInfo endpointInfo = new EndpointInfo();
		doReturn(endpointInfo).when(translator).getEndpointInfo(any(MethodModel.class), any(String.class), any(String.class));
		translator.insertOperationAndEndpointInfo(operationToEndpointInfo, method1, displayName, fullPath);

		MethodModel method2 = new MethodModel().withOperation(Operation.get);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.insertOperationAndEndpointInfo(operationToEndpointInfo, method2, displayName, fullPath);
		});
		assertEquals("OperationToEndpoint already contains operation get", exception.getMessage());
		verify(translator).getEndpointInfo(method1, displayName, fullPath);
	}

	@Test
	public void testInsertOperationAndEndpointInfo() {
		Map<String, EndpointInfo> operationToEndpointInfo = new LinkedHashMap<>();
		MethodModel method = new MethodModel().withOperation(Operation.get);
		String displayName = "DISPLAY_NAME";
		String fullPath = "/test/path";
		EndpointInfo endpointInfo = new EndpointInfo();
		doReturn(endpointInfo).when(translator).getEndpointInfo(any(MethodModel.class), any(String.class), any(String.class));

		// call under test.
		translator.insertOperationAndEndpointInfo(operationToEndpointInfo, method, displayName, fullPath);
		verify(translator).getEndpointInfo(method, displayName, fullPath);
		assertTrue(operationToEndpointInfo.containsKey("get"));
		assertEquals(endpointInfo, operationToEndpointInfo.get("get"));
	}

	@Test
	public void testInsertOperationAndEndpointInfoWithNullDisplayName() {
		String fullPath = "/test/path";
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.insertOperationAndEndpointInfo(new HashMap<>(), new MethodModel(), null, fullPath);
		});
		assertEquals("displayName is required.", exception.getMessage());
	}

	@Test
	public void testInsertOperationAndEndpointInfoWithNullMethod() {
		String fullPath = "/test/path";
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.insertOperationAndEndpointInfo(new HashMap<>(), null, "", fullPath);
		});
		assertEquals("method is required.", exception.getMessage());
	}

	@Test
	public void testInsertOperationAndEndpointInfoWithNullOperationToEndpoints() {
		String fullPath = "/test/path";
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.insertOperationAndEndpointInfo(null, new MethodModel(), "", fullPath);
		});
		assertEquals("operationToEndpoint is required.", exception.getMessage());
	}

	@Test
	public void testInsertOperationAndEndpointInfoWithNullFullPath() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.insertOperationAndEndpointInfo(new HashMap<>(), new MethodModel(), "", null);
		});
		assertEquals("fullPath is required.", exception.getMessage());
	}

	@Test
	public void testGetEndpointInfo() {
		String methodName = "METHOD_NAME";
		String displayName = "DISPLAY_NAME";
		String fullPath = "/test/path";
		List<ParameterModel> parameters = new ArrayList<>();
		RequestBodyModel requestBodyModel = new RequestBodyModel();
		ResponseModel responses = new ResponseModel();
		List<String> tags = new ArrayList<>(Arrays.asList(displayName));
		MethodModel method = new MethodModel().withName(methodName).withRequestBody(requestBodyModel)
				.withParameters(parameters).withResponse(responses);

		List<ParameterInfo> expectedParameters = new ArrayList<>();
		RequestBodyInfo requestBodyInfo = new RequestBodyInfo();
		Map<String, ResponseInfo> respones = new LinkedHashMap<>();
		doReturn(expectedParameters).when(translator).getParameters(any(List.class));
		doReturn(requestBodyInfo).when(translator).getRequestBodyInfo(any(RequestBodyModel.class));
		doReturn(respones).when(translator).getResponses(any(ResponseModel.class));

		EndpointInfo expectedEndpointInfo = new EndpointInfo().withTags(tags).withOperationId(fullPath)
				.withParameters(expectedParameters).withRequestBody(requestBodyInfo).withResponses(respones);

		// call under test.
		assertEquals(expectedEndpointInfo, translator.getEndpointInfo(method, displayName, fullPath));
		verify(translator).getParameters(parameters);
		verify(translator).getRequestBodyInfo(requestBodyModel);
		verify(translator).getResponses(responses);
	}
	
	@Test
	public void testGetEndpointInfoWithNullRequestBodyModel() {
		String methodName = "METHOD_NAME";
		String displayName = "DISPLAY_NAME";
		String fullPath = "/test/path";
		List<ParameterModel> parameters = new ArrayList<>();
		RequestBodyModel requestBodyModel = null;
		ResponseModel responses = new ResponseModel();
		List<String> tags = new ArrayList<>(Arrays.asList(displayName));
		MethodModel method = new MethodModel().withName(methodName).withRequestBody(requestBodyModel)
				.withParameters(parameters).withResponse(responses);

		List<ParameterInfo> expectedParameters = new ArrayList<>();
		Map<String, ResponseInfo> respones = new LinkedHashMap<>();
		doReturn(expectedParameters).when(translator).getParameters(any(List.class));
		doReturn(respones).when(translator).getResponses(any(ResponseModel.class));

		EndpointInfo expectedEndpointInfo = new EndpointInfo().withTags(tags).withOperationId(fullPath)
				.withParameters(expectedParameters).withRequestBody(null).withResponses(respones);

		// call under test.
		assertEquals(expectedEndpointInfo, translator.getEndpointInfo(method, displayName, fullPath));
		verify(translator).getParameters(parameters);
		verify(translator, times(0)).getRequestBodyInfo(any());
		verify(translator).getResponses(responses);
	}

	@Test
	public void testGetEndpointInfoWithNullDisplayName() {
		String fullPath = "/test/path";
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.getEndpointInfo(new MethodModel(), null, fullPath);
		});
		assertEquals("displayName is required.", exception.getMessage());
	}

	@Test
	public void testGetEndpointInfoWithNullMethod() {
		String fullPath = "/test/path";
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.getEndpointInfo(null, "", fullPath);
		});
		assertEquals("method is required.", exception.getMessage());
	}

	@Test
	public void testGetEndpointInfoWithNullFullPath() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			translator.getEndpointInfo(new MethodModel(), "", null);
		});
		assertEquals("fullPath is required.", exception.getMessage());
	}
	
	@Test
	public void testGetResponsesWithRedirectedEndpoint() {
		ResponseModel input = new ResponseModel().withIsRedirected(true);
		Map<String, ResponseInfo> responses = new HashMap<>();
		doReturn(responses).when(translator).generateResponsesForRedirectedEndpoint();
		
		// call under test
		assertEquals(responses, translator.getResponses(input));
		verify(translator).generateResponsesForRedirectedEndpoint();
	}

	@Test
	public void testGetResponses() {
		// should use id here instead
		ResponseModel input = new ResponseModel().withDescription(DESCRIPTION).withId(MOCK_CLASS_NAME).withStatusCode(200);

		Map<String, ResponseInfo> expectedResponses = new LinkedHashMap<>();
		Map<String, Schema> contentTypeToSchema = new HashMap<>();
		contentTypeToSchema.put("application/json", new Schema().withSchema(translator.getReferenceSchema(MOCK_CLASS_NAME)));
		ResponseInfo responseInfo = new ResponseInfo().withDescription(DESCRIPTION).withContent(contentTypeToSchema);
		String statusCode = "200";
		expectedResponses.put(statusCode, responseInfo);

		// call under test.
		assertEquals(expectedResponses, translator.getResponses(input));
		verify(translator, times(2)).getReferenceSchema(MOCK_CLASS_NAME);
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
	public void testGetResponsesWithVoidMethodReturnType() {
		ResponseModel input = new ResponseModel().withDescription(DESCRIPTION).withId("void").withStatusCode(200);

		Map<String, ResponseInfo> expectedResponses = new LinkedHashMap<>();
		ResponseInfo responseInfo = new ResponseInfo().withDescription(DESCRIPTION);
		String statusCode = "200";
		expectedResponses.put(statusCode, responseInfo);

		// call under test.
		assertEquals(expectedResponses, translator.getResponses(input));
		verify(translator, times(0)).getReferenceSchema(MOCK_CLASS_NAME);
	}
	
	@Test
	public void testGenerateResponsesForRedirectedEndpoint() {
		Map<String, ResponseInfo> expectedResponses = new LinkedHashMap<>();
		
		String statusCodeRedirected = "307";
		String statusCodeOk = "200";

		Map<String, Schema> statusCodeOkContentTypeToSchema = new HashMap<>();
		statusCodeOkContentTypeToSchema.put("text/plain", new Schema().withSchema(new JsonSchema()));
		ResponseInfo responseOk = new ResponseInfo().withDescription("Status 200 will be returned if the 'redirect' boolean param is false").withContent(statusCodeOkContentTypeToSchema);
		expectedResponses.put(statusCodeOk, responseOk);
		
		Map<String, Schema> statusCodeRedirectedContentTypeToSchema = new HashMap<>();
		ResponseInfo responseRedirected = new ResponseInfo().withDescription("Status 307 will be returned if the 'redirect' boolean param is true or null").withContent(statusCodeRedirectedContentTypeToSchema);
		expectedResponses.put(statusCodeRedirected, responseRedirected);
		
		// call under test
		assertEquals(expectedResponses, translator.generateResponsesForRedirectedEndpoint());
	}

	@Test
	public void testGetRequestBodyInfo() {
		RequestBodyModel input = new RequestBodyModel().withDescription(DESCRIPTION).withId(MOCK_CLASS_NAME).withRequired(true);

		Map<String, Schema> contentTypeToSchema = new LinkedHashMap<>();
		contentTypeToSchema.put("application/json", new Schema().withSchema(translator.getReferenceSchema(MOCK_CLASS_NAME)));
		RequestBodyInfo expected = new RequestBodyInfo().withRequired(true).withContent(contentTypeToSchema);

		// call under test.
		assertEquals(expected, translator.getRequestBodyInfo(input));
		verify(translator, times(2)).getReferenceSchema(MOCK_CLASS_NAME);
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
		doReturn(paramInfo).when(translator).getParameterInfo(any(ParameterModel.class));
		// call under test.
		assertEquals(Arrays.asList(paramInfo), translator.getParameters(Arrays.asList(paramModel)));
		verify(translator).getParameterInfo(paramModel);
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
				.withRequired(false).withId(MOCK_CLASS_NAME);
		ParameterInfo expected = new ParameterInfo().withDescription(DESCRIPTION).withIn("query").withRequired(false)
				.withSchema(translator.getReferenceSchema(MOCK_CLASS_NAME));

		// call under test.
		assertEquals(expected, translator.getParameterInfo(input));
		verify(translator, times(2)).getReferenceSchema(MOCK_CLASS_NAME);
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
