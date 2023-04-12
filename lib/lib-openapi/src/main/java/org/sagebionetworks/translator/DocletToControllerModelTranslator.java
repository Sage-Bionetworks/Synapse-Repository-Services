package org.sagebionetworks.translator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;

import org.sagebionetworks.controller.model.ControllerModel;
import org.sagebionetworks.controller.model.MethodModel;
import org.sagebionetworks.controller.model.Operation;
import org.sagebionetworks.controller.model.ParameterLocation;
import org.sagebionetworks.controller.model.ParameterModel;
import org.sagebionetworks.controller.model.RequestBodyModel;
import org.sagebionetworks.controller.model.ResponseModel;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
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

import jdk.javadoc.doclet.DocletEnvironment;

/**
 * This translator parses information from a Controller and creates the
 * appropriate ControllerModel.
 * 
 * @author lli
 *
 */
public class DocletToControllerModelTranslator {
	public static ControllerModel translate(DocletEnvironment docEnv) {
		DocTrees docTrees = docEnv.getDocTrees();
		ControllerModel controllerModel = new ControllerModel();
		for (TypeElement t : ElementFilter.typesIn(docEnv.getIncludedElements())) {
			if (!t.getKind().equals(ElementKind.CLASS)) continue;
			List<MethodModel> methods = getMethods(t.getEnclosedElements(), docTrees);
			controllerModel.withDisplayName(t.toString()).withPath("/").withMethods(methods);
			break;
		}
		return controllerModel;
	}
	
	/**
	 * Creates a list of MethodModel that represents all the methods in a Controller.
	 * @param enclosedElements - list of enclosed elements inside of a Controller.
	 * @param docTrees - tree used to get the document comment tree for a method.
	 * @return the created list of MethodModels
	 */
	static List<MethodModel> getMethods(List<? extends Element> enclosedElements, DocTrees docTrees) {
		List<MethodModel> methods = new ArrayList<>();
		for (ExecutableElement method : ElementFilter.methodsIn(enclosedElements)) {
			DocCommentTree docCommentTree = docTrees.getDocCommentTree(method);
			Map<String, Map<String, Object>> annotationToElements = getAnnotationToElements(
					method.getAnnotationMirrors());
			Map<String, String> parameterToDescription = docCommentTree == null ? new LinkedHashMap<>()
					: getParameterToDescription(docCommentTree.getBlockTags());

			MethodModel methodModel = new MethodModel().withPath(getMethodPath(annotationToElements))
					.withName(method.getSimpleName().toString())
					.withDescription(getBehaviorComment(docCommentTree.getFullBody()))
					.withOperation(getMethodOperation(annotationToElements))
					.withParameters(getParameters(method.getParameters(), parameterToDescription))
					.withRequestBody(getRequestBody(method.getParameters(), parameterToDescription))
					.withResponse(getResponseModel(method.getReturnType().getKind(), docCommentTree.getBlockTags(),
							annotationToElements));
			methods.add(methodModel);
		}
		return methods;
	}

	/**
	 * Gets a model that represents the response of a method.
	 * @param returnType - the return type of the method.
	 * @param blockTags - the parameter/return comments on the method.
	 * @param annotationToElements - maps an annotation to all of the elements inside of it.
	 * @return a model that represents the response of a method.
	 */
	static ResponseModel getResponseModel(TypeKind returnType, List<? extends DocTree> blockTags,
			Map<String, Map<String, Object>> annotationToElements) {
		return new ResponseModel().withDescription(getReturnComment(blockTags))
				.withStatusCode(getStatusCode(annotationToElements)).withContentType("application/json")
				.withSchema(getSchema(returnType));
	}

	/**
	 * Gets the status code of a method when it is successful.
	 * @param annotationToElements - maps an annotation name to a map of that annotation's element names to element values.
	 * @return the status code of a method.
	 */
	static int getStatusCode(Map<String, Map<String, Object>> annotationToElements) {
		if (!annotationToElements.containsKey(ResponseStatus.class.getSimpleName())) {
			throw new IllegalArgumentException("Method does not contain the ResponseStatus annotation.");
		}
		Map<String, Object> elementNameToDefinition = annotationToElements.get(ResponseStatus.class.getSimpleName());
		if (!elementNameToDefinition.containsKey("value")) {
			throw new IllegalArgumentException("The http status code is not defined in the ResponseStatus annotation.");
		}

		String status = elementNameToDefinition.get("value").toString();
		if (status.equals("OK")) {
			return HttpStatus.OK.value();
		}
		return 200;
	}

	/**
	 * Get the path that this method represents.
	 * @param annotationToElements - maps an annotation name to a map of that annotation's element names to element values.
	 * @return the path that this method represents.
	 */
	static String getMethodPath(Map<String, Map<String, Object>> annotationToElements) {
		if (!annotationToElements.containsKey(RequestMapping.class.getSimpleName())) {
			throw new IllegalArgumentException("Annotation does not contain the RequestMapping annotation type.");
		}
		Map<String, Object> elementNameToDefinition = annotationToElements.get(RequestMapping.class.getSimpleName());
		if (!elementNameToDefinition.containsKey("value")) {
			throw new IllegalArgumentException("The path is not defined in the RequestMapping annotation.");
		}
		return elementNameToDefinition.get("value").toString();
	}
	
	/**
	 * Get the CRUD operation being performed by this method.
	 * @param annotationToElements - maps an annotation name to a map of that annotation's element names to element values.
	 * @return the CRUD operation being performed.
	 */
	static Operation getMethodOperation(Map<String, Map<String, Object>> annotationToElements) {
		if (!annotationToElements.containsKey(RequestMapping.class.getSimpleName())) {
			throw new IllegalArgumentException("Annotation does not contain the RequestMapping annotation type.");
		}
		Map<String, Object> elementNameToDefinition = annotationToElements.get(RequestMapping.class.getSimpleName());
		if (!elementNameToDefinition.containsKey("method")) {
			throw new IllegalArgumentException("The operation type is not defined in the RequestMapping annotation.");
		}

		Object definition = elementNameToDefinition.get("method");
		String[] parts = definition.toString().split("\\.");
		String operation = parts[parts.length - 1];
		if (operation.equals(RequestMethod.GET.toString())) {
			return Operation.get;
		} else if (operation.equals(RequestMethod.POST.toString())) {
			return Operation.post;
		} else if (operation.equals(RequestMethod.PUT.toString())) {
			return Operation.put;
		} else {
			return Operation.delete;
		}
	}

	/**
	 * Constructs a map that maps an annotation name to a map of that annotation's element names to element values.
	 * @param methodAnnotations - all of the annotation present on a method.
	 * @return map of an annotation name to a map of that annotation's element names to element values.
	 */
	static Map<String, Map<String, Object>> getAnnotationToElements(List<? extends AnnotationMirror> methodAnnotations) {
		Map<String, Map<String, Object>> annotationToElements = new LinkedHashMap<>();
		if (methodAnnotations != null) {
			for (AnnotationMirror annotation : methodAnnotations) {
				String annotationName = getSimpleAnnotationName(annotation);
				Map<String, Object> elementNameToDefinition = new LinkedHashMap<>();
				for (ExecutableElement key : annotation.getElementValues().keySet()) {
					elementNameToDefinition.put(key.getSimpleName().toString(), annotation.getElementValues().get(key).getValue());
				}
				annotationToElements.put(annotationName, elementNameToDefinition);
			}
		}
		return annotationToElements;
	}
	
	/**
	 * Constructs a model that represents the request body for a method.
	 * @param parameters - the parameters of this method.
	 * @param parameterToDescription - maps a parameter name to that parameters description
	 * @return a model that represents the request body, null if a request body does not exist.
	 */
	static RequestBodyModel getRequestBody(List<? extends VariableElement> parameters,
			Map<String, String> paramToDescription) {
		for (VariableElement param : parameters) {
			AnnotationMirror annotation = getParameterAnnotation(param);
			String simpleAnnotationName = getSimpleAnnotationName(annotation);
			if (simpleAnnotationName.equals(RequestBody.class.getSimpleName())) {
				String paramName = param.getSimpleName().toString();
				String paramDescription = paramToDescription.getOrDefault(paramName, "");
				return new RequestBodyModel().withDescription(paramDescription).withRequired(true)
						.withSchema(getSchema(param.asType().getKind()));
			}
		}
		return null;
	}
	
	/**
	 * Constructs a list representing all of the parameters for a method (excluding parameter present in RequestBody).
	 * @param params - the parameters of the method.
	 * @param parameterToDescription - maps a parameter name to a description of that parameter.
	 * @return a list that represents all parameter for a method.
	 */
	static List<ParameterModel> getParameters(List<? extends VariableElement> params,
			Map<String, String> parameterToDescription) {
		List<ParameterModel> parameters = new ArrayList<>();
		for (VariableElement param : params) {
			ParameterLocation paramLocation = getParameterLocation(param);
			if (paramLocation == null) continue;
			String paramName = param.getSimpleName().toString();
			String paramDescription = parameterToDescription.getOrDefault(paramName, "");
			parameters.add(new ParameterModel().withDescription(paramDescription).withIn(paramLocation)
					.withName(paramName).withRequired(true).withSchema(getSchema(param.asType().getKind())));
		}
		return parameters;
	}
	
	/**
	 * Get the location of a parameter in the HTTP request.
	 * @param param - the parameter being looked at 
	 * @return location of a parameter, or null if it is not in the path or query.
	 */
	static ParameterLocation getParameterLocation(VariableElement param) {
		AnnotationMirror annotation = getParameterAnnotation(param);
		String simpleAnnotationName = getSimpleAnnotationName(annotation);
		if (simpleAnnotationName.equals(PathVariable.class.getSimpleName())) {
			return ParameterLocation.path;
		} else if (simpleAnnotationName.equals(RequestParam.class.getSimpleName())) {
			return ParameterLocation.query;
		} else {
			return null;
		}
	}
	
	/**
	 * Get the annotation for a parameter.
	 * @param param - the parameter being looked at
	 * @return annotation for the parameter
	 */
	static AnnotationMirror getParameterAnnotation(VariableElement param) {
		if (param == null) return null;
		List<? extends AnnotationMirror> annotations = param.getAnnotationMirrors();
		if (annotations.isEmpty()) {
			throw new IllegalArgumentException(
					"Each method parameter should have exactly one annotation, this one does not have any " + param.toString());
		}
		return annotations.get(0);
	}
	
	/**
	 * Get the simple name for an annotation.
	 * @param annotation - the annotation being looked at
	 * @return the simple name for the annotation.
	 */
	static String getSimpleAnnotationName(AnnotationMirror annotation) {
		return annotation.getAnnotationType().asElement().getSimpleName().toString();
	}

	/**
	 * Constructs the JsonSchema for a particular TypeKind.
	 * @param typeKind - the type of the element
	 * @return the schema of the element
	 */
	static JsonSchema getSchema(TypeKind typeKind) {
		JsonSchema schema = new JsonSchema();
		if (typeKind == null) return schema;
		Type type = getType(typeKind);
		schema.setType(type);
		if (type.equals(Type.integer)) {
			schema.setFormat("int32");
		}
		return schema;
	}

	/**
	 * Translates a TypeKind to a Type that JsonSchema recognizes.
	 * @param typeKind - the type of the element
	 * @return a Type as used in JsonSchema.
	 */
	static Type getType(TypeKind typeKind) {
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
		case DECLARED:
			return Type.string;
		default:
			return Type.integer;
		}
	}

	/**
	 * Constructs a map that maps a parameter name to a description of that parameter.
	 * @param blockTags - list of param/return comments on the method.
	 * @return map that maps a parameter name to a description of that parameter.
	 */
	static Map<String, String> getParameterToDescription(List<? extends DocTree> blockTags) {
		Map<String, String> parameterToDescription = new HashMap<>();
		if (blockTags == null) return parameterToDescription;
		for (DocTree comment : blockTags) {
			if (!comment.getKind().equals(DocTree.Kind.PARAM)) {
				continue;
			}
			ParamTree paramComment = (ParamTree) comment;
			parameterToDescription.put(paramComment.getName().toString(),
					paramComment.getDescription().isEmpty() ? "" : paramComment.getDescription().get(0).toString());
		}
		return parameterToDescription;
	}

	/**
	 * Get the return comment for a method.
	 * @param blockTags - list of param/return comments on the method.
	 * @return return comment for a method, or empty string if none exists.
	 */
	static String getReturnComment(List<? extends DocTree> blockTags) {
		if (blockTags == null) return "";
		for (DocTree comment : blockTags) {
			if (comment.getKind().equals(DocTree.Kind.RETURN)) {
				ReturnTree returnComment = (ReturnTree) comment;
				if (returnComment.getDescription().isEmpty()) {
					return "";
				}
				return returnComment.getDescription().get(0).toString();
			}
		}
		return "";
	}
	
	/**
	 * Get the behavior/overall comment for a method.
	 * @param fullBody - the body comment for the method.
	 * @return the behavior/overall comment for the method.
	 */
	static String getBehaviorComment(List<? extends DocTree> fullBody) {
		if (fullBody == null || fullBody.isEmpty()) return "";
		return fullBody.get(0).toString();
	}
}