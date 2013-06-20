package org.sagebionetworks.javadoc.web.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.javadoc.JavadocMockUtils;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.schema.ObjectSchema;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Type;

public class SchemaWriterTest {

	@Test
	public void testGetEffectiveSchema(){
		// One case where it should exist
		WikiPage wp = new WikiPage();
		String schema = SchemaWriter.getEffectiveSchema(WikiPage.class.getName());
		assertEquals(wp.getJSONSchema(), schema);
		// Another where it should not
		schema = SchemaWriter.getEffectiveSchema("not.a.real.Object");
		assertEquals(null, schema);
	}
	
	@Test
	public void testImplementsJSONEntityTrue(){
		ClassDoc cd = JavadocMockUtils.createMockJsonEntity("org.example.SomeJSONEntity");
		assertTrue(SchemaWriter.implementsJSONEntity(cd));
	}
	
	@Test
	public void testImplementsJSONEntityFalse(){
		ClassDoc cd = JavadocMockUtils.createMockClassDoc("org.example.not.a.JSONEntity");
		ClassDoc[] interfaces = JavadocMockUtils.createMockClassDocs(new String[]{"org.exmaple.some.interface"});
		when(cd.interfaces()).thenReturn(interfaces);
		assertFalse(SchemaWriter.implementsJSONEntity(cd));
	}
	
	@Test
	public void testFindSchemaFiles(){
		MethodDoc method = JavadocMockUtils.createMockMethodDoc("getSomething");
		// The return type and one parameter should be JSON entites
		String returnName = "org.example.SomeJsonEntity";
		String paramOne = "org.example.SomeNonJsonEntity";
		String paramTwo = "org.example.AnotherJsonEntity";
		ClassDoc returnClass = JavadocMockUtils.createMockJsonEntity(returnName);
		Type retunType = JavadocMockUtils.createMockType(returnName, returnClass);
		
		// Add one parameter that is a JSONEntity and another that is not
		ClassDoc paramTwoClass = JavadocMockUtils.createMockJsonEntity(paramTwo);
		Type paramTwoType = JavadocMockUtils.createMockType(paramTwo, paramTwoClass);
		Parameter[] params = new Parameter[]{
				JavadocMockUtils.createMockParameter("paramOne", paramOne),
				JavadocMockUtils.createMockParameter("paramTwo", paramTwo, paramTwoType),
		};
		
		when(method.returnType()).thenReturn(retunType);
		when(method.parameters()).thenReturn(params);
		
		Map<String, ObjectSchema> schemaMap = new HashMap<String, ObjectSchema>();
		// Make the call
		SchemaWriter.findSchemaFiles(schemaMap, method);
		assertEquals(2, schemaMap.size());
		assertTrue(schemaMap.containsKey(returnName));
		assertTrue(schemaMap.containsKey(paramTwo));
		assertFalse(schemaMap.containsKey(paramOne));
		
	}
}
