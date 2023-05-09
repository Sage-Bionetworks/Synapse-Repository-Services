package org.sagebionetworks.translator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;

import org.sagebionetworks.controller.annotations.model.ControllerInfoModel;
import org.sagebionetworks.controller.annotations.model.RequestMappingModel;
import org.sagebionetworks.controller.annotations.model.ResponseStatusModel;
import org.sagebionetworks.controller.model.ControllerModel;
import org.sagebionetworks.controller.model.MethodModel;
import org.sagebionetworks.controller.model.Operation;
import org.sagebionetworks.controller.model.ParameterLocation;
import org.sagebionetworks.controller.model.ParameterModel;
import org.sagebionetworks.controller.model.RequestBodyModel;
import org.sagebionetworks.controller.model.ResponseModel;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.util.DocTrees;

/**
 * This translator pulls information from a generic doclet model into our
 * representation of a controller model. This is a layer of abstraction that is
 * then used to export the OpenAPI specification of our API.
 * 
 * @author lli
 *
 */
public class ControllerToControllerModelTranslator {
	public ControllerModel translate(TypeElement controller, DocTrees docTrees) {
		ControllerModel controllerModel = new ControllerModel();
		List<MethodModel> methods = getMethods(controller.getEnclosedElements(), docTrees);
		ControllerInfoModel controllerInfo = getControllerInfoModel(controller.getAnnotationMirrors());
		controllerModel.withDisplayName(controllerInfo.getDisplayName()).withPath(controllerInfo.getPath()).withMethods(methods);
		return controllerModel;
	}
	
	/**
	 * Constructs a model that represents the annotations on a Controller.
	 * 
	 * @param annotations - the annotations for a controller
	 * @return a model that represents the annotations on a controller.
	 */
	public ControllerInfoModel getControllerInfoModel(List<? extends AnnotationMirror> annotations) {
		ValidateArgument.required(annotations, "annotations");
		for (AnnotationMirror annotation : annotations) {
			if (!ControllerInfo.class.getSimpleName().equals(getSimpleAnnotationName(annotation))) {
				continue;
			}
			ControllerInfoModel controllerInfo = new ControllerInfoModel();
			for (ExecutableElement key : annotation.getElementValues().keySet()) {
				String keyName = key.getSimpleName().toString();
				Object value = annotation.getElementValues().get(key).getValue();
				if (keyName.equals("displayName")) {
					controllerInfo.withDisplayName(value.toString());
				} else if (keyName.equals("path")) {
					controllerInfo.withPath(value.toString());
				}
			}
			ValidateArgument.required(controllerInfo.getPath(), "controllerInfo.path");
			ValidateArgument.required(controllerInfo.getDisplayName(), "controllerInfo.displayName");
			return controllerInfo;
		}
		throw new IllegalArgumentException("ControllerInfo annotation is not present in annotations.");
	}

	/**
	 * Creates a list of MethodModel that represents all the methods in a
	 * Controller.
	 * 
	 * @param enclosedElements - list of enclosed elements inside of a Controller.
	 * @param docTrees         - tree used to get the document comment tree for a
	 *                         method.
	 * @return the created list of MethodModels
	 */
	List<MethodModel> getMethods(List<? extends Element> enclosedElements, DocTrees docTrees) {
		List<MethodModel> methods = new ArrayList<>();
		for (ExecutableElement method : ElementFilter.methodsIn(enclosedElements)) {
			DocCommentTree docCommentTree = docTrees.getDocCommentTree(method);
			Map<String, String> parameterToDescription = getParameterToDescription(docCommentTree.getBlockTags());
			Map<Class, Object> annotationToModel = getAnnotationToModel(method.getAnnotationMirrors());
			if (!annotationToModel.containsKey(RequestMapping.class)) {
				throw new IllegalStateException("Method " + method.getSimpleName() + " missing RequestMapping annotation.");
			}
			if (!annotationToModel.containsKey(ResponseStatus.class)) {
				throw new IllegalStateException("Method " + method.getSimpleName() + " missing ResponseStatus annotation.");
			}
			Optional<String> behaviorComment = getBehaviorComment(docCommentTree.getFullBody());
			Optional<RequestBodyModel> requestBody = getRequestBody(method.getParameters(), parameterToDescription);
			MethodModel methodModel = new MethodModel()
					.withPath(getMethodPath((RequestMappingModel) annotationToModel.get(RequestMapping.class)))
					.withName(method.getSimpleName().toString())
					.withDescription(behaviorComment.isEmpty() ? null : behaviorComment.get())
					.withOperation(((RequestMappingModel) annotationToModel.get(RequestMapping.class)).getOperation())
					.withParameters(getParameters(method.getParameters(), parameterToDescription))
					.withRequestBody(requestBody.isEmpty() ? null : requestBody.get())
					.withResponse(getResponseModel(method.getReturnType().getKind(), docCommentTree.getBlockTags(),
							(ResponseStatusModel) annotationToModel.get(ResponseStatus.class)));
			methods.add(methodModel);
		}
		return methods;
	}

	/**
	 * Gets a model that represents the response of a method.
	 * 
	 * @param returnType           - the return type of the method.
	 * @param blockTags            - the parameter/return comments on the method.
	 * @param annotationToElements - maps an annotation to all of the elements
	 *                             inside of it.
	 * @return a model that represents the response of a method.
	 */
	ResponseModel getResponseModel(TypeKind returnType, List<? extends DocTree> blockTags,
			ResponseStatusModel responseStatus) {
		ValidateArgument.required(responseStatus, "ResponseStatus");
		ValidateArgument.required(responseStatus.getStatusCode(), "ResponseStatus.statusCode");
		Optional<String> returnComment = getReturnComment(blockTags);
		return new ResponseModel().withDescription(returnComment.isEmpty() ? null : returnComment.get())
				.withStatusCode(responseStatus.getStatusCode()).withSchema(getSchema(returnType));
	}

	/**
	 * Get the path that this method represents.
	 * 
	 * @param requestMapping - model of the RequestMapping annotation
	 * @return the path that this method represents.
	 */
	String getMethodPath(RequestMappingModel requestMapping) {
		ValidateArgument.required(requestMapping, "RequestMapping");
		ValidateArgument.required(requestMapping.getPath(), "RequestMapping.path");
		return requestMapping.getPath();
	}

	/**
	 * Constructs a map that maps an annotation class to a model that represents
	 * that annotation.
	 * 
	 * @param methodAnnotations - all of the annotation present on a method.
	 * @return map of an annotation class to model for that annotation.
	 */
	Map<Class, Object> getAnnotationToModel(List<? extends AnnotationMirror> methodAnnotations) {
		ValidateArgument.required(methodAnnotations, "Method annotations");
		Map<Class, Object> annotationToModel = new LinkedHashMap<>();
		for (AnnotationMirror annotation : methodAnnotations) {
			String annotationName = getSimpleAnnotationName(annotation);
			if (annotationName.equals("RequestMapping")) {
				annotationToModel.put(RequestMapping.class, getRequestMappingModel(annotation));
			} else if (annotationName.equals("ResponseStatus")) {
				annotationToModel.put(ResponseStatus.class, getResponseStatusModel(annotation));
			}
		}
		return annotationToModel;
	}
	
	/**
	 * Constructs a model that represents the ResponseStatus annotation.
	 * 
	 * @param annotation the annotation being looked at
	 * @return a model that represents the ResponseStatus annotation.
	 */
	ResponseStatusModel getResponseStatusModel(AnnotationMirror annotation) {
		ValidateArgument.required(annotation, "Annotation");
		ResponseStatusModel responseStatus = new ResponseStatusModel();
		for (ExecutableElement key : annotation.getElementValues().keySet()) {
			String keyName = key.getSimpleName().toString();
			if (keyName.equals("value") || keyName.equals("code")) {
				responseStatus.withStatusCode(getHttpStatusCode(annotation.getElementValues().get(key).getValue().toString()));
			}
		}
		return responseStatus;
	}
	
	/**
	 * Constructs a model that represents a RequestMapping annotation.
	 * 
	 * @param annotation the annotation being looked at
	 * @return a model that represents a RequestMapping annotation.
	 */
	RequestMappingModel getRequestMappingModel(AnnotationMirror annotation) {
		ValidateArgument.required(annotation, "Annotation");
		RequestMappingModel requestMapping = new RequestMappingModel();
		for (ExecutableElement key : annotation.getElementValues().keySet()) {
			String keyName = key.getSimpleName().toString();
			if (keyName.equals("value") || keyName.equals("path")) {
				requestMapping.withPath(annotation.getElementValues().get(key).getValue().toString());
			} else if (keyName.equals("method")) {
				String value = annotation.getElementValues().get(key).getValue().toString();
				String[] parts = value.split("\\.");
				requestMapping.withOperation(Operation.get(RequestMethod.valueOf(parts[parts.length - 1])));
			}
		}
		return requestMapping;
	}

	/**
	 * Get the HttpStatus of an endpoint
	 * 
	 * @param object - the status
	 * @return HttpStatus of an endpoint.
	 */
	int getHttpStatusCode(String object) {
		HttpStatus status = HttpStatus.valueOf(object);
		if (status.equals(HttpStatus.OK)) {
			return HttpStatus.OK.value();
		}
		throw new IllegalArgumentException("Could not translate HttpStatus for status " + status);
	}

	/**
	 * Constructs a model that represents the request body for a method.
	 * 
	 * @param parameters             - the parameters of this method.
	 * @param parameterToDescription - maps a parameter name to that parameters
	 *                               description
	 * @return optional that stores a model that represents the request body, or
	 *         empty if a request body does not exist.
	 */
	Optional<RequestBodyModel> getRequestBody(List<? extends VariableElement> parameters,
			Map<String, String> paramToDescription) {
		for (VariableElement param : parameters) {
			String simpleAnnotationName = getSimpleAnnotationName(getParameterAnnotation(param));
			if (RequestBody.class.getSimpleName().equals(simpleAnnotationName)) {
				String paramName = param.getSimpleName().toString();
				String paramDescription = paramToDescription.get(paramName);
				return Optional.of(new RequestBodyModel().withDescription(paramDescription).withRequired(true)
						.withSchema(getSchema(param.asType().getKind())));
			}
		}
		return Optional.empty();
	}

	/**
	 * Constructs a list representing all of the parameters for a method (excluding
	 * parameter present in RequestBody).
	 * 
	 * @param params                 - the parameters of the method.
	 * @param parameterToDescription - maps a parameter name to a description of
	 *                               that parameter.
	 * @return a list that represents all parameters for a method.
	 */
	List<ParameterModel> getParameters(List<? extends VariableElement> params,
			Map<String, String> parameterToDescription) {
		List<ParameterModel> parameters = new ArrayList<>();
		for (VariableElement param : params) {
			ParameterLocation paramLocation = getParameterLocation(param);
			if (paramLocation == null) {
				continue;
			}
			String paramName = param.getSimpleName().toString();
			String paramDescription = parameterToDescription.get(paramName);
			parameters.add(new ParameterModel().withDescription(paramDescription).withIn(paramLocation)
					.withName(paramName).withRequired(true).withSchema(getSchema(param.asType().getKind())));
		}
		return parameters;
	}

	/**
	 * Get the location of a parameter in the HTTP request.
	 * 
	 * @param param - the parameter being looked at
	 * @return location of a parameter, null if it is the RequestBody annotation.
	 */
	ParameterLocation getParameterLocation(VariableElement param) {
		String simpleAnnotationName = getSimpleAnnotationName(getParameterAnnotation(param));
		if (PathVariable.class.getSimpleName().equals(simpleAnnotationName)) {
			return ParameterLocation.path;
		}
		if (RequestParam.class.getSimpleName().equals(simpleAnnotationName)) {
			return ParameterLocation.query;
		}
		if (RequestBody.class.getSimpleName().equals(simpleAnnotationName)) {
			return null;
		}
		throw new IllegalArgumentException("Unable to get parameter location with annotation " + simpleAnnotationName);
	}

	/**
	 * Get the annotation for a parameter.
	 * 
	 * @param param - the parameter being looked at
	 * @return annotation for the parameter
	 */
	AnnotationMirror getParameterAnnotation(VariableElement param) {
		ValidateArgument.required(param, "Param");
		List<? extends AnnotationMirror> annotations = param.getAnnotationMirrors();
		if (annotations.size() != 1) {
			throw new IllegalArgumentException(
					"Each method parameter should have one annotation, this one has " + annotations.size());
		}
		return annotations.get(0);
	}

	/**
	 * Get the simple name for an annotation.
	 * 
	 * @param annotation - the annotation being looked at
	 * @return the simple name for the annotation.
	 */
	String getSimpleAnnotationName(AnnotationMirror annotation) {
		return annotation.getAnnotationType().asElement().getSimpleName().toString();
	}

	/**
	 * Constructs the JsonSchema for a particular TypeKind.
	 * 
	 * @param typeKind - the type of the element
	 * @return the schema of the element
	 */
	JsonSchema getSchema(TypeKind typeKind) {
		ValidateArgument.required(typeKind, "TypeKind");
		JsonSchema schema = new JsonSchema();
		Type type = getType(typeKind);
		schema.setType(type);
		switch (type) {
		case integer:
			schema.setFormat("int32");
			break;
		case string:
			break;
		default:
			throw new IllegalArgumentException("Type is unknown for type " + type);
		}
		return schema;
	}

	/**
	 * Translates a TypeKind to a Type that JsonSchema recognizes.
	 * 
	 * @param typeKind - the type of the element
	 * @return a Type as used in JsonSchema.
	 */
	Type getType(TypeKind typeKind) {
		switch (typeKind) {
		case INT:
			return Type.integer;
		case BOOLEAN:
			return Type._boolean;
		case FLOAT:
		case LONG:
		case DOUBLE:
			return Type.number;
		case ARRAY:
			return Type.array;
		// TODO: In the future, declared type can be more than just a String (other
		// classes or interfaces).
		case DECLARED:
			return Type.string;
		default:
			throw new IllegalArgumentException("Unable to translate TypeKind " + typeKind);
		}
	}

	/**
	 * Constructs a map that maps a parameter name to a description of that
	 * parameter.
	 * 
	 * @param blockTags - list of param/return comments on the method.
	 * @return map that maps a parameter name to a description of that parameter.
	 */
	Map<String, String> getParameterToDescription(List<? extends DocTree> blockTags) {
		Map<String, String> parameterToDescription = new HashMap<>();
		if (blockTags == null) {
			return parameterToDescription;
		}
		for (DocTree comment : blockTags) {
			if (!comment.getKind().equals(DocTree.Kind.PARAM)) {
				continue;
			}
			ParamTree paramComment = (ParamTree) comment;
			if (!paramComment.getDescription().isEmpty()) {
				parameterToDescription.put(paramComment.getName().toString(), paramComment.getDescription().toString());
			}

		}
		return parameterToDescription;
	}

	/**
	 * Get the return comment for a method.
	 * 
	 * @param blockTags - list of param/return comments on the method.
	 * @return return optional containing comment for a method, or empty optional if
	 *         there is none.
	 */
	Optional<String> getReturnComment(List<? extends DocTree> blockTags) {
		if (blockTags == null) {
			return Optional.empty();
		}
		for (DocTree comment : blockTags) {
			if (comment.getKind().equals(DocTree.Kind.RETURN)) {
				ReturnTree returnComment = (ReturnTree) comment;
				if (returnComment.getDescription().isEmpty()) {
					return Optional.empty();
				}
				return Optional.of(returnComment.getDescription().toString());
			}
		}
		return Optional.empty();
	}

	/**
	 * Gets an optional that contains the behavior/overall comment for a method.
	 * 
	 * @param fullBody - the body comment for the method.
	 * @return optional with the behavior/overall comment inside, or empty optional
	 *         if no behavior comment found.
	 */
	Optional<String> getBehaviorComment(List<? extends DocTree> fullBody) {
		if (fullBody == null || fullBody.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(fullBody.toString());
	}
}