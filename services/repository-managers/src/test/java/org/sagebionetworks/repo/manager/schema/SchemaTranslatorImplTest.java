package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.FORMAT;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class SchemaTranslatorImplTest {

	@InjectMocks
	SchemaTranslatorImpl translator;
	
	@Test
	public void testLoadSchemaFromClasspath() throws IOException, JSONObjectAdapterException {
		String fileEntityId = FileEntity.class.getName();
		// call under test
		ObjectSchemaImpl schema = translator.loadSchemaFromClasspath(fileEntityId);
		assertNotNull(schema);
		assertEquals(fileEntityId, schema.getId());
	}
	
	@Test
	public void testLoadSchemaFromClasspathWithNotFound() throws IOException, JSONObjectAdapterException {
		String id = "does.not.exist";
		String message = assertThrows(NotFoundException.class, () -> {
			translator.loadSchemaFromClasspath(id);
		}).getMessage();
		assertEquals("Cannot find: 'schema/does/not/exist.json' on the classpath", message);
	}

	@Test
	public void testLoadSchemaFromClasspathWithNullId() {
		String id = null;
		assertThrows(IllegalArgumentException.class, () -> {
			translator.loadSchemaFromClasspath(id);
		});
	}

	@Test
	public void testConvertFromInternalIdToExternalId() {
		String internalId = "org.sagebionetworks.repo.model.FileEntity";
		// call under test
		String externalId = translator.convertFromInternalIdToExternalId(internalId);
		assertEquals("org.sagebionetworks-repo.model.FileEntity", externalId);
	}

	@Test
	public void testConvertFromInternalIdToExternalIdWithUknownOrganization() {
		String internalId = "org.unknown.repo.model.FileEntity";
		String message = assertThrows(IllegalArgumentException.class, () -> {
			translator.convertFromInternalIdToExternalId(internalId);
		}).getMessage();
		assertEquals("Id has an unknown organization name: 'org.unknown.repo.model.FileEntity'", message);
	}

	@Test
	public void testConvertFromInternalIdToExternalIdWithNullId() {
		String internalId = null;
		// call under test
		String externalId = translator.convertFromInternalIdToExternalId(internalId);
		assertNull(externalId);
	}
	
	@Test
	public void testTranslateArray() {
		ObjectSchemaImpl one = new ObjectSchemaImpl();
		one.setId("org.sagebionetworks.one");
		ObjectSchemaImpl two = new ObjectSchemaImpl();
		two.setId("org.sagebionetworks.two");
		ObjectSchema[] array = new ObjectSchema[] {one, two};
		// Call under test
		List<JsonSchema> result = translator.translateArray(array);
		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals("org.sagebionetworks-one", result.get(0).get$id());
		assertEquals("org.sagebionetworks-two", result.get(1).get$id());
	}
	
	@Test
	public void testTranslateArrayWithNullArray() {
		ObjectSchema[] array = null;
		// Call under test
		List<JsonSchema> result = translator.translateArray(array);
		assertNull(result);
	}
	
	@Test
	public void testTranslateTypeWithString() {
		TYPE input = TYPE.STRING;
		// call under test
		Type result = translator.translateType(input);
		assertEquals(Type.string, result);
	}
	
	@Test
	public void testTranslateTypeWithArray() {
		TYPE input = TYPE.ARRAY;
		// call under test
		Type result = translator.translateType(input);
		assertEquals(Type.array, result);
	}
	
	@Test
	public void testTranslateTypeWithNumber() {
		TYPE input = TYPE.NUMBER;
		// call under test
		Type result = translator.translateType(input);
		assertEquals(Type.number, result);
	}
	
	@Test
	public void testTranslateTypeWithInteger() {
		TYPE input = TYPE.INTEGER;
		// call under test
		Type result = translator.translateType(input);
		assertEquals(Type.number, result);
	}
	
	@Test
	public void testTranslateTypeWithBoolean() {
		TYPE input = TYPE.BOOLEAN;
		// call under test
		Type result = translator.translateType(input);
		assertEquals(Type._boolean, result);
	}
	
	@Test
	public void testTranslateTypeWithNull() {
		TYPE input = TYPE.NULL;
		// call under test
		Type result = translator.translateType(input);
		assertEquals(Type._null, result);
	}
	
	@Test
	public void testTranslateTypeWithObject() {
		TYPE input = TYPE.OBJECT;
		// call under test
		Type result = translator.translateType(input);
		assertEquals(Type.object, result);
	}
	
	@Test
	public void testTranslateTypeWithInterface() {
		TYPE input = TYPE.INTERFACE;
		// call under test
		Type result = translator.translateType(input);
		assertEquals(Type.object, result);
	}
	
	@Test
	public void testTranslateTypeWithAny() {
		TYPE input = TYPE.ANY;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			translator.translateType(input);
		}).getMessage();
		assertEquals("There is no translation for type: 'ANY'", message);
	}
	
	@Test
	public void testTranslateTypeWithMap() {
		TYPE input = TYPE.MAP;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			translator.translateType(input);
		}).getMessage();
		assertEquals("There is no translation for type: 'MAP'", message);
	}
	
	@Test
	public void testTranslateTypeWithTupleArrayMap() {
		TYPE input = TYPE.TUPLE_ARRAY_MAP;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			translator.translateType(input);
		}).getMessage();
		assertEquals("There is no translation for type: 'TUPLE_ARRAY_MAP'", message);
	}
	
	@Test
	public void testTranslateTypeWithNullType() {
		TYPE input = null;
		// call under test
		Type result = translator.translateType(input);
		assertNull(result);
	}
	
	@Test
	public void testTranslateMap() {
		ObjectSchemaImpl one = new ObjectSchemaImpl(TYPE.STRING);
		one.setDescription("one");
		ObjectSchemaImpl two = new ObjectSchemaImpl(TYPE.NUMBER);
		two.setDescription("two");
		ObjectSchemaImpl three = new ObjectSchemaImpl(TYPE.BOOLEAN);
		three.setDescription("three");
		Map<String, ObjectSchema> inputMap = new LinkedHashMap<String, ObjectSchema>(3);
		inputMap.put("one", one);
		inputMap.put("two", two);
		inputMap.put("three", three);
		
		// Call under test
		LinkedHashMap<String, JsonSchema> result = translator.translateMap(inputMap);
		assertNotNull(result);
		// The order must match the order of the input
		List<String> keyOrder = new LinkedList<String>();
		for(String key: result.keySet()) {
			keyOrder.add(key);
		}
		assertEquals(Lists.newArrayList("one","two","three"), keyOrder);
		assertNotNull(result.get("one"));
		assertEquals("one", result.get("one").getDescription());
		assertNotNull(result.get("two"));
		assertEquals("two", result.get("two").getDescription());
		assertNotNull(result.get("three"));
		assertEquals("three", result.get("three").getDescription());
	}
	
	
	@Test
	public void testTranslateMapWithNull() {
		Map<String, ObjectSchema> inputMap = null;
		// Call under test
		LinkedHashMap<String, JsonSchema> result = translator.translateMap(inputMap);
		assertNull(result);
	}
	
	@Test
	public void testTranslateFormat() {
		FORMAT format = FORMAT.DATE_TIME;
		// call under test
		String result = translator.translateFormat(format);
		assertEquals(FORMAT.DATE_TIME.getJSONValue(), result);
	}
	
	@Test
	public void testTranslateFormtWithNull() {
		FORMAT format = null;
		// call under test
		String result = translator.translateFormat(format);
		assertNull(result);
	}
	
	//
	// Test on public translate.
	//
	
	@Test
	public void testTranslateWithNullObjectSchema() {
		ObjectSchemaImpl objectSchema = null;
		// call under test
		JsonSchema result = translator.translate(objectSchema);
		assertNull(result);
	}
	
	@Test
	public void testTranslateWithId() throws IOException, JSONObjectAdapterException {
		ObjectSchemaImpl objectSchema = new ObjectSchemaImpl();
		objectSchema.setId("org.sagebionetworks.repo.model.FileEntity");
		// Call under test
		JsonSchema resultSchema = translator.translate(objectSchema);
		assertNotNull(resultSchema);
		assertEquals("org.sagebionetworks-repo.model.FileEntity", resultSchema.get$id());
		assertEquals(SchemaTranslatorImpl.CURRENT_$SCHEMA, resultSchema.get$schema());
	}
	
	@Test
	public void testTranslateWith$ref() throws IOException, JSONObjectAdapterException {
		ObjectSchemaImpl objectSchema = new ObjectSchemaImpl();
		objectSchema.setRef("org.sagebionetworks.repo.model.FileEntity");
		// Call under test
		JsonSchema resultSchema = translator.translate(objectSchema);
		assertNotNull(resultSchema);
		assertEquals("org.sagebionetworks-repo.model.FileEntity", resultSchema.get$ref());
	}
	
	@Test
	public void testTranslateWithImplments() throws IOException, JSONObjectAdapterException {
		ObjectSchemaImpl objectSchema = new ObjectSchemaImpl();
		objectSchema.setId("org.sagebionetworks.repo.model.FileEntity");
		ObjectSchemaImpl imp = new ObjectSchemaImpl();
		imp.setRef("org.sagebionetworks.repo.model.Versionable");
		objectSchema.setImplements(new ObjectSchema[] {imp});
		
		// Call under test
		JsonSchema resultSchema = translator.translate(objectSchema);
		assertNotNull(resultSchema);
		assertNotNull(resultSchema.get$schema());
		assertEquals("org.sagebionetworks-repo.model.FileEntity", resultSchema.get$id());
		assertNotNull(resultSchema.getAllOf());
		assertEquals(1, resultSchema.getAllOf().size());
		JsonSchema allOfItem = resultSchema.getAllOf().get(0);
		assertEquals("org.sagebionetworks-repo.model.Versionable", allOfItem.get$ref());
		// only the root should have a $schema
		assertNull(allOfItem.get$schema());
	}
	
	@Test
	public void testTranslateWithProperties() {
		LinkedHashMap<String, ObjectSchema> properties = new LinkedHashMap<String, ObjectSchema>(2);
		properties.put("a", new ObjectSchemaImpl(TYPE.STRING));
		properties.put("b", new ObjectSchemaImpl(TYPE.NUMBER));
		ObjectSchemaImpl inputSchema = new ObjectSchemaImpl(TYPE.OBJECT);
		inputSchema.setId("org.sagebionetworks.Testing");
		inputSchema.setProperties(properties);
		
		// Call under test
		JsonSchema result = translator.translate(inputSchema);
		assertNotNull(result);
		assertNotNull(result.get$schema());
		assertNotNull(result.getProperties());
		assertEquals(3, result.getProperties().size());
		JsonSchema propA = result.getProperties().get("a");
		// only the root should have a $schema
		assertNull(propA.get$schema());
		assertNotNull(propA);
		assertEquals(Type.string, propA.getType());
		JsonSchema propB = result.getProperties().get("b");
		assertNotNull(propB);
		assertEquals(Type.number, propB.getType());
		assertNull(propB.get$schema());
		JsonSchema propConcreteType = result.getProperties().get("concreteType");
		assertNotNull(propConcreteType);
		assertEquals(Type.string, propConcreteType.getType());
		assertEquals(inputSchema.getId(), propConcreteType.get_const());
	}
	
	@Test
	public void testTranslateWithPropertiesNoId() {
		LinkedHashMap<String, ObjectSchema> properties = new LinkedHashMap<String, ObjectSchema>(2);
		properties.put("a", new ObjectSchemaImpl(TYPE.STRING));
		properties.put("b", new ObjectSchemaImpl(TYPE.NUMBER));
		ObjectSchemaImpl inputSchema = new ObjectSchemaImpl(TYPE.OBJECT);
		inputSchema.setId(null);
		inputSchema.setProperties(properties);
		
		// Call under test
		JsonSchema result = translator.translate(inputSchema);
		assertNotNull(result);
		assertNotNull(result.get$schema());
		assertNotNull(result.getProperties());
		assertEquals(2, result.getProperties().size());
		JsonSchema propA = result.getProperties().get("a");
		// only the root should have a $schema
		assertNull(propA.get$schema());
		assertNotNull(propA);
		assertEquals(Type.string, propA.getType());
		JsonSchema propB = result.getProperties().get("b");
		assertNotNull(propB);
		assertEquals(Type.number, propB.getType());
		assertNull(propB.get$schema());
		// the input schema did not have an ID so it should not have a concrete type.
		assertNull(result.getProperties().get("concreteType"));
	}
	
	@Test
	public void testTranslateWithType() {
		ObjectSchemaImpl one = new ObjectSchemaImpl();
		one.setType(TYPE.STRING);
		// call under test
		JsonSchema result = translator.translate(one);
		assertNotNull(result);
		assertEquals(Type.string, result.getType());
	}
	
	@Test
	public void testTranslateWithNullType() {
		ObjectSchemaImpl one = new ObjectSchemaImpl();
		one.setType(null);
		one.setId("org.sagebionetworks.model.Test");
		// call under test
		JsonSchema result = translator.translate(one);
		assertNotNull(result);
		assertEquals(null, result.getType());
		assertNotNull(result.getProperties());
		assertEquals(1, result.getProperties().size());
		JsonSchema concreteType = result.getProperties().get("concreteType");
		assertNotNull(concreteType);
		assertEquals(one.getId(), concreteType.get_const());
	}
	
	@Test
	public void testTranslateWithTypeObjectWithNullProperties() {
		ObjectSchemaImpl one = new ObjectSchemaImpl();
		one.setType(TYPE.OBJECT);
		one.setId("org.sagebionetworks.model.Test");
		one.setProperties(null);
		// call under test
		JsonSchema result = translator.translate(one);
		assertNotNull(result);
		assertEquals(Type.object, result.getType());
		assertNotNull(result.getProperties());
		assertEquals(1, result.getProperties().size());
		JsonSchema concreteType = result.getProperties().get("concreteType");
		assertNotNull(concreteType);
		assertEquals(one.getId(), concreteType.get_const());
	}
	
	@Test
	public void testTranslateWithTypeObjectWithProperties() {
		ObjectSchemaImpl one = new ObjectSchemaImpl();
		one.setType(TYPE.OBJECT);
		one.setId("org.sagebionetworks.model.Test");
		one.setProperties(new LinkedHashMap<String, ObjectSchema>(0));
		one.getProperties().put("test", new ObjectSchemaImpl(TYPE.STRING));
		// call under test
		JsonSchema result = translator.translate(one);
		assertNotNull(result);
		assertEquals(Type.object, result.getType());
		assertNotNull(result.getProperties());
		assertEquals(2, result.getProperties().size());
		JsonSchema concreteType = result.getProperties().get("concreteType");
		assertNotNull(concreteType);
		assertEquals(one.getId(), concreteType.get_const());
	}
	
	@Test
	public void testTranslateWithTypeInterface() {
		ObjectSchemaImpl one = new ObjectSchemaImpl();
		one.setType(TYPE.INTERFACE);
		one.setId("org.sagebionetworks.model.Test");
		// call under test
		JsonSchema result = translator.translate(one);
		assertNotNull(result);
		assertEquals(Type.object, result.getType());
		assertNull(result.getProperties());
	}
	
	@Test
	public void testTranslateWithDescription() {
		ObjectSchemaImpl one = new ObjectSchemaImpl(TYPE.STRING);
		one.setDescription("a description");
		// call under test
		JsonSchema result = translator.translate(one);
		assertNotNull(result);
		assertEquals(one.getDescription(), result.getDescription());
	}
	
	@Test
	public void testTranslateItems() {
		ObjectSchemaImpl arrayOfStrings = new ObjectSchemaImpl(TYPE.ARRAY);
		arrayOfStrings.setItems(new ObjectSchemaImpl(TYPE.STRING));
		// call under test
		JsonSchema result = translator.translate(arrayOfStrings);
		assertNotNull(result.get$schema());
		assertNotNull(result);
		assertEquals(Type.array, result.getType());
		JsonSchema items = result.getItems();
		assertNotNull(items);
		assertEquals(Type.string, items.getType());
		// only the root should have a $schema
		assertNull(items.get$schema());
	}
	
	@Test
	public void testTransalteTitle() {
		ObjectSchemaImpl hasTitle = new ObjectSchemaImpl(TYPE.STRING);
		hasTitle.setTitle("The Boss");
		// call under test
		JsonSchema result = translator.translate(hasTitle);
		assertNotNull(result);
		assertEquals(hasTitle.getTitle(), result.getTitle());
	}
	

	
	@Test
	public void testTranslateWithFileEntity() throws IOException, JSONObjectAdapterException {
		ObjectSchemaImpl fileEntityObjecSchema = translator.loadSchemaFromClasspath("org.sagebionetworks.repo.model.FileEntity");
		// Call under test
		JsonSchema resultSchema = translator.translate(fileEntityObjecSchema);
		assertNotNull(resultSchema);
		assertEquals("org.sagebionetworks-repo.model.FileEntity", resultSchema.get$id());
	}
	


}
