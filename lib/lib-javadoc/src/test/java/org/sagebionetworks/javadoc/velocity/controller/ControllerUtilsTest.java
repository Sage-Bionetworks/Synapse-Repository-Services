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
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;

public class ControllerUtilsTest {
	
	private static RootDoc rootDoc;
	private static ClassDoc controllerClassDoc;
	private static Map<String, MethodDoc> methodMap;
	
	@BeforeAll
	public static void beforeClass() throws Exception {
		// Lookup the test files.
		rootDoc = JavaDocTestUtil.buildRootDoc("ExampleController.java");
		assertNotNull(rootDoc);
        Iterator<ClassDoc> contollers = FilterUtils.controllerIterator(rootDoc.classes());
        assertNotNull(contollers);
        assertTrue(contollers.hasNext());
        controllerClassDoc = contollers.next();
        assertNotNull(controllerClassDoc);
        methodMap = new HashMap<String, MethodDoc>();
    	Iterator<MethodDoc> methodIt = FilterUtils.requestMappingIterator(controllerClassDoc.methods());
    	while(methodIt.hasNext()){
    		MethodDoc methodDoc = methodIt.next();
    		methodMap.put(methodDoc.name(), methodDoc);
    	}
	}
	
	@Test
	public void testTranslateToModel(){
		ControllerModel model = ControllerUtils.translateToModel(controllerClassDoc);
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
		MethodDoc method = methodMap.get("getRowMetadataDelta");
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
		MethodDoc method = methodMap.get("createCompetitionWikiPage");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		assertEquals("POST.evaluation.ownerId.wiki", model.getFullMethodName());
		assertEquals(new Link("${POST.evaluation.ownerId.wiki}", "POST /evaluation/{ownerId}/wiki"), model.getMethodLink());
	}
	
	@Test
	public void testPathVariables(){
		MethodDoc method = methodMap.get("createEntityWikiPage");
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
		MethodDoc method = methodMap.get("pathIncludesRegEx");
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
		MethodDoc method = methodMap.get("pathIncludesStar");
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
		MethodDoc method = methodMap.get("getRowMetadataDelta");
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
		MethodDoc method = methodMap.get("noAuthPathVariable");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		assertFalse(model.getIsAuthenticationRequired());
	}
	
	@Test
	public void testAuthenticationRequired(){
		MethodDoc method = methodMap.get("getRowMetadataDelta");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		assertTrue(model.getIsAuthenticationRequired());
	}
	
	@Test
	public void testAuthenticationRequiredViaHeader(){
		MethodDoc method = methodMap.get("authorizedService");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		assertTrue(model.getIsAuthenticationRequired());
	}
	
	@Test
	public void testInterface(){
		MethodDoc method = methodMap.get("getInterface");
		assertNotNull(method);
		// Now translate the message
		MethodModel model = ControllerUtils.translateMethod(method);
		assertNotNull(model);
		Link responseLink = model.getResponseBody();
		assertNotNull(responseLink);
	}

	@Test
	public void testGenericReturn() {
		MethodDoc method = methodMap.get("someGenericReturn");
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
		MethodDoc method = methodMap.get("someGenericParam");
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
