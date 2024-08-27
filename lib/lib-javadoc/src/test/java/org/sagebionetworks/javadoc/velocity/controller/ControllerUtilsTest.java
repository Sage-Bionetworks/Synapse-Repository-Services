package org.sagebionetworks.javadoc.velocity.controller;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.javadoc.JavaDocTestUtil;
import org.sagebionetworks.javadoc.web.services.FilterUtils;

import com.google.common.collect.ImmutableSet;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.ExecutableElement;
import jdk.javadoc.doclet.DocletEnvironment;

public class ControllerUtilsTest {
	
	private static DocletEnvironment DocletEnvironment;
	private static TypeElement controllerTypeElement;
	private static Map<String, ExecutableElement> methodMap;
	
	@BeforeAll
	public static void beforeClass() throws Exception {
		// Lookup the test files.
		DocletEnvironment = JavaDocTestUtil.buildDocletEnvironment("ExampleController.java");
		assertNotNull(DocletEnvironment);
        Iterator<TypeElement> contollers = FilterUtils.controllerIterator(DocletEnvironment.classes());
        assertNotNull(contollers);
        assertTrue(contollers.hasNext());
        controllerTypeElement = contollers.next();
        assertNotNull(controllerTypeElement);
        methodMap = new HashMap<String, ExecutableElement>();
    	Iterator<ExecutableElement> methodIt = FilterUtils.requestMappingIterator(controllerTypeElement.methods());
    	while(methodIt.hasNext()){
    		ExecutableElement ExecutableElement = methodIt.next();
    		methodMap.put(ExecutableElement.name(), ExecutableElement);
    	}
	}
	
	@Test
	public void testTranslateToModel(){
		ControllerModel model = ControllerUtils.translateToModel(controllerTypeElement);
		assertNotNull(model);
		System.out.println(model);
		assertEquals("ExampleController", model.getName());
		assertEquals("Example Service", model.getDisplayName());
		assertEquals("example/v1", model.getPath());
		assertNotNull(model.getClassDescription());
		assertNotNull(model.getMethods());
		assertTrue(model.getMethods().size() > 0);
	}
	
	@Test
	public void testTranslateMethod(){
		ExecutableElement method = methodMap.get("getRowMetadataDelta");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		assertEquals("/multiple/params", model.getUrl());
		assertEquals("GET", model.getHttpType());
		assertEquals("GET.multiple.params", model.getFullMethodName());
		assertEquals(new Link("${org.sagebionetworks.repo.model.migration.MigrationTypeList}", "MigrationTypeList"), model.getResponseBody());
		assertEquals(new Link("${org.sagebionetworks.repo.model.IdList}", "IdList"), model.getRequestBody());
		assertEquals(new Link("${GET.multiple.params}", "GET /multiple/params"), model.getMethodLink());
		assertNotNull(model.getDescription());
		assertNotNull(model.getShortDescription());
		assertEquals(ImmutableSet.of("view","modify"), new HashSet<String>(Arrays.asList(model.getRequiredScopes())));
	}
	
	@Test
	public void testTranslateMethodLink(){
		ExecutableElement method = methodMap.get("createCompetitionWikiPage");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		assertEquals("POST.evaluation.ownerId.wiki", model.getFullMethodName());
		assertEquals(new Link("${POST.evaluation.ownerId.wiki}", "POST /evaluation/{ownerId}/wiki"), model.getMethodLink());
	}
	
	@Test
	public void testPathVariables(){
		ExecutableElement method = methodMap.get("createEntityWikiPage");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		assertNotNull(model.getPathVariables());
		assertEquals(1, model.getPathVariables().size());
		ParameterModel pathParam = model.getPathVariables().get(0);
		assertNotNull(pathParam);
		assertEquals("ownerId", pathParam.getName());
		assertNotNull(pathParam.getDescription());
	}
	
	@Test
	public void testPathVariablesWithRegEx(){
		ExecutableElement method = methodMap.get("pathIncludesRegEx");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		assertNotNull(model.getPathVariables());
		assertEquals(1, model.getPathVariables().size());
		ParameterModel pathParam = model.getPathVariables().get(0);
		assertNotNull(pathParam);
		assertEquals("id", pathParam.getName());
		assertEquals("POST.someOther.id.secondId", model.getFullMethodName());
		assertNotNull(model.getMethodLink());
		assertEquals("POST /someOther/{id}/{secondId}", model.getMethodLink().getDisplay());
		assertEquals("/someOther/{id}/{secondId}", model.getUrl());
	}
	
	@Test
	public void testPathVariablesWithStar(){
		ExecutableElement method = methodMap.get("pathIncludesStar");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		assertNotNull(model.getPathVariables());
		assertEquals(1, model.getPathVariables().size());
		ParameterModel pathParam = model.getPathVariables().get(0);
		assertNotNull(pathParam);
		assertEquals("id", pathParam.getName());
		assertEquals("POST.someOther.id", model.getFullMethodName());
		assertNotNull(model.getMethodLink());
		assertEquals("POST /someOther/{id}", model.getMethodLink().getDisplay());
		assertEquals("/someOther/{id}", model.getUrl());
	}
	
	@Test
	public void testParameters(){
		ExecutableElement method = methodMap.get("getRowMetadataDelta");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		assertNotNull(model.getParameters());
		assertEquals(4, model.getParameters().size());
		// one
		ParameterModel param = model.getParameters().get(0);
		assertNotNull(param);
		assertEquals("type", param.getName());
		assertNotNull(param.getDescription());
		assertFalse(param.getIsOptional());
		// two
		param = model.getParameters().get(1);
		assertNotNull(param);
		assertEquals("limit", param.getName());
		assertNotNull(param.getDescription());
		assertTrue(param.getIsOptional());
		// three
		param = model.getParameters().get(2);
		assertNotNull(param);
		assertEquals("offset", param.getName());
		assertNotNull(param.getDescription());
		assertTrue(param.getIsOptional());
		// four
		param = model.getParameters().get(3);
		assertNotNull(param);
		assertEquals("foo", param.getName());
		assertNotNull(param.getDescription());
		assertTrue(param.getIsOptional());
	}

	@Test
	public void testAuthetincationNotRequired(){
		ExecutableElement method = methodMap.get("noAuthPathVariable");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		assertFalse(model.getIsAuthenticationRequired());
	}
	
	@Test
	public void testAuthenticationRequired(){
		ExecutableElement method = methodMap.get("getRowMetadataDelta");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		assertTrue(model.getIsAuthenticationRequired());
	}
	
	@Test
	public void testAuthenticationRequiredViaHeader(){
		ExecutableElement method = methodMap.get("authorizedService");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		assertTrue(model.getIsAuthenticationRequired());
	}
	
	@Test
	public void testInterface(){
		ExecutableElement method = methodMap.get("getInterface");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		Link responseLink = model.getResponseBody();
		assertNotNull(responseLink);
	}

	@Test
	public void testGenericReturn() {
		ExecutableElement method = methodMap.get("someGenericReturn");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		Link responseLink = model.getResponseBody();
		assertEquals(new Link("${org.sagebionetworks.javadoc.testclasses.GenericList}", "GenericList"), responseLink);
		Link[] responseGenParamLinks = model.getResponseBodyGenericParams();
		assertEquals(new Link("${org.sagebionetworks.repo.model.Entity}", "Entity"), responseGenParamLinks[0]);
	}

	@Test
	public void testGenericParam() {
		ExecutableElement method = methodMap.get("someGenericParam");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		Link requestLink = model.getRequestBody();
		assertEquals(new Link("${org.sagebionetworks.javadoc.testclasses.GenericList}", "GenericList"), requestLink);
		Link[] requestGenParamLinks = model.getRequestBodyGenericParams();
		assertEquals(new Link("${org.sagebionetworks.repo.model.Annotations}", "Annotations"), requestGenParamLinks[0]);
	}

	@Test
	public void testCreateTruncatedTextNull(){
		String result = ControllerUtils.createTruncatedText(100, null);
		assertEquals(null, result);
	}
	
	@Test
	public void testCreateTruncatedTextWithHTML(){
		String input = "This is <a href=\"some.link\">Dispaly</a>";
		// Spliting in the middle of an HTML tag will generate bad text so we need to avoid doing so.
		String result = ControllerUtils.createTruncatedText(15, input);
		String expected = "This is Dispaly&#8230";
		assertEquals(expected, result);
	}
	
	@Test
	public void testCreateTruncatedTextWithNoHTML(){
		String input = "This is text is longer than the max";
		// Spliting in the middle of an HTML tag will generate bad text so we need to avoid doing so.
		String result = ControllerUtils.createTruncatedText(15, input);
		String expected = "This is text is&#8230";
		assertEquals(expected, result);
	}
}
