package org.sagebionetworks.javadoc.velocity.controller;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.javadoc.JavaDocTestUtil;
import org.sagebionetworks.javadoc.web.services.FilterUtils;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;

public class ControllerUtilsTest {
	
	private static RootDoc rootDoc;
	private static ClassDoc controllerClassDoc;
	private static Map<String, MethodDoc> methodMap;
	@BeforeClass
	public static void beforeClass(){
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
		assertEquals("/migration/delata", model.getUrl());
		assertEquals("GET", model.getHttpType());
		assertEquals(new Link("${org.sagebionetworks.repo.model.migration.RowMetadataResult}", "RowMetadataResult"), model.getResponseBody());
		assertEquals(new Link("${org.sagebionetworks.repo.model.migration.IdList}", "IdList"), model.getRequestBody());
	}

}
