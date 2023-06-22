package org.sagebionetworks.translator;

import static org.junit.Assert.assertEquals;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sagebionetworks.javadoc.velocity.schema.SchemaUtils;
import org.sagebionetworks.javadoc.velocity.schema.TypeReference;
import org.sagebionetworks.openapi.server.ServerSideOnlyFactory;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.ObjectSchemaImpl;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class ObjectSchemaUtilsTest {
	private ObjectSchemaUtils util;
	
	@BeforeEach
	private void setUp() {
		this.util = Mockito.spy(new ObjectSchemaUtils());
	}
	
	@Test
	public void testGetConcreteClasses() {
		ServerSideOnlyFactory autoGen = new ServerSideOnlyFactory();
		Map<String, ObjectSchema> expected = new HashMap<>();
		Iterator<String> keySet = autoGen.getKeySetIterator();
		while (keySet.hasNext()) {
			String className = keySet.next();
			ObjectSchema schema = SchemaUtils.getSchema(className);
			SchemaUtils.recursiveAddTypes(expected, className, schema);
		}
		// call under test
		assertEquals(expected, util.getConcreteClasses(autoGen.getKeySetIterator()));
	}
	
	@Test
	public void testGetClassNameToJsonSchema() throws JSONObjectAdapterException {
		Map<String, ObjectSchema> classNameToObjectSchema = new HashMap<>();
		ObjectSchema objectSchema;
		JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
		objectSchema = new ObjectSchemaImpl(adpater);

		objectSchema.setType(TYPE.INTEGER);
		String className = "className";
		classNameToObjectSchema.put(className, objectSchema);
		Mockito.doReturn(new JsonSchema()).when(util).translateObjectSchemaToJsonSchema(any(ObjectSchema.class));
		
		Map<String, JsonSchema> expected = new HashMap<>();
		expected.put(className, new JsonSchema());
		// call under test
		assertEquals(expected, util.getClassNameToJsonSchema(classNameToObjectSchema));
		
		Mockito.verify(util).translateObjectSchemaToJsonSchema(objectSchema);
		Mockito.verify(util).insertOneOfPropertyForInterfaces(expected, new HashMap<>());
	}
	
	@Test
	public void testTranslateObjectSchemaToJsonSchemaWithNDescription() throws JSONObjectAdapterException {
		ObjectSchema objectSchema;
		JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
		objectSchema = new ObjectSchemaImpl(adpater);

		objectSchema.setType(TYPE.OBJECT);
		objectSchema.setProperties(new LinkedHashMap<>());
		objectSchema.setDescription("TESTING");
		
		Mockito.doReturn(Type.object).when(util).translateObjectSchemaTypeToJsonSchemaType(any(TYPE.class));
		Mockito.doReturn(new LinkedHashMap<>()).when(util).translatePropertiesFromObjectSchema(any(Map.class));
		
		JsonSchema expected = new JsonSchema();
		expected.setType(Type.object);
		expected.setProperties(new LinkedHashMap<>());
		expected.setDescription("TESTING");

		// call under test
		assertEquals(expected, util.translateObjectSchemaToJsonSchema(objectSchema));
		Mockito.verify(util).translateObjectSchemaTypeToJsonSchemaType(TYPE.OBJECT);
		Mockito.verify(util).translatePropertiesFromObjectSchema(new LinkedHashMap<>());
	}
	
	@Test
	public void testTranslateObjectSchemaToJsonSchemaWithNonPrimitiveType() throws JSONObjectAdapterException {
		ObjectSchema objectSchema;
		JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
		objectSchema = new ObjectSchemaImpl(adpater);

		objectSchema.setType(TYPE.OBJECT);
		objectSchema.setProperties(new LinkedHashMap<>());
		
		Mockito.doReturn(Type.object).when(util).translateObjectSchemaTypeToJsonSchemaType(any(TYPE.class));
		Mockito.doReturn(new LinkedHashMap<>()).when(util).translatePropertiesFromObjectSchema(any(Map.class));
		
		JsonSchema expected = new JsonSchema();
		expected.setType(Type.object);
		expected.setProperties(new LinkedHashMap<>());

		// call under test
		assertEquals(expected, util.translateObjectSchemaToJsonSchema(objectSchema));
		Mockito.verify(util).translateObjectSchemaTypeToJsonSchemaType(TYPE.OBJECT);
		Mockito.verify(util).translatePropertiesFromObjectSchema(new LinkedHashMap<>());
	}
	
	@Test
	public void testTranslateObjectSchemaToJsonSchemaWithPrimitiveType() throws JSONObjectAdapterException {
		ObjectSchema objectSchema;
		JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
		objectSchema = new ObjectSchemaImpl(adpater);
		objectSchema.setType(TYPE.INTEGER);
		
		Mockito.doReturn(new JsonSchema()).when(util).getSchemaForPrimitiveType(any(TYPE.class));
		Mockito.doReturn(true).when(util).isPrimitive(any(TYPE.class));
		
		assertEquals(new JsonSchema(), util.translateObjectSchemaToJsonSchema(objectSchema));
		Mockito.verify(util).isPrimitive(TYPE.INTEGER);
		Mockito.verify(util).getSchemaForPrimitiveType(TYPE.INTEGER);
	}
	
	@Test
	public void testTranslateObjectSchemaToJsonSchemaWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			util.translateObjectSchemaToJsonSchema(null);
		});
		assertEquals("objectSchema is required.", exception.getMessage());
	}
	
	@Test
	public void testTranslatePropertiesFromObjectSchema() throws JSONObjectAdapterException {
		Map<String, ObjectSchema> properties = new LinkedHashMap<>();
		ObjectSchema objectSchema1;
		ObjectSchema objectSchema2;
		JSONObjectAdapterImpl adpater1 = new JSONObjectAdapterImpl("{}");
		JSONObjectAdapterImpl adpater2 = new JSONObjectAdapterImpl("{properties: {}}");
		objectSchema1 = new ObjectSchemaImpl(adpater1);
		objectSchema1.setType(TYPE.STRING);
		objectSchema2 = new ObjectSchemaImpl(adpater2);
		objectSchema2.setType(TYPE.INTEGER);

		String className1 = "ClassName1";
		String className2 = "ClassName2";
		properties.put(className1, objectSchema1);
		properties.put(className2, objectSchema2);
		
		JsonSchema schema1 = new JsonSchema();
		schema1.setType(Type.string);
		JsonSchema schema2 = new JsonSchema();
		schema2.setType(Type.integer);
		Mockito.doReturn(schema1, schema2).when(util).translateObjectSchemaPropertyToJsonSchema(any(), any());
		Map<String, JsonSchema> expected = new LinkedHashMap<>();
		expected.put(className1, schema1);
		expected.put(className2, schema2);
		
		// call under test
		assertEquals(expected, util.translatePropertiesFromObjectSchema(properties));
		Mockito.verify(util, Mockito.times(2)).translateObjectSchemaPropertyToJsonSchema(any(), any());
		InOrder inOrder = Mockito.inOrder(util);
		inOrder.verify(util).translateObjectSchemaPropertyToJsonSchema(objectSchema1, TYPE.STRING);
		inOrder.verify(util).translateObjectSchemaPropertyToJsonSchema(objectSchema2, TYPE.INTEGER);
	}
	
	@Test
	public void testTranslatePropertiesFromObjectSchemaWithNullProperties() throws JSONObjectAdapterException {
		ObjectSchema objectSchema;
		JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
		objectSchema = new ObjectSchemaImpl(adpater);
		
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			util.translatePropertiesFromObjectSchema(null);
		});
		assertEquals("properties is required.", exception.getMessage());
	}
	
	@Test
	public void testTranslateObjectSchemaPropertyToJsonSchemaSetsDescriptionWhenPresent() throws JSONObjectAdapterException {
		ObjectSchema objectSchema;
		JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
		objectSchema = new ObjectSchemaImpl(adpater);
		objectSchema.setId("MOCK_ID");
		objectSchema.setDescription("MOCK_DESCRIPTION");
		
		// call under test
		JsonSchema result = util.translateObjectSchemaPropertyToJsonSchema(objectSchema, TYPE.OBJECT);
		assertTrue(result.getDescription().equals("MOCK_DESCRIPTION"));
	}
	
	@Test
	public void testTranslateObjectSchemaPropertyToJsonSchemaWithUnhandledType() throws JSONObjectAdapterException {
		ObjectSchema objectSchema;
		JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
		objectSchema = new ObjectSchemaImpl(adpater);
		
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			util.translateObjectSchemaPropertyToJsonSchema(objectSchema, TYPE.NULL);
		});
		assertEquals("Unsupported propertyType NULL", exception.getMessage());
	}
	
	@Test
	public void testTranslateObjectSchemaPropertyToJsonSchemaWithTupleArrayMapType() throws JSONObjectAdapterException {
		ObjectSchema objectSchema;
		JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
		objectSchema = new ObjectSchemaImpl(adpater);
		objectSchema.setKey(new ObjectSchemaImpl(adpater));
		objectSchema.setValue(new ObjectSchemaImpl(adpater));
		
		Mockito.doReturn(false).when(util).isPrimitive(any());
		
		Map<String, JsonSchema> translatedProperties = new LinkedHashMap<>();
		translatedProperties.put("key", new JsonSchema());
		translatedProperties.put("value", new JsonSchema());
		Mockito.doReturn(translatedProperties).when(util).translatePropertiesFromObjectSchema(any());
		
		JsonSchema result = new JsonSchema();
		result.setType(Type.object);
		result.setProperties(translatedProperties);
		
		// call under test
		assertEquals(result, util.translateObjectSchemaPropertyToJsonSchema(objectSchema, TYPE.TUPLE_ARRAY_MAP));
		Mockito.verify(util).isPrimitive(TYPE.TUPLE_ARRAY_MAP);
		Mockito.verify(util).translatePropertiesFromObjectSchema(any());
	}
	
	@Test
	public void testTranslateObjectSchemaPropertyToJsonSchemaWithMapType() throws JSONObjectAdapterException {
		ObjectSchema objectSchema;
		JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
		objectSchema = new ObjectSchemaImpl(adpater);
		objectSchema.setKey(new ObjectSchemaImpl(adpater));
		objectSchema.setValue(new ObjectSchemaImpl(adpater));
		
		Mockito.doReturn(false).when(util).isPrimitive(any());
		
		Map<String, JsonSchema> translatedProperties = new LinkedHashMap<>();
		translatedProperties.put("key", new JsonSchema());
		translatedProperties.put("value", new JsonSchema());
		Mockito.doReturn(translatedProperties).when(util).translatePropertiesFromObjectSchema(any());
		
		JsonSchema result = new JsonSchema();
		result.setType(Type.object);
		result.setProperties(translatedProperties);
		
		// call under test
		assertEquals(result, util.translateObjectSchemaPropertyToJsonSchema(objectSchema, TYPE.MAP));
		Mockito.verify(util).isPrimitive(TYPE.MAP);
		Mockito.verify(util).translatePropertiesFromObjectSchema(any());
	}
	
	@Test
	public void testTranslateObjectSchemaPropertyToJsonSchemaWithInterfaceType() throws JSONObjectAdapterException {
		ObjectSchema objectSchema;
		JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
		objectSchema = new ObjectSchemaImpl(adpater);
		objectSchema.setId("MOCK_ID");
		
		Mockito.doReturn(false).when(util).isPrimitive(any());
		Mockito.doReturn("MOCK_PATH").when(util).getPathInComponents(any());
		JsonSchema result = new JsonSchema();
		result.set$ref("MOCK_PATH");
		result.setType(Type.object);
		
		// call under test
		assertEquals(result, util.translateObjectSchemaPropertyToJsonSchema(objectSchema, TYPE.INTERFACE));
		Mockito.verify(util).isPrimitive(TYPE.INTERFACE);
		Mockito.verify(util).getPathInComponents("MOCK_ID");
	}
	
	@Test
	public void testTranslateObjectSchemaPropertyToJsonSchemaWithObjectType() throws JSONObjectAdapterException {
		ObjectSchema objectSchema;
		JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
		objectSchema = new ObjectSchemaImpl(adpater);
		objectSchema.setId("MOCK_ID");
		
		Mockito.doReturn(false).when(util).isPrimitive(any());
		Mockito.doReturn("MOCK_PATH").when(util).getPathInComponents(any());
		JsonSchema result = new JsonSchema();
		result.set$ref("MOCK_PATH");
		result.setType(Type.object);
		
		// call under test
		assertEquals(result, util.translateObjectSchemaPropertyToJsonSchema(objectSchema, TYPE.OBJECT));
		Mockito.verify(util).isPrimitive(TYPE.OBJECT);
		Mockito.verify(util).getPathInComponents("MOCK_ID");
	}
	
	@Test
	public void testTranslateObjectSchemaPropertyToJsonSchemaWithArrayType() throws JSONObjectAdapterException {
		ObjectSchema schema;
		ObjectSchema items;
		JSONObjectAdapterImpl adpater = new JSONObjectAdapterImpl();
		schema = new ObjectSchemaImpl(adpater);
		schema.setType(TYPE.ARRAY);
		
		JSONObjectAdapterImpl itemsAdapter = new JSONObjectAdapterImpl();
		items = new ObjectSchemaImpl(itemsAdapter);
		items.setType(TYPE.STRING);
		items.setId("MOCK_ID");
		
		schema.setItems(items);
		
		JsonSchema result = new JsonSchema();
		result.setType(Type.array);
		JsonSchema resultItems = new JsonSchema();
		resultItems.setType(Type.string);
		result.setItems(resultItems);
		
		// call under test
		assertEquals(result, util.translateObjectSchemaPropertyToJsonSchema(schema, TYPE.ARRAY));
		Mockito.verify(util, Mockito.times(2)).translateObjectSchemaPropertyToJsonSchema(any(), any());
		InOrder inOrder = Mockito.inOrder(util);
		inOrder.verify(util).translateObjectSchemaPropertyToJsonSchema(schema, TYPE.ARRAY);
		inOrder.verify(util).translateObjectSchemaPropertyToJsonSchema(items, TYPE.STRING);
	}
	
	@Test
	public void testTranslateObjectSchemaPropertyToJsonSchemaWithPrimitiveType() {
		Mockito.doReturn(true).when(util).isPrimitive(any());
		JsonSchema result = new JsonSchema();
		result.setType(Type.string);
		Mockito.doReturn(result).when(util).getSchemaForPrimitiveType(any());
		
		ObjectSchema property = Mockito.mock(ObjectSchema.class);
		TYPE propertyType = TYPE.STRING;
		
		// call under test
		assertEquals(result, util.translateObjectSchemaPropertyToJsonSchema(property, propertyType));
		Mockito.verify(util).isPrimitive(TYPE.STRING);
		Mockito.verify(util).getSchemaForPrimitiveType(TYPE.STRING);
	}
	
	@Test
	public void testTranslateObjectSchemaPropertyToJsonSchemaWithNullPropertyType() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			util.translateObjectSchemaPropertyToJsonSchema(Mockito.mock(ObjectSchema.class), null);
		});
		assertEquals("propertyType is required.", exception.getMessage());
	}
	
	@Test
	public void testTranslateObjectSchemaPropertyToJsonSchemaWithNullProperty() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			util.translateObjectSchemaPropertyToJsonSchema(null, TYPE.ARRAY);
		});
		assertEquals("property is required.", exception.getMessage());
	}
	
	@Test
	public void testIsPrimitiveWithNonPrimitiveType() {
		assertEquals(false, util.isPrimitive(TYPE.ARRAY));
	}
	
	@Test
	public void testIsPrimitiveWithPrimitiveTypes() {
		assertEquals(true, util.isPrimitive(TYPE.BOOLEAN));
		assertEquals(true, util.isPrimitive(TYPE.NUMBER));
		assertEquals(true, util.isPrimitive(TYPE.STRING));
		assertEquals(true, util.isPrimitive(TYPE.INTEGER));
	}
	
	@Test
	public void testInsertOneOfPropertyForInterfaces() {
		Map<String, JsonSchema> classNameToJsonSchema = new HashMap<>();
		Map<String, List<TypeReference>> interfaces = new HashMap<>();
		String className = "ClassName";
		classNameToJsonSchema.put(className, new JsonSchema());
		classNameToJsonSchema.put("mock.implementer.class.name", new JsonSchema());
		interfaces.put(className, new ArrayList<>());

		Set<TypeReference> implementers = new HashSet<>();
		TypeReference reference = Mockito.mock(TypeReference.class);
		Mockito.doReturn("mock.implementer.class.name").when(reference).getId();
		implementers.add(reference);
		Mockito.doReturn(implementers).when(util).getImplementers(any(String.class), any(Map.class));
		
		// call under test
		util.insertOneOfPropertyForInterfaces(classNameToJsonSchema, interfaces);
		
		JsonSchema expectedSchema = new JsonSchema();
		expectedSchema.set$ref("#/components/mock.implementer.class.name");
		List<JsonSchema> oneOf = new ArrayList<>();
		oneOf.add(expectedSchema);
		expectedSchema.setOneOf(oneOf);

		Map<String, JsonSchema> expectedClassNameToJsonSchema = new HashMap<>();
		expectedClassNameToJsonSchema.put(className, expectedSchema);
		expectedClassNameToJsonSchema.put("mock.implementer.class.name", new JsonSchema());
		
		assertEquals(classNameToJsonSchema, classNameToJsonSchema);
		Mockito.verify(util).getImplementers(className, interfaces);
	}
	
	@Test
	public void testInsertOneOfPropertyForInterfacesWithNonInterfaceClass() {
		Map<String, JsonSchema> classNameToJsonSchema = new HashMap<>();
		Map<String, List<TypeReference>> interfaces = new HashMap<>();
		String className = "ClassName";
		classNameToJsonSchema.put(className, new JsonSchema());
		// call under test
		util.insertOneOfPropertyForInterfaces(classNameToJsonSchema, interfaces);
		// should not modify classNameToJsonSchema
		Map<String, JsonSchema> expectedClassNameToJsonSchema = new HashMap<>();
		expectedClassNameToJsonSchema.put(className, new JsonSchema());
		assertEquals(expectedClassNameToJsonSchema, classNameToJsonSchema);
	}
	
	@Test
	public void testInsertOneOfPropertyForInterfacesWithIdMissingInClassNameToJsonSchema() {
		Map<String, JsonSchema> classNameToJsonSchema = new HashMap<>();
		Map<String, List<TypeReference>> interfaces = new HashMap<>();
		String className = "ClassName";
		classNameToJsonSchema.put(className, new JsonSchema());
		interfaces.put(className, new ArrayList<>());
		
		Set<TypeReference> implementers = new HashSet<>();
		TypeReference reference = Mockito.mock(TypeReference.class);
		Mockito.doReturn("MOCK_ID").when(reference).getId();
		implementers.add(reference);
		Mockito.doReturn(implementers).when(util).getImplementers(any(String.class), any(Map.class));
		
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			util.insertOneOfPropertyForInterfaces(classNameToJsonSchema, interfaces);
		});
		
		assertEquals("Implementation of ClassName interface with name MOCK_ID was not found.", exception.getMessage());
		Mockito.verify(util).getImplementers(className, interfaces);
	}
	
	@Test
	public void testGetPathInComponents() {
		// call under test
		assertEquals("#/components/schemas/CLASS_NAME", util.getPathInComponents("CLASS_NAME"));
	}
	
	@Test
	public void testGetPathInComponentsWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			util.getPathInComponents(null);
		});
		assertEquals("className is required.", exception.getMessage());
	}
	
	@Test
	public void testGetImplementersWithComplexCase() {
		// in the complex case, an interface is implemented by both concrete classes and other interfaces
		Map<String, List<TypeReference>> interfaces = new HashMap<>();
		
		List<TypeReference> references1 = new ArrayList<>();
		String interfaceId1 = "INTERFACE_ID_1";
		TypeReference reference1 = new TypeReference("CONCRETE_ID_1", false, false, false, new String[0], new String[0]);
		TypeReference reference2 = new TypeReference("INTERFACE_ID_2", false, false, false, new String[0], new String[0]);
		references1.add(reference1);
		references1.add(reference2);
		interfaces.put(interfaceId1, references1);
		
		String interfaceId2 = "INTERFACE_ID_2";
		List<TypeReference> references2 = new ArrayList<>();
		TypeReference reference3 = new TypeReference("CONCRETE_ID_2", false, false, false, new String[0], new String[0]);
		TypeReference reference4 = new TypeReference("CONCRETE_ID_3", false, false, false, new String[0], new String[0]);
		references2.add(reference3);
		references2.add(reference4);
		interfaces.put(interfaceId2, references2);
		
		
		assertEquals(new HashSet<>(Arrays.asList(reference1, reference3, reference4)), util.getImplementers(interfaceId1, interfaces));
	}
	
	@Test
	public void testGetImplementersWithBasicCase() {
		// in the basic case, an interface is only implemented by concrete classes
		String interfaceId = "INTERFACE_ID"; 
		Map<String, List<TypeReference>> interfaces = new HashMap<>();
		List<TypeReference> references = new ArrayList<>();
		TypeReference reference = new TypeReference("ID", false, false, false, new String[0], new String[0]);
		references.add(reference);
		interfaces.put(interfaceId, references);
		
		assertEquals(new HashSet<>(Arrays.asList(reference)), util.getImplementers(interfaceId, interfaces));
	}
	
	@Test
	public void testTranslateObjectSchemaTypeToJsonSchemaTypeWithUnhandledType() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			util.translateObjectSchemaTypeToJsonSchemaType(TYPE.TUPLE_ARRAY_MAP);
		});
		assertEquals("Unable to convert non-primitive type TUPLE_ARRAY_MAP", exception.getMessage());
	}
	
	@Test
	public void testTranslateObjectSchemaTypeToJsonSchemaTypeWithInterface() {
		assertEquals(Type.object, util.translateObjectSchemaTypeToJsonSchemaType(TYPE.INTERFACE));
	}
	
	@Test
	public void testTranslateObjectSchemaTypeToJsonSchemaTypeWithObject() {
		assertEquals(Type.object, util.translateObjectSchemaTypeToJsonSchemaType(TYPE.OBJECT));
	}
	
	@Test
	public void testTranslateObjectSchemaTypeToJsonSchemaTypeWithArray() {
		assertEquals(Type.array, util.translateObjectSchemaTypeToJsonSchemaType(TYPE.ARRAY));
	}
	
	@Test
	public void testTranslateObjectSchemaTypeToJsonSchemaTypeWithNullType() {
		assertEquals(Type._null, util.translateObjectSchemaTypeToJsonSchemaType(TYPE.NULL));
	}
	
	@Test
	public void testTranslateObjectSchemaTypeToJsonSchemaTypeWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			util.translateObjectSchemaTypeToJsonSchemaType(null);
		});
		assertEquals("type is required.", exception.getMessage());
	}
	
	@Test
	public void testGetSchemaForPrimitiveTypeWithUnhandledType() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			util.getSchemaForPrimitiveType(TYPE.MAP);
		});
		assertEquals("Unable to translate primitive type MAP", exception.getMessage());
	}
	
	@Test
	public void testGetSchemaForPrimitiveTypeWithBoolean() {
		JsonSchema expected = new JsonSchema();
		expected.setType(Type._boolean);
		// call under test
		assertEquals(expected, util.getSchemaForPrimitiveType(TYPE.BOOLEAN));
	}
	
	@Test
	public void testGetSchemaForPrimitiveTypeWithNumber() {
		JsonSchema expected = new JsonSchema();
		expected.setType(Type.number);
		// call under test
		assertEquals(expected, util.getSchemaForPrimitiveType(TYPE.NUMBER));
	}
	
	@Test
	public void testGetSchemaForPrimitiveTypeWithInteger() {
		JsonSchema expected = new JsonSchema();
		expected.setType(Type.integer);
		expected.setFormat("int32");
		// call under test
		assertEquals(expected, util.getSchemaForPrimitiveType(TYPE.INTEGER));
	}
	
	@Test
	public void testGetSchemaForPrimitiveTypeWithString() {
		JsonSchema expected = new JsonSchema();
		expected.setType(Type.string);
		// call under test
		assertEquals(expected, util.getSchemaForPrimitiveType(TYPE.STRING));
	}
	
	@Test
	public void testGetSchemaForPrimitiveTypeWithNull() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			util.getSchemaForPrimitiveType(null);
		});
		assertEquals("type is required.", exception.getMessage());
	}
}
