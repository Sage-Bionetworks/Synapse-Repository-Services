package org.sagebionetworks.javadoc.velocity.schema;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.javadoc.JavadocMockUtils;
import org.sagebionetworks.javadoc.testclasses.GenericList;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.schema.EnumValue;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.schema.generator.EffectiveSchemaUtil;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Type;

public class SchemaUtilsTest {

	ObjectSchema recursiveAnchor = null;
	
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
		String fullName = Team.class.getName();
		ObjectSchema schema = SchemaUtils.getSchema(fullName);
		assertNotNull(schema);
		assertEquals(schema.getId(), fullName);
	}
	
	@Test
	public void testFindSchemaFiles(){
		MethodDoc method = JavadocMockUtils.createMockMethodDoc("getSomething");
		// The return type and one parameter should be JSON entites
		String returnName = VersionInfo.class.getName();
		String paramOne = UserGroup.class.getName();
		String paramTwo = Team.class.getName();
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
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.STRING);
		schema.setDescription(description);
		SchemaFields field = SchemaUtils.translateToSchemaField(name, schema, recursiveAnchor);
		assertNotNull(field);
		assertEquals(name, field.getName());
		assertEquals(description, field.getDescription());
		assertEquals(new TypeReference(null, false, false, false, new String[] { TYPE.STRING.name() }, new String[] { null }), field.getType());
	}
	
	@Test
	public void testTypeToLinkStringString(){
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.STRING);
		TypeReference result = SchemaUtils.typeToLinkString(schema, recursiveAnchor);
		assertArrayEquals(new String[] { TYPE.STRING.name() }, result.getDisplay());
		assertArrayEquals(new String[] { null }, result.getHref());
		assertFalse(result.getIsArray());
		assertFalse(result.getIsUnique());
	}
	
	@Test
	public void testTypeToLinkStringBoolean(){
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.BOOLEAN);
		TypeReference result = SchemaUtils.typeToLinkString(schema, recursiveAnchor);
		assertArrayEquals(new String[] { TYPE.BOOLEAN.name() }, result.getDisplay());
		assertArrayEquals(new String[] { null }, result.getHref());
		assertFalse(result.getIsArray());
		assertFalse(result.getIsUnique());
	}
	
	@Test
	public void testTypeToLinkStringNumber(){
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.NUMBER);
		TypeReference result = SchemaUtils.typeToLinkString(schema, recursiveAnchor);
		assertArrayEquals(new String[] { TYPE.NUMBER.name() }, result.getDisplay());
		assertArrayEquals(new String[] { null }, result.getHref());
		assertFalse(result.getIsArray());
		assertFalse(result.getIsUnique());
	}
	
	@Test
	public void testTypeToLinkStringInteger(){
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.INTEGER);
		TypeReference result = SchemaUtils.typeToLinkString(schema, recursiveAnchor);
		assertArrayEquals(new String[] { TYPE.INTEGER.name() }, result.getDisplay());
		assertArrayEquals(new String[] { null }, result.getHref());
		assertFalse(result.getIsArray());
		assertFalse(result.getIsUnique());
	}
	
	@Test
	public void testTypeToLinkStringObject(){
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.OBJECT);
		String name = "Example";
		String id = "org.sagebionetworks.test."+name;
		schema.setId(id);
		schema.setName("Example");
		TypeReference result = SchemaUtils.typeToLinkString(schema, recursiveAnchor);
		assertArrayEquals(new String[] { name }, result.getDisplay());
		assertArrayEquals(new String[] { "${" + id + "}" }, result.getHref());
		assertFalse(result.getIsArray());
		assertFalse(result.getIsUnique());
	}
	
	@Test
	public void testTypeToLinkStringObjectNullId(){
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.OBJECT);
		schema.setId(null);
		assertThrows(IllegalArgumentException.class, ()->{
			SchemaUtils.typeToLinkString(schema, recursiveAnchor);
		});
	}
	
	@Test
	public void testTypeToLinkStringArrayNullItems(){
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.ARRAY);
		schema.setItems(null);
		assertThrows(IllegalArgumentException.class, ()->{
			SchemaUtils.typeToLinkString(schema, recursiveAnchor);
		});
	}
	
	@Test
	public void testTypeToLinkStringArrayPrimitive(){
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.ARRAY);
		schema.setItems(new ObjectSchemaImpl(TYPE.STRING));
		TypeReference result = SchemaUtils.typeToLinkString(schema, recursiveAnchor);
		assertArrayEquals(new String[] { TYPE.STRING.name() }, result.getDisplay());
		assertArrayEquals(new String[] { null }, result.getHref());
		assertTrue(result.getIsArray());
		assertFalse(result.getIsUnique());
	}
	
	@Test
	public void testTypeToLinkStringArrayObjects(){
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.ARRAY);
		schema.setItems(new ObjectSchemaImpl(TYPE.OBJECT));
		String name = "Example";
		String id = "org.sagebionetworks.test."+name;
		schema.getItems().setId(id);
		schema.getItems().setName(name);
		TypeReference result = SchemaUtils.typeToLinkString(schema, recursiveAnchor);
		assertArrayEquals(new String[] { name }, result.getDisplay());
		assertArrayEquals(new String[] { "${" + id + "}" }, result.getHref());
		assertTrue(result.getIsArray());
		assertFalse(result.getIsUnique());
	}
	
	@Test
	public void testTypeToLinkString_TupleArrayMapPrimitives() {
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.TUPLE_ARRAY_MAP);
		schema.setKey(new ObjectSchemaImpl(TYPE.STRING));
		schema.setValue(new ObjectSchemaImpl(TYPE.STRING));
		TypeReference result = SchemaUtils.typeToLinkString(schema, recursiveAnchor);
		assertArrayEquals(new String[] { TYPE.STRING.name(), TYPE.STRING.name() }, result.getDisplay());
		assertArrayEquals(new String[] { null, null }, result.getHref());
		assertFalse(result.getIsArray());
		assertFalse(result.getIsUnique());
		assertTrue(result.getIsMap());
	}

	@Test
	public void testTypeToLinkString_TupleArrayMapObjects() {
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.TUPLE_ARRAY_MAP);
		schema.setKey(new ObjectSchemaImpl(TYPE.OBJECT));
		schema.setValue(new ObjectSchemaImpl(TYPE.OBJECT));
		String name1 = "Example1";
		String name2 = "Example2";
		String id1 = "org.sagebionetworks.test." + name1;
		String id2 = "org.sagebionetworks.test." + name2;
		schema.getKey().setId(id1);
		schema.getKey().setName(name1);
		schema.getValue().setId(id2);
		schema.getValue().setName(name2);
		TypeReference result = SchemaUtils.typeToLinkString(schema, recursiveAnchor);
		assertArrayEquals(new String[] { name1, name2 }, result.getDisplay());
		assertArrayEquals(new String[] { "${" + id1 + "}", "${" + id2 + "}" }, result.getHref());
		assertFalse(result.getIsArray());
		assertFalse(result.getIsUnique());
		assertTrue(result.getIsMap());
	}

	@Test
	public void testTypeToLinkString_MapPrimitives() {
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.MAP);
		schema.setValue(new ObjectSchemaImpl(TYPE.STRING));
		TypeReference result = SchemaUtils.typeToLinkString(schema, recursiveAnchor);
		assertArrayEquals(new String[] { TYPE.STRING.name(), TYPE.STRING.name() }, result.getDisplay());
		assertArrayEquals(new String[] { null, null }, result.getHref());
		assertFalse(result.getIsArray());
		assertFalse(result.getIsUnique());
		assertTrue(result.getIsMap());
	}

	@Test
	public void testTypeToLinkString_MapObjects() {
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.MAP);
		schema.setValue(new ObjectSchemaImpl(TYPE.OBJECT));
		String name2 = "Example2";
		String id2 = "org.sagebionetworks.test." + name2;
		schema.getValue().setId(id2);
		schema.getValue().setName(name2);
		TypeReference result = SchemaUtils.typeToLinkString(schema, recursiveAnchor);
		assertArrayEquals(new String[] { TYPE.STRING.name(), name2 }, result.getDisplay());
		assertArrayEquals(new String[] { null, "${" + id2 + "}" }, result.getHref());
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
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.OBJECT);
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
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.OBJECT);
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
		ObjectSchema schema = new ObjectSchemaImpl(TYPE.OBJECT);
		schema.setId(id);
		schema.setDescription(description);
		schema.setSchema(effective);
		schema.setName(name);
		// Add some properties
		schema.setProperties(new LinkedHashMap<String, ObjectSchema>());
		schema.getProperties().put("someString", new ObjectSchemaImpl(TYPE.STRING));
		schema.getProperties().put("someBoolean", new ObjectSchemaImpl(TYPE.BOOLEAN));
		
		ObjectSchemaModel model = SchemaUtils.translateToModel(schema, null);
		assertNotNull(model);
		assertEquals(description, model.getDescription());
		assertEquals(id, model.getId());
		assertEquals(name, model.getName());
		assertEquals(effective, model.getEffectiveSchema());
		assertNotNull(model.getFields());
		assertEquals(2, model.getFields().size());
		assertEquals(new SchemaFields(new TypeReference(null, false, false, false, new String[] { TYPE.STRING.name() }, new String[] { null }),
				"someString", null), model.getFields().get(0));
		assertEquals(new SchemaFields(new TypeReference(null, false, false, false, new String[] { TYPE.BOOLEAN.name() }, new String[] { null }),
				"someBoolean", null), model.getFields().get(1));
	}

	//PLFM-5723
	@Test
	public void testRecursiveAddTypes_listOfEnums(){
		//PLFM-5723
		//create enum object
		String enumId = "org.sagebionetworks.test.TestEnum";
		ObjectSchema enumSchema = new ObjectSchemaImpl(TYPE.STRING);
		enumSchema.setId(enumId);
		enumSchema.setEnum(new EnumValue[]{ new EnumValue("ENUM_VAL1"), new EnumValue("ENUM_VAL2")});

		//create array of enums
		ObjectSchema arrayOfEnum = new ObjectSchemaImpl(TYPE.ARRAY);
		arrayOfEnum.setItems(enumSchema);

		//create object schema with a list containing enums
		String schemaToTestId = "org.sagebionetworks.test.TestEnumList";
		ObjectSchema schemaToTest = new ObjectSchemaImpl(TYPE.OBJECT);
		schemaToTest.setProperties(new LinkedHashMap<>(Collections.singletonMap("myEnumList", arrayOfEnum)));

		Map<String, ObjectSchema> resultMap = new HashMap<>();
		//method under test
		SchemaUtils.recursiveAddTypes(resultMap, schemaToTestId, schemaToTest);

		assertEquals(2, resultMap.size());
		assertNotNull(resultMap.get(schemaToTestId));
		assertNotNull(resultMap.get(enumId));
	}
	
	@Test
	public void testTranslateToModelRecurisveType() {
		ObjectSchema root = new ObjectSchemaImpl();
		root.setName("root");
		root.setId("path.root");
		root.set$recursiveAnchor(true);
		ObjectSchema refToSelf = new ObjectSchemaImpl();
		refToSelf.set$recursiveRef("#");
		root.putProperty("child", refToSelf);
		List<TypeReference> knownImplementaions = null;
		// call under test
		ObjectSchemaModel model = SchemaUtils.translateToModel(root, knownImplementaions);
		assertNotNull(model);
		assertNotNull(model.fields);
		assertEquals(1, model.fields.size());
		SchemaFields childField = model.fields.get(0);
		assertNotNull(childField);
		assertEquals("child", childField.name);
		assertNotNull(childField.type);
		assertEquals(1, childField.type.getDisplay().length);
		assertEquals("root", childField.type.getDisplay()[0]);
		assertEquals(1, childField.type.getHref().length);
		assertEquals("${path.root}", childField.type.getHref()[0]);
	}
	
	@Test
	public void testTranslateToModelWithDefaultImplementation() {
		ObjectSchema defaultImplementation = new ObjectSchemaImpl();
		
		defaultImplementation.setType(TYPE.OBJECT);
		defaultImplementation.setName("Impl");
		defaultImplementation.setId("path.to.Impl");
		
		ObjectSchema superInterface = new ObjectSchemaImpl();
		
		superInterface.setType(TYPE.INTERFACE);
		superInterface.setName("Interface");
		superInterface.setId("path.to.Interface");
		superInterface.setDefaultConcreteType(defaultImplementation.getId());
		
		TypeReference expectedReference = SchemaUtils.typeToLinkString(defaultImplementation, null);
		
		List<TypeReference> knownImplementaions = Arrays.asList(expectedReference);
		
		// call under test
		ObjectSchemaModel model = SchemaUtils.translateToModel(superInterface, knownImplementaions);

		assertEquals(expectedReference, model.getDefaultImplementation());
	}
	
	@Test
	public void testGetTypeDisplayRecursive() {
		ObjectSchema recursiveAnchor = new ObjectSchemaImpl();
		recursiveAnchor.setName("root");
		recursiveAnchor.setId("path.root");
		recursiveAnchor.set$recursiveAnchor(true);
		
		ObjectSchema refToSelf = new ObjectSchemaImpl();
		refToSelf.set$recursiveRef("#");
		
		// call under test
		String[] array = SchemaUtils.getTypeDisplay(refToSelf, recursiveAnchor);
		assertNotNull(array);
		assertEquals(1, array.length);
		assertEquals("root", array[0]);
	}
	
	@Test
	public void testGetTypeDisplayRecursiveAnchorNull() {
		ObjectSchema recursiveAnchor = null;
		
		ObjectSchema refToSelf = new ObjectSchemaImpl();
		refToSelf.set$recursiveRef("#");
		
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			SchemaUtils.getTypeDisplay(refToSelf, recursiveAnchor);
		});
	}
	
	@Test
	public void testGetTypeHRefRecursive() {
		ObjectSchema recursiveAnchor = new ObjectSchemaImpl();
		recursiveAnchor.setName("root");
		recursiveAnchor.setId("path.root");
		recursiveAnchor.set$recursiveAnchor(true);
		
		ObjectSchema refToSelf = new ObjectSchemaImpl();
		refToSelf.set$recursiveRef("#");
		
		// call under test
		String[] array = SchemaUtils.getTypeHref(refToSelf, recursiveAnchor);
		assertNotNull(array);
		assertEquals(1, array.length);
		assertEquals("${path.root}", array[0]);
	}
	
	@Test
	public void testGetTypeHRefRecursiveAnchorNull() {
		ObjectSchema recursiveAnchor = null;
		
		ObjectSchema refToSelf = new ObjectSchemaImpl();
		refToSelf.set$recursiveRef("#");
		
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			SchemaUtils.getTypeHref(refToSelf, recursiveAnchor);
		});
	}
}
