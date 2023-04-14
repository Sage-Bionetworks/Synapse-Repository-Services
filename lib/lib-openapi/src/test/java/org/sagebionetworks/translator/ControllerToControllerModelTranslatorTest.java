package org.sagebionetworks.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import org.springframework.web.bind.annotation.RequestMethod;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.IdentifierTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.util.DocTrees;

import jdk.javadoc.doclet.DocletEnvironment;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DocletToControllerModelTranslatorTest {
	private DocletToControllerModelTranslator translator;

	@Mock
	private DocletEnvironment docletEnvironment;
	
	private final String PARAM_1_NAME = "PARAM_1";
	private final String PARAM_2_NAME = "PARAM_2";
	private final String PARAM_1_DESCRIPTION = "PARAM_DESCRIPTION_1";
	private final String PARAM_2_DESCRIPTION = "PARAM_DESCRIPTION_2";
	private final String METHOD_NAME = "METHOD_NAME";
	private final String METHOD_BEHAVIOR_COMMENT = "BEHAVIOR_COMMENT";
	private final String METHOD_RETURN_COMMENT = "RETURN_COMMENT";
	private final String METHOD_PATH= "/fake/path";
	private final String CONTROLLER_NAME = "MOCK_CONTROLLER";	

	@BeforeEach
	private void setUp() {
		DocTrees docTrees = Mockito.mock(DocTrees.class);
		Set<Element> includedElements = new LinkedHashSet<>();
		Mockito.doReturn(includedElements).when(docletEnvironment).getIncludedElements();
		Mockito.doReturn(docTrees).when(docletEnvironment).getDocTrees();

		// set up DocTree
		DocCommentTree mockDocCommentTree = Mockito.mock(DocCommentTree.class);
		when(docTrees.getDocCommentTree(any(Element.class))).thenReturn(mockDocCommentTree);

		List<DocTree> blockTags = getMockBlockTags();
		Mockito.doReturn(blockTags).when(mockDocCommentTree).getBlockTags();

		List<DocTree> fullBody = getMockFullBody();
		Mockito.doReturn(fullBody).when(mockDocCommentTree).getFullBody();

		// Set up included elements
		TypeElement controller = getMockController();
		when(controller.getKind()).thenReturn(ElementKind.CLASS);
		includedElements.add(controller);
	}

	private TypeElement getMockController() {
		TypeElement controller = Mockito.mock(TypeElement.class);
		when(controller.toString()).thenReturn(CONTROLLER_NAME);
		List<Element> enclosedElements = new ArrayList<>();
		Mockito.doReturn(enclosedElements).when(controller).getEnclosedElements();

		ExecutableElement method = getMockMethod();
		enclosedElements.add(method);
		return controller;
	}

	private ExecutableElement getMockMethod() {
		ExecutableElement method = Mockito.mock(ExecutableElement.class);
		when(method.getKind()).thenReturn(ElementKind.METHOD);

		Name methodName = Mockito.mock(Name.class);
		when(method.getSimpleName()).thenReturn(methodName);
		when(methodName.toString()).thenReturn(METHOD_NAME);

		List<VariableElement> parameters = getMockParameters();
		Mockito.doReturn(parameters).when(method).getParameters();

		List<AnnotationMirror> methodAnnotations = getMockMethodAnnotations();
		Mockito.doReturn(methodAnnotations).when(method).getAnnotationMirrors();

		TypeMirror methodReturnType = Mockito.mock(TypeMirror.class);
		when(methodReturnType.getKind()).thenReturn(TypeKind.INT);
		when(method.getReturnType()).thenReturn(methodReturnType);

		return method;
	}

	private List<AnnotationMirror> getMockMethodAnnotations() {
		List<AnnotationMirror> methodAnnotations = new ArrayList<>();
		methodAnnotations.add(getRequestMappingAnnotation());
		methodAnnotations.add(getResponseStatusAnnotation());
		return methodAnnotations;
	}

	private AnnotationMirror getRequestMappingAnnotation() {
		AnnotationMirror annotation = Mockito.mock(AnnotationMirror.class);

		DeclaredType mockDeclaredType = getAnnotationType("RequestMapping");
		when(annotation.getAnnotationType()).thenReturn(mockDeclaredType);

		Map<ExecutableElement, AnnotationValue> elementValues = new HashMap<>();
		Mockito.doReturn(elementValues).when(annotation).getElementValues();
		setElementValue("method", RequestMethod.GET, elementValues);
		setElementValue("value", METHOD_PATH, elementValues);

		return annotation;
	}
	
	private AnnotationMirror getResponseStatusAnnotation() {
		AnnotationMirror annotation = Mockito.mock(AnnotationMirror.class);

		DeclaredType mockDeclaredType = getAnnotationType("ResponseStatus");
		when(annotation.getAnnotationType()).thenReturn(mockDeclaredType);

		Map<ExecutableElement, AnnotationValue> elementValues = new HashMap<>();
		Mockito.doReturn(elementValues).when(annotation).getElementValues();
		setElementValue("value", HttpStatus.OK, elementValues);

		return annotation;
	}

	private void setElementValue(String executableMethodName, Object value,
			Map<ExecutableElement, AnnotationValue> elementValues) {
		ExecutableElement annotationMethod = Mockito.mock(ExecutableElement.class);
		Name simpleName = Mockito.mock(Name.class);
		when(annotationMethod.getSimpleName()).thenReturn(simpleName);
		when(simpleName.toString()).thenReturn(executableMethodName);

		AnnotationValue annotationValue = Mockito.mock(AnnotationValue.class);
		when(annotationValue.getValue()).thenReturn(value);
		elementValues.put(annotationMethod, annotationValue);
	}

	private List<VariableElement> getMockParameters() {
		List<VariableElement> parameters = new ArrayList<>();
		
		VariableElement param1 = generateMockParameter(PARAM_1_NAME, TypeKind.INT, "PathVariable");
		parameters.add(param1);
		VariableElement param2 = generateMockParameter(PARAM_2_NAME, TypeKind.INT, "RequestBody");
		parameters.add(param2);
		
		return parameters;
	}

	private VariableElement generateMockParameter(String simpleParamName, TypeKind parameterKind,
			String annotationSimpleName) {
		VariableElement param = Mockito.mock(VariableElement.class);
		Name simpleName = Mockito.mock(Name.class);
		when(param.getSimpleName()).thenReturn(simpleName);
		when(simpleName.toString()).thenReturn(simpleParamName);
		TypeMirror paramKind = Mockito.mock(TypeMirror.class);
		when(param.asType()).thenReturn(paramKind);
		when(paramKind.getKind()).thenReturn(parameterKind);

		List<AnnotationMirror> annotations = new ArrayList<>();
		AnnotationMirror annotationMirror = Mockito.mock(AnnotationMirror.class);
		annotations.add(annotationMirror);
		Mockito.doReturn(annotations).when(param).getAnnotationMirrors();

		DeclaredType mockDeclaredType = getAnnotationType(annotationSimpleName);
		when(annotationMirror.getAnnotationType()).thenReturn(mockDeclaredType);

		return param;
	}

	private DeclaredType getAnnotationType(String simpleNameAsString) {
		DeclaredType mockDeclaredType = Mockito.mock(DeclaredType.class);
		Element element = Mockito.mock(Element.class);
		when(mockDeclaredType.asElement()).thenReturn(element);
		Name simpleName = Mockito.mock(Name.class);
		when(element.getSimpleName()).thenReturn(simpleName);
		when(simpleName.toString()).thenReturn(simpleNameAsString);
		return mockDeclaredType;
	}

	private List<DocTree> getMockFullBody() {
		List<DocTree> fullBody = new ArrayList<>();
		DocTree mockBehaviorComment = Mockito.mock(DocTree.class);
		when(mockBehaviorComment.toString()).thenReturn(METHOD_BEHAVIOR_COMMENT);
		fullBody.add(mockBehaviorComment);
		return fullBody;
	}

	private List<DocTree> getMockBlockTags() {
		List<DocTree> blockTags = new ArrayList<>();
		ParamTree mockParamComment1 = getMockParamComment(PARAM_1_NAME, PARAM_1_DESCRIPTION);
		blockTags.add(mockParamComment1);
		ParamTree mockParamComment2 = getMockParamComment(PARAM_2_NAME, PARAM_2_DESCRIPTION);
		blockTags.add(mockParamComment2);

		ReturnTree mockReturnComment = Mockito.mock(ReturnTree.class);
		when(mockReturnComment.getKind()).thenReturn(DocTree.Kind.RETURN);
		List<? extends DocTree> returnCommentDescriptions = Mockito.mock(List.class);
		DocTree mockReturnCommentDescription = Mockito.mock(DocTree.class);
		Mockito.doReturn(returnCommentDescriptions).when(mockReturnComment).getDescription();
		when(returnCommentDescriptions.isEmpty()).thenReturn(false);
		Mockito.doReturn(mockReturnCommentDescription).when(returnCommentDescriptions).get(0);
		when(mockReturnCommentDescription.toString()).thenReturn(METHOD_RETURN_COMMENT);
		blockTags.add(mockReturnComment);

		return blockTags;
	}
	
	private ParamTree getMockParamComment(String name, String description) {
		ParamTree mockParamComment = Mockito.mock(ParamTree.class);
		when(mockParamComment.getKind()).thenReturn(DocTree.Kind.PARAM);
		IdentifierTree paramIdentifier = Mockito.mock(IdentifierTree.class);
		when(mockParamComment.getName()).thenReturn(paramIdentifier);
		when(paramIdentifier.toString()).thenReturn(name);

		List<? extends DocTree> paramCommentDescriptions = Mockito.mock(List.class);
		Mockito.doReturn(paramCommentDescriptions).when(mockParamComment).getDescription();
		when(paramCommentDescriptions.isEmpty()).thenReturn(false);
		DocTree mockDescription = Mockito.mock(DocTree.class);
		Mockito.doReturn(mockDescription).when(paramCommentDescriptions).get(0);
		when(mockDescription.toString()).thenReturn(description);
		
		return mockParamComment;
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
		return new RequestBodyModel().withDescription(PARAM_2_DESCRIPTION)
				.withRequired(true).withSchema(expectedSchema);
	}
	
	private ResponseModel getExpectedResponseModel() {
		JsonSchema schema = new JsonSchema();
		schema.setType(Type.integer);
		schema.setFormat("int32");
		return new ResponseModel().withDescription(METHOD_RETURN_COMMENT).withContentType("application/json")
				.withStatusCode(200).withSchema(schema);
	}
	
	private List<MethodModel> getExpectedMethods() {
		List<MethodModel> expectedMethods = new ArrayList<>();
		MethodModel expectedMethod = new MethodModel().withPath(METHOD_PATH).withDescription(METHOD_BEHAVIOR_COMMENT)
				.withName(METHOD_NAME).withOperation(Operation.get).withParameters(getExpectedParameters())
				.withRequestBody(getExpectedRequestBody()).withResponse(getExpectedResponseModel());
		expectedMethods.add(expectedMethod);
		return expectedMethods;
	}
	
	@Test
	public void testTranslate() {
		ControllerModel expectedControllerModel = new ControllerModel().withDisplayName(CONTROLLER_NAME)
				.withMethods(getExpectedMethods()).withPath("/");
		// call under test
		assertEquals(expectedControllerModel, translator.translate(docletEnvironment));
	}
	
	@Test
	public void testGetMethods() {		
		List<? extends Element> includedElements = new ArrayList<>(docletEnvironment.getIncludedElements());
		TypeElement controller = (TypeElement) includedElements.get(0);
		// call under test
		assertEquals(getExpectedMethods(),
				translator.getMethods(controller.getEnclosedElements(), docletEnvironment.getDocTrees()));
	}

	@Test
	public void testGetMethodsWithEmptyEnclosedElements() {
		// call under test
		assertEquals(new ArrayList<>(), translator.getMethods(new ArrayList<>(), docletEnvironment.getDocTrees()));
	}

	@Test
	public void testGetResponseModelReturnsCorrectModel() {
		Element testElement = Mockito.mock(Element.class);
		DocCommentTree mockDocCommentTree = docletEnvironment.getDocTrees().getDocCommentTree(testElement);
		Map<String, Map<String, Object>> annotationToElements = new HashMap<>();
		annotationToElements.put("ResponseStatus", new HashMap<>());
		annotationToElements.get("ResponseStatus").put("value", HttpStatus.OK);
		
		ResponseModel actual = translator.getResponseModel(TypeKind.INT, mockDocCommentTree.getBlockTags(),
				annotationToElements);
		// call under test
		assertEquals(getExpectedResponseModel(), actual);
	}

	@Test
	public void testGetStatusCodeReturnsCorrectCode() {
		Map<String, Map<String, Object>> annotationToElements = new HashMap<>();
		annotationToElements.put("ResponseStatus", new HashMap<>());
		annotationToElements.get("ResponseStatus").put("placeholder", "placeholder");
		annotationToElements.get("ResponseStatus").put("value", HttpStatus.OK);
		// call under test
		assertEquals(200, translator.getStatusCode(annotationToElements));
	}

	@Test
	public void testGetStatusCodeWithoutResponseStatusValueAnnotation() {
		Map<String, Map<String, Object>> annotationToElements = new HashMap<>();
		annotationToElements.put("ResponseStatus", new HashMap<>());
		annotationToElements.get("ResponseStatus").put("placeholder", "placeholder");
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getStatusCode(annotationToElements);
		});
	}

	@Test
	public void testGetStatusCodeWithoutResponseStatusAnnotation() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getStatusCode(new HashMap<>());
		});
	}

	@Test
	public void testGetMethodPathReturnsCorrectPath() {
		Map<String, Map<String, Object>> annotationToElements = new HashMap<>();
		annotationToElements.put("RequestMapping", new HashMap<>());
		annotationToElements.get("RequestMapping").put("placeholder", "placeholder");
		annotationToElements.get("RequestMapping").put("value", METHOD_PATH);
		// call under test
		assertEquals(METHOD_PATH, translator.getMethodPath(annotationToElements));
	}

	@Test
	public void testGetMethodPathWithoutRequestMappingValueAnnotation() {
		Map<String, Map<String, Object>> annotationToElements = new HashMap<>();
		annotationToElements.put("RequestMapping", new HashMap<>());
		annotationToElements.get("RequestMapping").put("placeholder", "placeholder");
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getMethodPath(annotationToElements);
		});
	}

	@Test
	public void testGetMethodPathWithoutRequestMappingAnnotation() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getMethodPath(new HashMap<>());
		});
	}

	@Test
	public void testGetMethodOperationReturnsCorrectOperation() {
		Map<String, Map<String, Object>> annotationToElements = new HashMap<>();
		annotationToElements.put("RequestMapping", new HashMap<>());
		annotationToElements.get("RequestMapping").put("placeholder", "placeholder");
		annotationToElements.get("RequestMapping").put("method", RequestMethod.GET);
		// call under test
		assertEquals(Operation.get, translator.getMethodOperation(annotationToElements));
	}

	@Test
	public void testGetMethodOperationWithoutRequestMappingMethodAnnotation() {
		Map<String, Map<String, Object>> annotationToElements = new HashMap<>();
		annotationToElements.put("RequestMapping", new HashMap<>());
		annotationToElements.get("RequestMapping").put("placeholder", "placeholder");
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getMethodOperation(annotationToElements);
		});
	}

	@Test
	public void testGetMethodOperationWithoutRequestMappingAnnotation() {
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			translator.getMethodOperation(new HashMap<>());
		});
	}

	@Test
	public void testGetAnnotationToElementsWithRequestMappingAnnotation() {
		List<? extends Element> includedElements = new ArrayList<>(docletEnvironment.getIncludedElements());
		TypeElement controller = (TypeElement) includedElements.get(0);
		ExecutableElement method = (ExecutableElement) controller.getEnclosedElements().get(0);

		Map<String, Map<String, Object>> expectedAnnotationToElements = new HashMap<>();
		expectedAnnotationToElements.put("RequestMapping", new HashMap<>());
		expectedAnnotationToElements.get("RequestMapping").put("method", RequestMethod.GET);
		expectedAnnotationToElements.get("RequestMapping").put("value", METHOD_PATH);
		expectedAnnotationToElements.put("ResponseStatus", new HashMap<>());
		expectedAnnotationToElements.get("ResponseStatus").put("value", HttpStatus.OK);

		// call under test
		assertEquals(expectedAnnotationToElements, translator.getAnnotationToElements(method.getAnnotationMirrors()));
	}

	@Test
	public void testGetAnnotationToElementsWithEmptyOrNullAnnotations() {
		// call under test
		assertEquals(new LinkedHashMap<>(), translator.getAnnotationToElements(new ArrayList<>()));
		assertEquals(new LinkedHashMap<>(), translator.getAnnotationToElements(null));
	}

	@Test
	public void testGetRequestBodyReturnsCorrectRequestBodyModel() {
		List<? extends Element> includedElements = new ArrayList<>(docletEnvironment.getIncludedElements());
		TypeElement controller = (TypeElement) includedElements.get(0);
		ExecutableElement method = (ExecutableElement) controller.getEnclosedElements().get(0);
		List<VariableElement> parameters = new ArrayList<>(method.getParameters());

		VariableElement param2 = generateMockParameter(PARAM_2_NAME, TypeKind.INT, "RequestBody");
		parameters.add(param2);

		Map<String, String> mockParamToDescription = new HashMap<>();
		mockParamToDescription.put(PARAM_1_NAME, PARAM_1_DESCRIPTION);
		mockParamToDescription.put(PARAM_2_NAME, PARAM_2_DESCRIPTION);
		// call under test
		assertEquals(getExpectedRequestBody(), translator.getRequestBody(parameters, mockParamToDescription));
	}

	@Test
	public void testGetRequestBodyWithoutRequestBodyParameter() {
		// call under test
		assertEquals(null, translator.getRequestBody(new ArrayList<>(), new HashMap<>()));
	}

	@Test
	public void testGetParametersReturnsCorrectParameters() {
		List<? extends Element> includedElements = new ArrayList<>(docletEnvironment.getIncludedElements());
		TypeElement controller = (TypeElement) includedElements.get(0);
		ExecutableElement method = (ExecutableElement) controller.getEnclosedElements().get(0);
		List<? extends VariableElement> parameters = method.getParameters();

		Map<String, String> mockParamToDescription = new HashMap<>();
		mockParamToDescription.put(PARAM_1_NAME, PARAM_1_DESCRIPTION);

		// call under test
		assertEquals(getExpectedParameters(), translator.getParameters(parameters, mockParamToDescription));
	}

	@Test
	public void testGetParametersWithEmptyArray() {
		// call under test
		assertEquals(new ArrayList<>(), translator.getParameters(new ArrayList<>(), new HashMap<>()));
	}

	@Test
	public void testGetParameterLocationWithUnknownAnnotation() {
		VariableElement param = Mockito.mock(VariableElement.class);
		List<AnnotationMirror> annotations = new ArrayList<>();
		AnnotationMirror annotationMirror = Mockito.mock(AnnotationMirror.class);
		annotations.add(annotationMirror);
		Mockito.doReturn(annotations).when(param).getAnnotationMirrors();
		DeclaredType mockDeclaredType = getAnnotationType("UNKNOWN_ANNOTATION");
		when(annotationMirror.getAnnotationType()).thenReturn(mockDeclaredType);
		// call under test
		assertEquals(null, translator.getParameterLocation(param));
	}

	@Test
	public void testGetParameterLocationWithRequestParamAnnotation() {
		VariableElement param = Mockito.mock(VariableElement.class);
		List<AnnotationMirror> annotations = new ArrayList<>();
		AnnotationMirror annotationMirror = Mockito.mock(AnnotationMirror.class);
		annotations.add(annotationMirror);
		Mockito.doReturn(annotations).when(param).getAnnotationMirrors();
		DeclaredType mockDeclaredType = getAnnotationType("RequestParam");
		when(annotationMirror.getAnnotationType()).thenReturn(mockDeclaredType);
		// call under test
		assertEquals(ParameterLocation.query, translator.getParameterLocation(param));
	}

	@Test
	public void testGetParameterLocationWithPathVariableAnnotation() {
		List<? extends Element> includedElements = new ArrayList<>(docletEnvironment.getIncludedElements());
		TypeElement controller = (TypeElement) includedElements.get(0);
		ExecutableElement method = (ExecutableElement) controller.getEnclosedElements().get(0);
		List<? extends VariableElement> parameters = method.getParameters();
		VariableElement param = parameters.get(0);
		// call under test
		assertEquals(ParameterLocation.path, translator.getParameterLocation(param));
	}

	@Test
	public void testGetParameterAnnotationReturnsCorrectAnnotation() throws Exception {
		List<? extends Element> includedElements = new ArrayList<>(docletEnvironment.getIncludedElements());
		TypeElement controller = (TypeElement) includedElements.get(0);
		ExecutableElement method = (ExecutableElement) controller.getEnclosedElements().get(0);
		List<? extends VariableElement> parameters = method.getParameters();
		VariableElement param = parameters.get(0);
		List<? extends AnnotationMirror> annotations = param.getAnnotationMirrors();
		// call under test
		assertEquals(annotations.get(0), translator.getParameterAnnotation(param));
	}

	@Test
	public void testGetParameterAnnotationWithEmptyAnnotationMirrors() throws Exception {
		VariableElement mockParameter = Mockito.mock(VariableElement.class);
		when(mockParameter.getAnnotationMirrors()).thenReturn(new ArrayList<>());
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			translator.getParameterAnnotation(mockParameter);
		});
	}

	@Test
	public void testGetParameterAnnotationWithNull() {
		// call under test
		assertEquals(null, translator.getParameterAnnotation(null));
	}

	@Test
	public void testGetSchemaWithArrayTypeKind() {
		JsonSchema expectedSchema = new JsonSchema();
		expectedSchema.setType(Type.array);
		// call under test
		assertEquals(expectedSchema, translator.getSchema(TypeKind.ARRAY));
	}

	@Test
	public void testGetSchemaWithBooleanTypeKind() {
		JsonSchema expectedSchema = new JsonSchema();
		expectedSchema.setType(Type._boolean);
		// call under test
		assertEquals(expectedSchema, translator.getSchema(TypeKind.BOOLEAN));
	}

	@Test
	public void testGetSchemaWithNumberTypeKind() {
		JsonSchema expectedSchema = new JsonSchema();
		expectedSchema.setType(Type.number);
		// call under test
		assertEquals(expectedSchema, translator.getSchema(TypeKind.FLOAT));
		assertEquals(expectedSchema, translator.getSchema(TypeKind.LONG));
		assertEquals(expectedSchema, translator.getSchema(TypeKind.DOUBLE));
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
		assertEquals(new JsonSchema(), translator.getSchema(null));
	}

	@Test
	public void testGetParameterToDescriptionWithParamTreeInBlockTags() {
		Element testElement = Mockito.mock(Element.class);
		DocCommentTree mockDocCommentTree = docletEnvironment.getDocTrees().getDocCommentTree(testElement);

		Map<String, String> expectedParameterToDescription = new HashMap<>();
		expectedParameterToDescription.put(PARAM_1_NAME, PARAM_1_DESCRIPTION);
		expectedParameterToDescription.put(PARAM_2_NAME, PARAM_2_DESCRIPTION);
		// call under test
		assertEquals(expectedParameterToDescription,
				translator.getParameterToDescription(mockDocCommentTree.getBlockTags()));
	}

	@Test
	public void testGetParameterToDescriptionWithoutParamTreeInBlockTags() {
		ReturnTree mockReturnComment = Mockito.mock(ReturnTree.class);
		when(mockReturnComment.getKind()).thenReturn(DocTree.Kind.RETURN);
		// call under test
		assertEquals(new LinkedHashMap<>(),
				translator.getParameterToDescription(new ArrayList<>(Arrays.asList(mockReturnComment))));
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
	public void testGetBehaviorCommentWithNull() {
		// call under test
		assertEquals("", translator.getBehaviorComment(null));
	}

	@Test
	public void testGetBehaviorCommentWithEmptyArray() {
		// call under test
		assertEquals("", translator.getBehaviorComment(new ArrayList<>()));
	}

	@Test
	public void testGetBehaviorCommentWithDescriptionArray() {
		Element testElement = Mockito.mock(Element.class);
		DocCommentTree mockDocCommentTree = docletEnvironment.getDocTrees().getDocCommentTree(testElement);
		// call under test
		assertEquals(METHOD_BEHAVIOR_COMMENT, translator.getBehaviorComment(mockDocCommentTree.getFullBody()));
	}

	@Test
	public void testGetReturnCommentWithNull() {
		// call under test
		assertEquals("", translator.getReturnComment(null));
	}

	@Test
	public void testGetReturnCommentWithEmptyArray() {
		// call under test
		assertEquals("", translator.getReturnComment(new ArrayList<>()));
	}

	@Test
	public void testGetReturnCommentWithoutReturnTreeInArray() {
		ParamTree mockParamComment = Mockito.mock(ParamTree.class);
		when(mockParamComment.getKind()).thenReturn(DocTree.Kind.PARAM);
		// call under test
		assertEquals("", translator.getReturnComment(new ArrayList<>(Arrays.asList(mockParamComment))));
	}

	@Test
	public void testGetReturnCommentWithReturnTreeInArray() {
		Element testElement = Mockito.mock(Element.class);
		DocCommentTree mockDocCommentTree = docletEnvironment.getDocTrees().getDocCommentTree(testElement);

		// call under test
		assertEquals(METHOD_RETURN_COMMENT, translator.getReturnComment(mockDocCommentTree.getBlockTags()));
	}
}