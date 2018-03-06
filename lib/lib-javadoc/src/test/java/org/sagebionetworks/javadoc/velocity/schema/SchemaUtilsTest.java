package org.sagebionetworks.javadoc.velocity.schema;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.javadoc.JavadocMockUtils;
import org.sagebionetworks.javadoc.testclasses.GenericList;
import org.sagebionetworks.repo.model.file.ChunkRequest;
import org.sagebionetworks.repo.model.file.ChunkResult;
import org.sagebionetworks.repo.model.file.ChunkedFileToken;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.schema.EnumValue;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.schema.generator.EffectiveSchemaUtil;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Type;

public class SchemaUtilsTest {

	@Test
	public void testGetEffectiveSchema() throws IOException{
		// One case where it should exist
		String schema = SchemaUtils.getEffectiveSchema(WikiPage.class.getName());
		String expectedSchema = EffectiveSchemaUtil.loadEffectiveSchemaFromClasspath(WikiPage.class);
		assertEquals(expectedSchema, schema);
		// Another where it should not
		schema = SchemaUtils.getEffectiveSchema("not.a.real.Object");
		assertNull(schema);
	}
	
	@Test
	public void testGetEffectiveSchemaHasEffectiveSchema() throws IOException{
		String schema = SchemaUtils.getEffectiveSchema(GenericList.class.getName());
		String expectedSchema = new GenericList().getEffectiveSchema();
		assertEquals(expectedSchema, schema);
	}
	
	@Test
	public void testImplementsJSONEntityOrEnumWithJSONEntity(){
		ClassDoc cd = JavadocMockUtils.createMockJsonEntity("org.example.SomeJSONEntity");
		assertTrue(SchemaUtils.implementsJSONEntityOrEnum(cd));
	}

	@Test
	public void testImplementsJSONEntityOrEnumWithEnum(){
		ClassDoc cd = JavadocMockUtils.createMockEnum("org.example.SomeEnum");
		assertTrue(SchemaUtils.implementsJSONEntityOrEnum(cd));
	}
	
	@Test
	public void testImplementsJSONEntityFalse(){
		ClassDoc cd = JavadocMockUtils.createMockClassDoc("org.example.not.a.JSONEntity");
		ClassDoc[] interfaces = JavadocMockUtils.createMockClassDocs(new String[]{"org.exmaple.some.interface"});
		when(cd.interfaces()).thenReturn(interfaces);
		assertFalse(SchemaUtils.implementsJSONEntityOrEnum(cd));
	}
	
	@Test
	public void testEffectiveSchema(){
		String fullName = ChunkedFileToken.class.getName();
		ObjectSchema schema = SchemaUtils.getSchema(fullName);
		assertNotNull(schema);
		assertEquals(schema.getId(), fullName);
	}
	
	@Test
	public void testFindSchemaFiles(){
		MethodDoc method = JavadocMockUtils.createMockMethodDoc("getSomething");
		// The return type and one parameter should be JSON entites
		String returnName = ChunkedFileToken.class.getName();
		String paramOne = ChunkRequest.class.getName();
		String paramTwo = ChunkResult.class.getName();
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
		SchemaUtils.findSchemaFiles(schemaMap, method);
		assertEquals(2, schemaMap.size());
		assertTrue(schemaMap.containsKey(returnName));
		assertTrue(schemaMap.containsKey(paramTwo));
		assertFalse(schemaMap.containsKey(paramOne));
		
	}
	
	@Test
	public void testTranslateToSchemaFieldString(){
		String name = "someString";
		String description = "I am a tea pot!";
		ObjectSchema schema = new ObjectSchema(TYPE.STRING);
		schema.setDescription(description);
		SchemaFields field = SchemaUtils.translateToSchemaField(name, schema);
		assertNotNull(field);
		assertEquals(name, field.getName());
		assertEquals(description, field.getDescription());
		assertEquals(new TypeReference(false, false, false, new String[] { TYPE.STRING.name() }, new String[] { null }), field.getType());
	}
	
	@Test
	public void testTypeToLinkStringString(){
		ObjectSchema schema = new ObjectSchema(TYPE.STRING);
		TypeReference result = SchemaUtils.typeToLinkString(schema);
		assertArrayEquals(new String[] { TYPE.STRING.name() }, result.getDisplay());
		assertArrayEquals(new String[] { null }, result.getHref());
		assertFalse(result.getIsArray());
		assertFalse(result.getIsUnique());
	}
	
	@Test
	public void testTypeToLinkStringBoolean(){
		ObjectSchema schema = new ObjectSchema(TYPE.BOOLEAN);
		TypeReference result = SchemaUtils.typeToLinkString(schema);
		assertArrayEquals(new String[] { TYPE.BOOLEAN.name() }, result.getDisplay());
		assertArrayEquals(new String[] { null }, result.getHref());
		assertFalse(result.getIsArray());
		assertFalse(result.getIsUnique());
	}
	
	@Test
	public void testTypeToLinkStringNumber(){
		ObjectSchema schema = new ObjectSchema(TYPE.NUMBER);
		TypeReference result = SchemaUtils.typeToLinkString(schema);
		assertArrayEquals(new String[] { TYPE.NUMBER.name() }, result.getDisplay());
		assertArrayEquals(new String[] { null }, result.getHref());
		assertFalse(result.getIsArray());
		assertFalse(result.getIsUnique());
	}
	
	@Test
	public void testTypeToLinkStringInteger(){
		ObjectSchema schema = new ObjectSchema(TYPE.INTEGER);
		TypeReference result = SchemaUtils.typeToLinkString(schema);
		assertArrayEquals(new String[] { TYPE.INTEGER.name() }, result.getDisplay());
		assertArrayEquals(new String[] { null }, result.getHref());
		assertFalse(result.getIsArray());
		assertFalse(result.getIsUnique());
	}
	
	@Test
	public void testTypeToLinkStringObject(){
		ObjectSchema schema = new ObjectSchema(TYPE.OBJECT);
		String name = "Example";
		String id = "org.sagebionetworks.test."+name;
		schema.setId(id);
		schema.setName("Example");
		TypeReference result = SchemaUtils.typeToLinkString(schema);
		assertArrayEquals(new String[] { name }, result.getDisplay());
		assertArrayEquals(new String[] { "${" + id + "}" }, result.getHref());
		assertFalse(result.getIsArray());
		assertFalse(result.getIsUnique());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTypeToLinkStringObjectNullId(){
		ObjectSchema schema = new ObjectSchema(TYPE.OBJECT);
		schema.setId(null);
		SchemaUtils.typeToLinkString(schema);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTypeToLinkStringArrayNullItems(){
		ObjectSchema schema = new ObjectSchema(TYPE.ARRAY);
		schema.setItems(null);
		SchemaUtils.typeToLinkString(schema);
	}
	
	@Test
	public void testTypeToLinkStringArrayPrimitive(){
		ObjectSchema schema = new ObjectSchema(TYPE.ARRAY);
		schema.setItems(new ObjectSchema(TYPE.STRING));
		TypeReference result = SchemaUtils.typeToLinkString(schema);
		assertArrayEquals(new String[] { TYPE.STRING.name() }, result.getDisplay());
		assertArrayEquals(new String[] { null }, result.getHref());
		assertTrue(result.getIsArray());
		assertFalse(result.getIsUnique());
	}
	
	@Test
	public void testTypeToLinkStringArrayObjects(){
		ObjectSchema schema = new ObjectSchema(TYPE.ARRAY);
		schema.setItems(new ObjectSchema(TYPE.OBJECT));
		String name = "Example";
		String id = "org.sagebionetworks.test."+name;
		schema.getItems().setId(id);
		schema.getItems().setName(name);
		TypeReference result = SchemaUtils.typeToLinkString(schema);
		assertArrayEquals(new String[] { name }, result.getDisplay());
		assertArrayEquals(new String[] { "${" + id + "}" }, result.getHref());
		assertTrue(result.getIsArray());
		assertFalse(result.getIsUnique());
	}
	
	@Test
	public void testTypeToLinkStringMapPrimitives() {
		ObjectSchema schema = new ObjectSchema(TYPE.MAP);
		schema.setKey(new ObjectSchema(TYPE.STRING));
		schema.setValue(new ObjectSchema(TYPE.STRING));
		TypeReference result = SchemaUtils.typeToLinkString(schema);
		assertArrayEquals(new String[] { TYPE.STRING.name(), TYPE.STRING.name() }, result.getDisplay());
		assertArrayEquals(new String[] { null, null }, result.getHref());
		assertFalse(result.getIsArray());
		assertFalse(result.getIsUnique());
		assertTrue(result.getIsMap());
	}

	@Test
	public void testTypeToLinkStringMapObjects() {
		ObjectSchema schema = new ObjectSchema(TYPE.MAP);
		schema.setKey(new ObjectSchema(TYPE.OBJECT));
		schema.setValue(new ObjectSchema(TYPE.OBJECT));
		String name1 = "Example1";
		String name2 = "Example2";
		String id1 = "org.sagebionetworks.test." + name1;
		String id2 = "org.sagebionetworks.test." + name2;
		schema.getKey().setId(id1);
		schema.getKey().setName(name1);
		schema.getValue().setId(id2);
		schema.getValue().setName(name2);
		TypeReference result = SchemaUtils.typeToLinkString(schema);
		assertArrayEquals(new String[] { name1, name2 }, result.getDisplay());
		assertArrayEquals(new String[] { "${" + id1 + "}", "${" + id2 + "}" }, result.getHref());
		assertFalse(result.getIsArray());
		assertFalse(result.getIsUnique());
		assertTrue(result.getIsMap());
	}

	@Test
	public void testTranslateToModel(){
		String description = "top level description";
		String name = "Example";
		String id = "org.sagebionetworks.test."+name;
		String effective ="{\"id\":\""+id+"\"}";
		ObjectSchema schema = new ObjectSchema(TYPE.OBJECT);
		schema.setId(id);
		schema.setDescription(description);
		schema.setSchema(effective);
		schema.setName(name);
		ObjectSchemaModel model = SchemaUtils.translateToModel(schema, null);
		assertNotNull(model);
		assertEquals(description, model.getDescription());
		assertEquals(id, model.getId());
		assertEquals(name, model.getName());
		assertEquals(effective, model.getEffectiveSchema());
		assertNull(model.getEnumValues());
	}

	@Test
	public void testTranslateToModelForEnum(){
		String description = "top level description";
		String name = "Example";
		String id = "org.sagebionetworks.test."+name;
		String effective ="{}";
		ObjectSchema schema = new ObjectSchema(TYPE.OBJECT);
		schema.setId(id);
		schema.setDescription(description);
		schema.setSchema(effective);
		schema.setName(name);
		EnumValue a = new EnumValue("a");
		EnumValue b = new EnumValue("b", "b's description");
		schema.setEnum(new EnumValue[]{a,b});
		ObjectSchemaModel model = SchemaUtils.translateToModel(schema, null);
		assertNotNull(model);
		assertEquals(description, model.getDescription());
		assertEquals(id, model.getId());
		assertEquals(name, model.getName());
		assertEquals(effective, model.getEffectiveSchema());
		assertEquals(Arrays.asList(a,b), model.getEnumValues());
	}
	
	@Test
	public void testTranslateToModelWithProgs(){
		String description = "top level description";
		String name = "Example";
		String id = "org.sagebionetworks.test."+name;
		String effective ="{\"id\":\""+id+"\"}";
		ObjectSchema schema = new ObjectSchema(TYPE.OBJECT);
		schema.setId(id);
		schema.setDescription(description);
		schema.setSchema(effective);
		schema.setName(name);
		// Add some properties
		schema.setProperties(new LinkedHashMap<String, ObjectSchema>());
		schema.getProperties().put("someString", new ObjectSchema(TYPE.STRING));
		schema.getProperties().put("someBoolean", new ObjectSchema(TYPE.BOOLEAN));
		
		ObjectSchemaModel model = SchemaUtils.translateToModel(schema, null);
		assertNotNull(model);
		assertEquals(description, model.getDescription());
		assertEquals(id, model.getId());
		assertEquals(name, model.getName());
		assertEquals(effective, model.getEffectiveSchema());
		assertNotNull(model.getFields());
		assertEquals(2, model.getFields().size());
		assertEquals(new SchemaFields(new TypeReference(false, false, false, new String[] { TYPE.STRING.name() }, new String[] { null }),
				"someString", null), model.getFields().get(0));
		assertEquals(new SchemaFields(new TypeReference(false, false, false, new String[] { TYPE.BOOLEAN.name() }, new String[] { null }),
				"someBoolean", null), model.getFields().get(1));
	}
}
