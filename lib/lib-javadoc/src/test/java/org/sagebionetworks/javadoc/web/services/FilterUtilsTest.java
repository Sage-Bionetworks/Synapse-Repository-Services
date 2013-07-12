package org.sagebionetworks.javadoc.web.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.sagebionetworks.javadoc.JavadocMockUtils.createMockClassDoc;
import static org.sagebionetworks.javadoc.JavadocMockUtils.createMockMethodDoc;

import java.util.Iterator;

import org.junit.Test;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;

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
		ClassDoc[] testClassDocs = new ClassDoc[]{
				createMockClassDoc("one", new String[]{ControllerInfo.class.getName()}),
				createMockClassDoc("two", new String[]{"not.a.controller"}),
				createMockClassDoc("three", new String[]{ControllerInfo.class.getName()}),
		};
		// Create our iterator
		Iterator<ClassDoc> it = FilterUtils.controllerIterator(testClassDocs);
		assertNotNull(it);
		int count = 0;
		while(it.hasNext()){
			ClassDoc cd = it.next();
			assertNotNull(cd);
			assertTrue("Two should have been filtered out as it was not a controller", "one".equals(cd.qualifiedName()) || "three".equals(cd.qualifiedName()));
			count++;
		}
		assertEquals(2, count);
	}
	
	@Test
	public void testRequestMappingIterator(){
		// Create 3 clases
		MethodDoc[] testDocs = new MethodDoc[]{
				createMockMethodDoc("one", new String[]{"some.other.annotation",RequestMapping.class.getName()}),
				createMockMethodDoc("two", new String[]{"not.a.controller"}),
				createMockMethodDoc("three", new String[]{RequestMapping.class.getName()}),
				createMockMethodDoc("four", null),
		};
		// Create our iterator
		Iterator<MethodDoc> it = FilterUtils.requestMappingIterator(testDocs);
		assertNotNull(it);
		int count = 0;
		while(it.hasNext()){
			MethodDoc cd = it.next();
			assertNotNull(cd);
			assertTrue("Two and Four should have been filtered out: "+cd.qualifiedName(), "one".equals(cd.qualifiedName()) || "three".equals(cd.qualifiedName()));
			count++;
		}
		assertEquals(2, count);
	}
}
