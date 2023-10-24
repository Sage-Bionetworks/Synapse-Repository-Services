package org.sagebionetworks.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
		this.translator = Mockito.spy(new ControllerToControllerModelTranslator());
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
	public void testExtractControllerModels() {
		DocletEnvironment env = Mockito.mock(DocletEnvironment.class);
		TypeElement element1 = Mockito.mock(TypeElement.class);
		Mockito.doReturn(ElementKind.CLASS).when(element1).getKind();
		TypeElement element2 = Mockito.mock(TypeElement.class);
		Mockito.doReturn(ElementKind.CLASS).when(element2).getKind();
		Set<? extends Element> includedElements = new LinkedHashSet<>(Arrays.asList(element1, element2));
		Mockito.doReturn(includedElements).when(env).getIncludedElements();
		Mockito.doReturn(new ArrayList<>(includedElements)).when(translator).getControllers(any());

		DocTrees docTrees = Mockito.mock(DocTrees.class);
		Mockito.doReturn(docTrees).when(env).getDocTrees();
		Map<String, ObjectSchema> schemaMap = new HashMap<>();

		ControllerModel controller1 = new ControllerModel().withDescription("CONTROLLER_1");
		ControllerModel controller2 = new ControllerModel().withDescription("CONTROLLER_2");
		Mockito.doReturn(controller1, controller2).when(translator).translate(any(TypeElement.class),
				any(DocTrees.class), any(Map.class), any());

		Reporter reporter = Mockito.mock(Reporter.class);
		assertEquals(new ArrayList<>(Arrays.asList(controller1, controller2)),
				translator.extractControllerModels(env, schemaMap, reporter));

		InOrder inOrder = Mockito.inOrder(translator);
		inOrder.verify(translator).translate(element1, docTrees, schemaMap, reporter);
		inOrder.verify(translator).translate(element2, docTrees, schemaMap, reporter);
		verify(translator).getControllers(any());
	}

	@Test
	public void testExtractControllerModelsWithNoClassElements() {
		DocletEnvironment env = Mockito.mock(DocletEnvironment.class);
		TypeElement element1 = Mockito.mock(TypeElement.class);
		Mockito.doReturn(ElementKind.INTERFACE).when(element1).getKind();
		TypeElement element2 = Mockito.mock(TypeElement.class);
		Mockito.doReturn(ElementKind.INTERFACE).when(element2).getKind();
		Mockito.doReturn(new HashSet<>(Arrays.asList(element1, element2))).when(env).getIncludedElements();

		Reporter reporter = Mockito.mock(Reporter.class);
		assertEquals(new ArrayList<>(), translator.extractControllerModels(env, new HashMap<>(), reporter));
	}

	@Test
	public void testExtractControllersModelsWithEmptyIncludedElements() {
		DocletEnvironment env = Mockito.mock(DocletEnvironment.class);
		Mockito.doReturn(new HashSet<>()).when(env).getIncludedElements();

		Reporter reporter = Mockito.mock(Reporter.class);
		assertEquals(new ArrayList<>(), translator.extractControllerModels(env, new HashMap<>(), reporter));
	}

	@Test
	public void testGetControllers() {
		TypeElement element1 = Mockito.mock(TypeElement.class);
		TypeElement element2 = Mockito.mock(TypeElement.class);
		Set<TypeElement> files = new LinkedHashSet<>(Arrays.asList(element1, element2));

		Mockito.doReturn(true, false).when(translator).isController(any());

		// call under test
		assertEquals(new ArrayList<>(Arrays.asList(element1)), translator.getControllers(files));
		verify(translator, Mockito.times(2)).isController(any());
		InOrder inOrder = Mockito.inOrder(translator);
		inOrder.verify(translator).isController(element1);
		inOrder.verify(translator).isController(element2);
	}

	@Test
	public void testIsControllerWithControllerElement() {
		TypeElement file = Mockito.mock(TypeElement.class);
		Mockito.doReturn(ElementKind.CLASS).when(file).getKind();

		AnnotationMirror annotation1 = Mockito.mock(AnnotationMirror.class);
		AnnotationMirror annotation2 = Mockito.mock(AnnotationMirror.class);
		Mockito.doReturn(new ArrayList<>(Arrays.asList(annotation1, annotation2))).when(file).getAnnotationMirrors();

		Mockito.doReturn("ANNOTATION_NAME", ControllerInfo.class.getSimpleName()).when(translator)
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
		TypeElement file = Mockito.mock(TypeElement.class);
		Mockito.doReturn(ElementKind.CLASS).when(file).getKind();

		AnnotationMirror annotation1 = Mockito.mock(AnnotationMirror.class);
		AnnotationMirror annotation2 = Mockito.mock(AnnotationMirror.class);
		Mockito.doReturn(new ArrayList<>(Arrays.asList(annotation1, annotation2))).when(file).getAnnotationMirrors();

		Mockito.doReturn("ANNOTATION_NAME_1", "ANNOTATION_NAME_2").when(translator).getSimpleAnnotationName(any());

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

		TypeElement controller = Mockito.mock(TypeElement.class);
		List<Element> elements = new ArrayList<>();
		Mockito.doReturn(elements).when(controller).getEnclosedElements();
		List<AnnotationMirror> annoMirrors = new ArrayList<>();
		Mockito.doReturn(annoMirrors).when(controller).getAnnotationMirrors();
		Mockito.doReturn(getExpectedMethods()).when(translator).getMethods(any(List.class), any(DocTrees.class),
				any(Map.class), any());
		Mockito.doReturn(new ControllerInfoModel().withDisplayName(CONTROLLER_NAME).withPath(CONTROLLER_PATH))
				.when(translator).getControllerInfoModel(any(List.class));
		Mockito.doReturn(CONTROLLER_DESCRIPTION).when(translator).getControllerDescription(any(DocCommentTree.class));

		Map<String, ObjectSchema> schemaMap = new HashMap<>();

		DocTrees mockDocTree = Mockito.mock(DocTrees.class);
		DocCommentTree mockDocCommentTree = Mockito.mock(DocCommentTree.class);
		Mockito.doReturn(mockDocCommentTree).when(mockDocTree).getDocCommentTree(any(TypeElement.class));

		Reporter reporter = Mockito.mock(Reporter.class);
		Mockito.doNothing().when(reporter).print(any(), any());
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
		DocCommentTree mockDocCommentTree = Mockito.mock(DocCommentTree.class);
		List<DocTree> fullBody = new ArrayList<>();
		Mockito.doReturn(fullBody).when(mockDocCommentTree).getFullBody();
		Optional<String> result = Optional.of(CONTROLLER_DESCRIPTION);
		Mockito.doReturn(result).when(translator).getBehaviorComment(any(List.class));

		assertEquals(CONTROLLER_DESCRIPTION, translator.getControllerDescription(mockDocCommentTree));
		verify(translator).getBehaviorComment(fullBody);
		verify(mockDocCommentTree).getFullBody();
	}

	@Test
	public void testGetControllerDescriptionWithMissingComment() {
		DocCommentTree mockDocCommentTree = Mockito.mock(DocCommentTree.class);
		List<DocTree> fullBody = new ArrayList<>();
		Mockito.doReturn(fullBody).when(mockDocCommentTree).getFullBody();
		Optional<String> result = Optional.empty();
		Mockito.doReturn(result).when(translator).getBehaviorComment(any(List.class));

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

		verify(anno, Mockito.times(3)).getElementValues();
		verify(translator).getSimpleAnnotationName(anno);
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

		verify(anno, Mockito.times(2)).getElementValues();
		verify(translator).getSimpleAnnotationName(anno);
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
		verify(anno).getElementValues();
		verify(translator).getSimpleAnnotationName(anno);
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

		verify(anno, Mockito.times(2)).getElementValues();
		verify(translator).getSimpleAnnotationName(anno);
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

		verify(anno, Mockito.times(2)).getElementValues();
		verify(translator).getSimpleAnnotationName(anno);
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
		RequestMappingModel requestMapping = new RequestMappingModel().withOperation(Operation.get)
				.withPath(METHOD_PATH);
		annotationToModel.put(RequestMapping.class, requestMapping);
		ResponseStatusModel responseStatus = new ResponseStatusModel().withStatusCode(HttpStatus.OK.value());
		annotationToModel.put(ResponseStatus.class, responseStatus);
		Mockito.doReturn(annotationToModel).when(translator).getAnnotationToModel(any(List.class));

		Map<String, ObjectSchema> schemaMap = new HashMap<>();

		JsonSchema expectedRequestBodySchema = new JsonSchema();
		expectedRequestBodySchema.setType(Type.integer);
		expectedRequestBodySchema.setFormat("int32");
		RequestBodyModel requestBody = new RequestBodyModel().withDescription(PARAM_1_DESCRIPTION).withRequired(true)
				.withId(MOCK_CLASS_NAME);
		Mockito.doReturn(Optional.of(requestBody)).when(translator).getRequestBody(any(List.class), any(Map.class),
				any(Map.class));

		Mockito.doReturn(Optional.of(METHOD_BEHAVIOR_COMMENT)).when(translator).getBehaviorComment(any(List.class));
		Mockito.doReturn(METHOD_PATH).when(translator).getMethodPath(any(RequestMappingModel.class));
		Mockito.doReturn(getExpectedParameters()).when(translator).getParameters(any(List.class), any(Map.class),
				any(Map.class));
		Mockito.doReturn(getExpectedResponseModel()).when(translator).getResponseModel(any(ExecutableElement.class),
				any(List.class), any(Map.class), any(Map.class));

		Reporter reporter = Mockito.mock(Reporter.class);
		Mockito.doNothing().when(reporter).print(any(), any());
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
		Name methodName = Mockito.mock(Name.class);
		when(method.getSimpleName()).thenReturn(methodName);
		when(methodName.toString()).thenReturn(METHOD_NAME);
		when(method.getKind()).thenReturn(ElementKind.METHOD);
		enclosedElements.add(method);

		// Mock translator method calls
		Map<String, String> parameterToDescription = new HashMap<>();
		parameterToDescription.put(PARAM_1_NAME, PARAM_1_DESCRIPTION);
		Mockito.doReturn(parameterToDescription).when(translator).getParameterToDescription(any(List.class));
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		Map<Class, Object> annotationToModel = new HashMap<>();
		RequestMappingModel requestMapping = new RequestMappingModel().withOperation(Operation.get)
				.withPath(METHOD_PATH);
		annotationToModel.put(RequestMapping.class, requestMapping);
		ResponseStatusModel responseStatus = new ResponseStatusModel().withStatusCode(HttpStatus.OK.value());
		annotationToModel.put(ResponseStatus.class, responseStatus);
		Mockito.doReturn(annotationToModel).when(translator).getAnnotationToModel(any(List.class));
		Mockito.doReturn(Optional.empty()).when(translator).getRequestBody(any(List.class), any(Map.class),
				any(Map.class));
		Mockito.doReturn(Optional.empty()).when(translator).getBehaviorComment(any(List.class));
		Mockito.doReturn(METHOD_PATH).when(translator).getMethodPath(any(RequestMappingModel.class));
		Mockito.doReturn(getExpectedParameters()).when(translator).getParameters(any(List.class), any(Map.class),
				any(Map.class));
		Mockito.doReturn(getExpectedResponseModel()).when(translator).getResponseModel(any(ExecutableElement.class),
				any(List.class), any(Map.class), any(Map.class));

		List<MethodModel> expectedMethods = new ArrayList<>();
		MethodModel expectedMethod = new MethodModel().withPath(METHOD_PATH).withName(METHOD_NAME)
				.withOperation(Operation.get).withParameters(getExpectedParameters())
				.withResponse(getExpectedResponseModel());
		expectedMethods.add(expectedMethod);

		Reporter reporter = Mockito.mock(Reporter.class);
		Mockito.doNothing().when(reporter).print(any(), any());
		// call under test
		assertEquals(expectedMethods, translator.getMethods(enclosedElements, docTrees, schemaMap, reporter));

		verify(docTrees).getDocCommentTree(method);
		verify(mockDocCommentTree, Mockito.times(2)).getBlockTags();
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

		Map<String, ObjectSchema> schemaMap = new HashMap<>();

		Reporter reporter = Mockito.mock(Reporter.class);
		Mockito.doNothing().when(reporter).print(any(), any());
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
		Mockito.doNothing().when(mockReporter).print(any(), any());

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
		Reporter reporter = Mockito.mock(Reporter.class);
		// call under test
		assertEquals(new ArrayList<>(),
				translator.getMethods(new ArrayList<>(), Mockito.mock(DocTrees.class), new HashMap<>(), reporter));
	}

	@Test
	public void testGetResponseModelWithRedirectedEndpoint() {
		Mockito.doReturn(true).when(translator).isRedirect(any());
		Mockito.doReturn("DESCRIPTION").when(translator).getResponseDescription(any(), any());

		ResponseModel response = new ResponseModel();
		Mockito.doReturn(response).when(translator).generateRedirectedResponseModel(any());

		ExecutableElement method = Mockito.mock(ExecutableElement.class);
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
		Mockito.doReturn(false).when(translator).isRedirect(any());
		Mockito.doReturn("DESCRIPTION").when(translator).getResponseDescription(any(), any());

		ResponseModel response = new ResponseModel();
		Mockito.doReturn(response).when(translator).generateResponseModel(any(), any(), any(), any());

		ExecutableElement method = Mockito.mock(ExecutableElement.class);
		TypeMirror returnType = Mockito.mock(TypeMirror.class);
		Mockito.doReturn(returnType).when(method).getReturnType();
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
			translator.getResponseModel(Mockito.mock(ExecutableElement.class), new ArrayList<>(), new HashMap<>(),
					null);
		});
		assertEquals("schemaMap is required.", exception.getMessage());
	}

	@Test
	public void testGetResponseModelWithNullAnnotationToModel() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getResponseModel(Mockito.mock(ExecutableElement.class), new ArrayList<>(), null,
					new HashMap<>());
		});
		assertEquals("annotationToModel is required.", exception.getMessage());
	}

	@Test
	public void testGetResponseModelWithNullBlockTags() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getResponseModel(Mockito.mock(ExecutableElement.class), null, new HashMap<>(), new HashMap<>());
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
		Mockito.doReturn(Optional.empty()).when(translator).getReturnComment(any());
		List<? extends DocTree> blockTags = new ArrayList<>();
		ExecutableElement method = Mockito.mock(ExecutableElement.class);
		TypeMirror returnType = Mockito.mock(TypeMirror.class);
		Mockito.doReturn(returnType).when(method).getReturnType();
		Mockito.doReturn(TypeKind.VOID).when(returnType).getKind();

		// call under test
		assertEquals("Void", translator.getResponseDescription(blockTags, method));
		verify(translator).getReturnComment(blockTags);
	}

	@Test
	public void testGetResponseDescriptionWithEmptyReturnComment() {
		Mockito.doReturn(Optional.empty()).when(translator).getReturnComment(any());
		List<? extends DocTree> blockTags = new ArrayList<>();
		ExecutableElement method = Mockito.mock(ExecutableElement.class);
		TypeMirror returnType = Mockito.mock(TypeMirror.class);
		Mockito.doReturn(returnType).when(method).getReturnType();
		Mockito.doReturn(TypeKind.INT).when(returnType).getKind();

		// call under test
		assertEquals("Auto-generated description", translator.getResponseDescription(blockTags, method));
		verify(translator).getReturnComment(blockTags);
	}

	@Test
	public void testGetResponseDescription() {
		Mockito.doReturn(Optional.of("DESCRIPTION")).when(translator).getReturnComment(any());
		List<? extends DocTree> blockTags = new ArrayList<>();

		// call under test
		assertEquals("DESCRIPTION", translator.getResponseDescription(blockTags, Mockito.mock(ExecutableElement.class)));
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
		TypeMirror returnType = Mockito.mock(TypeMirror.class);
		Map<Class, Object> annotationToModel = new HashMap<>();
		String description = "DESCRIPTION";
		Map<String, ObjectSchema> schemaMap = new HashMap<>();

		Mockito.doNothing().when(translator).populateSchemaMap(any(), any(), any());
		String returnClassName = "RETURN_CLASS_NAME";
		Mockito.doReturn(returnClassName).when(returnType).toString();
		Mockito.doReturn(TypeKind.BOOLEAN).when(returnType).getKind();

		ResponseStatusModel responseStatus = new ResponseStatusModel().withStatusCode(200);
		annotationToModel.put(ResponseStatus.class, responseStatus);

		ResponseModel expected = new ResponseModel().withDescription(description)
				.withStatusCode(responseStatus.getStatusCode()).withId(returnClassName);
		// call under test
		assertEquals(expected, translator.generateResponseModel(returnType, annotationToModel, description, schemaMap));
		verify(translator).populateSchemaMap(returnClassName, TypeKind.BOOLEAN, schemaMap);
	}

	@Test
	public void testGenerateResponseModelWithMissingResponseStatusAnnotation() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.generateResponseModel(Mockito.mock(TypeMirror.class), new HashMap<>(), "DESCRIPTION",
					new HashMap<>());
		});
		assertEquals("Missing response status in annotationToModel.", exception.getMessage());
	}

	@Test
	public void testIsRedirectWithVoidReturnAndNoRedirectParam() {
		ExecutableElement method = Mockito.mock(ExecutableElement.class);
		Mockito.doReturn(new ArrayList<>()).when(method).getParameters();
		TypeMirror returnType = Mockito.mock(TypeMirror.class);
		Mockito.doReturn(returnType).when(method).getReturnType();
		Mockito.doReturn(TypeKind.VOID).when(returnType).getKind();
		Mockito.doReturn(false).when(translator).containsRedirectParam(any());

		assertFalse(translator.isRedirect(method));
	}

	@Test
	public void testIsRedirect() {
		ExecutableElement method = Mockito.mock(ExecutableElement.class);
		Mockito.doReturn(new ArrayList<>()).when(method).getParameters();
		TypeMirror returnType = Mockito.mock(TypeMirror.class);
		Mockito.doReturn(returnType).when(method).getReturnType();
		Mockito.doReturn(TypeKind.VOID).when(returnType).getKind();
		Mockito.doReturn(true).when(translator).containsRedirectParam(any());
		
		// call under test
		assertTrue(translator.isRedirect(method));
		
		Mockito.doReturn(TypeKind.INT).when(returnType).getKind();
		// call under test
		assertFalse(translator.isRedirect(method));
		
		Mockito.doReturn(false).when(translator).containsRedirectParam(any());
		// call under test
		assertFalse(translator.isRedirect(method));
	}
	
	@Test
	public void testContainsRedirectParamWithRedirect() {
		List<VariableElement> params = new ArrayList<>();
		VariableElement param = Mockito.mock(VariableElement.class);
		Name paramName = Mockito.mock(Name.class);
		Mockito.doReturn("redirect").when(paramName).toString();
		Mockito.doReturn(paramName).when(param).getSimpleName();
		
		TypeMirror paramType = Mockito.mock(TypeMirror.class);
		Mockito.doReturn(TypeKind.BOOLEAN).when(paramType).getKind();
		Mockito.doReturn(paramType).when(param).asType();
		
		params.add(param);
		assertTrue(translator.containsRedirectParam(params));
	}
	
	@Test
	public void testContainsRedirectParamWithoutRedirect() {
		List<VariableElement> params = new ArrayList<>();
		VariableElement param = Mockito.mock(VariableElement.class);
		Name paramName = Mockito.mock(Name.class);
		Mockito.doReturn("WRONG_NAME").when(paramName).toString();
		Mockito.doReturn(paramName).when(param).getSimpleName();
		
		TypeMirror paramType = Mockito.mock(TypeMirror.class);
		Mockito.doReturn(TypeKind.BOOLEAN).when(paramType).getKind();
		Mockito.doReturn(paramType).when(param).asType();
		
		params.add(param);
		// call under test
		assertFalse(translator.containsRedirectParam(params));
		
		// call under test
		Mockito.doReturn(TypeKind.DECLARED).when(paramType).getKind();
		Mockito.doReturn(Boolean.class.getName()).when(paramName).toString();
		assertFalse(translator.containsRedirectParam(params));
		
		Mockito.doReturn("redirect").when(paramName).toString();
		Mockito.doReturn(TypeKind.INT).when(paramType).getKind();
		// call under test
		assertFalse(translator.containsRedirectParam(params));
		
		Mockito.doReturn("WRONG_NAME").when(paramName).toString();
		assertFalse(translator.containsRedirectParam(params));
	}
	
	@Test
	public void testPopulateSchemaMapWithBooleanWrapperClass() {
		ObjectSchema schema = null;
		try {
			JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
			schema = new ObjectSchemaImpl(adpater);
		} catch (Exception e) {
			// this should never happen
			throw new RuntimeException("Error creating adapter for schema");
		}
		Mockito.doReturn(schema).when(translator).generateObjectSchemaForPrimitiveType(any(TypeKind.class));
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		// call under test
		translator.populateSchemaMap(Boolean.class.getName(), TypeKind.DECLARED, schemaMap);

		Map<String, ObjectSchema> expectedSchemaMap = new HashMap<>();
		expectedSchemaMap.put(Boolean.class.getName(), schema);
		assertEquals(expectedSchemaMap, schemaMap);

		verify(translator).generateObjectSchemaForPrimitiveType(TypeKind.BOOLEAN);
	}

	@Test
	public void testPopulateSchemaMapWithString() {
		ObjectSchema schema = null;
		try {
			JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
			schema = new ObjectSchemaImpl(adpater);
		} catch (Exception e) {
			// this should never happen
			throw new RuntimeException("Error creating adapter for schema");
		}
		Mockito.doReturn(schema).when(translator).generateObjectSchemaForPrimitiveType(any(TypeKind.class));
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		// call under test
		translator.populateSchemaMap(String.class.getName(), TypeKind.DECLARED, schemaMap);

		Map<String, ObjectSchema> expectedSchemaMap = new HashMap<>();
		expectedSchemaMap.put(String.class.getName(), schema);
		assertEquals(expectedSchemaMap, schemaMap);

		verify(translator).generateObjectSchemaForPrimitiveType(TypeKind.DECLARED);
	}

	@Test
	public void testPopulateSchemaMapWithPrimitiveType() {

		ObjectSchema schema = null;
		try {
			JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
			schema = new ObjectSchemaImpl(adpater);
		} catch (Exception e) {
			// this should never happen
			throw new RuntimeException("Error creating adapter for schema");
		}
		Mockito.doReturn(schema).when(translator).generateObjectSchemaForPrimitiveType(any(TypeKind.class));
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		// call under test
		translator.populateSchemaMap(int.class.getName(), TypeKind.INT, schemaMap);

		Map<String, ObjectSchema> expectedSchemaMap = new HashMap<>();
		expectedSchemaMap.put(int.class.getName(), schema);
		assertEquals(expectedSchemaMap, schemaMap);

		verify(translator).generateObjectSchemaForPrimitiveType(TypeKind.INT);
	}

	@Test
	public void testPopulateSchemaMapWithNonPrimitiveType() {
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		// call under test
		translator.populateSchemaMap(Husky.class.getName(), TypeKind.DECLARED, schemaMap);

		Map<String, ObjectSchema> expectedSchemaMap = new HashMap<>();
		SchemaUtils.recursiveAddTypes(expectedSchemaMap, Husky.class.getName(), null);
		assertEquals(expectedSchemaMap, schemaMap);
	}

	@Test
	public void testPopulateSchemaMapWithNullSchemaMap() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.populateSchemaMap("", TypeKind.ARRAY, null);
		});
		assertEquals("schemaMap is required.", exception.getMessage());
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
	public void testPopulateSchemaMapWithNullClassName() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.populateSchemaMap(null, TypeKind.ARRAY, new HashMap<>());
		});
		assertEquals("className is required.", exception.getMessage());
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

		verify(anno1, Mockito.times(3)).getElementValues();
		verify(anno2, Mockito.times(2)).getElementValues();
		verify(translator, Mockito.times(2)).getSimpleAnnotationName(any(AnnotationMirror.class));
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

		verify(translator).getResponseStatusModel(mockAnnoMirror);
		verify(translator).getResponseStatusModel(mockAnnoMirror);
		verify(translator, Mockito.times(2)).getSimpleAnnotationName(mockAnnoMirror);
	}

	@Test
	public void testGetAnnotationToModelWithUnhandledAnnotation() {
		List<AnnotationMirror> methodAnnotations = new ArrayList<>();
		AnnotationMirror mock = Mockito.mock(AnnotationMirror.class);
		methodAnnotations.add(mock);
		Mockito.doReturn("UNKNOWN").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));

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
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoToElementValues = new HashMap<>();
		Mockito.doReturn(annoToElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoToElementValues, "code", "OK");

		ResponseStatusModel expected = new ResponseStatusModel().withStatusCode(HttpStatus.OK.value());
		assertEquals(expected, translator.getResponseStatusModel(anno));

		verify(anno, Mockito.times(2)).getElementValues();
	}

	@Test
	public void testGetResponseStatusModelWithValueKeyName() {
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoToElementValues = new HashMap<>();
		Mockito.doReturn(annoToElementValues).when(anno).getElementValues();
		addAnnotationElementValues(annoToElementValues, "value", "OK");

		ResponseStatusModel expected = new ResponseStatusModel().withStatusCode(HttpStatus.OK.value());
		assertEquals(expected, translator.getResponseStatusModel(anno));

		verify(anno, Mockito.times(2)).getElementValues();
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
		AnnotationMirror anno = Mockito.mock(AnnotationMirror.class);
		Map<ExecutableElement, AnnotationValue> annoToElementValues = new HashMap<>();
		Mockito.doReturn(annoToElementValues).when(anno).getElementValues();

		ExecutableElement key = Mockito.mock(ExecutableElement.class);
		Name simpleName = Mockito.mock(Name.class);
		when(key.getSimpleName()).thenReturn(simpleName);
		when(simpleName.toString()).thenReturn("UNKNOWN");
		annoToElementValues.put(key, null);

		assertEquals(new RequestMappingModel(), translator.getRequestMappingModel(anno));

		verify(anno).getElementValues();
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
		verify(anno, Mockito.times(3)).getElementValues();
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
		verify(anno, Mockito.times(3)).getElementValues();
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
		verify(anno, Mockito.times(2)).getElementValues();
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

		VariableElement param1 = Mockito.mock(VariableElement.class);
		TypeMirror paramType = Mockito.mock(TypeMirror.class);
		when(param1.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn(MOCK_CLASS_NAME);
		parameters.add(param1);

		VariableElement param2 = Mockito.mock(VariableElement.class);
		Name paramName = Mockito.mock(Name.class);
		when(param2.getSimpleName()).thenReturn(paramName);
		when(paramName.toString()).thenReturn(PARAM_1_NAME);
		TypeMirror type = Mockito.mock(TypeMirror.class);
		when(param2.asType()).thenReturn(type);
		when(type.getKind()).thenReturn(TypeKind.INT);
		when(type.toString()).thenReturn(MOCK_CLASS_NAME);
		parameters.add(param2);

		AnnotationMirror mockAnnoMirror = Mockito.mock(AnnotationMirror.class);
		Mockito.doReturn(mockAnnoMirror, mockAnnoMirror).when(translator)
				.getParameterAnnotation(any(VariableElement.class));
		Mockito.doReturn("RequestParam", "RequestBody").when(translator)
				.getSimpleAnnotationName(any(AnnotationMirror.class));

		Map<String, String> mockParamToDescription = new HashMap<>();
		mockParamToDescription.put(PARAM_1_NAME, PARAM_1_DESCRIPTION);

		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		// call under test
		assertEquals(Optional.of(getExpectedRequestBody()),
				translator.getRequestBody(parameters, mockParamToDescription, schemaMap));

		verify(param2).getSimpleName();
		verify(param2, Mockito.times(3)).asType();
		verify(type).getKind();
		verify(translator).populateSchemaMap(MOCK_CLASS_NAME, TypeKind.INT, schemaMap);
		;
		verify(translator, Mockito.times(2)).getSimpleAnnotationName(mockAnnoMirror);
	}

	@Test
	public void testGetRequestBodyWithEmptyArray() {
		// call under test
		assertEquals(Optional.empty(), translator.getRequestBody(new ArrayList<>(), new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGetRequestBodyWithHttpServletResponseParameter() {
		List<VariableElement> parameters = new ArrayList<>();

		VariableElement param = Mockito.mock(VariableElement.class);
		TypeMirror paramType = Mockito.mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn("javax.servlet.http.HttpServletResponse");
		parameters.add(param);

		// call under test
		assertEquals(Optional.empty(), translator.getRequestBody(parameters, new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGetRequestBodyWithHttpServletRequestParameter() {
		List<VariableElement> parameters = new ArrayList<>();

		VariableElement param = Mockito.mock(VariableElement.class);
		TypeMirror paramType = Mockito.mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn("javax.servlet.http.HttpServletRequest");
		parameters.add(param);

		// call under test
		assertEquals(Optional.empty(), translator.getRequestBody(parameters, new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGetRequestBodyWithUriComponentsBuilderParameter() {
		List<VariableElement> parameters = new ArrayList<>();

		VariableElement param = Mockito.mock(VariableElement.class);
		TypeMirror paramType = Mockito.mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn("org.springframework.web.util.UriComponentsBuilder");
		parameters.add(param);

		// call under test
		assertEquals(Optional.empty(), translator.getRequestBody(parameters, new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGetParametersWithRequestBodyAnnotation() {
		List<VariableElement> params = new ArrayList<>();
		VariableElement param = Mockito.mock(VariableElement.class);
		TypeMirror paramType = Mockito.mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn(MOCK_CLASS_NAME);
		params.add(param);
		AnnotationMirror mockAnnoMirror = Mockito.mock(AnnotationMirror.class);
		Mockito.doReturn(mockAnnoMirror).when(translator).getParameterAnnotation(any(VariableElement.class));
		Mockito.doReturn("RequestBody").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));

		// call under test
		assertEquals(new ArrayList<>(), translator.getParameters(params, new HashMap<>(), new HashMap<>()));

		verify(translator).getParameterAnnotation(param);
		verify(translator).getSimpleAnnotationName(mockAnnoMirror);
	}

	@Test
	public void testGetParameters() {
		Mockito.doReturn(ParameterLocation.path).when(translator).getParameterLocation(any(VariableElement.class));
		JsonSchema expectedSchema = new JsonSchema();
		expectedSchema.setType(Type.integer);
		expectedSchema.setFormat("int32");

		List<VariableElement> parameters = new ArrayList<>();
		VariableElement param = Mockito.mock(VariableElement.class);
		Name paramName = Mockito.mock(Name.class);
		when(param.getSimpleName()).thenReturn(paramName);
		when(paramName.toString()).thenReturn(PARAM_1_NAME);
		TypeMirror paramType = Mockito.mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.getKind()).thenReturn(TypeKind.INT);
		when(paramType.toString()).thenReturn(MOCK_CLASS_NAME);
		parameters.add(param);

		Map<String, String> mockParamToDescription = new HashMap<>();
		mockParamToDescription.put(PARAM_1_NAME, PARAM_1_DESCRIPTION);

		// call under test
		Map<String, ObjectSchema> schemaMap = new HashMap<>();
		assertEquals(getExpectedParameters(), translator.getParameters(parameters, mockParamToDescription, schemaMap));

		verify(translator).getParameterLocation(param);
		verify(translator).populateSchemaMap(MOCK_CLASS_NAME, TypeKind.INT, schemaMap);
		verify(param).getSimpleName();
		verify(param, Mockito.times(3)).asType();
		verify(paramType).getKind();
	}

	@Test
	public void testGetParametersWithEmptyArray() {
		// call under test
		assertEquals(new ArrayList<>(), translator.getParameters(new ArrayList<>(), new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGetParametersWithHttpServletRequestParameter() {
		List<VariableElement> parameters = new ArrayList<>();

		VariableElement param = Mockito.mock(VariableElement.class);
		TypeMirror paramType = Mockito.mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn("javax.servlet.http.HttpServletRequest");
		parameters.add(param);

		// call under test
		assertEquals(new ArrayList<>(), translator.getParameters(parameters, new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGetParametersWithHttpServletResponseParameter() {
		List<VariableElement> parameters = new ArrayList<>();

		VariableElement param = Mockito.mock(VariableElement.class);
		TypeMirror paramType = Mockito.mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn("javax.servlet.http.HttpServletResponse");
		parameters.add(param);

		// call under test
		assertEquals(new ArrayList<>(), translator.getParameters(parameters, new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGetParametersWithUriComponentsBuilderParameter() {
		List<VariableElement> parameters = new ArrayList<>();

		VariableElement param = Mockito.mock(VariableElement.class);
		TypeMirror paramType = Mockito.mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramType);
		when(paramType.toString()).thenReturn("org.springframework.web.util.UriComponentsBuilder");
		parameters.add(param);

		// call under test
		assertEquals(new ArrayList<>(), translator.getParameters(parameters, new HashMap<>(), new HashMap<>()));
	}

	@Test
	public void testGenerateObjectSchemaForPrimitiveTypeWithUnhandledType() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.generateObjectSchemaForPrimitiveType(TypeKind.MODULE);
		});
		assertEquals("Unrecognized primitive type MODULE", exception.getMessage());
	}

	@Test
	public void testGenerateObjectSchemaForPrimitiveTypeWithStringType() {
		ObjectSchema schema = null;
		try {
			JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
			schema = new ObjectSchemaImpl(adpater);
		} catch (Exception e) {
			// this should never happen
			throw new RuntimeException("Error creating adapter for schema");
		}

		schema.setType(TYPE.STRING);
		assertEquals(schema, translator.generateObjectSchemaForPrimitiveType(TypeKind.DECLARED));
	}

	@Test
	public void testGenerateObjectSchemaForPrimitiveTypeWithNumberType() {
		ObjectSchema schema = null;
		try {
			JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
			schema = new ObjectSchemaImpl(adpater);
		} catch (Exception e) {
			// this should never happen
			throw new RuntimeException("Error creating adapter for schema");
		}

		schema.setType(TYPE.NUMBER);
		assertEquals(schema, translator.generateObjectSchemaForPrimitiveType(TypeKind.DOUBLE));
		assertEquals(schema, translator.generateObjectSchemaForPrimitiveType(TypeKind.LONG));
		assertEquals(schema, translator.generateObjectSchemaForPrimitiveType(TypeKind.FLOAT));
	}

	@Test
	public void testGenerateObjectSchemaForPrimitiveTypeWithBooleanType() {
		ObjectSchema schema = null;
		try {
			JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
			schema = new ObjectSchemaImpl(adpater);
		} catch (Exception e) {
			// this should never happen
			throw new RuntimeException("Error creating adapter for schema");
		}

		schema.setType(TYPE.BOOLEAN);
		assertEquals(schema, translator.generateObjectSchemaForPrimitiveType(TypeKind.BOOLEAN));
	}

	@Test
	public void testGenerateObjectSchemaForPrimitiveTypeWithIntType() {
		ObjectSchema schema = null;
		try {
			JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
			schema = new ObjectSchemaImpl(adpater);
		} catch (Exception e) {
			// this should never happen
			throw new RuntimeException("Error creating adapter for schema");
		}

		schema.setType(TYPE.INTEGER);
		assertEquals(schema, translator.generateObjectSchemaForPrimitiveType(TypeKind.INT));
	}

	@Test
	public void testGenerateObjectSchemaForPrimitiveTypeWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.generateObjectSchemaForPrimitiveType(null);
		});
		assertEquals("type is required.", exception.getMessage());
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
		verify(translator).getSimpleAnnotationName(mockAnnoMirror);
		verify(translator).getParameterAnnotation(mockVarElement);
	}

	@Test
	public void testGetParameterLocationWithRequestBodyAnnotation() {
		Mockito.doReturn("RequestBody").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		AnnotationMirror mockAnnoMirror = Mockito.mock(AnnotationMirror.class);
		Mockito.doReturn(mockAnnoMirror).when(translator).getParameterAnnotation(any(VariableElement.class));
		VariableElement mockVarElement = Mockito.mock(VariableElement.class);
		// call under test
		assertEquals(null, translator.getParameterLocation(mockVarElement));

		verify(translator).getSimpleAnnotationName(mockAnnoMirror);
		verify(translator).getParameterAnnotation(mockVarElement);
	}

	@Test
	public void testGetParameterLocationWithRequestParamAnnotation() {
		Mockito.doReturn("RequestParam").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		AnnotationMirror mockAnnoMirror = Mockito.mock(AnnotationMirror.class);
		Mockito.doReturn(mockAnnoMirror).when(translator).getParameterAnnotation(any(VariableElement.class));
		VariableElement mockVarElement = Mockito.mock(VariableElement.class);

		// call under test
		assertEquals(ParameterLocation.query, translator.getParameterLocation(mockVarElement));

		verify(translator).getSimpleAnnotationName(mockAnnoMirror);
		verify(translator).getParameterAnnotation(mockVarElement);
	}

	@Test
	public void testGetParameterLocationWithPathVariableAnnotation() {
		Mockito.doReturn("PathVariable").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		Mockito.doReturn(Mockito.mock(AnnotationMirror.class)).when(translator).getParameterAnnotation(any());
		// call under test
		assertEquals(ParameterLocation.path, translator.getParameterLocation(null));
	}

	@Test
	public void testGetParameterLocationWithRequestHeaderAnnotation() {
		Mockito.doReturn("RequestHeader").when(translator).getSimpleAnnotationName(any(AnnotationMirror.class));
		AnnotationMirror mockAnnoMirror = Mockito.mock(AnnotationMirror.class);
		Mockito.doReturn(mockAnnoMirror).when(translator).getParameterAnnotation(any(VariableElement.class));
		VariableElement mockVarElement = Mockito.mock(VariableElement.class);

		// call under test
		assertEquals(ParameterLocation.header, translator.getParameterLocation(mockVarElement));

		verify(translator).getSimpleAnnotationName(mockAnnoMirror);
		verify(translator).getParameterAnnotation(mockVarElement);
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

		verify(mockParameter).getAnnotationMirrors();
		verify(annotationMirrors).size();
		verify(annotationMirrors).get(0);
	}

	@Test
	public void testGetParameterAnnotationWithAnnotationMirrorsSize2() {
		VariableElement mockParameter = Mockito.mock(VariableElement.class);
		List<? extends AnnotationMirror> annotationMirrors = Mockito.mock(List.class);
		Mockito.doReturn(annotationMirrors).when(mockParameter).getAnnotationMirrors();
		Name mockParamName = Mockito.mock(Name.class);
		when(mockParameter.getSimpleName()).thenReturn(mockParamName);
		when(mockParamName.toString()).thenReturn("testMethod");
		when(annotationMirrors.size()).thenReturn(2);
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			translator.getParameterAnnotation(mockParameter);
		});
		assertEquals("Each method parameter should have one annotation, testMethod has 2", exception.getMessage());
		verify(mockParameter).getAnnotationMirrors();
		verify(annotationMirrors, Mockito.times(2)).size();
	}

	@Test
	public void testGetParameterAnnotationWithEmptyAnnotationMirrors() throws Exception {
		VariableElement mockParameter = Mockito.mock(VariableElement.class);
		when(mockParameter.getAnnotationMirrors()).thenReturn(new ArrayList<>());
		Name mockParamName = Mockito.mock(Name.class);
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
		AnnotationMirror annotation = Mockito.mock(AnnotationMirror.class);

		DeclaredType mockDeclaredType = Mockito.mock(DeclaredType.class);
		Element element = Mockito.mock(Element.class);
		when(mockDeclaredType.asElement()).thenReturn(element);
		Name simpleName = Mockito.mock(Name.class);
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
		ParamTree param = Mockito.mock(ParamTree.class);
		when(param.getKind()).thenReturn(DocTree.Kind.PARAM);
		List<? extends DocTree> description = Mockito.mock(List.class);
		Mockito.doReturn(description).when(param).getDescription();
		when(description.isEmpty()).thenReturn(true);
		blockTags.add(param);

		// call under test
		assertEquals(new LinkedHashMap<>(), translator.getParameterToDescription(blockTags));

		verify(param).getKind();
		verify(description).isEmpty();
	}

	@Test
	public void testGetParameterToDescriptionWithoutParamTreeInBlockTags() {
		ReturnTree mockReturnComment = Mockito.mock(ReturnTree.class);
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
		ParamTree mockParamComment = Mockito.mock(ParamTree.class);
		when(mockParamComment.getKind()).thenReturn(DocTree.Kind.PARAM);
		// call under test
		assertEquals(Optional.empty(), translator.getReturnComment(new ArrayList<>(Arrays.asList(mockParamComment))));

		verify(mockParamComment).getKind();
	}

	@Test
	public void testGetReturnCommentWithEmptyDescription() {
		List<DocTree> blockTags = new ArrayList<>();
		ReturnTree mockReturnComment = Mockito.mock(ReturnTree.class);
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
		ReturnTree mockReturnComment = Mockito.mock(ReturnTree.class);
		when(mockReturnComment.getKind()).thenReturn(DocTree.Kind.RETURN);
		List<? extends DocTree> descriptions = Mockito.mock(List.class);
		when(descriptions.toString()).thenReturn(METHOD_RETURN_COMMENT);
		Mockito.doReturn(descriptions).when(mockReturnComment).getDescription();
		blockTags.add(mockReturnComment);

		// call under test
		assertEquals(Optional.of(METHOD_RETURN_COMMENT), translator.getReturnComment(blockTags));

		verify(mockReturnComment).getKind();
		verify(mockReturnComment, Mockito.times(2)).getDescription();
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

		verify(fullBody).isEmpty();
	}
}