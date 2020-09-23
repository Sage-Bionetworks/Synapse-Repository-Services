package org.sagebionetworks.repo.manager.schema;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ObjectType;
import org.sagebionetworks.repo.model.schema.ValidationException;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class JsonSchemaValidationManagerImplTest {


	@InjectMocks
	JsonSchemaValidationManagerImpl manager;

	String objectId;
	ObjectType objectType;
	String objectEtag;
	String concreteType;

	@BeforeEach
	public void before() {
		objectId = "syn123";
		objectType = ObjectType.entity;
		objectEtag = "some-etag";
		concreteType = "foo.bar.Testing";
	}

	@Test
	public void testValidationWithValid() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/Enum.json");
		JsonSubject subject = setupSubject();
		subject.toJson().put("enumKey", "a");
		// call under test
		ValidationResults result = manager.validate(schema, subject);
		
		assertNotNull(result);
		assertEquals(objectId, result.getObjectId());
		assertEquals(objectType, result.getObjectType());
		assertEquals(objectEtag, result.getObjectEtag());
		assertTrue(result.getIsValid());
		assertNotNull(result.getValidatedOn());
		assertNull(result.getValidationErrorMessage());
		assertNull(result.getAllValidationMessages());
		assertNull(result.getValidationException());
	}
	
	@Test
	public void testValidationWithInvalid() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/Enum.json");
		JsonSubject subject = setupSubject();
		subject.toJson().put("enumKey", "c");
		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertEquals(objectId, result.getObjectId());
		assertEquals(objectType, result.getObjectType());
		assertEquals(objectEtag, result.getObjectEtag());
		assertEquals("hasEnum", result.getSchema$id());
		assertFalse(result.getIsValid());
		assertNotNull(result.getValidatedOn());
		assertEquals("#: only 1 subschema matches out of 2", result.getValidationErrorMessage());
		assertEquals(Lists.newArrayList("#/enumKey: c is not a valid enum value"), result.getAllValidationMessages());
		// root exception
		ValidationException exception = result.getValidationException();
		assertNotNull(exception);
		assertEquals("allOf", exception.getKeyword());
		assertEquals("#: only 1 subschema matches out of 2", exception.getMessage());
		assertEquals("#/enumKey", exception.getPointerToViolation());
		assertEquals("#/properties/enumKey", exception.getSchemaLocation());
		List<ValidationException> causingExceptions = exception.getCausingExceptions();
		assertNotNull(causingExceptions);
		assertEquals(1, causingExceptions.size());
		// sub exception
		ValidationException subException = causingExceptions.get(0);
		assertEquals("enum", subException.getKeyword());
		assertEquals("c is not a valid enum value", subException.getMessage());
		assertEquals("#/enumKey", subException.getPointerToViolation());
		assertNull(subException.getSchemaLocation());
	}
	
	@Test
	public void testValidationWithNullSchema() {
		JsonSchema schema = null;
		JsonSubject subject =  Mockito.mock(JsonSubject.class);
		assertThrows(IllegalArgumentException.class, ()->{
			 manager.validate(schema, subject);
		});
	}
	
	@Test
	public void testValidationWithNullSubject() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/Enum.json");
		JsonSubject subject = null;
		assertThrows(IllegalArgumentException.class, ()->{
			 manager.validate(schema, subject);
		});
	}
	
	/**
	 * Expect validation to ignore the 'source' attribute.
	 * @throws Exception
	 */
	@Test
	public void testValidationWithSourced() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/HasSource.json");
		assertEquals("http://some.domain.org/original/work", schema.getSource());
		JsonSubject subject = setupSubject();
		// call under test
		ValidationResults result = manager.validate(schema, subject);
		
		assertNotNull(result);
		assertEquals(objectId, result.getObjectId());
		assertEquals(objectType, result.getObjectType());
		assertEquals(objectEtag, result.getObjectEtag());
		assertTrue(result.getIsValid());
		assertNotNull(result.getValidatedOn());
		assertNull(result.getValidationErrorMessage());
		assertNull(result.getAllValidationMessages());
		assertNull(result.getValidationException());
	}
	
	/**
	 * Test for PLFM-6316 to add the 'required' key word.
	 */
	@Test
	public void testValidationWithRequiredWithRequired() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/HasRequired.json");
		assertNotNull(schema.getProperties());
		assertEquals(3, schema.getProperties().size());
		List<String> requiredValues = schema.getRequired();
		assertNotNull(requiredValues);
		assertEquals(Lists.newArrayList("requireMe","requireMeToo"), schema.getRequired());
		
		// include all three values for this test
		JsonSubject subject = setupSubject();
		subject.toJson().put("requireMe", "one");
		subject.toJson().put("requireMeToo", "two");
		subject.toJson().put("notRequired", "two");

		// call under test
		ValidationResults result = manager.validate(schema, subject);

		assertNotNull(result);
		assertEquals(objectId, result.getObjectId());
		assertEquals(objectType, result.getObjectType());
		assertEquals(objectEtag, result.getObjectEtag());
		assertTrue(result.getIsValid());
		assertNotNull(result.getValidatedOn());
		assertNull(result.getValidationErrorMessage());
		assertNull(result.getAllValidationMessages());
		assertNull(result.getValidationException());
	}
	
	@Test
	public void testValidationWithRequiredWithOutRequired() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/HasRequired.json");
		assertNotNull(schema.getProperties());
		assertEquals(3, schema.getProperties().size());
		List<String> requiredValues = schema.getRequired();
		assertNotNull(requiredValues);
		assertEquals(Lists.newArrayList("requireMe","requireMeToo"), schema.getRequired());
		
		// exclude the optional value
		JsonSubject subject = setupSubject();
		subject.toJson().put("requireMe", "one");
		subject.toJson().put("requireMeToo", "two");

		// call under test
		ValidationResults result = manager.validate(schema, subject);

		assertNotNull(result);
		assertEquals(objectId, result.getObjectId());
		assertEquals(objectType, result.getObjectType());
		assertEquals(objectEtag, result.getObjectEtag());
		assertTrue(result.getIsValid());
		assertNotNull(result.getValidatedOn());
		assertNull(result.getValidationErrorMessage());
		assertNull(result.getAllValidationMessages());
		assertNull(result.getValidationException());
	}
	
	@Test
	public void testValidationWithRequiredWithMissingRequired() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/HasRequired.json");
		assertNotNull(schema.getProperties());
		assertEquals(3, schema.getProperties().size());
		List<String> requiredValues = schema.getRequired();
		assertNotNull(requiredValues);
		assertEquals(Lists.newArrayList("requireMe","requireMeToo"), schema.getRequired());
		
		// include the optional but exclude one of the required
		JsonSubject subject = setupSubject();
		subject.toJson().put("requireMe", "one");
		subject.toJson().put("notRequired", "two");

		// call under test
		ValidationResults result = manager.validate(schema, subject);

		assertNotNull(result);
		assertEquals(objectId, result.getObjectId());
		assertEquals(objectType, result.getObjectType());
		assertEquals(objectEtag, result.getObjectEtag());
		assertFalse(result.getIsValid());
		assertNotNull(result.getValidatedOn());
		assertEquals("required key [requireMeToo] not found", result.getValidationErrorMessage());
	}
	
	/**
	 * Test for PLFM-6403
	 * @throws Exception
	 */
	@Test
	public void testValidationWithIntegerTypeAndIntegerValue() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/HasInteger.json");
		assertNotNull(schema.getProperties());
		assertEquals(1, schema.getProperties().size());
		JsonSchema subSchema = schema.getProperties().get("someInteger");
		assertNotNull(subSchema);
		assertEquals(Type.integer, subSchema.getType());
		JsonSubject subject = setupSubject();
		subject.toJson().put("someInteger", 123);

		// call under test
		ValidationResults result = manager.validate(schema, subject);

		assertNotNull(result);
		assertEquals(objectId, result.getObjectId());
		assertEquals(objectType, result.getObjectType());
		assertEquals(objectEtag, result.getObjectEtag());
		assertTrue(result.getIsValid());
		assertNotNull(result.getValidatedOn());
		assertNull(result.getValidationErrorMessage());
		assertNull(result.getAllValidationMessages());
		assertNull(result.getValidationException());
	}

	@Test
	public void testValidationWithIntegerTypeAndNegativeIntegerValue() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/HasInteger.json");
		assertNotNull(schema.getProperties());
		assertEquals(1, schema.getProperties().size());
		JsonSchema subSchema = schema.getProperties().get("someInteger");
		assertNotNull(subSchema);
		assertEquals(Type.integer, subSchema.getType());
		JsonSubject subject = setupSubject();
		subject.toJson().put("someInteger", -123);

		// call under test
		ValidationResults result = manager.validate(schema, subject);

		assertNotNull(result);
		assertEquals(objectId, result.getObjectId());
		assertEquals(objectType, result.getObjectType());
		assertEquals(objectEtag, result.getObjectEtag());
		assertTrue(result.getIsValid());
		assertNotNull(result.getValidatedOn());
		assertNull(result.getValidationErrorMessage());
		assertNull(result.getAllValidationMessages());
		assertNull(result.getValidationException());
	}

	@Test
	public void testValidationWithIntegerTypeAndDoubleValue() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/HasInteger.json");
		assertNotNull(schema.getProperties());
		assertEquals(1, schema.getProperties().size());
		JsonSchema subSchema = schema.getProperties().get("someInteger");
		assertNotNull(subSchema);
		assertEquals(Type.integer, subSchema.getType());
		JsonSubject subject = setupSubject();
		subject.toJson().put("someInteger", 123.456);

		// call under test
		ValidationResults result = manager.validate(schema, subject);

		assertNotNull(result);
		assertEquals(objectId, result.getObjectId());
		assertEquals(objectType, result.getObjectType());
		assertEquals(objectEtag, result.getObjectEtag());
		assertFalse(result.getIsValid());
		assertNotNull(result.getValidatedOn());
		assertEquals("expected type: Integer, found: Double", result.getValidationErrorMessage());
	}

	
	public JsonSubject setupSubject() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("objectId", objectId);
		jsonObject.put("objectType", objectType);
		jsonObject.put("objectEtag", objectEtag);
		
		JsonSubject subject = Mockito.mock(JsonSubject.class);
		when(subject.getObjectId()).thenReturn(objectId);
		when(subject.getObjectEtag()).thenReturn(objectEtag);
		when(subject.getObjectType()).thenReturn(objectType);
		when(subject.toJson()).thenReturn(jsonObject);

		return subject;
	}

	/**
	 * Load a schema from the classpath.
	 * 
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public JsonSchema loadSchemaFromClasspath(String name) throws Exception {
		String jsonString = loadStringFromClasspath(name);
		return new JsonSchema(new JSONObjectAdapterImpl(new JSONObject(jsonString)));
	}

	/**
	 * Load the file contents from the class path.
	 * 
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public String loadStringFromClasspath(String name) throws Exception {
		try (InputStream in = JsonSchemaValidationManagerImplTest.class.getClassLoader().getResourceAsStream(name);) {
			if (in == null) {
				throw new IllegalArgumentException("Cannot find: '" + name + "' on the classpath");
			}
			return IOUtils.toString(in, "UTF-8");
		}
	}

}
