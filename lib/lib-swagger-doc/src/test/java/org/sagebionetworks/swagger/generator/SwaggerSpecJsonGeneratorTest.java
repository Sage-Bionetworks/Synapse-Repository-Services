package org.sagebionetworks.swagger.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.swagger.datamodel.ApiInfo;
import org.sagebionetworks.swagger.datamodel.ServerInfo;
import org.sagebionetworks.swagger.datamodel.SwaggerSpecModel;
import org.sagebionetworks.swagger.datamodel.pathinfo.EndpointInfo;
import org.sagebionetworks.swagger.datamodel.pathinfo.ParameterInfo;
import org.sagebionetworks.swagger.datamodel.pathinfo.PathInfo;
import org.sagebionetworks.swagger.datamodel.pathinfo.RequestBodyInfo;
import org.sagebionetworks.swagger.datamodel.pathinfo.ResponseInfo;
import org.sagebionetworks.swagger.datamodel.pathinfo.ResponsesInfo;
import org.sagebionetworks.swagger.generator.SwaggerSpecJsonGenerator;

public class SwaggerSpecJsonGeneratorTest {
	
	private static JSONObject expectedBasicExampleControllerJson;
	private static JSONObject generatedBasicExampleControllerJson;
	
	@BeforeAll
	public static void setup() throws Exception {
        InputStream is = SwaggerSpecJsonGeneratorTest.class.getClassLoader().getResourceAsStream("BasicExampleControllerSwaggerSpec.json");
        assertNotNull(is);
        String jsonTxt = IOUtils.toString(is, "UTF-8");
        expectedBasicExampleControllerJson = new JSONObject(jsonTxt);
        assertNotNull(expectedBasicExampleControllerJson);
        
        SwaggerSpecModel swaggerSpecModel = generateBasicSwaggerSpecModel();
        SwaggerSpecJsonGenerator swaggerSpecJsonGenerator = new SwaggerSpecJsonGenerator(swaggerSpecModel);
        generatedBasicExampleControllerJson = swaggerSpecJsonGenerator.generateJson();
        assertNotNull(generatedBasicExampleControllerJson);
    }
	
	private static SwaggerSpecModel generateBasicSwaggerSpecModel() {
		String springVersion = "3.0.1";
		
		String apiTitle = "OpenAPI definition";
		String apiVersion = "v0";
		ApiInfo apiInfo = new ApiInfo(apiTitle, apiVersion);
		SwaggerSpecModel swaggerSpecModel = new SwaggerSpecModel(springVersion, apiInfo);
		
		ServerInfo serverInfo = new ServerInfo("http://localhost:8080", "Generated server url");
		swaggerSpecModel.addServer(serverInfo);
		
		addPathsToSwaggerModel(swaggerSpecModel);
		return swaggerSpecModel;
	}
	
	private static void addPathsToSwaggerModel(SwaggerSpecModel swaggerSpecModel) {
		PathInfo path1 = generatePath1Info();
		swaggerSpecModel.addPath(path1);
		
		PathInfo path2 = generatePath2Info();
		swaggerSpecModel.addPath(path2);
	}
	
	private static PathInfo generatePath1Info() {
		PathInfo path = new PathInfo("/person/{name}");
		List<String> tags = new ArrayList<>(Arrays.asList("basic-example-controller"));
		String operationId = "addPerson";
		
		List<ParameterInfo> parameters = new ArrayList<>();
		JSONObject parameterSchema = new JSONObject();
		parameterSchema.put("type", "string");
		parameters.add(new ParameterInfo("name", "path", parameterSchema));
		
		RequestBodyInfo requestBody = new RequestBodyInfo();
		requestBody.setIsRequired(true);
		JSONObject requestBodyJson = new JSONObject();
		requestBodyJson.put("type", "integer");
		requestBodyJson.put("format", "int32");
		JSONObject requestBodySchema = new JSONObject();
		requestBodySchema.put("schema", requestBodyJson);
		requestBody.addContentTypeAndSchema("application/json", requestBodySchema);
		
		ResponsesInfo responses = new ResponsesInfo();
		JSONObject responseInfoJson = new JSONObject();
		responseInfoJson.put("type", "string");
		JSONObject responseInfoSchema = new JSONObject();
		responseInfoSchema.put("schema", responseInfoJson);
		ResponseInfo responseInfo = new ResponseInfo("OK");
		responseInfo.addContentTypeAndResponse("*/*", responseInfoSchema);
		responses.addStatusCodeWithResponse("200", responseInfo);
		
		path.addEndpointInfo("post" ,new EndpointInfo(tags, operationId, parameters, requestBody, responses));
		return path;
	}
	
	private static PathInfo generatePath2Info() {
		PathInfo path = new PathInfo("/person/age/{name}");
		List<String> tags = new ArrayList<>(Arrays.asList("basic-example-controller"));
		String operationId = "getPersonAge";
		
		List<ParameterInfo> parameters = new ArrayList<>();
		JSONObject parameterSchema = new JSONObject();
		parameterSchema.put("type", "string");
		parameters.add(new ParameterInfo("name", "path", parameterSchema));
		
		ResponsesInfo responses = new ResponsesInfo();
		JSONObject responseInfoJson = new JSONObject();
		responseInfoJson.put("type", "integer");
		responseInfoJson.put("format", "int32");
		JSONObject responseInfoSchema = new JSONObject();
		responseInfoSchema.put("schema", responseInfoJson);
		ResponseInfo responseInfo = new ResponseInfo("OK");
		responseInfo.addContentTypeAndResponse("*/*", responseInfoSchema);
		responses.addStatusCodeWithResponse("200", responseInfo);
		
		path.addEndpointInfo("get" ,new EndpointInfo(tags, operationId, parameters, null, responses));
		return path;
	}
	
	@Test
	public void testOpenApiVersionsMatch() {
		assertEquals(expectedBasicExampleControllerJson.get("openapi"), generatedBasicExampleControllerJson.get("openapi"));
	}
	
	@Test
	public void testApiInfoMatch() {
		assertEquals(expectedBasicExampleControllerJson.get("info").toString(),
				generatedBasicExampleControllerJson.get("info").toString());
	}

	@Test
	public void testServersMatch() {
		assertEquals(expectedBasicExampleControllerJson.get("servers").toString(),
				generatedBasicExampleControllerJson.get("servers").toString());
	}

	@Test
	public void testPathsMatch() {
		assertEquals(expectedBasicExampleControllerJson.get("paths").toString(),
				generatedBasicExampleControllerJson.get("paths").toString());
	}

	@Test
	public void testComponentsMatch() {
		assertEquals(expectedBasicExampleControllerJson.get("components").toString(),
				generatedBasicExampleControllerJson.get("components").toString());
	}
	
}
