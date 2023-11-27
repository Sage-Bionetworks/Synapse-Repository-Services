package org.sagebionetworks.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
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
import org.sagebionetworks.javadoc.velocity.schema.SchemaUtils;
import org.sagebionetworks.openapi.pet.Husky;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
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

import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.DocletEnvironment;

@ExtendWith(MockitoExtension.class)
public class ControllerToControllerModelTranslatorTest {
	@Mock
	ExecutableElement mockMethod;
	@Mock
	Name mockName;
	@Mock
	DocTrees mockDocTrees;
	@Mock
	Reporter mockReporter;
	@Mock
	VariableElement mockParameter;
	@Mock
	TypeMirror mockParameterType;
	@Mock
	TypeMirror mockGenericType;
	@Mock
	DeclaredType mockGenericDeclaredType;
	@Mock
	TypeElement mockTypeElement;
	@Mock
	AnnotationMirror mockAnnotationMirror;

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
	private final String CONTROLLER_DESCRIPTION = "CONTROLLER_DESCRIPTION";
	private final String MOCK_CLASS_NAME = "CLASS_NAME";

	@BeforeEach
	private void setUp() {
		this.translator = spy(new ControllerToControllerModelTranslator());
	}

	private List<ParameterModel> getExpectedParameters() {
		List<ParameterModel> expectedParameters = new ArrayList<>();

		JsonSchema expectedSchema = new JsonSchema();
		expectedSchema.setType(Type.integer);
		expectedSchema.setFormat("int32");
		ParameterModel expectedParameter = new ParameterModel().withDescription(PARAM_1_DESCRIPTION)
				.withIn(ParameterLocation.path).withName(PARAM_1_NAME).withRequired(true).withId(MOCK_CLASS_NAME);
		expectedParameters.add(expectedParameter);

		return expectedParameters;
	}

	public RequestBodyModel getExpectedRequestBody() {
		JsonSchema expectedSchema = new JsonSchema();
		expectedSchema.setType(Type.integer);
		expectedSchema.setFormat("int32");
		return new RequestBodyModel().withDescription(PARAM_1_DESCRIPTION).withRequired(true).withId(MOCK_CLASS_NAME);
	}

	private ResponseModel getExpectedResponseModel() {
		JsonSchema schema = new JsonSchema();
		schema.setType(Type.integer);
		schema.setFormat("int32");
		return new ResponseModel().withDescription(METHOD_RETURN_COMMENT).withStatusCode(200).withId(MOCK_CLASS_NAME);
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
		ExecutableElement key = mock(ExecutableElement.class);
		Name simpleName = mock(Name.class);
		when(key.getSimpleName()).thenReturn(simpleName);
		when(simpleName.toString()).thenReturn(keyName);

		AnnotationValue annotationValue = mock(AnnotationValue.class);
		when(annotationValue.getValue()).thenReturn(value);

		elementValues.put(key, annotationValue);
	}

	private ParamTree getParamTree(String parameterName, String paramDescription) {
		ParamTree param = mock(ParamTree.class);
		when(param.getKind()).thenReturn(DocTree.Kind.PARAM);

		IdentifierTree paramName = mock(IdentifierTree.class);
		when(paramName.toString()).thenReturn(parameterName);
		when(param.getName()).thenReturn(paramName);
		List<? extends DocTree> description = mock(List.class);
		when(description.isEmpty()).thenReturn(false);
		when(description.toString()).thenReturn(paramDescription);
		doReturn(description).when(param).getDescription();

		return param;
	}

	@Test
	public void testExtractControllerModels() {
		DocletEnvironment env = mock(DocletEnvironment.class);
		TypeElement element1 = mock(TypeElement.class);
		doReturn(ElementKind.CLASS).when(element1).getKind();
		TypeElement element2 = mock(TypeElement.class);
		doReturn(ElementKind.CLASS).when(element2).getKind();
		Set<? extends Element> includedElements = new LinkedHashSet<>(Arrays.asList(element1, element2));
		doReturn(includedElements).when(env).getIncludedElements();
		doReturn(new ArrayList<>(includedElements)).when(translator).getControllers(any());

		DocTrees docTrees = mock(DocTrees.class);
		doReturn(docTrees).when(env).getDocTrees();
		Map<String, ObjectSchema> schemaMap = new HashMap<>();

		ControllerModel controller1 = new ControllerModel().withDescription("CONTROLLER_1");
		ControllerModel controller2 = new ControllerModel().withDescription("CONTROLLER_2");
		doReturn(controller1, controller2).when(translator).translate(any(TypeElement.class),
				any(DocTrees.class), any(Map.class), any());

		Reporter reporter = mock(Reporter.class);
		assertEquals(new ArrayList<>(Arrays.asList(controller1, controller2)),
				translator.extractControllerModels(env, schemaMap, reporter));

		InOrder inOrder = Mockito.inOrder(translator);
		inOrder.verify(translator).translate(element1, docTrees, schemaMap, reporter);
		inOrder.verify(translator).translate(element2, docTrees, schemaMap, reporter);
		verify(translator).getControllers(any());
	}

	@Test
	public void testExtractControllerModelsWithNoClassElements() {
		DocletEnvironment env = mock(DocletEnvironment.class);
		TypeElement element1 = mock(TypeElement.class);
		doReturn(ElementKind.INTERFACE).when(element1).getKind();
		TypeElement element2 = mock(TypeElement.class);
		doReturn(ElementKind.INTERFACE).when(element2).getKind();
		doReturn(new HashSet<>(Arrays.asList(element1, element2))).when(env).getIncludedElements();

		Reporter reporter = mock(Reporter.class);
		assertEquals(new ArrayList<>(), translator.extractControllerModels(env, new HashMap<>(), reporter));
	}

	@Test
	public void testExtractControllersModelsWithEmptyIncludedElements() {
		DocletEnvironment env = mock(DocletEnvironment.class);
		doReturn(new HashSet<>()).when(env).getIncludedElements();

		Reporter reporter = mock(Reporter.class);
		assertEquals(new ArrayList<>(), translator.extractControllerModels(env, new HashMap<>(), reporter));
	}

	@Test
	public void testGetControllers() {
		TypeElement element1 = mock(TypeElement.class);
		TypeElement element2 = mock(TypeElement.class);
		Set<TypeElement> files = new LinkedHashSet<>(Arrays.asList(element1, element2));

		doReturn(true, false).when(translator).isController(any());

		// call under test
		assertEquals(new ArrayList<>(Arrays.asList(element1)), translator.getControllers(files));
		verify(translator, Mockito.times(2)).isController(any());
		InOrder inOrder = Mockito.inOrder(translator);
		inOrder.verify(translator).isController(element1);
		inOrder.verify(translator).isController(element2);
	}

	@Test
	public void testIsControllerWithControllerElement() {
		TypeElement file = mock(TypeElement.class);
		doReturn(ElementKind.CLASS).when(file).getKind();

		AnnotationMirror annotation1 = mock(AnnotationMirror.class);
		AnnotationMirror annotation2 = mock(AnnotationMirror.class);
		doReturn(new ArrayList<>(Arrays.asList(annotation1, annotation2))).when(file).getAnnotationMirrors();

		doReturn("ANNOTATION_NAME", ControllerInfo.class.getSimpleName()).when(translator)
				.getSimpleAnnotationName(any());

		// call under test
		assertTrue(translator.isController(file));
		verify(translator, Mockito.times(2)).getSimpleAnnotationName(any());
		InOrder inOrder = Mockito.inOrder(translator);
		inOrder.verify(translator).getSimpleAnnotationName(annotation1);
		inOrder.verify(translator).getSimpleAnnotationName(annotation2);
	}

	@Test
	public void testIsControllerWithNonControllerElement() {
		TypeElement file = mock(TypeElement.class);
		doReturn(ElementKind.CLASS).when(file).getKind();

		AnnotationMirror annotation1 = mock(AnnotationMirror.class);
		AnnotationMirror annotation2 = mock(AnnotationMirror.class);
		doReturn(new ArrayList<>(Arrays.asList(annotation1, annotation2))).when(file).getAnnotationMirrors();

		doReturn("ANNOTATION_NAME_1", "ANNOTATION_NAME_2").when(translator).getSimpleAnnotationName(any());

		// call under test
		assertFalse(translator.isController(file));
		verify(translator, Mockito.times(2)).getSimpleAnnotationName(any());
		InOrder inOrder = Mockito.inOrder(translator);
		inOrder.verify(translator).getSimpleAnnotationName(annotation1);
		inOrder.verify(translator).getSimpleAnnotationName(annotation2);
	}

	@Test
	public void testIsControllerWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.isController(null);
		});
		assertEquals("file is required.", exception.getMessage());
	}

	@Test
	public void testTranslate() {
		ControllerModel expectedControllerModel = new ControllerModel().withDisplayName(CONTROLLER_NAME)
				.withMethods(getExpectedMethods()).withPath(CONTROLLER_PATH).withDescription(CONTROLLER_DESCRIPTION);

		TypeElement controller = mock(TypeElement.class);
		List<Element> elements = new ArrayList<>();
		doReturn(elements).when(controller).getEnclosedElements();
		List<AnnotationMirror> annoMirrors = new ArrayList<>();
		doReturn(annoMirrors).when(controller).getAnnotationMirrors();
		doReturn(getExpectedMethods()).when(translator).getMethods(any(List.class), any(DocTrees.class),
				any(Map.class), any());
		doReturn(new ControllerInfoModel().withDisplayName(CONTROLLER_NAME).withPath(CONTROLLER_PATH))
				.when(translator).getControllerInfoModel(any(List.class));
		doReturn(CONTROLLER_DESCRIPTION).when(translator).getControllerDescription(any(DocCommentTree.class));

		Map<String, ObjectSchema> schemaMap = new HashMap<>();

		DocTrees mockDocTree = mock(DocTrees.class);
		DocCommentTree mockDocCommentTree = mock(DocCommentTree.class);
		doReturn(mockDocCommentTree).when(mockDocTree).getDocCommentTree(any(TypeElement.class));

		Reporter reporter = mock(Reporter.class);
		doNothing().when(reporter).print(any(), any());
		// call under test
		assertEquals(expectedControllerModel, translator.translate(controller, mockDocTree, schemaMap, reporter));

		verify(controller).getEnclosedElements();
		verify(controller).getAnnotationMirrors();
		verify(translator).getMethods(elements, mockDocTree, schemaMap, reporter);
		verify(translator).getControllerInfoModel(annoMirrors);
		verify(translator).getControllerDescription(mockDocCommentTree);
	}

	@Test
	public void testGetControllerDescription() {
		DocCommentTree mockDocCommentTree = mock(DocCommentTree.class);
		List<DocTree> fullBody = new ArrayList<>();
		doReturn(fullBody).when(mockDocCommentTree).getFullBody();
		Optional<String> result = Optional.of(CONTROLLER_DESCRIPTION);
		doReturn(result).when(translator).getBehaviorComment(any(List.class));

		assertEquals(CONTROLLER_DESCRIPTION, translator.getControllerDescription(mockDocCommentTree));
		verify(translator).getBehaviorComment(fullBody);
		verify(mockDocCommentTree).getFullBody();
	}

	@Test
	public void testGetControllerDescriptionWithMissingComment() {
		DocCommentTree mockDocCommentTree = mock(DocCommentTree.class);
		List<DocTree> fullBody = new ArrayList<>();
		doReturn(fullBody).when(mockDocCommentTree).getFullBody();
		Optional<String> result = Optional.empty();
		doReturn(result).when(translator).getBehaviorComment(any(List.class));

		assertEquals(null, translator.getControllerDescription(mockDocCommentTree));
		verify(translator).getBehaviorComment(fullBody);
		verify(mockDocCommentTree).getFullBody();
	}

	@Test
	public void testGetControllerDescriptionWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getControllerDescription(null);
		});
		assertEquals("controllerTree is required.", exception.getMessage());
	}

	@Test
	public void testGetControllerInfoModel() {
		List<AnnotationMirror> annotations = new ArrayList<>();
		AnnotationMirror anno = mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoElementValues = new HashMap<>();
		doReturn(annoElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoElementValues, "path", CONTROLLER_PATH);
		addAnnotationElementValues(annoElementValues, "displayName", CONTROLLER_NAME);
		annotations.add(anno);
		doReturn("ControllerInfo").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));

		ControllerInfoModel controllerInfo = new ControllerInfoModel().withDisplayName(CONTROLLER_NAME)
				.withPath(CONTROLLER_PATH);
		assertEquals(controllerInfo, translator.getControllerInfoModel(annotations));

		verify(anno, Mockito.times(3)).getElementValues();
		verify(translator).getSimpleAnnotationName(anno);
	}

	@Test
	public void testGetControllerInfoModelWithUnknownAnnotation() {
		List<AnnotationMirror> annotations = new ArrayList<>();
		AnnotationMirror anno = mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoElementValues = new HashMap<>();
		doReturn(annoElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoElementValues, "UNKNOWN", "UNKNOWN");
		annotations.add(anno);
		doReturn("ControllerInfo").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getControllerInfoModel(annotations);
		});

		assertEquals("controllerInfo.path is required.", exception.getMessage());

		verify(anno, Mockito.times(2)).getElementValues();
		verify(translator).getSimpleAnnotationName(anno);
	}

	@Test
	public void testGetControllerInfoModelMissingPathAndDisplayName() {
		List<AnnotationMirror> annotations = new ArrayList<>();
		AnnotationMirror anno = mock(AnnotationMirror.class);
		doReturn(new HashMap<>()).when(anno).getElementValues();
		annotations.add(anno);
		doReturn("ControllerInfo").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getControllerInfoModel(annotations);
		});

		assertEquals("controllerInfo.path is required.", exception.getMessage());
		verify(anno).getElementValues();
		verify(translator).getSimpleAnnotationName(anno);
	}

	@Test
	public void testGetControllerInfoModelMissingPath() {
		List<AnnotationMirror> annotations = new ArrayList<>();
		AnnotationMirror anno = mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoElementValues = new HashMap<>();
		doReturn(annoElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoElementValues, "displayName", CONTROLLER_NAME);
		annotations.add(anno);
		doReturn("ControllerInfo").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getControllerInfoModel(annotations);
		});

		assertEquals("controllerInfo.path is required.", exception.getMessage());

		verify(anno, Mockito.times(2)).getElementValues();
		verify(translator).getSimpleAnnotationName(anno);
	}

	@Test
	public void testGetControllerInfoModelMissingDisplayName() {
		List<AnnotationMirror> annotations = new ArrayList<>();
		AnnotationMirror anno = mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoElementValues = new HashMap<>();
		doReturn(annoElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoElementValues, "path", CONTROLLER_PATH);
		annotations.add(anno);
		doReturn("ControllerInfo").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getControllerInfoModel(annotations);
		});

		assertEquals("controllerInfo.displayName is required.", exception.getMessage());

		verify(anno, Mockito.times(2)).getElementValues();
		verify(translator).getSimpleAnnotationName(anno);
	}

	@Test
	public void testGetControllerInfoModelWithoutControllerInfoAnnotation() {
		List<AnnotationMirror> annotations = new ArrayList<>();
		AnnotationMirror anno = mock(AnnotationMirror.class);
		annotations.add(anno);

		doReturn("WRONG_ANNOTATION").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getControllerInfoModel(annotations);
		});

		assertEquals("ControllerInfo annotation is not present in annotations.", exception.getMessage());
		verify(translator).getSimpleAnnotationName(anno);
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
		DocTrees docTrees = mock(DocTrees.class);
		DocCommentTree mockDocCommentTree = mock(DocCommentTree.class);
		when(docTrees.getDocCommentTree(any(Element.class))).thenReturn(mockDocCommentTree);
		List<DocTree> blockTags = new ArrayList<>();
		List<DocTree> fullBody = new ArrayList<>();
		doReturn(blockTags).when(mockDocCommentTree).getBlockTags();
		doReturn(fullBody).when(mockDocCommentTree).getFullBody();

		List<Element> enclosedElements = new ArrayList<>();
		ExecutableElement method = mock(ExecutableElement.class);
		List<AnnotationMirror> annoMirrors = new ArrayList<>();
		doReturn(annoMirrors).when(method).getAnnotationMirrors();
		List<VariableElement> parameters = new ArrayList<>();
		doReturn(parameters).when(method).getParameters();
		TypeMirror returnType = mock(TypeMirror.class);
		Name methodName = mock(Name.class);
		when(method.getSimpleName()).thenReturn(methodName);
		when(methodName.toString()).thenReturn(METHOD_NAME);
		when(method.getKind()).thenReturn(ElementKind.METHOD);
		enclosedElements.add(method);

		// Mock translator method calls
		Map<String, String> parameterToDescription = new HashMap<>();
		parameterToDescription.put(PARAM_1_NAME, PARAM_1_DESCRIPTION);
		doReturn(parameterToDescription).when(translator).getParameterToDescription(any(List.class));

		Map<Class, Object> annotationToModel = new HashMap<>();
		RequestMappingModel requestMapping = new RequestMappingModel().withOperation(Operation.get)
				.withPath(METHOD_PATH);
		annotationToModel.put(RequestMapping.class, requestMapping);
		ResponseStatusModel responseStatus = new ResponseStatusModel().withStatusCode(HttpStatus.OK.value());
		annotationToModel.put(ResponseStatus.class, responseStatus);
		doReturn(annotationToModel).when(translator).getAnnotationToModel(any(List.class));

		Map<String, ObjectSchema> schemaMap = new HashMap<>();

		JsonSchema expectedRequestBodySchema = new JsonSchema();
		expectedRequestBodySchema.setType(Type.integer);
		expectedRequestBodySchema.setFormat("int32");
		RequestBodyModel requestBody = new RequestBodyModel().withDescription(PARAM_1_DESCRIPTION).withRequired(true)
				.withId(MOCK_CLASS_NAME);
		doReturn(Optional.of(requestBody)).when(translator).getRequestBody(any(List.class), any(Map.class),
				any(Map.class));

		doReturn(Optional.of(METHOD_BEHAVIOR_COMMENT)).when(translator).getBehaviorComment(any(List.class));
		doReturn(METHOD_PATH).when(translator).getMethodPath(any(RequestMappingModel.class));
		doReturn(getExpectedParameters()).when(translator).getParameters(any(List.class), any(Map.class),
				any(Map.class));
		doReturn(getExpectedResponseModel()).when(translator).getResponseModel(any(ExecutableElement.class),
				any(List.class), any(Map.class), any(Map.class));

		Reporter reporter = mock(Reporter.class);
		doNothing().when(reporter).print(any(), any());
		// call under test
		assertEquals(getExpectedMethods(), translator.getMethods(enclosedElements, docTrees, schemaMap, reporter));

		verify(docTrees).getDocCommentTree(method);
		verify(mockDocCommentTree).getFullBody();
		verify(translator).getParameterToDescription(blockTags);
		verify(translator).getAnnotationToModel(annoMirrors);
		verify(translator).getRequestBody(parameters, parameterToDescription, schemaMap);
		verify(translator).getBehaviorComment(blockTags);
		verify(translator).getMethodPath(requestMapping);
		verify(translator).getParameters(parameters, parameterToDescription, schemaMap);
		verify(translator).getResponseModel(method, blockTags, annotationToModel, schemaMap);
	}

	@Test
	public void testGetMethodsWithEmptyDescriptionAndRequestBody() {
		DocTrees docTrees = mock(DocTrees.class);
		DocCommentTree mockDocCommentTree = mock(DocCommentTree.class);
		when(docTrees.getDocCommentTree(any(Element.class))).thenReturn(mockDocCommentTree);
		List<DocTree> blockTags = new ArrayList<>();
		List<DocTree> fullBody = new ArrayList<>();
		doReturn(blockTags).when(mockDocCommentTree).getBlockTags();
		doReturn(fullBody).when(mockDocCommentTree).getFullBody();

		List<Element> enclosedElements = new ArrayList<>();
		ExecutableElement method = mock(ExecutableElement.class);
		List<AnnotationMirror> annoMirrors = new ArrayList<>();
		doReturn(annoMirrors).when(method).getAnnotationMirrors();
		List<VariableElement> parameters = new ArrayList<>();
		doReturn(parameters).when(method).getParameters();
		Name methodName = mock(Name.class);
		when(method.getSimpleName()).thenReturn(methodName);
		when(methodName.toString()).thenReturn(METHOD_NAME);
		when(method.getKind()).thenReturn(ElementKind.METHOD);
		enclosedElements.add(method);

		// Mock translator method calls
		Map<String, String> parameterToDescription = new HashMap<>();
		parameterToDescription.put(PARAM_1_NAME, PARAM_1_DESCRIPTION);
		doReturn(parameterToDescription).when(translator).getParameterToDescription(any(List.class));
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		Map<Class, Object> annotationToModel = new HashMap<>();
		RequestMappingModel requestMapping = new RequestMappingModel().withOperation(Operation.get)
				.withPath(METHOD_PATH);
		annotationToModel.put(RequestMapping.class, requestMapping);
		ResponseStatusModel responseStatus = new ResponseStatusModel().withStatusCode(HttpStatus.OK.value());
		annotationToModel.put(ResponseStatus.class, responseStatus);
		doReturn(annotationToModel).when(translator).getAnnotationToModel(any(List.class));
		doReturn(Optional.empty()).when(translator).getRequestBody(any(List.class), any(Map.class),
				any(Map.class));
		doReturn(Optional.empty()).when(translator).getBehaviorComment(any(List.class));
		doReturn(METHOD_PATH).when(translator).getMethodPath(any(RequestMappingModel.class));
		doReturn(getExpectedParameters()).when(translator).getParameters(any(List.class), any(Map.class),
				any(Map.class));
		doReturn(getExpectedResponseModel()).when(translator).getResponseModel(any(ExecutableElement.class),
				any(List.class), any(Map.class), any(Map.class));

		List<MethodModel> expectedMethods = new ArrayList<>();
		MethodModel expectedMethod = new MethodModel().withPath(METHOD_PATH).withName(METHOD_NAME)
				.withOperation(Operation.get).withParameters(getExpectedParameters())
				.withResponse(getExpectedResponseModel());
		expectedMethods.add(expectedMethod);

		Reporter reporter = mock(Reporter.class);
		doNothing().when(reporter).print(any(), any());
		// call under test
		assertEquals(expectedMethods, translator.getMethods(enclosedElements, docTrees, schemaMap, reporter));

		verify(docTrees).getDocCommentTree(method);
		verify(mockDocCommentTree, times(2)).getBlockTags();
		verify(mockDocCommentTree).getFullBody();
		verify(translator).getParameterToDescription(blockTags);
		verify(translator).getAnnotationToModel(annoMirrors);
		verify(translator).getRequestBody(parameters, parameterToDescription, schemaMap);
		verify(translator).getBehaviorComment(blockTags);
		verify(translator).getMethodPath(requestMapping);
		verify(translator).getParameters(parameters, parameterToDescription, schemaMap);
		verify(translator).getResponseModel(method, blockTags, annotationToModel, schemaMap);
	}

	@Test
	public void testGetMethodsWhenMissingRequestMappingAnnotation() {
		DocTrees docTrees = mock(DocTrees.class);
		DocCommentTree mockDocCommentTree = mock(DocCommentTree.class);
		when(docTrees.getDocCommentTree(any(Element.class))).thenReturn(mockDocCommentTree);

		List<Element> enclosedElements = new ArrayList<>();
		ExecutableElement method = mock(ExecutableElement.class);
		when(method.getKind()).thenReturn(ElementKind.METHOD);
		when(method.getAnnotationMirrors()).thenReturn(new ArrayList<>());
		Name name = mock(Name.class);
		when(method.getSimpleName()).thenReturn(name);
		when(name.toString()).thenReturn(METHOD_NAME);
		enclosedElements.add(method);

		// Mock translator method calls
		Map<String, String> parameterToDescription = new HashMap<>();
		doReturn(parameterToDescription).when(translator).getParameterToDescription(any(List.class));

		Map<Class, Object> annotationToModel = new HashMap<>();
		doReturn(annotationToModel).when(translator).getAnnotationToModel(any(List.class));

		Map<String, ObjectSchema> schemaMap = new HashMap<>();

		Reporter reporter = mock(Reporter.class);
		doNothing().when(reporter).print(any(), any());
		// Missing RequestMapping annotations
		IllegalStateException exception1 = assertThrows(IllegalStateException.class, () -> {
			translator.getMethods(enclosedElements, docTrees, schemaMap, reporter);
		});
		assertEquals("Method " + METHOD_NAME + " missing RequestMapping annotation.", exception1.getMessage());

		verify(docTrees).getDocCommentTree(method);
		verify(method).getKind();
		verify(method).getAnnotationMirrors();
		verify(method).getSimpleName();
	}

	@Test
	public void testGetMethodsWithDeprecatedAnnotation() {
		List<Element> enclosedElements = new ArrayList<>();
		when(mockMethod.getKind()).thenReturn(ElementKind.METHOD);
		when(mockMethod.getModifiers()).thenReturn(new HashSet<>(Arrays.asList(Modifier.PUBLIC)));
		when(mockMethod.getSimpleName()).thenReturn(mockName);
		when(mockName.toString()).thenReturn(METHOD_NAME);
		enclosedElements.add(mockMethod);
		when(mockMethod.getAnnotation(any())).thenReturn((Annotation) () -> Deprecated.class);
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		doNothing().when(mockReporter).print(any(), any());

		List<MethodModel> expectedMethods = new ArrayList<>();

		// call under test
		assertEquals(expectedMethods, translator.getMethods(enclosedElements, mockDocTrees, schemaMap, mockReporter));

		verify(mockMethod).getKind();
		verify(mockMethod).getSimpleName();
		verify(mockMethod).getModifiers();
	}

	@Test
	public void testGetMethodsWithPrivateModifier() {
		List<Element> enclosedElements = new ArrayList<>();
		when(mockMethod.getKind()).thenReturn(ElementKind.METHOD);
		when(mockMethod.getModifiers()).thenReturn(new HashSet<>(Arrays.asList(Modifier.PRIVATE)));
		enclosedElements.add(mockMethod);
		Map<String, ObjectSchema> schemaMap = new HashMap<>();

		List<MethodModel> expectedMethods = new ArrayList<>();

		// call under test
		assertEquals(expectedMethods, translator.getMethods(enclosedElements, mockDocTrees, schemaMap, mockReporter));
		verify(mockMethod).getModifiers();
	}

	@Test
	public void testGetMethodsWithStaticModifier() {
		List<Element> enclosedElements = new ArrayList<>();
		when(mockMethod.getKind()).thenReturn(ElementKind.METHOD);
		when(mockMethod.getModifiers()).thenReturn(new HashSet<>(Arrays.asList(Modifier.STATIC)));
		enclosedElements.add(mockMethod);
		Map<String, ObjectSchema> schemaMap = new HashMap<>();

		List<MethodModel> expectedMethods = new ArrayList<>();

		// call under test
		assertEquals(expectedMethods, translator.getMethods(enclosedElements, mockDocTrees, schemaMap, mockReporter));
		verify(mockMethod).getModifiers();
	}

	@Test
	public void testGetMethodsWithEmptyEnclosedElements() {
		Reporter reporter = mock(Reporter.class);
		// call under test
		assertEquals(new ArrayList<>(),
				translator.getMethods(new ArrayList<>(), mock(DocTrees.class), new HashMap<>(), reporter));
	}

	@Test
	public void testGetResponseModelWithRedirectedEndpoint() {
		doReturn(true).when(translator).isRedirect(any());
		doReturn("DESCRIPTION").when(translator).getResponseDescription(any(), any());

		ResponseModel response = new ResponseModel();
		doReturn(response).when(translator).generateRedirectedResponseModel(any());

		ExecutableElement method = mock(ExecutableElement.class);
		List<? extends DocTree> blockTags = new ArrayList<>();
		Map<Class, Object> annotationToModel = new HashMap<>();
		Map<String, ObjectSchema> schemaMap = new HashMap<>();

		// call under test
		assertEquals(response, translator.getResponseModel(method, blockTags, annotationToModel, schemaMap));
		verify(translator).isRedirect(method);
		verify(translator).generateRedirectedResponseModel("DESCRIPTION");
		verify(translator).getResponseDescription(blockTags, method);
	}

	@Test
	public void testGetResponseModelWithNonRedirectedEndpoint() {
		doReturn(false).when(translator).isRedirect(any());
		doReturn("DESCRIPTION").when(translator).getResponseDescription(any(), any());

		ResponseModel response = new ResponseModel();
		doReturn(response).when(translator).generateResponseModel(any(), any(), any(), any());

		ExecutableElement method = mock(ExecutableElement.class);
		TypeMirror returnType = mock(TypeMirror.class);
		doReturn(returnType).when(method).getReturnType();
		List<? extends DocTree> blockTags = new ArrayList<>();
		Map<Class, Object> annotationToModel = new HashMap<>();
		Map<String, ObjectSchema> schemaMap = new HashMap<>();

		// call under test
		assertEquals(response, translator.getResponseModel(method, blockTags, annotationToModel, schemaMap));
		verify(translator).isRedirect(method);
		verify(translator).generateResponseModel(returnType, annotationToModel, "DESCRIPTION", schemaMap);
		verify(translator).getResponseDescription(blockTags, method);
	}

	@Test
	public void testGetResponseModelWithNullSchemaMap() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getResponseModel(mock(ExecutableElement.class), new ArrayList<>(), new HashMap<>(),
					null);
		});
		assertEquals("schemaMap is required.", exception.getMessage());
	}

	@Test
	public void testGetResponseModelWithNullAnnotationToModel() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getResponseModel(mock(ExecutableElement.class), new ArrayList<>(), null,
					new HashMap<>());
		});
		assertEquals("annotationToModel is required.", exception.getMessage());
	}

	@Test
	public void testGetResponseModelWithNullBlockTags() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getResponseModel(mock(ExecutableElement.class), null, new HashMap<>(), new HashMap<>());
		});
		assertEquals("blockTags is required.", exception.getMessage());
	}

	@Test
	public void testGetResponseModelWithNullMethod() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getResponseModel(null, new ArrayList<>(), new HashMap<>(), new HashMap<>());
		});
		assertEquals("method is required.", exception.getMessage());
	}

	@Test
	public void testGetResponseDescriptionVoidReturnTypeWithEmptyReturnComment() {
		doReturn(Optional.empty()).when(translator).getReturnComment(any());
		List<? extends DocTree> blockTags = new ArrayList<>();
		ExecutableElement method = mock(ExecutableElement.class);
		TypeMirror returnType = mock(TypeMirror.class);
		doReturn(returnType).when(method).getReturnType();
		doReturn(TypeKind.VOID).when(returnType).getKind();

		// call under test
		assertEquals("Void", translator.getResponseDescription(blockTags, method));
		verify(translator).getReturnComment(blockTags);
	}

	@Test
	public void testGetResponseDescriptionWithEmptyReturnComment() {
		doReturn(Optional.empty()).when(translator).getReturnComment(any());
		List<? extends DocTree> blockTags = new ArrayList<>();
		ExecutableElement method = mock(ExecutableElement.class);
		TypeMirror returnType = mock(TypeMirror.class);
		doReturn(returnType).when(method).getReturnType();
		doReturn(TypeKind.INT).when(returnType).getKind();

		// call under test
		assertEquals("Auto-generated description", translator.getResponseDescription(blockTags, method));
		verify(translator).getReturnComment(blockTags);
	}

	@Test
	public void testGetResponseDescription() {
		doReturn(Optional.of("DESCRIPTION")).when(translator).getReturnComment(any());
		List<? extends DocTree> blockTags = new ArrayList<>();

		// call under test
		assertEquals("DESCRIPTION", translator.getResponseDescription(blockTags, mock(ExecutableElement.class)));
		verify(translator).getReturnComment(blockTags);
	}

	@Test
	public void testGenerateRedirectedResponseModel() {
		String description = "DESCRIPTION";
		ResponseModel expected = new ResponseModel().withDescription(description).withIsRedirected(true);

		// call under test
		assertEquals(expected, translator.generateRedirectedResponseModel(description));
	}

	@Test
	public void testGenerateResponseModel() {
		TypeMirror returnType = mock(TypeMirror.class);
		Map<Class, Object> annotationToModel = new HashMap<>();
		String description = "DESCRIPTION";
		Map<String, ObjectSchema> schemaMap = new HashMap<>();

		doNothing().when(translator).populateSchemaMapForConcreteType(any(), any(), any());
		String returnClassName = "RETURN_CLASS_NAME";
		doReturn(returnClassName).when(returnType).toString();
		doReturn(TypeKind.BOOLEAN).when(returnType).getKind();

		ResponseStatusModel responseStatus = new ResponseStatusModel().withStatusCode(200);
		annotationToModel.put(ResponseStatus.class, responseStatus);

		ResponseModel expected = new ResponseModel().withDescription(description)
				.withStatusCode(responseStatus.getStatusCode()).withId(returnClassName);
		// call under test
		assertEquals(expected, translator.generateResponseModel(returnType, annotationToModel, description, schemaMap));
		verify(translator).populateSchemaMapForConcreteType(returnClassName, TypeKind.BOOLEAN, schemaMap);
	}

	@Test
	public void testGenerateResponseModelWithMissingResponseStatusAnnotation() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.generateResponseModel(mock(TypeMirror.class), new HashMap<>(), "DESCRIPTION",
					new HashMap<>());
		});
		assertEquals("Missing response status in annotationToModel.", exception.getMessage());
	}

	@Test
	public void testIsRedirectWithVoidReturnAndNoRedirectParam() {
		ExecutableElement method = mock(ExecutableElement.class);
		doReturn(new ArrayList<>()).when(method).getParameters();
		TypeMirror returnType = mock(TypeMirror.class);
		doReturn(returnType).when(method).getReturnType();
		doReturn(TypeKind.VOID).when(returnType).getKind();
		doReturn(false).when(translator).containsRedirectParam(any());

		assertFalse(translator.isRedirect(method));
	}

	@Test
	public void testIsRedirect() {
		ExecutableElement method = mock(ExecutableElement.class);
		doReturn(new ArrayList<>()).when(method).getParameters();
		TypeMirror returnType = mock(TypeMirror.class);
		doReturn(returnType).when(method).getReturnType();
		doReturn(TypeKind.VOID).when(returnType).getKind();
		doReturn(true).when(translator).containsRedirectParam(any());
		
		// call under test
		assertTrue(translator.isRedirect(method));
		
		doReturn(TypeKind.INT).when(returnType).getKind();
		// call under test
		assertFalse(translator.isRedirect(method));
		
		doReturn(false).when(translator).containsRedirectParam(any());
		// call under test
		assertFalse(translator.isRedirect(method));
	}
	
	@Test
	public void testContainsRedirectParamWithRedirect() {
		List<VariableElement> params = new ArrayList<>();
		VariableElement param = mock(VariableElement.class);
		Name paramName = mock(Name.class);
		doReturn("redirect").when(paramName).toString();
		doReturn(paramName).when(param).getSimpleName();
		
		TypeMirror paramType = mock(TypeMirror.class);
		doReturn(TypeKind.BOOLEAN).when(paramType).getKind();
		doReturn(paramType).when(param).asType();
		
		params.add(param);
		assertTrue(translator.containsRedirectParam(params));
	}
	
	@Test
	public void testContainsRedirectParamWithoutRedirect() {
		List<VariableElement> params = new ArrayList<>();
		VariableElement param = mock(VariableElement.class);
		Name paramName = mock(Name.class);
		doReturn("WRONG_NAME").when(paramName).toString();
		doReturn(paramName).when(param).getSimpleName();
		
		TypeMirror paramType = mock(TypeMirror.class);
		doReturn(TypeKind.BOOLEAN).when(paramType).getKind();
		doReturn(paramType).when(param).asType();
		
		params.add(param);
		// call under test
		assertFalse(translator.containsRedirectParam(params));
		
		// call under test
		doReturn(TypeKind.DECLARED).when(paramType).getKind();
		doReturn(Boolean.class.getName()).when(paramName).toString();
		assertFalse(translator.containsRedirectParam(params));
		
		doReturn("redirect").when(paramName).toString();
		doReturn(TypeKind.INT).when(paramType).getKind();
		// call under test
		assertFalse(translator.containsRedirectParam(params));
		
		doReturn("WRONG_NAME").when(paramName).toString();
		assertFalse(translator.containsRedirectParam(params));
	}

	@Test
	public void testPopulateSchemaMapWithConcreteType() {
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		String schemaId = "org.sagebionetworks.schemaId";

		doReturn(Optional.empty()).when(translator).getTypeArguments(any(TypeMirror.class));
		doReturn(TypeKind.DECLARED).when(mockParameterType).getKind();
		doNothing().when(translator).populateSchemaMapForConcreteType(any(String.class), any(TypeKind.class), any());

		// call under test
		translator.populateSchemaMap(schemaId, mockParameterType, schemaMap);

		verify(translator).getTypeArguments(mockParameterType);
		verify(mockParameterType).getKind();
		verify(translator).populateSchemaMapForConcreteType(schemaId, TypeKind.DECLARED, schemaMap);
		verify(translator, never()).populateSchemaMapForGenericType(any(), any(), any(), any());
	}

	@Test
	public void testPopulateSchemaMapWithGenericType() {
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		String schemaId = "ListOfString";
		String argumentSchemaId = "org.sagebionetworks.schemaId";

		List<TypeMirror> argumentTypes = List.of(mockParameterType);

		doReturn(TypeKind.DECLARED).when(mockParameterType).getKind();
		doReturn(Optional.of(argumentTypes)).when(translator).getTypeArguments(any(TypeMirror.class));
		doReturn(argumentSchemaId).when(translator).getSchemaIdForType(any(TypeMirror.class));
		doNothing().when(translator).populateSchemaMapForConcreteType(any(String.class), any(TypeKind.class), any());
		doNothing().when(translator).populateSchemaMapForGenericType(any(String.class), any(TypeMirror.class), any(), any());

		// call under test
		translator.populateSchemaMap(schemaId, mockGenericType, schemaMap);

		verify(translator).getTypeArguments(mockGenericType);
		verify(translator).getSchemaIdForType(mockParameterType);
		verify(translator).populateSchemaMapForConcreteType(argumentSchemaId, TypeKind.DECLARED, schemaMap);
		verify(translator).populateSchemaMapForGenericType(schemaId, mockGenericType, mockParameterType, schemaMap);
		verify(mockGenericType, never()).getKind();
	}

	@Test
	public void testPopulateSchemaMapWithNullSchemaId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.populateSchemaMap(null, mockParameterType, new HashMap<>());
		});
		assertEquals("schemaId is required.", exception.getMessage());
	}

	@Test
	public void testPopulateSchemaMapWithNullType() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.populateSchemaMap("", null, new HashMap<>());
		});
		assertEquals("type is required.", exception.getMessage());
	}

	@Test
	public void testPopulateSchemaMapWithNullSchemaMap() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.populateSchemaMap("", mockParameterType, null);
		});
		assertEquals("schemaMap is required.", exception.getMessage());
	}

	@Test
	public void testPopulateSchemaMapForConcreteTypeWithNonPrimitiveType() {
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		// call under test
		translator.populateSchemaMapForConcreteType(Husky.class.getName(), TypeKind.DECLARED, schemaMap);

		Map<String, ObjectSchema> expectedSchemaMap = new HashMap<>();
		SchemaUtils.recursiveAddTypes(expectedSchemaMap, Husky.class.getName(), null);
		assertEquals(expectedSchemaMap, schemaMap);
	}

	@Test
	public void testPopulateSchemaMapForConcreteTypeWithNullSchemaMap() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.populateSchemaMapForConcreteType("", TypeKind.ARRAY, null);
		});
		assertEquals("schemaMap is required.", exception.getMessage());
	}

	@Test
	public void testPopulateSchemaMapForConcreteTypeWithNullType() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.populateSchemaMapForConcreteType("", null, new HashMap<>());
		});
		assertEquals("type is required.", exception.getMessage());
	}

	@Test
	public void testPopulateSchemaMapForConcreteTypeWithNullClassName() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.populateSchemaMapForConcreteType(null, TypeKind.ARRAY, new HashMap<>());
		});
		assertEquals("className is required.", exception.getMessage());
	}

	@Test
	public void testPopulateSchemaMapForGenericTypeWithListType() {
		String schemaId = "ListOfString";
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		ObjectSchema objectSchema = new ObjectSchemaImpl();
		List<TypeMirror> typeArguments = List.of(mockParameterType);

		doReturn("java.util.List").when(translator).getGenericClassName(any(TypeMirror.class));
		doReturn("java.lang.String").when(mockParameterType).toString();
		doReturn(objectSchema).when(translator).generateArrayObjectSchema(any(String.class));

		// call under test
		translator.populateSchemaMapForGenericType(schemaId, mockGenericType, mockParameterType, schemaMap);

		Map<String, ObjectSchema> expectedSchemaMap = new HashMap<>();
		expectedSchemaMap.put(schemaId, objectSchema);

		assertEquals(expectedSchemaMap, schemaMap);

		verify(translator).getGenericClassName(mockGenericType);
		verify(translator).generateArrayObjectSchema("java.lang.String");
		verify(translator, never()).generateObjectSchemaForGenericType(	any(), any(), any());
	}

	@Test
	public void testPopulateSchemaMapForGenericTypeWithCustomType() {
		String schemaId = "PaginatedResultsOfString";
		String genericClassName = "org.sagebionetworks.reflection.model.PaginatedResults";
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		ObjectSchema objectSchema = new ObjectSchemaImpl();

		doReturn(genericClassName).when(translator).getGenericClassName(any(TypeMirror.class));
		doReturn(objectSchema).when(translator).generateObjectSchemaForGenericType(any(String.class), any(String.class), any());

		// call under test
		translator.populateSchemaMapForGenericType(schemaId, mockGenericType, mockParameterType, schemaMap);

		Map<String, ObjectSchema> expectedSchemaMap = new HashMap<>();
		expectedSchemaMap.put(schemaId, objectSchema);

		verify(translator).getGenericClassName(mockGenericType);
		verify(translator).generateObjectSchemaForGenericType(schemaId, genericClassName, mockParameterType);
		assertEquals(expectedSchemaMap, schemaMap);
		verify(translator, never()).generateArrayObjectSchema(any());
	}

	@Test
	public void testPopulateSchemaMapForGenericTypeWithUnsupportedType() {
		doReturn("java.util.HashMap").when(translator).getGenericClassName(any(TypeMirror.class));

		UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> {
			// call under test
			translator.populateSchemaMapForGenericType("", mockGenericType, mockParameterType, new HashMap<>());
		});
		assertEquals("Generic class java.util.HashMap is not supported by the OpenAPI translator", exception.getMessage());
	}

	@Test
	public void testPopulateSchemaMapForGenericTypeWithNullSchemaId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.populateSchemaMapForGenericType(null, mockGenericType, mockParameterType, new HashMap<>());
		});
		assertEquals("schemaId is required.", exception.getMessage());
	}

	@Test
	public void testPopulateSchemaMapForGenericTypeWithNullType() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.populateSchemaMapForGenericType("", null, mockParameterType, new HashMap<>());
		});
		assertEquals("genericType is required.", exception.getMessage());
	}

	@Test
	public void testPopulateSchemaMapForGenericTypeWithNullArgumentType() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.populateSchemaMapForGenericType("", mockGenericType, null, new HashMap<>());
		});
		assertEquals("argumentType is required.", exception.getMessage());
	}

	@Test
	public void testPopulateSchemaMapForGenericTypeWithNullSchemaMap() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.populateSchemaMapForGenericType("", mockGenericType, mockParameterType, null);
		});
		assertEquals("schemaMap is required.", exception.getMessage());
	}

	@Test
	public void testGetSchemaIdForTypeWithGenericType() {
		List<TypeMirror> argumentList = List.of(mockParameterType);

		doReturn(Optional.of(argumentList)).when(translator).getTypeArguments(any(TypeMirror.class));
		doReturn("ListOfString").when(translator).getSchemaIdForGenericType(any((TypeMirror.class)));

		// call under test
		String resultId = translator.getSchemaIdForType(mockGenericType);

		assertEquals("ListOfString", resultId);

		verify(translator).getTypeArguments(mockGenericType);
		verify(translator).getSchemaIdForGenericType(mockGenericType);
	}

	@Test
	public void testGetSchemaIdForTypeWithConcreteType() {
		doReturn(Optional.empty()).when(translator).getTypeArguments(any(TypeMirror.class));
		doReturn("org.sagebionetworks.test").when(mockParameterType).toString();

		// call under test
		String resultId = translator.getSchemaIdForType(mockParameterType);

		assertEquals("org.sagebionetworks.test", resultId);
		verify(translator, never()).getSchemaIdForGenericType(any());
	}

	@Test
	public void testGetSchemaIdForTypeWithNullTypeMirror() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getSchemaIdForType(null);
		});
		assertEquals("typeMirror is required.", exception.getMessage());
	}

	@Test
	public void testGetSchemaIdForGenericType() {
		String genericClassName = "java.util.List";
		String concreteClassName = "java.lang.String";

		doReturn(genericClassName).when(translator).getGenericClassName(any(TypeMirror.class));
		doReturn(Optional.of(List.of(mockParameterType))).when(translator).getTypeArguments(any(TypeMirror.class));
		doReturn(concreteClassName).when(mockParameterType).toString();

		// call under test
		String resultId = translator.getSchemaIdForGenericType(mockGenericType);

		assertEquals("ListOfString", resultId);

		verify(translator).getGenericClassName(mockGenericType);
		verify(translator).getTypeArguments(mockGenericType);
	}

	@Test
	public void testGetSchemaIdForGenericTypeUnknownClass() {
		String genericClassName = "java.util.List";
		String concreteClassName = "java.lang.DoesNotExist";

		doReturn(genericClassName).when(translator).getGenericClassName(any(TypeMirror.class));
		doReturn(Optional.of(List.of(mockParameterType))).when(translator).getTypeArguments(any(TypeMirror.class));
		doReturn(concreteClassName).when(mockParameterType).toString();

		// call under test
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			// call under test
			translator.getSchemaIdForGenericType(mockGenericType);
		});
		assertEquals("java.lang.ClassNotFoundException: java.lang.DoesNotExist", exception.getMessage());
	}

	@Test
	public void testGetSchemaIdForGenericTypeWithNullTypeMirror() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getSchemaIdForGenericType(null);
		});
		assertEquals("genericType is required.", exception.getMessage());
	}

	@Test
	public void testGetTypeArguments() {
		List<TypeMirror> argumentTypes = List.of(mockParameterType);

		doReturn(argumentTypes).when(mockGenericDeclaredType).getTypeArguments();

		// call under test
		Optional<List<? extends TypeMirror>> resultList = translator.getTypeArguments(mockGenericDeclaredType);

		assertEquals(argumentTypes, resultList.get());

		verify(mockGenericDeclaredType).getTypeArguments();
	}

	@Test
	public void testGetTypeArgumentsWithNullTypeMirror() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getTypeArguments(null);
		});
		assertEquals("typeMirror is required.", exception.getMessage());
	}

	@Test
	public void testGenerateObjectSchemaForGenericType() throws JSONObjectAdapterException {
		doReturn("java.lang.String").when(mockParameterType).toString();

		// call under test
		ObjectSchema resultSchema= translator.generateObjectSchemaForGenericType("PaginatedResultsOfString", "org.sagebionetworks.reflection.model.PaginatedResults", mockParameterType);

		String expectedItemsSchemaString = "{\"type\":\"array\",\"items\":{\"type\": \"string\"}}";
		ObjectSchema expectedItemSchema = new ObjectSchemaImpl(new JSONObjectAdapterImpl(expectedItemsSchemaString));

		assertEquals(expectedItemSchema, resultSchema.getProperties().get("results"));
	}

	@Test
	public void testGenerateObjectSchemaForGenericTypeWithNullSchemaId() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.generateObjectSchemaForGenericType(null, "", mockParameterType);
		});
		assertEquals("schemaId is required.", exception.getMessage());
	}

	@Test
	public void testGenerateObjectSchemaForGenericTypeWithNullGenericClassName() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.generateObjectSchemaForGenericType("", null, mockParameterType);
		});
		assertEquals("genericClassName is required.", exception.getMessage());
	}

	@Test
	public void testGenerateObjectSchemaForGenericTypeWithNullArgumentType() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.generateObjectSchemaForGenericType("", "", null);
		});
		assertEquals("argumentType is required.", exception.getMessage());
	}

	@Test
	public void testGenerateArrayObjectSchemaWithString() {
		String typeName = "java.lang.String";
		ObjectSchema itemSchema = new ObjectSchemaImpl();
		itemSchema.setType(TYPE.STRING);
		ObjectSchema expectedSchema = new ObjectSchemaImpl();
		expectedSchema.setType(TYPE.ARRAY);
		expectedSchema.setItems(itemSchema);

		// call under test
		ObjectSchema resultSchema= translator.generateArrayObjectSchema(typeName);

		assertEquals(expectedSchema, resultSchema);
	}

	@Test
	public void testGenerateArrayObjectSchemaWithConcreteClass() {
		String typeName = "org.sagebionetworks.concreteclass";
		ObjectSchema itemSchema = new ObjectSchemaImpl();
		itemSchema.setId(typeName);
		itemSchema.setType(TYPE.OBJECT);
		ObjectSchema expectedSchema = new ObjectSchemaImpl();
		expectedSchema.setType(TYPE.ARRAY);
		expectedSchema.setItems(itemSchema);

		// call under test
		ObjectSchema resultSchema= translator.generateArrayObjectSchema(typeName);

		assertEquals(expectedSchema, resultSchema);
	}

	@Test
	public void testGenerateArrayObjectSchemaWithStringNullTypeName() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.generateArrayObjectSchema(null);
		});
		assertEquals("typeName is required.", exception.getMessage());
	}

	@Test
	public void testGetGenericClassName() {
		doReturn(mockTypeElement).when(mockGenericDeclaredType).asElement();
		doReturn(mockName).when(mockTypeElement).getQualifiedName();
		doReturn("java.util.List").when(mockName).toString();

		// call under test
		String resultName = translator.getGenericClassName(mockGenericDeclaredType);

		assertEquals("java.util.List", resultName);

		verify(mockGenericDeclaredType).asElement();
		verify(mockTypeElement).getQualifiedName();
	}

	@Test
	public void testGetGenericClassNameWithNullGenericType() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getGenericClassName(null);
		});
		assertEquals("genericType is required.", exception.getMessage());
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
	public void testGetMethodPathWithRegularExpression() {
		String fakePath = "/test/{id:.+}/test";
		assertEquals("/test/{id}/test", translator.getMethodPath(new RequestMappingModel().withPath(fakePath)));
	}

	@Test
	public void testAnnotationToModelWithPathAndCodeAnnotations() {
		List<AnnotationMirror> methodAnnotations = new ArrayList<>();

		AnnotationMirror anno1 = mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> anno1ElementValues = new HashMap<>();
		doReturn(anno1ElementValues).when(anno1).getElementValues();
		addAnnotationElementValues(anno1ElementValues, "path", METHOD_PATH);
		addAnnotationElementValues(anno1ElementValues, "method", RequestMethod.GET);

		AnnotationMirror anno2 = mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> anno2ElementValues = new HashMap<>();
		doReturn(anno2ElementValues).when(anno2).getElementValues();
		addAnnotationElementValues(anno2ElementValues, "code", "OK");

		methodAnnotations.add(anno1);
		methodAnnotations.add(anno2);

		doReturn("RequestMapping", "ResponseStatus").when(translator)
				.getSimpleAnnotationName(any(AnnotationMirror.class));

		Map<Class, Object> expectedAnnotationToModel = new LinkedHashMap<>();
		expectedAnnotationToModel.put(RequestMapping.class,
				new RequestMappingModel().withOperation(Operation.get).withPath(METHOD_PATH));
		expectedAnnotationToModel.put(ResponseStatus.class,
				new ResponseStatusModel().withStatusCode(HttpStatus.OK.value()));

		assertEquals(expectedAnnotationToModel, translator.getAnnotationToModel(methodAnnotations));

		verify(anno1, times(3)).getElementValues();
		verify(anno2, times(2)).getElementValues();
		verify(translator, times(2)).getSimpleAnnotationName(any(AnnotationMirror.class));
		InOrder inOrder = inOrder(translator);
		inOrder.verify(translator).getSimpleAnnotationName(anno1);
		inOrder.verify(translator).getSimpleAnnotationName(anno2);
	}

	@Test
	public void testGetAnnotationToModel() {
		List<AnnotationMirror> methodAnnotations = new ArrayList<>();
		AnnotationMirror mockAnnoMirror = mock(AnnotationMirror.class);
		methodAnnotations.add(mockAnnoMirror);
		methodAnnotations.add(mockAnnoMirror);

		RequestMappingModel requestMapping = new RequestMappingModel().withOperation(Operation.get)
				.withPath(METHOD_PATH);
		ResponseStatusModel responseStatus = new ResponseStatusModel().withStatusCode(HttpStatus.OK.value());

		Map<Class, Object> expectedAnnotationToModel = new LinkedHashMap<>();
		expectedAnnotationToModel.put(RequestMapping.class, requestMapping);
		expectedAnnotationToModel.put(ResponseStatus.class, responseStatus);

		doReturn(requestMapping).when(translator).getRequestMappingModel(any(AnnotationMirror.class));
		doReturn(responseStatus).when(translator).getResponseStatusModel(any(AnnotationMirror.class));
		doReturn("RequestMapping", "ResponseStatus").when(translator)
				.getSimpleAnnotationName(any(AnnotationMirror.class));

		assertEquals(expectedAnnotationToModel, translator.getAnnotationToModel(methodAnnotations));

		verify(translator).getResponseStatusModel(mockAnnoMirror);
		verify(translator).getResponseStatusModel(mockAnnoMirror);
		verify(translator, times(2)).getSimpleAnnotationName(mockAnnoMirror);
	}

	@Test
	public void testGetAnnotationToModelWithUnhandledAnnotation() {
		List<AnnotationMirror> methodAnnotations = new ArrayList<>();
		AnnotationMirror mock = mock(AnnotationMirror.class);
		methodAnnotations.add(mock);
		doReturn("UNKNOWN").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));

		Map<Class, Object> expectedResult = new LinkedHashMap<>();
		expectedResult.put(ResponseStatus.class, new ResponseStatusModel().withStatusCode(200));

		assertEquals(expectedResult, translator.getAnnotationToModel(methodAnnotations));

		verify(translator).getSimpleAnnotationName(mock);
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
		AnnotationMirror anno = mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoToElementValues = new HashMap<>();
		doReturn(annoToElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoToElementValues, "code", "OK");

		ResponseStatusModel expected = new ResponseStatusModel().withStatusCode(HttpStatus.OK.value());
		assertEquals(expected, translator.getResponseStatusModel(anno));

		verify(anno, times(2)).getElementValues();
	}

	@Test
	public void testGetResponseStatusModelWithValueKeyName() {
		AnnotationMirror anno = mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoToElementValues = new HashMap<>();
		doReturn(annoToElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoToElementValues, "value", "OK");

		ResponseStatusModel expected = new ResponseStatusModel().withStatusCode(HttpStatus.OK.value());
		assertEquals(expected, translator.getResponseStatusModel(anno));

		verify(anno, times(2)).getElementValues();
	}

	@Test
	public void testGetResponseStatusModelWithUnknownAnnotation() {
		AnnotationMirror anno = mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoToElementValues = new HashMap<>();
		doReturn(annoToElementValues).when(anno).getElementValues();

		ExecutableElement key = mock(ExecutableElement.class);
		Name simpleName = mock(Name.class);
		when(key.getSimpleName()).thenReturn(simpleName);
		when(simpleName.toString()).thenReturn("UNKNOWN");
		annoToElementValues.put(key, null);

		assertEquals(new ResponseStatusModel(), translator.getResponseStatusModel(anno));

		verify(key).getSimpleName();
		verify(anno).getElementValues();
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
		AnnotationMirror anno = mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoToElementValues = new HashMap<>();
		doReturn(annoToElementValues).when(anno).getElementValues();

		ExecutableElement key = mock(ExecutableElement.class);
		Name simpleName = mock(Name.class);
		when(key.getSimpleName()).thenReturn(simpleName);
		when(simpleName.toString()).thenReturn("UNKNOWN");
		annoToElementValues.put(key, null);

		assertEquals(new RequestMappingModel(), translator.getRequestMappingModel(anno));

		verify(anno).getElementValues();
	}

	@Test
	public void testGetRequestMappingModelWithPathKeyName() {
		AnnotationMirror anno = mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> anno1ElementValues = new HashMap<>();
		doReturn(anno1ElementValues).when(anno).getElementValues();
		addAnnotationElementValues(anno1ElementValues, "path", METHOD_PATH);
		addAnnotationElementValues(anno1ElementValues, "method", RequestMethod.GET);

		RequestMappingModel expected = new RequestMappingModel().withOperation(Operation.get).withPath(METHOD_PATH);
		assertEquals(expected, translator.getRequestMappingModel(anno));
		verify(anno, times(3)).getElementValues();
	}

	@Test
	public void testGetRequestMappingModelWithValueKeyName() {
		AnnotationMirror anno = mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> anno1ElementValues = new HashMap<>();
		doReturn(anno1ElementValues).when(anno).getElementValues();
		addAnnotationElementValues(anno1ElementValues, "value", METHOD_PATH);
		addAnnotationElementValues(anno1ElementValues, "method", RequestMethod.GET);

		RequestMappingModel expected = new RequestMappingModel().withOperation(Operation.get).withPath(METHOD_PATH);

		// call under test
		assertEquals(expected, translator.getRequestMappingModel(anno));
		verify(anno, times(3)).getElementValues();
	}

	@Test
	public void testGetRequestMappingModelWithIncorrectMethodValue() {
		AnnotationMirror anno = mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoElementValues = new HashMap<>();
		doReturn(annoElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoElementValues, "method", RequestMethod.PATCH);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getRequestMappingModel(anno);
		});

		assertEquals("No operation found for RequestMethod PATCH", exception.getMessage());
		verify(anno, times(2)).getElementValues();
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
	public void testGetHttpStatusCodeWithUnhandledStatus() {
		// call under test
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getHttpStatusCode("CONTINUE");
		});
		assertEquals("Could not translate HttpStatus for status 100 CONTINUE", exception.getMessage());
	}

	@Test
	public void testGetHttpStatusCodeWithCreatedStatus() {
		assertEquals(201, translator.getHttpStatusCode("CREATED"));
	}

	@Test
	public void testGetHttpStatusCodeWithOkStatus() {
		assertEquals(200, translator.getHttpStatusCode("OK"));
	}

	@Test
	public void testGetHttpStatusCodeWithNoContentStatus() {
		assertEquals(204, translator.getHttpStatusCode("NO_CONTENT"));
	}

	@Test
	public void testGetHttpStatusCodeWithAcceptedStatus() {
		assertEquals(202, translator.getHttpStatusCode("ACCEPTED"));
	}

	@Test
	public void testGetHttpStatusCodeWithGoneStatus() {
		assertEquals(410, translator.getHttpStatusCode("GONE"));
	}

	@Test
	public void testGetRequestBody() {
		List<VariableElement> parameters = new ArrayList<>();

		VariableElement param1 = mock(VariableElement.class);
		TypeMirror paramType = mock(TypeMirror.class);
		when(param1.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn(MOCK_CLASS_NAME);
		parameters.add(param1);

		VariableElement param2 = mock(VariableElement.class);
		Name paramName = mock(Name.class);
		when(param2.getSimpleName()).thenReturn(paramName);
		when(paramName.toString()).thenReturn(PARAM_1_NAME);
		TypeMirror type = mock(TypeMirror.class);
		when(param2.asType()).thenReturn(type);
		when(type.getKind()).thenReturn(TypeKind.INT);
		when(type.toString()).thenReturn(MOCK_CLASS_NAME);
		parameters.add(param2);

		AnnotationMirror mockAnnoMirror = mock(AnnotationMirror.class);
		doReturn(mockAnnoMirror, mockAnnoMirror).when(translator)
				.getParameterAnnotation(any(VariableElement.class));
		doReturn("RequestParam", "RequestBody").when(translator)
				.getSimpleAnnotationName(any(AnnotationMirror.class));

		Map<String, String> mockParamToDescription = new HashMap<>();
		mockParamToDescription.put(PARAM_1_NAME, PARAM_1_DESCRIPTION);

		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		// call under test
		assertEquals(Optional.of(getExpectedRequestBody()),
				translator.getRequestBody(parameters, mockParamToDescription, schemaMap));

		verify(param2).getSimpleName();
		verify(param2, times(1)).asType();
		verify(type).getKind();
		verify(translator).populateSchemaMapForConcreteType(MOCK_CLASS_NAME, TypeKind.INT, schemaMap);
		;
		verify(translator, times(2)).getSimpleAnnotationName(mockAnnoMirror);
	}

	@Test
	public void testGetRequestBodyWithEmptyArray() {
		// call under test
		assertEquals(Optional.empty(), translator.getRequestBody(new ArrayList<>(), new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGetRequestBodyWithHttpServletResponseParameter() {
		List<VariableElement> parameters = new ArrayList<>();

		VariableElement param = mock(VariableElement.class);
		TypeMirror paramType = mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn("javax.servlet.http.HttpServletResponse");
		parameters.add(param);

		// call under test
		assertEquals(Optional.empty(), translator.getRequestBody(parameters, new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGetRequestBodyWithHttpServletRequestParameter() {
		List<VariableElement> parameters = new ArrayList<>();

		VariableElement param = mock(VariableElement.class);
		TypeMirror paramType = mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn("javax.servlet.http.HttpServletRequest");
		parameters.add(param);

		// call under test
		assertEquals(Optional.empty(), translator.getRequestBody(parameters, new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGetRequestBodyWithUriComponentsBuilderParameter() {
		List<VariableElement> parameters = new ArrayList<>();

		VariableElement param = mock(VariableElement.class);
		TypeMirror paramType = mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn("org.springframework.web.util.UriComponentsBuilder");
		parameters.add(param);

		// call under test
		assertEquals(Optional.empty(), translator.getRequestBody(parameters, new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGetParametersWithRequestBodyAnnotation() {
		List<VariableElement> params = new ArrayList<>();
		VariableElement param = mock(VariableElement.class);
		TypeMirror paramType = mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn(MOCK_CLASS_NAME);
		params.add(param);
		AnnotationMirror mockAnnoMirror = mock(AnnotationMirror.class);
		doReturn(mockAnnoMirror).when(translator).getParameterAnnotation(any(VariableElement.class));
		doReturn("RequestBody").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));

		// call under test
		assertEquals(new ArrayList<>(), translator.getParameters(params, new HashMap<>(), new HashMap<>()));

		verify(translator).getParameterAnnotation(param);
		verify(translator).getSimpleAnnotationName(mockAnnoMirror);
	}

	@Test
	public void testGetParameters() {
		doReturn(ParameterLocation.path).when(translator).getParameterLocation(any(VariableElement.class));
		JsonSchema expectedSchema = new JsonSchema();
		expectedSchema.setType(Type.integer);
		expectedSchema.setFormat("int32");

		List<VariableElement> parameters = new ArrayList<>();
		VariableElement param = mock(VariableElement.class);
		Name paramName = mock(Name.class);
		when(param.getSimpleName()).thenReturn(paramName);
		when(paramName.toString()).thenReturn(PARAM_1_NAME);
		TypeMirror paramType = mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.getKind()).thenReturn(TypeKind.INT);
		when(paramType.toString()).thenReturn(MOCK_CLASS_NAME);
		parameters.add(param);

		Map<String, String> mockParamToDescription = new HashMap<>();
		mockParamToDescription.put(PARAM_1_NAME, PARAM_1_DESCRIPTION);

		doReturn(mockAnnotationMirror).when(translator).getParameterAnnotation(any());
		Map<ExecutableElement, AnnotationValue> elementValues = new HashMap<>();
		addAnnotationElementValues(elementValues, "value", PARAM_1_NAME);
		doReturn(elementValues).when(mockAnnotationMirror).getElementValues();

		// call under test
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		assertEquals(getExpectedParameters(), translator.getParameters(parameters, mockParamToDescription, schemaMap));

		verify(translator).getParameterLocation(param);
		verify(translator).populateSchemaMapForConcreteType(MOCK_CLASS_NAME, TypeKind.INT, schemaMap);
		verify(param).getSimpleName();
		verify(param, times(1)).asType();
		verify(paramType).getKind();
		verify(translator).getParameterAnnotation(param);
		verify(mockAnnotationMirror, times(2)).getElementValues();
	}

	@Test
	public void testGetParametersWithEmptyArray() {
		// call under test
		assertEquals(new ArrayList<>(), translator.getParameters(new ArrayList<>(), new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGetParametersWithHttpServletRequestParameter() {
		List<VariableElement> parameters = new ArrayList<>();

		VariableElement param = mock(VariableElement.class);
		TypeMirror paramType = mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn("javax.servlet.http.HttpServletRequest");
		parameters.add(param);

		// call under test
		assertEquals(new ArrayList<>(), translator.getParameters(parameters, new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGetParametersWithHttpServletResponseParameter() {
		List<VariableElement> parameters = new ArrayList<>();

		VariableElement param = mock(VariableElement.class);
		TypeMirror paramType = mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn("javax.servlet.http.HttpServletResponse");
		parameters.add(param);

		// call under test
		assertEquals(new ArrayList<>(), translator.getParameters(parameters, new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGetParametersWithUriComponentsBuilderParameter() {
		List<VariableElement> parameters = new ArrayList<>();

		VariableElement param = mock(VariableElement.class);
		TypeMirror paramType = mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn("org.springframework.web.util.UriComponentsBuilder");
		parameters.add(param);

		// call under test
		assertEquals(new ArrayList<>(), translator.getParameters(parameters, new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGetParametersWithPathVariableNameDifferentFromMethodVariableName() {
		List<VariableElement> parameters = new ArrayList<>();
		parameters.add(mockParameter);
		when(mockParameter.asType()).thenReturn(mockParameterType);
		when(mockParameterType.toString()).thenReturn(MOCK_CLASS_NAME);
		when(mockParameter.getSimpleName()).thenReturn(mockName);
		when(mockName.toString()).thenReturn(PARAM_1_NAME);
		when(mockParameterType.getKind()).thenReturn(TypeKind.INT);

		doReturn(ParameterLocation.path).when(translator).getParameterLocation(any());
		doReturn(mockAnnotationMirror).when(translator).getParameterAnnotation(any());
		Map<ExecutableElement, AnnotationValue> elementValues = new HashMap<>();
		addAnnotationElementValues(elementValues, "value", PARAM_2_NAME);
		doReturn(elementValues).when(mockAnnotationMirror).getElementValues();

		Map<String, String> paramToDescription = new HashMap<>();
		paramToDescription.put(PARAM_2_NAME, PARAM_2_DESCRIPTION);

		ParameterModel expectedParam = new ParameterModel().withDescription(PARAM_2_DESCRIPTION).withIn(ParameterLocation.path)
				.withName(PARAM_2_NAME).withRequired(true).withId(MOCK_CLASS_NAME);
		List<ParameterModel> expectedParameters = Arrays.asList(expectedParam);

		// call under test
		assertEquals(expectedParameters, translator.getParameters(parameters, paramToDescription, new HashMap<>()));

		verify(mockParameter, times(1)).asType();
		verify(mockParameter).getSimpleName();
		verify(mockParameterType).getKind();
		verify(translator).getParameterLocation(mockParameter);
		verify(translator).getParameterAnnotation(mockParameter);
		verify(mockAnnotationMirror, times(2)).getElementValues();
	}

	@Test
	public void testGetParameterLocationWithUnknownAnnotation() {
		AnnotationMirror mockAnnoMirror = mock(AnnotationMirror.class);
		doReturn(mockAnnoMirror).when(translator).getParameterAnnotation(any(VariableElement.class));
		doReturn("UNKNOWN").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		VariableElement mockVarElement = mock(VariableElement.class);
		// call under test
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getParameterLocation(mockVarElement);
		});
		assertEquals("Unable to get parameter location with annotation UNKNOWN", exception.getMessage());
		verify(translator).getSimpleAnnotationName(mockAnnoMirror);
		verify(translator).getParameterAnnotation(mockVarElement);
	}

	@Test
	public void testGetParameterLocationWithRequestBodyAnnotation() {
		doReturn("RequestBody").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		AnnotationMirror mockAnnoMirror = mock(AnnotationMirror.class);
		doReturn(mockAnnoMirror).when(translator).getParameterAnnotation(any(VariableElement.class));
		VariableElement mockVarElement = mock(VariableElement.class);
		// call under test
		assertEquals(null, translator.getParameterLocation(mockVarElement));

		verify(translator).getSimpleAnnotationName(mockAnnoMirror);
		verify(translator).getParameterAnnotation(mockVarElement);
	}

	@Test
	public void testGetParameterLocationWithRequestParamAnnotation() {
		doReturn("RequestParam").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		AnnotationMirror mockAnnoMirror = mock(AnnotationMirror.class);
		doReturn(mockAnnoMirror).when(translator).getParameterAnnotation(any(VariableElement.class));
		VariableElement mockVarElement = mock(VariableElement.class);

		// call under test
		assertEquals(ParameterLocation.query, translator.getParameterLocation(mockVarElement));

		verify(translator).getSimpleAnnotationName(mockAnnoMirror);
		verify(translator).getParameterAnnotation(mockVarElement);
	}

	@Test
	public void testIsParameterRequiredTrue() {
		Map<ExecutableElement, AnnotationValue> elementValues = new HashMap<>();
		addAnnotationElementValues(elementValues, "required", true);
		doReturn(elementValues).when(mockAnnotationMirror).getElementValues();

		// call under test
		assertTrue(translator.isParameterRequired(mockAnnotationMirror));

		verify(mockAnnotationMirror).getElementValues();
	}

	@Test
	public void testIsParameterRequiredFalse() {
		Map<ExecutableElement, AnnotationValue> elementValues = new HashMap<>();
		addAnnotationElementValues(elementValues, "required", false);
		doReturn(elementValues).when(mockAnnotationMirror).getElementValues();

		// call under test
		assertFalse(translator.isParameterRequired(mockAnnotationMirror));

		verify(mockAnnotationMirror).getElementValues();
	}

	@Test
	public void testIsParemeterRequiredWithNullParamAnnotation() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.isParameterRequired(null);
		});
		assertEquals("paramAnnotation is required.", exception.getMessage());
	}


	@Test
	public void testGetParameterLocationWithPathVariableAnnotation() {
		doReturn("PathVariable").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		doReturn(mock(AnnotationMirror.class)).when(translator).getParameterAnnotation(any());
		// call under test
		assertEquals(ParameterLocation.path, translator.getParameterLocation(null));
	}

	@Test
	public void testGetParameterLocationWithRequestHeaderAnnotation() {
		doReturn("RequestHeader").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		AnnotationMirror mockAnnoMirror = mock(AnnotationMirror.class);
		doReturn(mockAnnoMirror).when(translator).getParameterAnnotation(any(VariableElement.class));
		VariableElement mockVarElement = mock(VariableElement.class);

		// call under test
		assertEquals(ParameterLocation.header, translator.getParameterLocation(mockVarElement));

		verify(translator).getSimpleAnnotationName(mockAnnoMirror);
		verify(translator).getParameterAnnotation(mockVarElement);
	}

	@Test
	public void testGetParameterAnnotation() throws Exception {
		VariableElement mockParameter = mock(VariableElement.class);
		List<? extends AnnotationMirror> annotationMirrors = mock(List.class);
		doReturn(annotationMirrors).when(mockParameter).getAnnotationMirrors();
		when(annotationMirrors.size()).thenReturn(1);
		AnnotationMirror annotation = mock(AnnotationMirror.class);
		doReturn(annotation).when(annotationMirrors).get(0);
		// call under test
		assertEquals(annotation, translator.getParameterAnnotation(mockParameter));

		verify(mockParameter).getAnnotationMirrors();
		verify(annotationMirrors).size();
		verify(annotationMirrors).get(0);
	}

	@Test
	public void testGetParameterAnnotationWithAnnotationMirrorsSize2() {
		VariableElement mockParameter = mock(VariableElement.class);
		List<? extends AnnotationMirror> annotationMirrors = mock(List.class);
		doReturn(annotationMirrors).when(mockParameter).getAnnotationMirrors();
		Name mockParamName = mock(Name.class);
		when(mockParameter.getSimpleName()).thenReturn(mockParamName);
		when(mockParamName.toString()).thenReturn("testMethod");
		when(annotationMirrors.size()).thenReturn(2);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getParameterAnnotation(mockParameter);
		});
		assertEquals("Each method parameter should have one annotation, testMethod has 2", exception.getMessage());
		verify(mockParameter).getAnnotationMirrors();
		verify(annotationMirrors, times(2)).size();
	}

	@Test
	public void testGetParameterAnnotationWithEmptyAnnotationMirrors() throws Exception {
		VariableElement mockParameter = mock(VariableElement.class);
		when(mockParameter.getAnnotationMirrors()).thenReturn(new ArrayList<>());
		Name mockParamName = mock(Name.class);
		when(mockParameter.getSimpleName()).thenReturn(mockParamName);
		when(mockParamName.toString()).thenReturn("testMethod");
		// call under test
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getParameterAnnotation(mockParameter);
		});
		assertEquals("Each method parameter should have one annotation, testMethod has 0", exception.getMessage());
		verify(mockParameter).getAnnotationMirrors();
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
		AnnotationMirror annotation = mock(AnnotationMirror.class);

		DeclaredType mockDeclaredType = mock(DeclaredType.class);
		Element element = mock(Element.class);
		when(mockDeclaredType.asElement()).thenReturn(element);
		Name simpleName = mock(Name.class);
		when(element.getSimpleName()).thenReturn(simpleName);
		when(simpleName.toString()).thenReturn(ANNOTATION_NAME);

		when(annotation.getAnnotationType()).thenReturn(mockDeclaredType);
		assertEquals(ANNOTATION_NAME, translator.getSimpleAnnotationName(annotation));

		verify(mockDeclaredType).asElement();
		verify(element).getSimpleName();
		verify(annotation).getAnnotationType();
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
		ParamTree param = mock(ParamTree.class);
		when(param.getKind()).thenReturn(DocTree.Kind.PARAM);
		List<? extends DocTree> description = mock(List.class);
		doReturn(description).when(param).getDescription();
		when(description.isEmpty()).thenReturn(true);
		blockTags.add(param);

		// call under test
		assertEquals(new LinkedHashMap<>(), translator.getParameterToDescription(blockTags));

		verify(param).getKind();
		verify(description).isEmpty();
	}

	@Test
	public void testGetParameterToDescriptionWithoutParamTreeInBlockTags() {
		ReturnTree mockReturnComment = mock(ReturnTree.class);
		when(mockReturnComment.getKind()).thenReturn(DocTree.Kind.RETURN);
		// call under test
		assertEquals(new LinkedHashMap<>(),
				translator.getParameterToDescription(new ArrayList<>(Arrays.asList(mockReturnComment))));

		verify(mockReturnComment).getKind();
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
		ParamTree mockParamComment = mock(ParamTree.class);
		when(mockParamComment.getKind()).thenReturn(DocTree.Kind.PARAM);
		// call under test
		assertEquals(Optional.empty(), translator.getReturnComment(new ArrayList<>(Arrays.asList(mockParamComment))));

		verify(mockParamComment).getKind();
	}

	@Test
	public void testGetReturnCommentWithEmptyDescription() {
		List<DocTree> blockTags = new ArrayList<>();
		ReturnTree mockReturnComment = mock(ReturnTree.class);
		when(mockReturnComment.getKind()).thenReturn(DocTree.Kind.RETURN);
		when(mockReturnComment.getDescription()).thenReturn(new ArrayList<>());
		blockTags.add(mockReturnComment);

		assertEquals(Optional.empty(), translator.getReturnComment(blockTags));

		verify(mockReturnComment).getKind();
		verify(mockReturnComment).getDescription();
	}

	@Test
	public void testGetReturnComment() {
		List<DocTree> blockTags = new ArrayList<>();
		ReturnTree mockReturnComment = mock(ReturnTree.class);
		when(mockReturnComment.getKind()).thenReturn(DocTree.Kind.RETURN);
		List<? extends DocTree> descriptions = mock(List.class);
		when(descriptions.toString()).thenReturn(METHOD_RETURN_COMMENT);
		doReturn(descriptions).when(mockReturnComment).getDescription();
		blockTags.add(mockReturnComment);

		// call under test
		assertEquals(Optional.of(METHOD_RETURN_COMMENT), translator.getReturnComment(blockTags));

		verify(mockReturnComment).getKind();
		verify(mockReturnComment, times(2)).getDescription();
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
		List<? extends DocTree> fullBody = mock(List.class);
		when(fullBody.isEmpty()).thenReturn(false);
		when(fullBody.toString()).thenReturn(METHOD_BEHAVIOR_COMMENT);
		// call under test
		assertEquals(Optional.of(METHOD_BEHAVIOR_COMMENT), translator.getBehaviorComment(fullBody));

		verify(fullBody).isEmpty();
	}

	@Test
	public void testGetJsonSchemaTypeForClassWithString() {
		assertEquals(Type.string, translator.getJsonSchemaBasicTypeForClass("java.lang.String").get());
	}

	@Test
	public void testGetJsonSchemaTypeForClassWithIntegerClass() {
		assertEquals(Type.integer, translator.getJsonSchemaBasicTypeForClass("java.lang.Integer").get());
	}

	@Test
	public void testGetJsonSchemaTypeForClassWithBooleanClass() {
		assertEquals(Optional.of(Type._boolean), translator.getJsonSchemaBasicTypeForClass("java.lang.Boolean"));
	}

	@Test
	public void testGetJsonSchemaTypeForClassWithLongClass() {
		assertEquals(Optional.of(Type.number), translator.getJsonSchemaBasicTypeForClass("java.lang.Long"));
	}

	@Test
	public void testGetJsonSchemaTypeForClassWithObjectClass() {
		assertEquals(Optional.of(Type.object), translator.getJsonSchemaBasicTypeForClass("java.lang.Object"));
	}

	@Test
	public void testGetJsonSchemaTypeForClassWithBooleanPrimitive() {
		assertEquals(Optional.of(Type._boolean), translator.getJsonSchemaBasicTypeForClass("boolean"));
	}

	@Test
	public void testGetJsonSchemaTypeForClassWithIntPrimitive() {
		assertEquals(Optional.of(Type.integer), translator.getJsonSchemaBasicTypeForClass("int"));
	}

	@Test
	public void testGetJsonSchemaTypeForClassWithLongPrimitive() {
		assertEquals(Optional.of(Type.number), translator.getJsonSchemaBasicTypeForClass("long"));
	}

	@Test
	public void testGetJsonSchemaTypeForClassWithBooleanResult() {
		assertEquals(Optional.of(Type._boolean), translator.getJsonSchemaBasicTypeForClass("org.sagebionetworks.repo.model.BooleanResult"));
	}

	@Test
	public void testGetJsonSchemaTypeForClassWithJsonObject() {
		assertEquals(Optional.of(Type.object), translator.getJsonSchemaBasicTypeForClass("org.json.JSONObject"));
	}

	@Test
	public void testGetJsonSchemaTypeForClassWithObjectSchema() {
		assertEquals(Optional.of(Type.object), translator.getJsonSchemaBasicTypeForClass("org.sagebionetworks.schema.ObjectSchema"));
	}

	@Test
	public void testGetJsonSchemaTypeForClassNotBasicType() {
		assertEquals(Optional.empty(), translator.getJsonSchemaBasicTypeForClass("org.sagebionetworks.model.dog"));
	}

}