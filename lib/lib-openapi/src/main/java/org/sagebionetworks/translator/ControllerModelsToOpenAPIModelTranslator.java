package org.sagebionetworks.translator;

import java.util.*;

import jdk.javadoc.doclet.Reporter;
import org.sagebionetworks.controller.model.ControllerModel;
import org.sagebionetworks.controller.model.MethodModel;
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
import org.sagebionetworks.util.ValidateArgument;

import javax.tools.Diagnostic;

public class ControllerModelsToOpenAPIModelTranslator {
	public static final Map<String, Type> ResponsePrimitives = Map.of(
			"java.lang.String", Type.string,
			"java.lang.Long", Type.number,
			"java.lang.Integer", Type.number,
			"java.lang.Boolean", Type._boolean,
			"org.sagebionetworks.repo.model.BooleanResult", Type._boolean,
			"org.json.JSONObject", Type.object,
			"java.lang.Object", Type.object,
			// TODO: This is a JSON Schema
			"org.sagebionetworks.schema.ObjectSchema", Type.object);
	private final Map<String, JsonSchema> classNameToJsonSchema;

	public ControllerModelsToOpenAPIModelTranslator(Map<String, JsonSchema> classNameToJsonSchema) {
		this.classNameToJsonSchema = classNameToJsonSchema;
	}

	/**
	 * Translates a list of controller models to an OpenAPI model.
	 *
	 * @param controllerModels - the list of controller models to be translated
	 * @return the resulting OpenAPI model.
	 */
	public OpenAPISpecModel translate(List<ControllerModel> controllerModels, Reporter reporter) {
		ValidateArgument.required(controllerModels, "controllerModels");
		List<TagInfo> tags = new ArrayList<>();
		Map<String, Map<String, EndpointInfo>> paths = new LinkedHashMap<>();
		for (ControllerModel controllerModel : controllerModels) {
			reporter.print(Diagnostic.Kind.NOTE, "Generating OpenAPI spec for controller " + controllerModel.getDisplayName());
			String displayName = controllerModel.getDisplayName();
			String basePath = controllerModel.getPath();
			String description = controllerModel.getDescription();
			List<MethodModel> methods = controllerModel.getMethods();
			insertPaths(methods, basePath, displayName, paths, reporter);
			tags.add(new TagInfo().withDescription(description).withName(displayName));
		}
		return new OpenAPISpecModel().withInfo(getApiInfo()).withOpenapi("3.0.1").withServers(getServers())
				.withComponents(getComponents()).withPaths(paths).withTags(tags);
	}

	/**
	 * Generates and returns the components section of the OpenAPI specification.
	 *
	 * @return a nested map, from component_type (schemas, parameters) -> { class_name -> JsonSchema}.
	 */
	Map<String, Map<String, JsonSchema>> getComponents() {
		Map<String, Map<String, JsonSchema>> components = new LinkedHashMap<>();
		components.put("schemas", classNameToJsonSchema);
		return components;
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
					 Map<String, Map<String, EndpointInfo>> paths, Reporter reporter) {
		ValidateArgument.required(methods, "methods");
		ValidateArgument.required(basePath, "basePath");
		ValidateArgument.required(displayName, "displayName");
		ValidateArgument.required(paths, "paths");
		for (MethodModel method : methods) {
			String methodPath = method.getPath();
			// trim off the starting and ending quotation marks found in the path.
			methodPath = methodPath.substring(1, methodPath.length() - 1);
			String fullPath = basePath + methodPath;
			// make sure the fullPath starts with a "/"
			if (fullPath.charAt(0) != '/') {
				fullPath = "/" + fullPath;
			}
			paths.putIfAbsent(fullPath, new LinkedHashMap<>());
			insertOperationAndEndpointInfo(paths.get(fullPath), method, displayName, reporter);
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
										String displayName, Reporter reporter) {
		ValidateArgument.required(operationToEndpoint, "operationToEndpoint");
		ValidateArgument.required(method, "method");
		ValidateArgument.required(displayName, "displayName");
		String operation = method.getOperation().toString();
		if (operationToEndpoint.containsKey(operation)) {
			throw new IllegalArgumentException("OperationToEndpoint already contains operation " + operation);
		}
		operationToEndpoint.put(operation, getEndpointInfo(method, displayName, reporter));
	}

	/**
	 * Get a object that represents the endpoint information from the method being
	 * looked at.
	 *
	 * @param method      - the method being looked at
	 * @param displayName - the name of the controller where this method resides.
	 * @return an object that represents the endpoint of the method.
	 */
	EndpointInfo getEndpointInfo(MethodModel method, String displayName, Reporter reporter) {
		ValidateArgument.required(method, "method");
		ValidateArgument.required(displayName, "displayName");
		List<String> tags = new ArrayList<>(Arrays.asList(displayName));
		String operationId = method.getName();
		EndpointInfo endpointInfo = new EndpointInfo().withTags(tags).withOperationId(operationId)
				.withParameters(getParameters(method.getParameters()))
				.withRequestBody(method.getRequestBody() == null ? null : getRequestBodyInfo(method.getRequestBody()))
				.withResponses(getResponses(method.getResponse(), reporter));
		return endpointInfo;
	}

	/**
	 * Generates a JsonSchema that is a reference to a class defined in
	 * the "components" section of the specification with class name of "id"
	 *
	 * @param id the id of the class
	 * @return JsonSchema that is a reference to class in "components"
	 */
	JsonSchema getReferenceSchema(String id) {
		ValidateArgument.required(id, "id");
		JsonSchema schema = new JsonSchema();

		if (classMapsToPrimitiveType(id)) {
			schema.setType(ResponsePrimitives.get(id));
		} else if (classIsGenericType(id)) {
			// TODO: handle generic types
			schema.setType(Type.object);
		} else {
			schema.set$ref("#/components/schemas/" + id);
		}
		return schema;
	}

	/**
	 * Constructs and object that represents the responses of a method.
	 *
	 * @param response - a model that represents the response of a method.
	 * @return a map whose keys represent the status code and values are objects
	 * that describe the response.
	 */
	Map<String, ResponseInfo> getResponses(ResponseModel response, Reporter reporter) {
		ValidateArgument.required(response, "response");
		if (response.getIsRedirected()) {
			return generateResponsesForRedirectedEndpoint();
		}

		Map<String, ResponseInfo> responses = new LinkedHashMap<>();
		ResponseInfo responseInfo = new ResponseInfo().withDescription(response.getDescription());

		if (!"void".equals(response.getId())) {
			Map<String, Schema> contentTypeToSchema = new HashMap<>();
			contentTypeToSchema.put(response.getContentType(), new Schema().withSchema(getReferenceSchema(response.getId())));
			responseInfo = responseInfo.withContent(contentTypeToSchema);
		}

		String statusCode = "" + response.getStatusCode();
		responses.put(statusCode, responseInfo);
		return responses;
	}

	private static boolean classMapsToPrimitiveType(String id) {
		return ResponsePrimitives.containsKey(id);
	}

	private static boolean classIsGenericType(String id) {
		return id.contains("<") && id.contains(">");
	}


	/**
	 * If the endpoint is redirected, the response can either be a
	 *
	 * @param contentTypeToSchema
	 */
	Map<String, ResponseInfo> generateResponsesForRedirectedEndpoint() {
		Map<String, ResponseInfo> responses = new LinkedHashMap<>();

		// the two possible status codes for a redirected endpoint
		String statusCodeRedirected = "307";
		String statusCodeOk = "200";

		Map<String, Schema> statusCodeOkContentTypeToSchema = new HashMap<>();
		statusCodeOkContentTypeToSchema.put("text/plain", new Schema().withSchema(new JsonSchema()));
		ResponseInfo responseOk = new ResponseInfo().withDescription("Status 200 will be returned if the 'redirect' boolean param is false").withContent(statusCodeOkContentTypeToSchema);
		responses.put(statusCodeOk, responseOk);

		Map<String, Schema> statusCodeRedirectedContentTypeToSchema = new HashMap<>();
		ResponseInfo responseRedirected = new ResponseInfo().withDescription("Status 307 will be returned if the 'redirect' boolean param is true or null").withContent(statusCodeRedirectedContentTypeToSchema);
		responses.put(statusCodeRedirected, responseRedirected);

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
		contentTypeToSchema.put(contentType, new Schema().withSchema(getReferenceSchema(requestBody.getId())));
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
				.withSchema(getReferenceSchema(parameter.getId()));
		return parameterInfo;
	}
}