package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;
import org.junit.Test;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.ExampleEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONArrayAdapterImpl;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;


public class JSONEntityHttpMessageConverterHelperTest {
	
	private static final Set<Class <? extends JSONEntity>> CLASSES_TO_VALIDATE_CONVERSION 
		= Collections.singleton(CreateSchemaRequest.class);

	@Test 
	public void testReadToString() throws IOException{
		String value = "This string should make a round trip!";
		StringReader reader = new StringReader(value);
		String clone = JSONEntityHttpMessageConverterHelper.readToString(reader);
		assertEquals(value, clone);
	}

	@Test
	public void testReadEntity() throws JSONObjectAdapterException, IOException{
		ExampleEntity entity = new ExampleEntity();
		entity.setName("name");
		// this version requires a class name fo the entity type.
		entity.setDoubleList(new ArrayList<Double>());
		entity.getDoubleList().add(123.45);
		entity.getDoubleList().add(4.56);
		// To string
		String jsonString =EntityFactory.createJSONStringForEntity(entity);
		StringReader reader = new StringReader(jsonString);
		ExampleEntity clone = (ExampleEntity) JSONEntityHttpMessageConverterHelper.readEntity(reader);
		assertEquals(entity, clone);
	}

	@Test (expected=JSONObjectAdapterException.class)
	public void testReadEntityNullType() throws JSONObjectAdapterException, IOException{
		ExampleEntity entity = new ExampleEntity();
		entity.setName("name");
		// this version requires a class name fo the entity type.
		entity.setConcreteType(null);
		entity.setDoubleList(new ArrayList<Double>());
		entity.getDoubleList().add(123.45);
		entity.getDoubleList().add(4.56);
		// To string
		String jsonString =EntityFactory.createJSONStringForEntity(entity);
		StringReader reader = new StringReader(jsonString);
		JSONEntityHttpMessageConverterHelper.readEntity(reader);
	}

	/**
	 * This test was added for PLFM-1280.
	 * @throws JSONObjectAdapterException
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testCreateEntityFromAdapterClassNotFound() throws JSONObjectAdapterException{
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter.put("concreteType", "org.sagebionetworks.FakeClass");
		JSONEntityHttpMessageConverterHelper.createEntityFromAdapter(adapter);
	}

	/**
	 * This test was added for PLFM-1280.
	 * @throws JSONObjectAdapterException
	 */
	@Test (expected=JSONObjectAdapterException.class)
	public void testCreateEntityFromAdapterBadJSON() throws JSONObjectAdapterException{
		// Test a valid entity type with a field that does not exist on that type.
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter.put("entityType", ExampleEntity.class.getName());
		adapter.put("notAField", "shoudld not exist");
		JSONEntityHttpMessageConverterHelper.createEntityFromAdapter(adapter);
	}
	
	
	@Test
	public void testValidateJSONEntityWithValid() throws Exception {
		// setup
		JSONObjectAdapter schema = new JSONObjectAdapterImpl();
		schema.put("description", "Expect this to fail");
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter.put("concreteType", "org.sagebionetworks.repo.model.schema.CreateSchemaRequest");
		adapter.put("schema", schema);
		String beforeJsonString = adapter.toJSONString();
		CreateSchemaRequest entity = new CreateSchemaRequest(adapter);
		// call under test
		JSONEntityHttpMessageConverterHelper.validateJSONEntity(entity, beforeJsonString);
	}
	
	
	@Test
	public void testValidateJSONEntityWithExtraField() throws Exception {
		// setup no exception to be thrown
		JSONObjectAdapter schema = new JSONObjectAdapterImpl();
		schema.put("description", "Expect this to fail");
		schema.put("notPartOfSpecification", "random");
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter.put("concreteType", "org.sagebionetworks.repo.model.schema.CreateSchemaRequest");
		adapter.put("schema", schema);
		String beforeJsonString = adapter.toJSONString();
		CreateSchemaRequest entity = new CreateSchemaRequest(adapter);
		// call under test
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JSONEntityHttpMessageConverterHelper.validateJSONEntity(entity, beforeJsonString);
		}).getMessage();
		assertEquals(message, "JSON Element in Entity is Unsupported: notPartOfSpecification");
	}
	
	
	@Test
	public void testValidateJSONEntityWithExtraFieldInEmbeddedSchema() throws Exception {
		// setup no exception to be thrown
		JSONObjectAdapter items = new JSONObjectAdapterImpl();
		items.put("notPartOfSpecification", "random");
		JSONObjectAdapter schema = new JSONObjectAdapterImpl();
		// items is a JsonSchema
		schema.put("items", items);
		schema.put("description", "Expect this to fail");
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter.put("concreteType", "org.sagebionetworks.repo.model.schema.CreateSchemaRequest");
		adapter.put("schema", schema);
		String beforeJsonString = adapter.toJSONString();
		CreateSchemaRequest entity = new CreateSchemaRequest(adapter);
		// call under test
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JSONEntityHttpMessageConverterHelper.validateJSONEntity(entity, beforeJsonString);
		}).getMessage();
		assertEquals(message, "JSON Element in Entity is Unsupported: notPartOfSpecification");
	}
	
	@Test
	public void testValidateJSONEntityWithExtraFieldInArray() throws Exception {
		// setup no exception to be thrown
		JSONArrayAdapter allOf = new JSONArrayAdapterImpl();
		JSONObjectAdapter schemaInArray1 = new JSONObjectAdapterImpl();
		schemaInArray1.put("notPartOfSpecification", "random");
		JSONObjectAdapter schemaInArray2 = new JSONObjectAdapterImpl();
		schemaInArray2.put("description", "this is valid though");
		allOf.put(0, schemaInArray2);
		allOf.put(1, schemaInArray1);
		JSONObjectAdapter schema = new JSONObjectAdapterImpl();
		// "allOf" is an array of JsonSchemas
		schema.put("allOf", allOf);
		schema.put("description", "Expect this to fail");
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter.put("concreteType", "org.sagebionetworks.repo.model.schema.CreateSchemaRequest");
		adapter.put("schema", schema);
		String beforeJsonString = adapter.toJSONString();
		CreateSchemaRequest entity = new CreateSchemaRequest(adapter);
		// call under test
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JSONEntityHttpMessageConverterHelper.validateJSONEntity(entity, beforeJsonString);
		}).getMessage();
		assertEquals(message, "JSON Element in Entity is Unsupported: notPartOfSpecification");
	}
	
	@Test
	public void testValidateJSONEntityWithExtraFieldInMap() throws Exception {
		// setup no exception to be thrown
		JSONObjectAdapter properties = new JSONObjectAdapterImpl();
		JSONObjectAdapter schema1 = new JSONObjectAdapterImpl();
		schema1.put("description", "Expect this to fail");
		JSONObjectAdapter schema2 = new JSONObjectAdapterImpl();
		schema2.put("notPartOfSpecification", "random");
		properties.put("schema1", schema1);
		properties.put("schema2", schema2);
		JSONObjectAdapter schema = new JSONObjectAdapterImpl();
		// properties is a map of String to JsonSchema
		schema.put("properties", properties);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter.put("schema", schema);
		adapter.put("concreteType", "org.sagebionetworks.repo.model.schema.CreateSchemaRequest");
		String beforeJsonString = adapter.toJSONString();
		CreateSchemaRequest entity = new CreateSchemaRequest(adapter);
		// call under test
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JSONEntityHttpMessageConverterHelper.validateJSONEntity(entity, beforeJsonString);
		}).getMessage();
		assertEquals(message, "JSON Element in Entity is Unsupported: notPartOfSpecification");
	}

	@Test
	public void testValidateJSONEntityWithRequired() throws Exception {
		// setup
		JsonSchema schema = new JsonSchema();
		schema.setDescription("test description");
		schema.setRequired(Arrays.asList("one", "two"));
		JSONObjectAdapter schemaAdapter = new JSONObjectAdapterImpl();
		schema.writeToJSONObject(schemaAdapter);
		
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter.put("concreteType", "org.sagebionetworks.repo.model.schema.CreateSchemaRequest");
		adapter.put("schema", schemaAdapter);
		String beforeJsonString = adapter.toJSONString();
		CreateSchemaRequest entity = new CreateSchemaRequest(adapter);
		// call under test
		JSONEntityHttpMessageConverterHelper.validateJSONEntity(entity, beforeJsonString);
	}
	
	@Test
	public void testValidateJSONEntityRecursiveWithMissingRequiredElement() throws Exception {
		// this test covers arrays of strings, missing element in array
		// setup
		JsonSchema parsedSchema = new JsonSchema();
		parsedSchema.setDescription("test description");
		parsedSchema.setRequired(Arrays.asList("one", "two"));
		JSONObjectAdapter parsedSchemaAdapter = new JSONObjectAdapterImpl();
		parsedSchema.writeToJSONObject(parsedSchemaAdapter);
		JSONObjectAdapter parsedAdapter = new JSONObjectAdapterImpl();
		parsedAdapter.put("concreteType", "org.sagebionetworks.repo.model.schema.CreateSchemaRequest");
		parsedAdapter.put("schema", parsedSchemaAdapter);
		
		JsonSchema originalSchema = new JsonSchema();
		originalSchema.setDescription("test description");
		originalSchema.setRequired(Arrays.asList("one", "two", "three"));
		JSONObjectAdapter originalSchemaAdapter = new JSONObjectAdapterImpl();
		originalSchema.writeToJSONObject(originalSchemaAdapter);
		JSONObjectAdapter originalAdapter = new JSONObjectAdapterImpl();
		originalAdapter.put("concreteType", "org.sagebionetworks.repo.model.schema.CreateSchemaRequest");
		originalAdapter.put("schema", originalSchemaAdapter);

		String message = assertThrows(IllegalArgumentException.class, () -> { 
			JSONEntityHttpMessageConverterHelper.validateJSONEntityRecursive(parsedAdapter, originalAdapter);
		}).getMessage();
		assertEquals("Missing element in child array of required element on conversion", message);
	}
	
	@Test
	public void testValidateJSONEntityRecursiveWithIntegerArray() throws Exception {
		// setup
		int firstVal = 1;
		int secondVal = 2;
		JSONObjectAdapter parsedAdapter = new JSONObjectAdapterImpl();
		JSONArrayAdapter parsedArray = new JSONArrayAdapterImpl();
		parsedArray.put(0, firstVal);
		parsedArray.put(1, secondVal);
		parsedAdapter.put("arrayKey", parsedArray);
		
		JSONObjectAdapter originalAdapter = new JSONObjectAdapterImpl();
		JSONArrayAdapter originalArray = new JSONArrayAdapterImpl();
		originalArray.put(0, firstVal);
		originalArray.put(1, secondVal);
		originalAdapter.put("arrayKey", originalArray);
		
		// call under test
		JSONEntityHttpMessageConverterHelper.validateJSONEntityRecursive(parsedAdapter, originalAdapter);
	}
	
	@Test
	public void testValidateJSONEntityRecursiveWithInvalidArrayOfIntegers() throws Exception {
		// setup
		int firstVal = 1;
		int secondVal = 2;
		JSONObjectAdapter parsedAdapter = new JSONObjectAdapterImpl();
		JSONArrayAdapter parsedArray = new JSONArrayAdapterImpl();
		parsedArray.put(0, firstVal);
		parsedAdapter.put("arrayKey", parsedArray);
		
		JSONObjectAdapter originalAdapter = new JSONObjectAdapterImpl();
		JSONArrayAdapter originalArray = new JSONArrayAdapterImpl();
		originalArray.put(0, firstVal);
		originalArray.put(1, secondVal);
		originalAdapter.put("arrayKey", originalArray);
		
		// call under test
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JSONEntityHttpMessageConverterHelper.validateJSONEntityRecursive(parsedAdapter, originalAdapter);
		}).getMessage();
		
		assertEquals("Missing element in child array of arrayKey element on conversion", message);
	}
	
	@Test
	public void testValidateJSONEntityRecursiveWithArrayOfArrays() throws Exception {
		// set up
		// The following object is being set up: { "arrayKey": [[], [true, false]] }
		JSONObjectAdapter parsedAdapter = new JSONObjectAdapterImpl();
		JSONArrayAdapter parsedArray = new JSONArrayAdapterImpl();
		JSONArrayAdapter innerParsedArrayOne = new JSONArrayAdapterImpl();
		JSONArrayAdapter innerParsedArrayTwo = new JSONArrayAdapterImpl();
		innerParsedArrayTwo.put(0, true);
		innerParsedArrayTwo.put(1, false);
		parsedArray.put(0, innerParsedArrayOne);
		parsedArray.put(1, innerParsedArrayTwo);
		parsedAdapter.put("arrayKey", parsedArray);
		
		JSONObjectAdapter originalAdapter = new JSONObjectAdapterImpl();
		JSONArrayAdapter originalArray = new JSONArrayAdapterImpl();
		JSONArrayAdapter innerOriginalArrayOne = new JSONArrayAdapterImpl();
		JSONArrayAdapter innerOriginalArrayTwo = new JSONArrayAdapterImpl();
		innerOriginalArrayTwo.put(0, true);
		innerOriginalArrayTwo.put(1, false);
		originalArray.put(0, innerOriginalArrayOne);
		originalArray.put(1, innerOriginalArrayTwo);
		originalAdapter.put("arrayKey", originalArray);
		
		// call under test
		JSONEntityHttpMessageConverterHelper.validateJSONEntityRecursive(parsedAdapter, originalAdapter);
	}
	
	@Test
	public void testValidateJSONEntityRecursiveWithMissingArrayInArrayOfArrays() throws Exception {
		// setup
		// parsedAdapter: { "arrayKey": [[]] }
		// originalAdapter: { "arrayKey": [[], [true, false]] }
		JSONObjectAdapter parsedAdapter = new JSONObjectAdapterImpl();
		JSONArrayAdapter parsedArray = new JSONArrayAdapterImpl();
		JSONArrayAdapter innerParsedArrayOne = new JSONArrayAdapterImpl();
		parsedArray.put(0, innerParsedArrayOne);
		parsedAdapter.put("arrayKey", parsedArray);
		
		JSONObjectAdapter originalAdapter = new JSONObjectAdapterImpl();
		JSONArrayAdapter originalArray = new JSONArrayAdapterImpl();
		JSONArrayAdapter innerOriginalArrayOne = new JSONArrayAdapterImpl();
		JSONArrayAdapter innerOriginalArrayTwo = new JSONArrayAdapterImpl();
		innerOriginalArrayTwo.put(0, true);
		innerOriginalArrayTwo.put(1, false);
		originalArray.put(0, innerOriginalArrayOne);
		originalArray.put(1, innerOriginalArrayTwo);
		originalAdapter.put("arrayKey", originalArray);
		
		// call under test
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JSONEntityHttpMessageConverterHelper.validateJSONEntityRecursive(parsedAdapter, originalAdapter);
		}).getMessage();
		
		assertEquals("Missing element in child array of arrayKey element on conversion", message);
	}
	
	@Test
	public void testValidateJSONEntityRecursiveWithValidArrayOfJSONObjects() throws Exception {
		// set up
		// The following object is being set up: { "arrayKey": [[], [true, false]] }
		JSONObjectAdapter parsedAdapter = new JSONObjectAdapterImpl();
		JSONArrayAdapter parsedArray = new JSONArrayAdapterImpl();
		JSONObjectAdapter parsedObjectOne = new JSONObjectAdapterImpl();
		JSONObjectAdapter parsedObjectTwo = new JSONObjectAdapterImpl();
		parsedObjectOne.put("stringKey", "value");
		parsedArray.put(0, parsedObjectOne);
		parsedArray.put(1, parsedObjectTwo);
		parsedAdapter.put("arrayKey", parsedArray);
		
		JSONObjectAdapter originalAdapter = new JSONObjectAdapterImpl();
		JSONArrayAdapter originalArray = new JSONArrayAdapterImpl();
		JSONObjectAdapter originalObjectOne = new JSONObjectAdapterImpl();
		JSONObjectAdapter originalObjectTwo = new JSONObjectAdapterImpl();
		originalObjectOne.put("stringKey", "value");
		originalArray.put(0, originalObjectOne);
		originalArray.put(1, originalObjectTwo);
		originalAdapter.put("arrayKey", originalArray);
		
		// call under test
		JSONEntityHttpMessageConverterHelper.validateJSONEntityRecursive(parsedAdapter, originalAdapter);
	}
	
	
	@Test
	public void testPLFM_2079() throws Exception{
		// In the past we used the "entityType" field to determine which implementation Entity to create when a caller passed an JSON string.
		// This was specific to Entity so when the JSON schema project tackled the same problem "concreteType" was used instead of entityType.
		// We then switch Entities to use concreteType but we did not want this to be a breaking API change.
		// So when a old client uses "entityType" it should not break.
		
		// Create some JSON using a project entity.
		Project project = new Project();
		project.setName("someProject");
		project.setParentId("syn123");
		project.setId("syn456");
		JSONObject jsonObject = new JSONObject();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonObject);
		project.writeToJSONObject(adapter);
		// Swap the concreteType field with entityType
		String type = jsonObject.getString("concreteType");
		jsonObject.remove("concreteType");
		// replace it with entity type
		jsonObject.put("entityType", type);
		String json = adapter.toJSONString();
		assertTrue(json.indexOf("entityType") > 0);
		assertFalse(json.indexOf("concreteType") > 0);
		// Now make sure we can parse the json
		try{
			Project clone = (Project) JSONEntityHttpMessageConverterHelper.read(json, 
					null, Entity.class, CLASSES_TO_VALIDATE_CONVERSION);
			assertNotNull(clone);
			// It should match the original
			assertEquals(project, clone);
		}catch(Exception e){
			throw new RuntimeException(json,e);
		}
		
	}
		
	@Test
	public void testReadWhereEntityTypeNotToBeValidated() throws Exception {
			// PLFM-6320
			// empty set, not entities to validate
			Set<Class <? extends JSONEntity>> set = new HashSet<>();
			JSONObjectAdapter schema = new JSONObjectAdapterImpl();
			schema.put("description", "Expect this to fail");
			// unsupported element
			schema.put("notPartOfSpecification", "random");
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
			adapter.put("concreteType", "org.sagebionetworks.repo.model.schema.CreateSchemaRequest");
			adapter.put("schema", schema);
			String jsonString = adapter.toJSONString();
			// call under test
			CreateSchemaRequest result = (CreateSchemaRequest)JSONEntityHttpMessageConverterHelper.read(jsonString, 
					null, CreateSchemaRequest.class, set);
			assertNotNull(result);
	}
	
	@Test
	public void testReadWhereValidationOfInvalidSuccess() throws Exception {
		// PLFM-6320
		// Invalid element, and the entity is one in which we want to validate
		// setup no exception to be thrown
		JSONObjectAdapter schema = new JSONObjectAdapterImpl();
		schema.put("description", "Expect this to fail");
		schema.put("notPartOfSpecification", "random");
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		adapter.put("concreteType", "org.sagebionetworks.repo.model.schema.CreateSchemaRequest");
		adapter.put("schema", schema);
		String jsonString = adapter.toJSONString();
		// call under test
		String message = assertThrows(IllegalArgumentException.class, () -> {
			JSONEntityHttpMessageConverterHelper.read(jsonString, null, 
					CreateSchemaRequest.class, CLASSES_TO_VALIDATE_CONVERSION);
		}).getMessage();
		assertEquals(message, "JSON Element in Entity is Unsupported: notPartOfSpecification");
	}
}
