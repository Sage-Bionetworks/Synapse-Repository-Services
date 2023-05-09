package org.sagebionetworks.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.IdentifierTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.util.DocTrees;

@ExtendWith(MockitoExtension.class)
public class ControllerToControllerModelTranslatorTest {
	private ControllerToControllerModelTranslator translator;

	private final String PARAM_1_NAME = "PARAM_1";
	private final String PARAM_2_NAME = "PARAM_2";
	private final String PARAM_1_DESCRIPTION = "PARAM_DESCRIPTION_1";
	private final String PARAM_2_DESCRIPTION = "PARAM_DESCRIPTION_2";
	private final String METHOD_NAME = "METHOD_NAME";
	private final String METHOD_BEHAVIOR_COMMENT = "BEHAVIOR_COMMENT";
	private final String METHOD_RETURN_COMMENT = "RETURN_COMMENT";
	private final String METHOD_PATH = "/fake/path";
	private final String ANNOTATION_NAME = "ANNOTATION_NAME";
	private final String CONTROLLER_NAME = "Person";
	private final String CONTROLLER_PATH = "repo/v1/person";

	@BeforeEach
	private void setUp() {
		this.translator = Mockito.spy(new ControllerToControllerModelTranslator());
	}

	private List<ParameterModel> getExpectedParameters() {
		List<ParameterModel> expectedParameters = new ArrayList<>();

		JsonSchema expectedSchema = new JsonSchema();
		expectedSchema.setType(Type.integer);
		expectedSchema.setFormat("int32");
		ParameterModel expectedParameter = new ParameterModel().withDescription(PARAM_1_DESCRIPTION)
				.withIn(ParameterLocation.path).withName(PARAM_1_NAME).withRequired(true).withSchema(expectedSchema);
		expectedParameters.add(expectedParameter);

		return expectedParameters;
	}

	public RequestBodyModel getExpectedRequestBody() {
		JsonSchema expectedSchema = new JsonSchema();
		expectedSchema.setType(Type.integer);
		expectedSchema.setFormat("int32");
		return new RequestBodyModel().withDescription(PARAM_1_DESCRIPTION).withRequired(true)
				.withSchema(expectedSchema);
	}

	private ResponseModel getExpectedResponseModel() {
		JsonSchema schema = new JsonSchema();
		schema.setType(Type.integer);
		schema.setFormat("int32");
		return new ResponseModel().withDescription(METHOD_RETURN_COMMENT).withStatusCode(200).withSchema(schema);
	}

	private List<MethodModel> getExpectedMethods() {
		List<MethodModel> expectedMethods = new ArrayList<>();
		MethodModel expectedMethod = new MethodModel().withPath(METHOD_PATH).withDescription(METHOD_BEHAVIOR_COMMENT)
				.withName(METHOD_NAME).withOperation(Operation.get).withParameters(getExpectedParameters())
				.withRequestBody(getExpectedRequestBody()).withResponse(getExpectedResponseModel());
		expectedMethods.add(expectedMethod);
		return expectedMethods;
	}

	private void addAnnotationElementValues(Map<ExecutableElement, AnnotationValue> elementValues, String keyName,
			Object value) {
		ExecutableElement key = Mockito.mock(ExecutableElement.class);
		Name simpleName = Mockito.mock(Name.class);
		when(key.getSimpleName()).thenReturn(simpleName);
		when(simpleName.toString()).thenReturn(keyName);

		AnnotationValue annotationValue = Mockito.mock(AnnotationValue.class);
		when(annotationValue.getValue()).thenReturn(value);

		elementValues.put(key, annotationValue);
	}
	
	private ParamTree getParamTree(String parameterName, String paramDescription) {
		ParamTree param = Mockito.mock(ParamTree.class);
		when(param.getKind()).thenReturn(DocTree.Kind.PARAM);

		IdentifierTree paramName = Mockito.mock(IdentifierTree.class);
		when(paramName.toString()).thenReturn(parameterName);
		when(param.getName()).thenReturn(paramName);
		List<? extends DocTree> description = Mockito.mock(List.class);
		when(description.isEmpty()).thenReturn(false);
		when(description.toString()).thenReturn(paramDescription);
		Mockito.doReturn(description).when(param).getDescription();

		return param;
	}

	@Test
	public void testTranslate() {
		ControllerModel expectedControllerModel = new ControllerModel().withDisplayName(CONTROLLER_NAME)
				.withMethods(getExpectedMethods()).withPath(CONTROLLER_PATH);

		TypeElement controller = Mockito.mock(TypeElement.class);
		List<Element> elements = new ArrayList<>();
		Mockito.doReturn(elements).when(controller).getEnclosedElements();
		List<AnnotationMirror> annoMirrors = new ArrayList<>();
		Mockito.doReturn(annoMirrors).when(controller).getAnnotationMirrors();
		Mockito.doReturn(getExpectedMethods()).when(translator).getMethods(any(List.class), any(DocTrees.class));
		Mockito.doReturn(new ControllerInfoModel().withDisplayName(CONTROLLER_NAME).withPath(CONTROLLER_PATH))
				.when(translator).getControllerInfoModel(any(List.class));
		
		DocTrees mockDocTree = Mockito.mock(DocTrees.class);
		// call under test
		assertEquals(expectedControllerModel, translator.translate(controller, mockDocTree));
		
		Mockito.verify(controller, Mockito.times(1)).getEnclosedElements();
		Mockito.verify(controller, Mockito.times(1)).getAnnotationMirrors();
		Mockito.verify(translator, Mockito.times(1)).getMethods(elements, mockDocTree);
		Mockito.verify(translator, Mockito.times(1)).getControllerInfoModel(annoMirrors);
	}

	@Test
	public void testGetControllerInfoModel() {
		List<AnnotationMirror> annotations = new ArrayList<>();
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoElementValues = new HashMap<>();
		Mockito.doReturn(annoElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoElementValues, "path", CONTROLLER_PATH);
		addAnnotationElementValues(annoElementValues, "displayName", CONTROLLER_NAME);
		annotations.add(anno);
		Mockito.doReturn("ControllerInfo").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));

		ControllerInfoModel controllerInfo = new ControllerInfoModel().withDisplayName(CONTROLLER_NAME)
				.withPath(CONTROLLER_PATH);
		assertEquals(controllerInfo, translator.getControllerInfoModel(annotations));
		
		Mockito.verify(anno, Mockito.times(3)).getElementValues();
		Mockito.verify(translator, Mockito.times(1)).getSimpleAnnotationName(anno);
	}
	
	@Test
	public void testGetControllerInfoModelWithUnknownAnnotation() {
		List<AnnotationMirror> annotations = new ArrayList<>();
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoElementValues = new HashMap<>();
		Mockito.doReturn(annoElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoElementValues, "UNKNOWN", "UNKNOWN");
		annotations.add(anno);
		Mockito.doReturn("ControllerInfo").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getControllerInfoModel(annotations);
		});
		
		assertEquals("controllerInfo.path is required.", exception.getMessage());
		
		Mockito.verify(anno, Mockito.times(2)).getElementValues();
		Mockito.verify(translator, Mockito.times(1)).getSimpleAnnotationName(anno);
	}

	@Test
	public void testGetControllerInfoModelMissingPathAndDisplayName() {
		List<AnnotationMirror> annotations = new ArrayList<>();
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		Mockito.doReturn(new HashMap<>()).when(anno).getElementValues();
		annotations.add(anno);
		Mockito.doReturn("ControllerInfo").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getControllerInfoModel(annotations);
		});
		
		assertEquals("controllerInfo.path is required.", exception.getMessage());
		Mockito.verify(anno, Mockito.times(1)).getElementValues();
		Mockito.verify(translator, Mockito.times(1)).getSimpleAnnotationName(anno);
	}

	@Test
	public void testGetControllerInfoModelMissingPath() {
		List<AnnotationMirror> annotations = new ArrayList<>();
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoElementValues = new HashMap<>();
		Mockito.doReturn(annoElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoElementValues, "displayName", CONTROLLER_NAME);
		annotations.add(anno);
		Mockito.doReturn("ControllerInfo").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getControllerInfoModel(annotations);
		});
		
		assertEquals("controllerInfo.path is required.", exception.getMessage());
		
		Mockito.verify(anno, Mockito.times(2)).getElementValues();
		Mockito.verify(translator, Mockito.times(1)).getSimpleAnnotationName(anno);
	}

	@Test
	public void testGetControllerInfoModelMissingDisplayName() {
		List<AnnotationMirror> annotations = new ArrayList<>();
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoElementValues = new HashMap<>();
		Mockito.doReturn(annoElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoElementValues, "path", CONTROLLER_PATH);
		annotations.add(anno);
		Mockito.doReturn("ControllerInfo").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getControllerInfoModel(annotations);
		});
		
		assertEquals("controllerInfo.displayName is required.", exception.getMessage());
		
		Mockito.verify(anno, Mockito.times(2)).getElementValues();
		Mockito.verify(translator, Mockito.times(1)).getSimpleAnnotationName(anno);
	}

	@Test
	public void testGetControllerInfoModelWithoutControllerInfoAnnotation() {
		List<AnnotationMirror> annotations = new ArrayList<>();
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		annotations.add(anno);

		Mockito.doReturn("WRONG_ANNOTATION").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getControllerInfoModel(annotations);
		});
		
		assertEquals("ControllerInfo annotation is not present in annotations.", exception.getMessage());
		Mockito.verify(translator, Mockito.times(1)).getSimpleAnnotationName(anno);
	}

	@Test
	public void testGetControllerInfoModelWithEmptyAnnotations() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getControllerInfoModel(new ArrayList<>());
		});
		assertEquals("ControllerInfo annotation is not present in annotations.", exception.getMessage());
	}

	@Test
	public void testGetControllerInfoModelWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getControllerInfoModel(null);
		});
		assertEquals("annotations is required.", exception.getMessage());
	}

	@Test
	public void testGetMethods() {
		DocTrees docTrees = Mockito.mock(DocTrees.class);
		DocCommentTree mockDocCommentTree = Mockito.mock(DocCommentTree.class);
		when(docTrees.getDocCommentTree(any(Element.class))).thenReturn(mockDocCommentTree);
		List<DocTree> blockTags = new ArrayList<>();
		List<DocTree> fullBody = new ArrayList<>();
		Mockito.doReturn(blockTags).when(mockDocCommentTree).getBlockTags();
		Mockito.doReturn(fullBody).when(mockDocCommentTree).getFullBody();

		List<Element> enclosedElements = new ArrayList<>();
		ExecutableElement method = Mockito.mock(ExecutableElement.class);
		List<AnnotationMirror> annoMirrors = new ArrayList<>();
		Mockito.doReturn(annoMirrors).when(method).getAnnotationMirrors();
		List<VariableElement> parameters = new ArrayList<>();
		Mockito.doReturn(parameters).when(method).getParameters();
		TypeMirror returnType = Mockito.mock(TypeMirror.class);
		when(returnType.getKind()).thenReturn(TypeKind.INT);
		when(method.getReturnType()).thenReturn(returnType);
		Name methodName = Mockito.mock(Name.class);
		when(method.getSimpleName()).thenReturn(methodName);
		when(methodName.toString()).thenReturn(METHOD_NAME);
		when(method.getKind()).thenReturn(ElementKind.METHOD);
		enclosedElements.add(method);

		// Mock translator method calls
		Map<String, String> parameterToDescription = new HashMap<>();
		parameterToDescription.put(PARAM_1_NAME, PARAM_1_DESCRIPTION);
		Mockito.doReturn(parameterToDescription).when(translator).getParameterToDescription(any(List.class));

		Map<Class, Object> annotationToModel = new HashMap<>();
		RequestMappingModel requestMapping = new RequestMappingModel().withOperation(Operation.get).withPath(METHOD_PATH);
		annotationToModel.put(RequestMapping.class, requestMapping);
		ResponseStatusModel responseStatus = new ResponseStatusModel().withStatusCode(HttpStatus.OK.value());
		annotationToModel.put(ResponseStatus.class, responseStatus);
		Mockito.doReturn(annotationToModel).when(translator).getAnnotationToModel(any(List.class));

		JsonSchema expectedRequestBodySchema = new JsonSchema();
		expectedRequestBodySchema.setType(Type.integer);
		expectedRequestBodySchema.setFormat("int32");
		RequestBodyModel requestBody = new RequestBodyModel().withDescription(PARAM_1_DESCRIPTION).withRequired(true)
				.withSchema(expectedRequestBodySchema);
		Mockito.doReturn(Optional.of(requestBody)).when(translator).getRequestBody(any(List.class), any(Map.class));

		Mockito.doReturn(Optional.of(METHOD_BEHAVIOR_COMMENT)).when(translator).getBehaviorComment(any(List.class));
		Mockito.doReturn(METHOD_PATH).when(translator).getMethodPath(any(RequestMappingModel.class));
		Mockito.doReturn(getExpectedParameters()).when(translator).getParameters(any(List.class), any(Map.class));
		Mockito.doReturn(getExpectedResponseModel()).when(translator).getResponseModel(any(TypeKind.class), any(),
				any());

		// call under test
		assertEquals(getExpectedMethods(), translator.getMethods(enclosedElements, docTrees));
		
		Mockito.verify(docTrees, Mockito.times(1)).getDocCommentTree(method);
		Mockito.verify(mockDocCommentTree, Mockito.times(2)).getBlockTags();
		Mockito.verify(mockDocCommentTree, Mockito.times(1)).getFullBody();
		Mockito.verify(returnType, Mockito.times(1)).getKind();
		Mockito.verify(method, Mockito.times(1)).getReturnType();
		Mockito.verify(method, Mockito.times(1)).getSimpleName();
		Mockito.verify(method, Mockito.times(1)).getKind();
		Mockito.verify(method, Mockito.times(1)).getAnnotationMirrors();
		Mockito.verify(method, Mockito.times(2)).getParameters();
		Mockito.verify(translator, Mockito.times(1)).getParameterToDescription(blockTags);
		Mockito.verify(translator, Mockito.times(1)).getAnnotationToModel(annoMirrors);
		Mockito.verify(translator).getRequestBody(parameters, parameterToDescription);
		Mockito.verify(translator, Mockito.times(1)).getBehaviorComment(blockTags);
		Mockito.verify(translator, Mockito.times(1)).getMethodPath(requestMapping);
		Mockito.verify(translator, Mockito.times(1)).getParameters(parameters, parameterToDescription);
		Mockito.verify(translator, Mockito.times(1)).getResponseModel(TypeKind.INT, blockTags, responseStatus);
	}

	@Test
	public void testGetMethodsWithEmptyDescriptionAndRequestBody() {
		DocTrees docTrees = Mockito.mock(DocTrees.class);
		DocCommentTree mockDocCommentTree = Mockito.mock(DocCommentTree.class);
		when(docTrees.getDocCommentTree(any(Element.class))).thenReturn(mockDocCommentTree);
		List<DocTree> blockTags = new ArrayList<>();
		List<DocTree> fullBody = new ArrayList<>();
		Mockito.doReturn(blockTags).when(mockDocCommentTree).getBlockTags();
		Mockito.doReturn(fullBody).when(mockDocCommentTree).getFullBody();

		List<Element> enclosedElements = new ArrayList<>();
		ExecutableElement method = Mockito.mock(ExecutableElement.class);
		List<AnnotationMirror> annoMirrors = new ArrayList<>();
		Mockito.doReturn(annoMirrors).when(method).getAnnotationMirrors();
		List<VariableElement> parameters = new ArrayList<>();
		Mockito.doReturn(parameters).when(method).getParameters();
		TypeMirror returnType = Mockito.mock(TypeMirror.class);
		when(returnType.getKind()).thenReturn(TypeKind.INT);
		when(method.getReturnType()).thenReturn(returnType);
		Name methodName = Mockito.mock(Name.class);
		when(method.getSimpleName()).thenReturn(methodName);
		when(methodName.toString()).thenReturn(METHOD_NAME);
		when(method.getKind()).thenReturn(ElementKind.METHOD);
		enclosedElements.add(method);

		// Mock translator method calls
		Map<String, String> parameterToDescription = new HashMap<>();
		parameterToDescription.put(PARAM_1_NAME, PARAM_1_DESCRIPTION);
		Mockito.doReturn(parameterToDescription).when(translator).getParameterToDescription(any(List.class));

		Map<Class, Object> annotationToModel = new HashMap<>();
		RequestMappingModel requestMapping = new RequestMappingModel().withOperation(Operation.get).withPath(METHOD_PATH);
		annotationToModel.put(RequestMapping.class, requestMapping);
		ResponseStatusModel responseStatus = new ResponseStatusModel().withStatusCode(HttpStatus.OK.value());
		annotationToModel.put(ResponseStatus.class, responseStatus);
		Mockito.doReturn(annotationToModel).when(translator).getAnnotationToModel(any(List.class));
		Mockito.doReturn(Optional.empty()).when(translator).getRequestBody(any(List.class), any(Map.class));
		Mockito.doReturn(Optional.empty()).when(translator).getBehaviorComment(any(List.class));
		Mockito.doReturn(METHOD_PATH).when(translator).getMethodPath(any(RequestMappingModel.class));
		Mockito.doReturn(getExpectedParameters()).when(translator).getParameters(any(List.class), any(Map.class));
		Mockito.doReturn(getExpectedResponseModel()).when(translator).getResponseModel(any(TypeKind.class), any(List.class),
				any(ResponseStatusModel.class));

		List<MethodModel> expectedMethods = new ArrayList<>();
		MethodModel expectedMethod = new MethodModel().withPath(METHOD_PATH).withName(METHOD_NAME)
				.withOperation(Operation.get).withParameters(getExpectedParameters())
				.withResponse(getExpectedResponseModel());
		expectedMethods.add(expectedMethod);
		// call under test
		assertEquals(expectedMethods, translator.getMethods(enclosedElements, docTrees));
		
		Mockito.verify(docTrees, Mockito.times(1)).getDocCommentTree(method);
		Mockito.verify(mockDocCommentTree, Mockito.times(2)).getBlockTags();
		Mockito.verify(mockDocCommentTree, Mockito.times(1)).getFullBody();
		Mockito.verify(returnType, Mockito.times(1)).getKind();
		Mockito.verify(method, Mockito.times(1)).getReturnType();
		Mockito.verify(method, Mockito.times(1)).getSimpleName();
		Mockito.verify(method, Mockito.times(1)).getKind();
		Mockito.verify(method, Mockito.times(1)).getAnnotationMirrors();
		Mockito.verify(method, Mockito.times(2)).getParameters();
		Mockito.verify(translator, Mockito.times(1)).getParameterToDescription(blockTags);
		Mockito.verify(translator, Mockito.times(1)).getAnnotationToModel(annoMirrors);
		Mockito.verify(translator).getRequestBody(parameters, parameterToDescription);
		Mockito.verify(translator, Mockito.times(1)).getBehaviorComment(blockTags);
		Mockito.verify(translator, Mockito.times(1)).getMethodPath(requestMapping);
		Mockito.verify(translator, Mockito.times(1)).getParameters(parameters, parameterToDescription);
		Mockito.verify(translator, Mockito.times(1)).getResponseModel(TypeKind.INT, blockTags, responseStatus);
	}

	@Test
	public void testGetMethodsWhenMissingMethodAnnotations() {
		DocTrees docTrees = Mockito.mock(DocTrees.class);
		DocCommentTree mockDocCommentTree = Mockito.mock(DocCommentTree.class);
		when(docTrees.getDocCommentTree(any(Element.class))).thenReturn(mockDocCommentTree);

		List<Element> enclosedElements = new ArrayList<>();
		ExecutableElement method = Mockito.mock(ExecutableElement.class);
		when(method.getKind()).thenReturn(ElementKind.METHOD);
		when(method.getAnnotationMirrors()).thenReturn(new ArrayList<>());
		Name name = Mockito.mock(Name.class);
		when(method.getSimpleName()).thenReturn(name);
		when(name.toString()).thenReturn(METHOD_NAME);
		enclosedElements.add(method);

		// Mock translator method calls
		Map<String, String> parameterToDescription = new HashMap<>();
		Mockito.doReturn(parameterToDescription).when(translator).getParameterToDescription(any(List.class));

		Map<Class, Object> annotationToModel = new HashMap<>();
		Mockito.doReturn(annotationToModel).when(translator).getAnnotationToModel(any(List.class));

		// Missing RequestMapping + ResponseStatus annotations
		IllegalStateException exception1 = assertThrows(IllegalStateException.class, () -> {
			translator.getMethods(enclosedElements, docTrees);
		});
		assertEquals("Method " + METHOD_NAME + " missing RequestMapping annotation.", exception1.getMessage());
		
		Mockito.verify(docTrees, Mockito.times(1)).getDocCommentTree(method);
		Mockito.verify(method, Mockito.times(1)).getKind();
		Mockito.verify(method, Mockito.times(1)).getAnnotationMirrors();
		Mockito.verify(method, Mockito.times(1)).getSimpleName();

		annotationToModel.put(RequestMapping.class,
				new RequestMappingModel().withOperation(Operation.get).withPath(METHOD_PATH));
		// Missing ResponseStatus annotation
		IllegalStateException exception2 = assertThrows(IllegalStateException.class, () -> {
			translator.getMethods(enclosedElements, docTrees);
		});
		assertEquals("Method " + METHOD_NAME + " missing ResponseStatus annotation.", exception2.getMessage());
		
		Mockito.verify(docTrees, Mockito.times(2)).getDocCommentTree(method);
		Mockito.verify(method, Mockito.times(2)).getKind();
		Mockito.verify(method, Mockito.times(2)).getAnnotationMirrors();
		Mockito.verify(method, Mockito.times(2)).getSimpleName();


		annotationToModel.remove(RequestMapping.class);
		annotationToModel.put(ResponseStatus.class, new ResponseStatusModel().withStatusCode(HttpStatus.OK.value()));
		// Missing RequestMapping annotation
		IllegalStateException exception3 = assertThrows(IllegalStateException.class, () -> {
			translator.getMethods(enclosedElements, docTrees);
		});
		
		assertEquals("Method " + METHOD_NAME + " missing RequestMapping annotation.", exception3.getMessage());
		
		Mockito.verify(docTrees, Mockito.times(3)).getDocCommentTree(method);
		Mockito.verify(method, Mockito.times(3)).getKind();
		Mockito.verify(method, Mockito.times(3)).getAnnotationMirrors();
		Mockito.verify(method, Mockito.times(3)).getSimpleName();

	}

	@Test
	public void testGetMethodsWithEmptyEnclosedElements() {
		// call under test
		assertEquals(new ArrayList<>(), translator.getMethods(new ArrayList<>(), Mockito.mock(DocTrees.class)));
	}

	@Test
	public void testGetResponseModel() {
		Mockito.doReturn(Optional.of(METHOD_RETURN_COMMENT)).when(translator).getReturnComment(any(List.class));

		JsonSchema expectedSchema = new JsonSchema();
		expectedSchema.setType(Type.integer);
		expectedSchema.setFormat("int32");
		ResponseModel expectedResponse = new ResponseModel().withDescription(METHOD_RETURN_COMMENT).withStatusCode(200)
				.withContentType("application/json").withSchema(expectedSchema);
		List<DocTree> blockTags = new ArrayList<>();
		assertEquals(expectedResponse, translator.getResponseModel(TypeKind.INT, blockTags,
				new ResponseStatusModel().withStatusCode(HttpStatus.OK.value())));
		
		Mockito.verify(translator, Mockito.times(1)).getReturnComment(blockTags);
	}

	@Test
	public void testGetResponseModelWithEmptyReturnComment() {
		Mockito.doReturn(Optional.empty()).when(translator).getReturnComment(any(List.class));

		JsonSchema expectedSchema = new JsonSchema();
		expectedSchema.setType(Type.integer);
		expectedSchema.setFormat("int32");
		ResponseModel expectedResponse = new ResponseModel().withDescription(null).withStatusCode(200)
				.withContentType("application/json").withSchema(expectedSchema);
		List<DocTree> blockTags = new ArrayList<>();
		assertEquals(expectedResponse, translator.getResponseModel(TypeKind.INT, blockTags,
				new ResponseStatusModel().withStatusCode(HttpStatus.OK.value())));
		
		Mockito.verify(translator, Mockito.times(1)).getReturnComment(blockTags);
	}

	@Test
	public void testGetResponseModelWithNullStatus() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getResponseModel(TypeKind.INT, new ArrayList<>(), new ResponseStatusModel().withStatusCode(null));
		});
		assertEquals("ResponseStatus.statusCode is required.", exception.getMessage());
	}

	@Test
	public void testGetResponseModelWithNullResonseStatusModel() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getResponseModel(TypeKind.INT, new ArrayList<>(), null);
		});
		assertEquals("ResponseStatus is required.", exception.getMessage());
	}

	@Test
	public void testGetMethodPath() {
		// call under test
		assertEquals(METHOD_PATH, translator.getMethodPath(new RequestMappingModel().withPath(METHOD_PATH)));
	}

	@Test
	public void testGetMethodPathWithNullPath() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getMethodPath(new RequestMappingModel());
		});
		assertEquals("RequestMapping.path is required.", exception.getMessage());
	}

	@Test
	public void testGetMethodPathWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getMethodPath(null);
		});
		assertEquals("RequestMapping is required.", exception.getMessage());
	}

	@Test
	public void testAnnotationToModelWithPathAndCodeAnnotations() {
		List<AnnotationMirror> methodAnnotations = new ArrayList<>();

		AnnotationMirror anno1 = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> anno1ElementValues = new HashMap<>();
		Mockito.doReturn(anno1ElementValues).when(anno1).getElementValues();
		addAnnotationElementValues(anno1ElementValues, "path", METHOD_PATH);
		addAnnotationElementValues(anno1ElementValues, "method", RequestMethod.GET);

		AnnotationMirror anno2 = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> anno2ElementValues = new HashMap<>();
		Mockito.doReturn(anno2ElementValues).when(anno2).getElementValues();
		addAnnotationElementValues(anno2ElementValues, "code", "OK");

		methodAnnotations.add(anno1);
		methodAnnotations.add(anno2);

		Mockito.doReturn("RequestMapping", "ResponseStatus").when(translator)
				.getSimpleAnnotationName(any(AnnotationMirror.class));

		Map<Class, Object> expectedAnnotationToModel = new LinkedHashMap<>();
		expectedAnnotationToModel.put(RequestMapping.class,
				new RequestMappingModel().withOperation(Operation.get).withPath(METHOD_PATH));
		expectedAnnotationToModel.put(ResponseStatus.class,
				new ResponseStatusModel().withStatusCode(HttpStatus.OK.value()));

		assertEquals(expectedAnnotationToModel, translator.getAnnotationToModel(methodAnnotations));
		
		Mockito.verify(anno1, Mockito.times(3)).getElementValues();
		Mockito.verify(anno2, Mockito.times(2)).getElementValues();
		Mockito.verify(translator, Mockito.times(2)).getSimpleAnnotationName(any(AnnotationMirror.class));
		InOrder inOrder = Mockito.inOrder(translator);
		inOrder.verify(translator).getSimpleAnnotationName(anno1);
		inOrder.verify(translator).getSimpleAnnotationName(anno2);
	}

	@Test
	public void testGetAnnotationToModel() {
		List<AnnotationMirror> methodAnnotations = new ArrayList<>();
		AnnotationMirror mockAnnoMirror = Mockito.mock(AnnotationMirror.class);
		methodAnnotations.add(mockAnnoMirror);
		methodAnnotations.add(mockAnnoMirror);

		RequestMappingModel requestMapping = new RequestMappingModel().withOperation(Operation.get)
				.withPath(METHOD_PATH);
		ResponseStatusModel responseStatus = new ResponseStatusModel().withStatusCode(HttpStatus.OK.value());

		Map<Class, Object> expectedAnnotationToModel = new LinkedHashMap<>();
		expectedAnnotationToModel.put(RequestMapping.class, requestMapping);
		expectedAnnotationToModel.put(ResponseStatus.class, responseStatus);

		Mockito.doReturn(requestMapping).when(translator).getRequestMappingModel(any(AnnotationMirror.class));
		Mockito.doReturn(responseStatus).when(translator).getResponseStatusModel(any(AnnotationMirror.class));
		Mockito.doReturn("RequestMapping", "ResponseStatus").when(translator)
				.getSimpleAnnotationName(any(AnnotationMirror.class));

		assertEquals(expectedAnnotationToModel, translator.getAnnotationToModel(methodAnnotations));
		
		Mockito.verify(translator, Mockito.times(1)).getResponseStatusModel(mockAnnoMirror);
		Mockito.verify(translator, Mockito.times(1)).getResponseStatusModel(mockAnnoMirror);
		Mockito.verify(translator, Mockito.times(2)).getSimpleAnnotationName(mockAnnoMirror);
	}
	
	@Test
	public void testGetAnnotationToModelWithUnhandledAnnotation() {
		List<AnnotationMirror> methodAnnotations = new ArrayList<>();
		AnnotationMirror mock = Mockito.mock(AnnotationMirror.class);
		methodAnnotations.add(mock);
		Mockito.doReturn("UNKNOWN").when(translator)
				.getSimpleAnnotationName(any(AnnotationMirror.class));

		assertEquals(new HashMap<>(), translator.getAnnotationToModel(methodAnnotations));
		
		Mockito.verify(translator, Mockito.times(1)).getSimpleAnnotationName(mock);
	}

	@Test
	public void testGetAnnotationToModelWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getAnnotationToModel(null);
		});
		assertEquals("Method annotations is required.", exception.getMessage());
	}

	@Test
	public void testGetResponseStatusModelWithCodeKeyName() {
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoToElementValues = new HashMap<>();
		Mockito.doReturn(annoToElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoToElementValues, "code", "OK");

		ResponseStatusModel expected = new ResponseStatusModel().withStatusCode(HttpStatus.OK.value());
		assertEquals(expected, translator.getResponseStatusModel(anno));
		
		Mockito.verify(anno, Mockito.times(2)).getElementValues();
	}

	@Test
	public void testGetResponseStatusModelWithValueKeyName() {
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoToElementValues = new HashMap<>();
		Mockito.doReturn(annoToElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoToElementValues, "value", "OK");

		ResponseStatusModel expected = new ResponseStatusModel().withStatusCode(HttpStatus.OK.value());
		assertEquals(expected, translator.getResponseStatusModel(anno));
		
		Mockito.verify(anno, Mockito.times(2)).getElementValues();
	}
	
	@Test
	public void testGetResponseStatusModelWithUnknownAnnotation() {
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoToElementValues = new HashMap<>();
		Mockito.doReturn(annoToElementValues).when(anno).getElementValues();
		
		ExecutableElement key = Mockito.mock(ExecutableElement.class);
		Name simpleName = Mockito.mock(Name.class);
		when(key.getSimpleName()).thenReturn(simpleName);
		when(simpleName.toString()).thenReturn("UNKNOWN");
		annoToElementValues.put(key, null);
		
		assertEquals(new ResponseStatusModel(), translator.getResponseStatusModel(anno));
		
		Mockito.verify(key, Mockito.times(1)).getSimpleName();
		Mockito.verify(anno, Mockito.times(1)).getElementValues();
	}

	@Test
	public void testGetResponseStatusModelWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getResponseStatusModel(null);
		});
		assertEquals("Annotation is required.", exception.getMessage());
	}
	
	@Test
	public void testGetRequestMappingModelWithUnknownAnnotation() {
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoToElementValues = new HashMap<>();
		Mockito.doReturn(annoToElementValues).when(anno).getElementValues();
		
		ExecutableElement key = Mockito.mock(ExecutableElement.class);
		Name simpleName = Mockito.mock(Name.class);
		when(key.getSimpleName()).thenReturn(simpleName);
		when(simpleName.toString()).thenReturn("UNKNOWN");
		annoToElementValues.put(key, null);
		
		assertEquals(new RequestMappingModel(), translator.getRequestMappingModel(anno));
		
		Mockito.verify(anno, Mockito.times(1)).getElementValues();
	}

	@Test
	public void testGetRequestMappingModelWithPathKeyName() {
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> anno1ElementValues = new HashMap<>();
		Mockito.doReturn(anno1ElementValues).when(anno).getElementValues();
		addAnnotationElementValues(anno1ElementValues, "path", METHOD_PATH);
		addAnnotationElementValues(anno1ElementValues, "method", RequestMethod.GET);

		RequestMappingModel expected = new RequestMappingModel().withOperation(Operation.get).withPath(METHOD_PATH);
		assertEquals(expected, translator.getRequestMappingModel(anno));
		Mockito.verify(anno, Mockito.times(3)).getElementValues();
	}

	@Test
	public void testGetRequestMappingModelWithValueKeyName() {
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> anno1ElementValues = new HashMap<>();
		Mockito.doReturn(anno1ElementValues).when(anno).getElementValues();
		addAnnotationElementValues(anno1ElementValues, "value", METHOD_PATH);
		addAnnotationElementValues(anno1ElementValues, "method", RequestMethod.GET);

		RequestMappingModel expected = new RequestMappingModel().withOperation(Operation.get).withPath(METHOD_PATH);
		
		// call under test
		assertEquals(expected, translator.getRequestMappingModel(anno));
		Mockito.verify(anno, Mockito.times(3)).getElementValues();
	}

	@Test
	public void testGetRequestMappingModelWithIncorrectMethodValue() {
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoElementValues = new HashMap<>();
		Mockito.doReturn(annoElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoElementValues, "method", RequestMethod.PATCH);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getRequestMappingModel(anno);
		});
		
		assertEquals("No operation found for RequestMethod PATCH", exception.getMessage());
		Mockito.verify(anno, Mockito.times(2)).getElementValues();
	}
	
	@Test
	public void getRequestMappingModelWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getRequestMappingModel(null);
		});
		assertEquals("Annotation is required.", exception.getMessage());
	}

	@Test
	public void testGetHttpStatusCodeWithUnhandledObject() {
		// call under test
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getHttpStatusCode("STRING");
		});
		assertEquals("No enum constant org.springframework.http.HttpStatus.STRING", exception.getMessage());
	}

	@Test
	public void testGetHttpstatusCodeWithUnhandledStatus() {
		// call under test
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getHttpStatusCode("ACCEPTED");
		});
		assertEquals("Could not translate HttpStatus for status 202 ACCEPTED", exception.getMessage());
	}

	@Test
	public void testGetHttpStatusCodeWithOkStatus() {
		assertEquals(200, translator.getHttpStatusCode("OK"));
	}

	@Test
	public void testGetRequestBody() {
		List<VariableElement> parameters = new ArrayList<>();

		VariableElement param1 = Mockito.mock(VariableElement.class);
		parameters.add(param1);

		VariableElement param2 = Mockito.mock(VariableElement.class);
		Name paramName = Mockito.mock(Name.class);
		when(param2.getSimpleName()).thenReturn(paramName);
		when(paramName.toString()).thenReturn(PARAM_1_NAME);
		TypeMirror type = Mockito.mock(TypeMirror.class);
		when(param2.asType()).thenReturn(type);
		when(type.getKind()).thenReturn(TypeKind.INT);
		parameters.add(param2);
		
		AnnotationMirror mockAnnoMirror = Mockito.mock(AnnotationMirror.class);
		Mockito.doReturn(mockAnnoMirror, mockAnnoMirror).when(translator).getParameterAnnotation(any(VariableElement.class));
		Mockito.doReturn("RequestParam", "RequestBody").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));

		Map<String, String> mockParamToDescription = new HashMap<>();
		mockParamToDescription.put(PARAM_1_NAME, PARAM_1_DESCRIPTION);
		// call under test
		assertEquals(Optional.of(getExpectedRequestBody()),
				translator.getRequestBody(parameters, mockParamToDescription));
		
		Mockito.verify(param2, Mockito.times(1)).getSimpleName();
		Mockito.verify(param2, Mockito.times(1)).asType();
		Mockito.verify(type, Mockito.times(1)).getKind();
		Mockito.verify(translator, Mockito.times(2)).getSimpleAnnotationName(mockAnnoMirror);
	}

	@Test
	public void testGetRequestBodyWithEmptyArray() {
		// call under test
		assertEquals(Optional.empty(), translator.getRequestBody(new ArrayList<>(), new HashMap<>()));
	}

	@Test
	public void testGetParametersWithRequestBodyAnnotation() {
		List<VariableElement> params = new ArrayList<>();
		VariableElement param = Mockito.mock(VariableElement.class);
		params.add(param);
		AnnotationMirror mockAnnoMirror = Mockito.mock(AnnotationMirror.class);
		Mockito.doReturn(mockAnnoMirror).when(translator).getParameterAnnotation(any(VariableElement.class));
		Mockito.doReturn("RequestBody").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));

		// call under test
		assertEquals(new ArrayList<>(), translator.getParameters(params, new HashMap<>()));
		
		Mockito.verify(translator, Mockito.times(1)).getParameterAnnotation(param);
		Mockito.verify(translator, Mockito.times(1)).getSimpleAnnotationName(mockAnnoMirror);
	}

	@Test
	public void testGetParameters() {
		Mockito.doReturn(ParameterLocation.path).when(translator).getParameterLocation(any(VariableElement.class));
		JsonSchema expectedSchema = new JsonSchema();
		expectedSchema.setType(Type.integer);
		expectedSchema.setFormat("int32");
		Mockito.doReturn(expectedSchema).when(translator).getSchema(any(TypeKind.class));

		List<VariableElement> parameters = new ArrayList<>();
		VariableElement param = Mockito.mock(VariableElement.class);
		Name paramName = Mockito.mock(Name.class);
		when(param.getSimpleName()).thenReturn(paramName);
		when(paramName.toString()).thenReturn(PARAM_1_NAME);
		TypeMirror paramType = Mockito.mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.getKind()).thenReturn(TypeKind.INT);
		parameters.add(param);

		Map<String, String> mockParamToDescription = new HashMap<>();
		mockParamToDescription.put(PARAM_1_NAME, PARAM_1_DESCRIPTION);

		// call under test
		assertEquals(getExpectedParameters(), translator.getParameters(parameters, mockParamToDescription));
		
		Mockito.verify(translator, Mockito.times(1)).getParameterLocation(param);
		Mockito.verify(translator, Mockito.times(1)).getSchema(TypeKind.INT);
		Mockito.verify(param, Mockito.times(1)).getSimpleName();
		Mockito.verify(param, Mockito.times(1)).asType();
		Mockito.verify(paramType, Mockito.times(1)).getKind();
	}

	@Test
	public void testGetParametersWithEmptyArray() {
		// call under test
		assertEquals(new ArrayList<>(), translator.getParameters(new ArrayList<>(), new HashMap<>()));
	}

	@Test
	public void testGetParameterLocationWithUnknownAnnotation() {
		AnnotationMirror mockAnnoMirror = Mockito.mock(AnnotationMirror.class);
		Mockito.doReturn(mockAnnoMirror).when(translator).getParameterAnnotation(any(VariableElement.class));
		Mockito.doReturn("UNKNOWN").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		VariableElement mockVarElement = Mockito.mock(VariableElement.class);
		// call under test
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getParameterLocation(mockVarElement);
		});
		assertEquals("Unable to get parameter location with annotation UNKNOWN", exception.getMessage());
		Mockito.verify(translator, Mockito.times(1)).getSimpleAnnotationName(mockAnnoMirror);
		Mockito.verify(translator, Mockito.times(1)).getParameterAnnotation(mockVarElement);
	}

	@Test
	public void testGetParameterLocationWithRequestBodyAnnotation() {
		Mockito.doReturn("RequestBody").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		AnnotationMirror mockAnnoMirror = Mockito.mock(AnnotationMirror.class);
		Mockito.doReturn(mockAnnoMirror).when(translator).getParameterAnnotation(any(VariableElement.class));
		VariableElement mockVarElement = Mockito.mock(VariableElement.class);
		// call under test
		assertEquals(null, translator.getParameterLocation(mockVarElement));
		
		Mockito.verify(translator, Mockito.times(1)).getSimpleAnnotationName(mockAnnoMirror);
		Mockito.verify(translator, Mockito.times(1)).getParameterAnnotation(mockVarElement);
	}

	@Test
	public void testGetParameterLocationWithRequestParamAnnotation() {
		Mockito.doReturn("RequestParam").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		AnnotationMirror mockAnnoMirror = Mockito.mock(AnnotationMirror.class);
		Mockito.doReturn(mockAnnoMirror).when(translator).getParameterAnnotation(any(VariableElement.class));
		VariableElement mockVarElement = Mockito.mock(VariableElement.class);
		
		// call under test
		assertEquals(ParameterLocation.query, translator.getParameterLocation(mockVarElement));
		
		Mockito.verify(translator, Mockito.times(1)).getSimpleAnnotationName(mockAnnoMirror);
		Mockito.verify(translator, Mockito.times(1)).getParameterAnnotation(mockVarElement);
	}

	@Test
	public void testGetParameterLocationWithPathVariableAnnotation() {
		Mockito.doReturn("PathVariable").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		Mockito.doReturn(Mockito.mock(AnnotationMirror.class)).when(translator).getParameterAnnotation(any());
		// call under test
		assertEquals(ParameterLocation.path, translator.getParameterLocation(null));
	}

	@Test
	public void testGetParameterAnnotation() throws Exception {
		VariableElement mockParameter = Mockito.mock(VariableElement.class);
		List<? extends AnnotationMirror> annotationMirrors = Mockito.mock(List.class);
		Mockito.doReturn(annotationMirrors).when(mockParameter).getAnnotationMirrors();
		when(annotationMirrors.size()).thenReturn(1);
		AnnotationMirror annotation = Mockito.mock(AnnotationMirror.class);
		Mockito.doReturn(annotation).when(annotationMirrors).get(0);
		// call under test
		assertEquals(annotation, translator.getParameterAnnotation(mockParameter));
		
		Mockito.verify(mockParameter, Mockito.times(1)).getAnnotationMirrors();
		Mockito.verify(annotationMirrors, Mockito.times(1)).size();
		Mockito.verify(annotationMirrors, Mockito.times(1)).get(0);
	}

	@Test
	public void testGetParameterAnnotationWithAnnotationMirrorsSize2() {
		VariableElement mockParameter = Mockito.mock(VariableElement.class);
		List<? extends AnnotationMirror> annotationMirrors = Mockito.mock(List.class);
		Mockito.doReturn(annotationMirrors).when(mockParameter).getAnnotationMirrors();
		when(annotationMirrors.size()).thenReturn(2);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getParameterAnnotation(mockParameter);
		});
		assertEquals("Each method parameter should have one annotation, this one has 2", exception.getMessage());
		Mockito.verify(mockParameter, Mockito.times(1)).getAnnotationMirrors();
		Mockito.verify(annotationMirrors, Mockito.times(2)).size();
	}

	@Test
	public void testGetParameterAnnotationWithEmptyAnnotationMirrors() throws Exception {
		VariableElement mockParameter = Mockito.mock(VariableElement.class);
		when(mockParameter.getAnnotationMirrors()).thenReturn(new ArrayList<>());
		// call under test
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getParameterAnnotation(mockParameter);
		});
		assertEquals("Each method parameter should have one annotation, this one has 0", exception.getMessage());
		Mockito.verify(mockParameter, Mockito.times(1)).getAnnotationMirrors();
	}

	@Test
	public void testGetParameterAnnotationWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getParameterAnnotation(null);
		});
		assertEquals("Param is required.", exception.getMessage());
	}

	@Test
	public void testGetSimpleAnnotationName() {
		AnnotationMirror annotation = Mockito.mock(AnnotationMirror.class);

		DeclaredType mockDeclaredType = Mockito.mock(DeclaredType.class);
		Element element = Mockito.mock(Element.class);
		when(mockDeclaredType.asElement()).thenReturn(element);
		Name simpleName = Mockito.mock(Name.class);
		when(element.getSimpleName()).thenReturn(simpleName);
		when(simpleName.toString()).thenReturn(ANNOTATION_NAME);

		when(annotation.getAnnotationType()).thenReturn(mockDeclaredType);
		assertEquals(ANNOTATION_NAME, translator.getSimpleAnnotationName(annotation));
		
		Mockito.verify(mockDeclaredType, Mockito.times(1)).asElement();
		Mockito.verify(element, Mockito.times(1)).getSimpleName();
		Mockito.verify(annotation, Mockito.times(1)).getAnnotationType();
	}

	@Test
	public void testGetSchemaWithUnhandledTypeKind() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getSchema(TypeKind.BOOLEAN);
		});
		assertEquals("Type is unknown for type _boolean", exception.getMessage());
	}

	@Test
	public void testGetSchemaWithStringTypeKind() {
		JsonSchema expectedSchema = new JsonSchema();
		expectedSchema.setType(Type.string);
		// call under test
		assertEquals(expectedSchema, translator.getSchema(TypeKind.DECLARED));
	}

	@Test
	public void testGetSchemaWithIntTypeKind() {
		JsonSchema expectedSchema = new JsonSchema();
		expectedSchema.setType(Type.integer);
		expectedSchema.setFormat("int32");
		// call under test
		assertEquals(expectedSchema, translator.getSchema(TypeKind.INT));
	}

	@Test
	public void testGetSchemaWithNullTypeKind() {
		// call under test
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getSchema(null);
		});
		assertEquals("TypeKind is required.", exception.getMessage());
	}

	@Test
	public void testGetTypeForUnhandledType() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getType(TypeKind.EXECUTABLE);
		});
		assertEquals("Unable to translate TypeKind EXECUTABLE", exception.getMessage());
	}

	@Test
	public void testGetTypeForString() {
		assertEquals(Type.string, translator.getType(TypeKind.DECLARED));
	}

	@Test
	public void testGetTypeForArray() {
		assertEquals(Type.array, translator.getType(TypeKind.ARRAY));
	}

	@Test
	public void testGetTypeForNumber() {
		// do all cases here.
		assertEquals(Type.number, translator.getType(TypeKind.FLOAT));
		assertEquals(Type.number, translator.getType(TypeKind.LONG));
		assertEquals(Type.number, translator.getType(TypeKind.DOUBLE));
	}

	@Test
	public void testGetTypeForBoolean() {
		// do all cases here.
		assertEquals(Type._boolean, translator.getType(TypeKind.BOOLEAN));
	}

	@Test
	public void testGetTypeForInteger() {
		// do all cases here.
		assertEquals(Type.integer, translator.getType(TypeKind.INT));
	}

	@Test
	public void testGetParameterToDescription() {
		List<DocTree> blockTags = new ArrayList<>();
		blockTags.add(getParamTree(PARAM_1_NAME, PARAM_1_DESCRIPTION));
		blockTags.add(getParamTree(PARAM_2_NAME, PARAM_2_DESCRIPTION));

		Map<String, String> expectedParameterToDescription = new HashMap<>();
		expectedParameterToDescription.put(PARAM_1_NAME, PARAM_1_DESCRIPTION);
		expectedParameterToDescription.put(PARAM_2_NAME, PARAM_2_DESCRIPTION);
		// call under test
		assertEquals(expectedParameterToDescription, translator.getParameterToDescription(blockTags));
	}

	@Test
	public void testGetParameterToDescriptionWithEmptyDescription() {
		List<DocTree> blockTags = new ArrayList<>();
		ParamTree param = Mockito.mock(ParamTree.class);
		when(param.getKind()).thenReturn(DocTree.Kind.PARAM);
		List<? extends DocTree> description = Mockito.mock(List.class);
		Mockito.doReturn(description).when(param).getDescription();
		when(description.isEmpty()).thenReturn(true);
		blockTags.add(param);

		// call under test
		assertEquals(new LinkedHashMap<>(), translator.getParameterToDescription(blockTags));
		
		Mockito.verify(param, Mockito.times(1)).getKind();
		Mockito.verify(description, Mockito.times(1)).isEmpty();
	}

	@Test
	public void testGetParameterToDescriptionWithoutParamTreeInBlockTags() {
		ReturnTree mockReturnComment = Mockito.mock(ReturnTree.class);
		when(mockReturnComment.getKind()).thenReturn(DocTree.Kind.RETURN);
		// call under test
		assertEquals(new LinkedHashMap<>(),
				translator.getParameterToDescription(new ArrayList<>(Arrays.asList(mockReturnComment))));
		
		Mockito.verify(mockReturnComment, Mockito.times(1)).getKind();
	}

	@Test
	public void testGetParameterToDescriptionWithNullBlockTags() {
		// call under test
		assertEquals(new LinkedHashMap<>(), translator.getParameterToDescription(null));
	}

	@Test
	public void testGetParameterToDescriptionWithEmptyBlockTags() {
		// call under test
		assertEquals(new LinkedHashMap<>(), translator.getParameterToDescription(new ArrayList<>()));
	}

	@Test
	public void testGetReturnCommentWithNull() {
		// call under test
		assertEquals(Optional.empty(), translator.getReturnComment(null));
	}

	@Test
	public void testGetReturnCommentWithEmptyArray() {
		// call under test
		assertEquals(Optional.empty(), translator.getReturnComment(new ArrayList<>()));
	}

	@Test
	public void testGetReturnCommentWithoutReturnTreeInArray() {
		ParamTree mockParamComment = Mockito.mock(ParamTree.class);
		when(mockParamComment.getKind()).thenReturn(DocTree.Kind.PARAM);
		// call under test
		assertEquals(Optional.empty(), translator.getReturnComment(new ArrayList<>(Arrays.asList(mockParamComment))));
		
		Mockito.verify(mockParamComment, Mockito.times(1)).getKind();
	}

	@Test
	public void testGetReturnCommentWithEmptyDescription() {
		List<DocTree> blockTags = new ArrayList<>();
		ReturnTree mockReturnComment = Mockito.mock(ReturnTree.class);
		when(mockReturnComment.getKind()).thenReturn(DocTree.Kind.RETURN);
		when(mockReturnComment.getDescription()).thenReturn(new ArrayList<>());
		blockTags.add(mockReturnComment);

		assertEquals(Optional.empty(), translator.getReturnComment(blockTags));
		
		Mockito.verify(mockReturnComment, Mockito.times(1)).getKind();
		Mockito.verify(mockReturnComment, Mockito.times(1)).getDescription();
	}

	@Test
	public void testGetReturnComment() {
		List<DocTree> blockTags = new ArrayList<>();
		ReturnTree mockReturnComment = Mockito.mock(ReturnTree.class);
		when(mockReturnComment.getKind()).thenReturn(DocTree.Kind.RETURN);
		List<? extends DocTree> descriptions = Mockito.mock(List.class);
		when(descriptions.toString()).thenReturn(METHOD_RETURN_COMMENT);
		Mockito.doReturn(descriptions).when(mockReturnComment).getDescription();
		blockTags.add(mockReturnComment);

		// call under test
		assertEquals(Optional.of(METHOD_RETURN_COMMENT), translator.getReturnComment(blockTags));
		
		Mockito.verify(mockReturnComment, Mockito.times(1)).getKind();
		Mockito.verify(mockReturnComment, Mockito.times(2)).getDescription();
	}

	@Test
	public void testGetBehaviorCommentWithNull() {
		// call under test
		assertEquals(Optional.empty(), translator.getBehaviorComment(null));
	}

	@Test
	public void testGetBehaviorCommentWithEmptyArray() {
		// call under test
		assertEquals(Optional.empty(), translator.getBehaviorComment(new ArrayList<>()));
	}

	@Test
	public void testGetBehaviorComment() {
		List<? extends DocTree> fullBody = Mockito.mock(List.class);
		when(fullBody.isEmpty()).thenReturn(false);
		when(fullBody.toString()).thenReturn(METHOD_BEHAVIOR_COMMENT);
		// call under test
		assertEquals(Optional.of(METHOD_BEHAVIOR_COMMENT), translator.getBehaviorComment(fullBody));
		
		Mockito.verify(fullBody, Mockito.times(1)).isEmpty();
	}
}