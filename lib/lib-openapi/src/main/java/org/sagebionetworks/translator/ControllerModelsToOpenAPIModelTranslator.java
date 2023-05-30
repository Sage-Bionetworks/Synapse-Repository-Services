package org.sagebionetworks.translator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.controller.model.ControllerModel;
import org.sagebionetworks.controller.model.MethodModel;
import org.sagebionetworks.controller.model.ParameterModel;
import org.sagebionetworks.controller.model.RequestBodyModel;
import org.sagebionetworks.controller.model.ResponseModel;
import org.sagebionetworks.openapi.datamodel.ApiInfo;
import org.sagebionetworks.openapi.datamodel.ComponentInfo;
import org.sagebionetworks.openapi.datamodel.OpenAPISpecModel;
import org.sagebionetworks.openapi.datamodel.ServerInfo;
import org.sagebionetworks.openapi.datamodel.TagInfo;
import org.sagebionetworks.openapi.datamodel.pathinfo.EndpointInfo;
import org.sagebionetworks.openapi.datamodel.pathinfo.ParameterInfo;
import org.sagebionetworks.openapi.datamodel.pathinfo.RequestBodyInfo;
import org.sagebionetworks.openapi.datamodel.pathinfo.ResponseInfo;
import org.sagebionetworks.openapi.datamodel.pathinfo.Schema;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.util.ValidateArgument;

public class ControllerModelsToOpenAPIModelTranslator {
	private Map<String, JsonSchema> classNameToJsonSchema;
	
	public ControllerModelsToOpenAPIModelTranslator(Map<String, JsonSchema> classNameToJsonSchema) {
		this.classNameToJsonSchema = classNameToJsonSchema;
	}
	
	/**
	 * Translates a list of controller models to an OpenAPI model.
	 * 
	 * @param controllerModels - the list of controller models to be translated
	 * @return the resulting OpenAPI model.
	 */
	public OpenAPISpecModel translate(List<ControllerModel> controllerModels) {
		ValidateArgument.required(controllerModels, "controllerModels");
		List<TagInfo> tags = new ArrayList<>();
		Map<String, Map<String, EndpointInfo>> paths = new LinkedHashMap<>();
		for (ControllerModel controllerModel : controllerModels) {
			String displayName = controllerModel.getDisplayName();
			String basePath = controllerModel.getPath();
			String description = controllerModel.getDescription();
			List<MethodModel> methods = controllerModel.getMethods();
			insertPaths(methods, basePath, displayName, paths);
			tags.add(new TagInfo().withDescription(description).withName(displayName));
		}
		return new OpenAPISpecModel().withInfo(getApiInfo()).withOpenapi("3.0.1").withServers(getServers())
				.withComponents(null).withPaths(paths).withTags(tags);
	}

	/**
	 * Get the API information, such as the title and the version.
	 * 
	 * @return an object that represents the API information
	 */
	ApiInfo getApiInfo() {
		return new ApiInfo().withTitle("Sample OpenAPI definition").withVersion("v1");
	}

	/**
	 * Get server information, such as URLs and description.
	 * 
	 * @return a list of objects that represents information on the servers.
	 */
	List<ServerInfo> getServers() {
		ServerInfo server = new ServerInfo().withUrl("https://repo-prod.prod.sagebase.org")
				.withDescription("This is the generated server URL");
		return new ArrayList<>(Arrays.asList(server));
	}

	/**
	 * Inserts the paths from the given methods into the "paths" map
	 * 
	 * @param methods     - the methods whose paths are to be inserted
	 * @param basePath    - the base path of the controller
	 * @param displayName - the display name of the controller
	 * @param paths       - the map which we are inserting paths into.
	 */
	void insertPaths(List<MethodModel> methods, String basePath, String displayName,
			Map<String, Map<String, EndpointInfo>> paths) {
		ValidateArgument.required(methods, "methods");
		ValidateArgument.required(basePath, "basePath");
		ValidateArgument.required(displayName, "displayName");
		ValidateArgument.required(paths, "paths");
		for (MethodModel method : methods) {
			String methodPath = method.getPath();
			// trim off the starting and ending quotation marks found in the path.
			methodPath = methodPath.substring(1, methodPath.length() - 1);
			String fullPath = basePath + methodPath;
			paths.putIfAbsent(fullPath, new LinkedHashMap<>());
			insertOperationAndEndpointInfo(paths.get(fullPath), method, displayName);
		}
	}

	/**
	 * Insert an operation and its corresponding endpoint information into the map.
	 * 
	 * @param operationToEndpoint - the map to which we are inserting these values
	 * @param method              - the method being looked at
	 * @param displayName         - the display name of the controller in which this
	 *                            method resides.
	 */
	void insertOperationAndEndpointInfo(Map<String, EndpointInfo> operationToEndpoint, MethodModel method,
			String displayName) {
		ValidateArgument.required(operationToEndpoint, "operationToEndpoint");
		ValidateArgument.required(method, "method");
		ValidateArgument.required(displayName, "displayName");
		String operation = method.getOperation().toString();
		if (operationToEndpoint.containsKey(operation)) {
			throw new IllegalArgumentException("OperationToEndpoint already contains operation " + operation);
		}
		operationToEndpoint.put(operation, getEndpointInfo(method, displayName));
	}

	/**
	 * Get a object that represents the endpoint information from the method being
	 * looked at.
	 * 
	 * @param method      - the method being looked at
	 * @param displayName - the name of the controller where this method resides.
	 * @return an object that represents the endpoint of the method.
	 */
	EndpointInfo getEndpointInfo(MethodModel method, String displayName) {
		ValidateArgument.required(method, "method");
		ValidateArgument.required(displayName, "displayName");
		List<String> tags = new ArrayList<>(Arrays.asList(displayName));
		String operationId = method.getName();
		EndpointInfo endpointInfo = new EndpointInfo().withTags(tags).withOperationId(operationId)
				.withParameters(getParameters(method.getParameters()))
				.withRequestBody(method.getRequestBody() == null ? null : getRequestBodyInfo(method.getRequestBody()))
				.withResponses(getResponses(method.getResponse()));
		return endpointInfo;
	}

	/**
	 * Constructs and object that represents the responses of a method.
	 * 
	 * @param response - a model that represents the response of a method.
	 * @return a map whose keys represent the status code and values are objects
	 *         that describe the response.
	 */
	Map<String, ResponseInfo> getResponses(ResponseModel response) {
		ValidateArgument.required(response, "response");
		Map<String, ResponseInfo> responses = new LinkedHashMap<>();
		Map<String, Schema> contentTypeToSchema = new HashMap<>();
		contentTypeToSchema.put(response.getContentType(), new Schema().withSchema(classNameToJsonSchema.get(response.getId())));
		ResponseInfo responseInfo = new ResponseInfo().withDescription(response.getDescription())
				.withContent(contentTypeToSchema);

		String statusCode = "" + response.getStatusCode();
		responses.put(statusCode, responseInfo);
		return responses;
	}

	/**
	 * Construct a model that represents the Request Body for the OpenAPI model.
	 * 
	 * @param requestBody - the request body representation from the ControllerModel
	 * @return a model that represents the request body
	 */
	RequestBodyInfo getRequestBodyInfo(RequestBodyModel requestBody) {
		ValidateArgument.required(requestBody, "requestBody");
		String contentType = "application/json";
		Map<String, Schema> contentTypeToSchema = new LinkedHashMap<>();
		contentTypeToSchema.put(contentType, new Schema().withSchema(classNameToJsonSchema.get(requestBody.getId())));
		return new RequestBodyInfo().withRequired(requestBody.isRequired()).withContent(contentTypeToSchema);
	}

	/**
	 * Constructs a list of objects that represents the parameters of the method.
	 * 
	 * @param method - the method being looked at.
	 * @return a list that represents the parameters of the method/endpoint.
	 */
	List<ParameterInfo> getParameters(List<ParameterModel> params) {
		ValidateArgument.required(params, "params");
		List<ParameterInfo> parameters = new ArrayList<>();
		for (ParameterModel parameter : params) {
			parameters.add(getParameterInfo(parameter));
		}
		return parameters;
	}

	/**
	 * Converts the ControllerModel way of representing a parameter to the OpenAPI
	 * model's way.
	 * 
	 * @param parameter - the parameter being looked at.
	 * @return a model that represents the parameter.
	 */
	ParameterInfo getParameterInfo(ParameterModel parameter) {
		ValidateArgument.required(parameter, "parameter");
		ParameterInfo parameterInfo = new ParameterInfo();
		parameterInfo.withName(parameter.getName()).withDescription(parameter.getDescription())
				.withRequired(parameter.isRequired()).withIn(parameter.getIn().toString())
				.withSchema(classNameToJsonSchema.get(parameter.getId()));
		return parameterInfo;
	}
}
