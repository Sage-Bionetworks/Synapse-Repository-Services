package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.repo.model.ExampleEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.query.FieldType;
import org.sagebionetworks.schema.FORMAT;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.TYPE;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

/**
 * Unit test for the field type cache
 * 
 * @author John
 *
 */
public class FieldTypeCacheTest {
	

	@Test
	public void testInvalidNames() {
		// There are all invalid names
		String[] invalidNames = new String[] { "~", "!", "@", "#", "$", "%",
				"^", "&", "*", "(", ")", "\"", "\n\t", "'", "?", "<", ">", "/",
				";", "{", "}", "|", "=", "+", "-", "White\n\t Space", null, "" };
		for (int i = 0; i < invalidNames.length; i++) {
			try {
				// These are all bad names
				FieldTypeCache.checkKeyName(invalidNames[i]);
				fail("Name: " + invalidNames[i] + " is invalid");
			} catch (InvalidModelException e) {
				// Expected
			}
		}
	}

	@Test
	public void testValidNames() throws InvalidModelException {
		// There are all invalid names
		List<String> vlaidNames = new ArrayList<String>();
		// All lower
		for (char ch = 'a'; ch <= 'z'; ch++) {
			vlaidNames.add("" + ch);
		}
		// All upper
		for (char ch = 'A'; ch <= 'Z'; ch++) {
			vlaidNames.add("" + ch);
		}
		// all numbers
		for (char ch = '0'; ch <= '9'; ch++) {
			vlaidNames.add("" + ch);
		}
		// underscore
		vlaidNames.add("_");
		vlaidNames.add(" Trimable ");
		vlaidNames.add("A1_b3po");
		for (int i = 0; i < vlaidNames.size(); i++) {
			// These are all bad names
			FieldTypeCache.checkKeyName(vlaidNames.get(i));
		}
	}
	
	@Test
	public void testGetFieldTypeForPropertyString(){
		String key = "test";
		String id = "test.id.org";
		ObjectSchema propSchema = new ObjectSchema(TYPE.STRING);
		assertEquals(FieldType.STRING_ATTRIBUTE, FieldTypeCache.getFieldTypeForProperty(key, id, propSchema));
	}
	
	@Test
	public void testGetFieldTypeForPropertyLong(){
		String key = "test";
		String id = "test.id.org";
		ObjectSchema propSchema = new ObjectSchema(TYPE.INTEGER);
		assertEquals(FieldType.LONG_ATTRIBUTE, FieldTypeCache.getFieldTypeForProperty(key, id, propSchema));
	}
	
	@Test
	public void testGetFieldTypeForPropertyDouble(){
		String key = "test";
		String id = "test.id.org";
		ObjectSchema propSchema = new ObjectSchema(TYPE.NUMBER);
		assertEquals(FieldType.DOUBLE_ATTRIBUTE, FieldTypeCache.getFieldTypeForProperty(key, id, propSchema));
	}
	
	@Test
	public void testGetFieldTypeForPropertyBoolean(){
		String key = "test";
		String id = "test.id.org";
		ObjectSchema propSchema = new ObjectSchema(TYPE.BOOLEAN);
		assertEquals(FieldType.STRING_ATTRIBUTE, FieldTypeCache.getFieldTypeForProperty(key, id, propSchema));
	}
	
	@Test
	public void testGetFieldTypeForPropertyStringDate(){
		String key = "test";
		String id = "test.id.org";
		ObjectSchema propSchema = new ObjectSchema(TYPE.STRING);
		propSchema.setFormat(FORMAT.DATE_TIME);
		assertEquals(FieldType.DATE_ATTRIBUTE, FieldTypeCache.getFieldTypeForProperty(key, id, propSchema));
	}
	
	@Test
	public void testGetFieldTypeForPropertyLongDate(){
		String key = "test";
		String id = "test.id.org";
		ObjectSchema propSchema = new ObjectSchema(TYPE.INTEGER);
		propSchema.setFormat(FORMAT.UTC_MILLISEC);
		assertEquals(FieldType.DATE_ATTRIBUTE, FieldTypeCache.getFieldTypeForProperty(key, id, propSchema));
	}
	
	@Test
	public void testGetFieldTypeForPropertyArrayString(){
		String key = "test";
		String id = "test.id.org";
		ObjectSchema propSchema = new ObjectSchema(TYPE.ARRAY);
		propSchema.setItems(new ObjectSchema(TYPE.STRING));
		assertEquals(FieldType.STRING_ATTRIBUTE, FieldTypeCache.getFieldTypeForProperty(key, id, propSchema));
	}
		
	@Test
	public void testValidateAndAddToCachePrimary(){
		// test that a field already mapped to a primary field remains mapped to a primary field.
		Map<String, FieldType> cache = new HashMap<String, FieldType>();
		String key = "test";
		cache.put(key, FieldType.PRIMARY_FIELD);
		FieldTypeCache.validateAndAddToCache(key, FieldType.DOUBLE_ATTRIBUTE, cache);
		// Should still be mapped to primary
		assertEquals(FieldType.PRIMARY_FIELD, cache.get(key));
	}
	
	@Test
	public void testValidateAndAddToCachNew(){
		// test that a field already mapped to a primary field remains mapped to a primary field.
		Map<String, FieldType> cache = new HashMap<String, FieldType>();
		String key = "test";
		FieldTypeCache.validateAndAddToCache(key, FieldType.DOUBLE_ATTRIBUTE, cache);
		// Should still be mapped to primary
		assertEquals(FieldType.DOUBLE_ATTRIBUTE, cache.get(key));
	}
	
	@Test
	public void testValidateAndAddToCachDuplicateNoConflict(){
		// test that a field already mapped to a primary field remains mapped to a primary field.
		Map<String, FieldType> cache = new HashMap<String, FieldType>();
		String key = "test";
		// It is already mapped to the double type.  Since the new is the same type there is no problem.
		cache.put(key, FieldType.DOUBLE_ATTRIBUTE);
		FieldTypeCache.validateAndAddToCache(key, FieldType.DOUBLE_ATTRIBUTE, cache);
		// Should still be mapped to primary
		assertEquals(FieldType.DOUBLE_ATTRIBUTE, cache.get(key));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateAndAddToCachDuplicateConfilct(){
		// test that a field already mapped to a primary field remains mapped to a primary field.
		Map<String, FieldType> cache = new HashMap<String, FieldType>();
		String key = "test";
		// It is already mapped to the double type. 
		cache.put(key, FieldType.DOUBLE_ATTRIBUTE);
		// This should fail since we are already mapped to a double.
		FieldTypeCache.validateAndAddToCache(key, FieldType.STRING_ATTRIBUTE, cache);
	}
	
	@Test
	public void testNodeNames() throws Exception{
		// Make sure all node fields are primary fields.
		Field[] fields = Node.class.getDeclaredFields();
		for(Field field: fields){
			// Add the primary fields from the node class
			assertEquals(FieldType.PRIMARY_FIELD, FieldTypeCache.getInstance().getTypeForName(field.getName()));
		}
	}
	
	@Test
	public void testAllExampleEntity() throws Exception{
		
		// This is the expected mapping:
		Map<String, FieldType> expected = new HashMap<String, FieldType>();
		expected.put("concept", FieldType.STRING_ATTRIBUTE);
		expected.put("versionLabel", FieldType.PRIMARY_FIELD);
		expected.put("etag", FieldType.STRING_ATTRIBUTE);
		expected.put("singleDouble", FieldType.DOUBLE_ATTRIBUTE);
		expected.put("modifiedBy", FieldType.STRING_ATTRIBUTE);
		expected.put("singleString", FieldType.STRING_ATTRIBUTE);
		expected.put("stringList", FieldType.STRING_ATTRIBUTE);
		
		ObjectSchema exampleSchema = EntityFactory.createEntityFromJSONString(ExampleEntity.EFFECTIVE_SCHEMA, ObjectSchema.class);
		Iterator<String> it = exampleSchema.getProperties().keySet().iterator();
		while(it.hasNext()){
			String key = it.next();
			FieldType type = FieldTypeCache.getInstance().getTypeForName(key);
			assertNotNull(type);
			FieldType expectedType = expected.get(key);
			if(expectedType != null){
				assertEquals(expectedType, type);
			}
			System.out.println(key+": "+type.name());
		}
	}

}
