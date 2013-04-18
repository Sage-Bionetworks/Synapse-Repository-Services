package org.sagebionetworks.javadoc.web.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.Iterator;

import org.junit.Test;
import org.mockito.Mockito;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.tools.javadoc.Main;

/**
 * Test for the SpringMVCDoclet
 * @author jmhill
 *
 */
public class SpringMVCDocletTest {

	@Test
	public void testMain(){
		String targetPath = System.getProperty("targetPath");
		assertNotNull("To run this test you must include the -DtargetPath=<your build target dir>",targetPath);
		// Validate that the target dir exists
		File targetDir = new File(targetPath);
		if(!targetDir.exists()){
			System.out.println("Creating target directory: "+targetDir.getAbsolutePath());
			targetDir.mkdirs();
		}
		assertTrue(targetDir.exists());
		assertTrue(targetDir.isDirectory());
		// Run a sample javadoc
		String fileName = "ExampleController.java";
		URL url = SpringMVCDocletTest.class.getClassLoader().getResource(fileName);
		assertNotNull("Failed to find: "+fileName+" on the classpath");
		File file = new File(url.getFile().replaceAll("%20", " "));
		int result = Main.execute(new String[]{"-doclet", SpringMVCDoclet.class.getName(), file.getAbsolutePath()});
		assertEquals(0, result);
	}
	@Test
	public void testJavadocTool(){
		// Create a test Javadoc tool
		
	}
	
	@Test
	public void testControllerIterator(){
		// Create 3 clases
		ClassDoc[] testClassDocs = new ClassDoc[]{
				createMockClassDoc(true, "one"),
				createMockClassDoc(false, "two"),
				createMockClassDoc(true, "three"),
		};
		// Create our iterator
		Iterator<ClassDoc> it = SpringMVCDoclet.controllerIterator(testClassDocs);
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
	
	/**
	 * Create a mock ClassDoc for testing.
	 * @param isController - True of the class represents a Spring controller.
	 * @return
	 */
	public ClassDoc createMockClassDoc(boolean isController, String name){
		ClassDoc mockDoc = Mockito.mock(ClassDoc.class);
		AnnotationDesc[] mocAnnotations = new AnnotationDesc[]{
				Mockito.mock(AnnotationDesc.class),
		};
		when(mockDoc.annotations()).thenReturn(mocAnnotations);
		String toStringValue = null;
		if(isController){
			toStringValue = SpringMVCDoclet.ORG_SPRINGFRAMEWORK_STEREOTYPE_CONTROLLER;
		}else{
			toStringValue = "@not.a.contoller";
		}
		when(mockDoc.qualifiedName()).thenReturn(name);
		when(mocAnnotations[0].toString()).thenReturn(toStringValue);
		return mockDoc;
	}
}
