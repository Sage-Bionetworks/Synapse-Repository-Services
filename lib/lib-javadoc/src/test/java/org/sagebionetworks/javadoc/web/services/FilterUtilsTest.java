package org.sagebionetworks.javadoc.web.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.sagebionetworks.javadoc.JavadocMockUtils.createMockTypeElement;
import static org.sagebionetworks.javadoc.JavadocMockUtils.createMockExecutableElement;

import java.util.Iterator;

import org.junit.Test;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.ExecutableElement;

/**
 * Tests for FilterUtils.
 * 
 * @author John
 *
 */
public class FilterUtilsTest {

	@Test
	public void testControllerIterator(){
		// Create 3 clases
		TypeElement[] testTypeElements = new TypeElement[]{
				createMockTypeElement("one", new String[]{ControllerInfo.class.getName()}),
				createMockTypeElement("two", new String[]{"not.a.controller"}),
				createMockTypeElement("three", new String[]{ControllerInfo.class.getName()}),
		};
		// Create our iterator
		Iterator<TypeElement> it = FilterUtils.controllerIterator(testTypeElements);
		assertNotNull(it);
		int count = 0;
		while(it.hasNext()){
			TypeElement cd = it.next();
			assertNotNull(cd);
			assertTrue("Two should have been filtered out as it was not a controller", "one".equals(cd.qualifiedName()) || "three".equals(cd.qualifiedName()));
			count++;
		}
		assertEquals(2, count);
	}
	
	@Test
	public void testRequestMappingIterator(){
		// Create 3 clases
		ExecutableElement[] testDocs = new ExecutableElement[]{
				createMockExecutableElement("one", new String[]{"some.other.annotation",RequestMapping.class.getName()}),
				createMockExecutableElement("two", new String[]{"not.a.controller"}),
				createMockExecutableElement("three", new String[]{RequestMapping.class.getName()}),
				createMockExecutableElement("four", null),
				createMockExecutableElement("five", new String[]{RequestMapping.class.getName(), Deprecated.class.getName()}),
		};
		// Create our iterator
		Iterator<ExecutableElement> it = FilterUtils.requestMappingIterator(testDocs);
		assertNotNull(it);
		int count = 0;
		while(it.hasNext()){
			ExecutableElement cd = it.next();
			assertNotNull(cd);
			assertTrue("Two, Four, and Five should have been filtered out: "+cd.qualifiedName(), "one".equals(cd.qualifiedName()) || "three".equals(cd.qualifiedName()));
			count++;
		}
		assertEquals(2, count);
	}
}
