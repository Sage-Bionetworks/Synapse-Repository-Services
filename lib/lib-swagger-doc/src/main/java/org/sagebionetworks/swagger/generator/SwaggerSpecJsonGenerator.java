package org.sagebionetworks.swagger.generator;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.swagger.datamodel.ApiInfo;
import org.sagebionetworks.swagger.datamodel.ServerInfo;
import org.sagebionetworks.swagger.datamodel.SwaggerSpecModel;
import org.sagebionetworks.swagger.datamodel.pathinfo.EndpointInfo;
import org.sagebionetworks.swagger.datamodel.pathinfo.ParameterInfo;
import org.sagebionetworks.swagger.datamodel.pathinfo.PathInfo;
import org.sagebionetworks.swagger.datamodel.pathinfo.RequestBodyInfo;
import org.sagebionetworks.swagger.datamodel.pathinfo.ResponseInfo;
import org.sagebionetworks.swagger.datamodel.pathinfo.ResponsesInfo;

/**
 * This generator generates a JSONObject based on a SwaggerSpecModel that conforms to the OpenAPI
 * specification standards.
 * 
 * @author lli
 *
 */
public class SwaggerSpecJsonGenerator {
	
	public static JSONObject generateJson(SwaggerSpecModel swaggerSpecModel) {
		JSONObject json = new JSONObject();
		json.put("openapi", swaggerSpecModel.getOpenapiVersion());
		
		JSONObject info = generateApiInfo(swaggerSpecModel.getApiInfo());
		json.put("info", info);
		
		JSONArray servers = generateServers(swaggerSpecModel.getServers());
		json.put("servers", servers);
		
		JSONObject paths = generatePaths(swaggerSpecModel.getPaths());
		json.put("paths", paths);
		
		JSONObject components = generateComponents();
		json.put("components", components);
		
		return json;
	}
	
	private static JSONObject generatePaths(List<PathInfo> paths) {
		JSONObject pathsJson = new JSONObject();
		for (PathInfo path : paths) {
			String endpointPath = path.getPath();
			JSONObject endpointJson = new JSONObject();
			
			Map<String, EndpointInfo> operationToEndpointInfo = path.getOperationToEndpointInfo();
			for (String crudOperation : operationToEndpointInfo.keySet()) {
				JSONObject crudOperationJson = new JSONObject();
				EndpointInfo endpointInfo = operationToEndpointInfo.get(crudOperation);
				crudOperationJson.put("tags", getTags(endpointInfo));
				crudOperationJson.put("operationId", endpointInfo.getOperationID());
				if (!endpointInfo.getParameters().isEmpty()) {
					crudOperationJson.put("parameters", getParameters(endpointInfo));
				}
				if (endpointInfo.getRequestBody() != null) {
					crudOperationJson.put("requestBody", getRequestBody(endpointInfo));
				}
				crudOperationJson.put("responses", getResponse(endpointInfo));
				
				endpointJson.put(crudOperation, crudOperationJson);
			}
			pathsJson.put(endpointPath, endpointJson);
		}
		return pathsJson;
	}
	
	private static JSONObject getRequestBody(EndpointInfo endpointInfo) {
		JSONObject content = new JSONObject();
		
		RequestBodyInfo requestBodyInfo = endpointInfo.getRequestBody();
		Map<String, JSONObject> contentTypeToSchema = requestBodyInfo.getContentTypeToSchema();
		for (String contentType : contentTypeToSchema.keySet()) {
			content.put(contentType, contentTypeToSchema.get(contentType));
		}
		
		JSONObject requestBody = new JSONObject();
		requestBody.put("content", content);
		requestBody.put("required", requestBodyInfo.isRequired());
		
		return requestBody;
	}
	
	private static JSONObject getResponse(EndpointInfo endpointInfo) {
		JSONObject responsesJson = new JSONObject();
		
		ResponsesInfo responsesInfo = endpointInfo.getResponses();
		Map<String, ResponseInfo> statusCodeToResponses = responsesInfo.getStatusCodesAndResponses();
		for (String statusCode : statusCodeToResponses.keySet()) {
			ResponseInfo responseInfo = statusCodeToResponses.get(statusCode);
			
			JSONObject responseJson = new JSONObject();
			responseJson.put("description", responseInfo.getDescription());
			
			JSONObject contentJson = new JSONObject();
			Map<String, JSONObject> contentTypeToResponse = responseInfo.getContentTypeToResponse();
			for (String contentType : contentTypeToResponse.keySet()) {
				contentJson.put(contentType, contentTypeToResponse.get(contentType));
			}
			
			responseJson.put("content", contentJson);
			responsesJson.put(statusCode, responseJson);
		}
		
		return responsesJson;
	}
	
	private static JSONArray getParameters(EndpointInfo endpointInfo) {
		JSONArray parameters = new JSONArray();
		for (ParameterInfo parameter : endpointInfo.getParameters()) {
			JSONObject parameterJson = new JSONObject();
			parameterJson.put("name", parameter.getName());
			parameterJson.put("in", parameter.getIn());
			parameterJson.put("required", parameter.isRequired());
			parameterJson.put("schema", parameter.getSchema());
			
			parameters.put(parameterJson);
		}
		return parameters;
	}
	
	private static JSONArray getTags(EndpointInfo endpointInfo) {
		JSONArray tags = new JSONArray();
		for (String tag : endpointInfo.getTags()) {
			tags.put(tag);
		}
		return tags;
	}
	
	private static JSONArray generateServers(List<ServerInfo> servers) {
		JSONArray serversArr = new JSONArray();
		for (ServerInfo server : servers) {
			JSONObject serverJson = new JSONObject();
			serverJson.put("url", server.getUrl());
			serverJson.put("description", server.getDescription());
			serversArr.put(serverJson);
		}
		return serversArr;
	}
	
	private static JSONObject generateComponents() {
		// Leave as an empty object for now
		return new JSONObject();
	}
	
	private static JSONObject generateApiInfo(ApiInfo apiInfo) {
		JSONObject info = new JSONObject();
		info.put("title", apiInfo.getTitle());
		info.put("version", apiInfo.getVersion());
		return info;
	}
}
