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
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ObjectType;
import org.sagebionetworks.repo.model.schema.Type;
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
		JsonSubject subject = Mockito.mock(JsonSubject.class);
		assertThrows(IllegalArgumentException.class, () -> {
			manager.validate(schema, subject);
		});
	}

	@Test
	public void testValidationWithNullSubject() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/Enum.json");
		JsonSubject subject = null;
		assertThrows(IllegalArgumentException.class, () -> {
			manager.validate(schema, subject);
		});
	}

	/**
	 * Expect validation to ignore the 'source' attribute.
	 * 
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
		assertEquals(Lists.newArrayList("requireMe", "requireMeToo"), schema.getRequired());

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
		assertEquals(Lists.newArrayList("requireMe", "requireMeToo"), schema.getRequired());

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
		assertEquals(Lists.newArrayList("requireMe", "requireMeToo"), schema.getRequired());

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
	 * 
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

	@Test
	public void testValidationWithPattern() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/HasPattern.json");
		assertNotNull(schema.getProperties());
		assertEquals(1, schema.getProperties().size());
		JsonSchema subSchema = schema.getProperties().get("somePattern");
		assertNotNull(subSchema);
		assertEquals(Type.string, subSchema.getType());
		assertEquals("^(\\([0-9]{3}\\))?[0-9]{3}-[0-9]{4}$", subSchema.getPattern());

		JsonSubject subject = setupSubject();
		subject.toJson().put("somePattern", "(888)555-1212");

		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertTrue(result.getIsValid());

		// should not be valid against the pattern
		subject.toJson().put("somePattern", "(800)FLOWERS");
		// call under test
		result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertEquals("string [(800)FLOWERS] does not match pattern ^(\\([0-9]{3}\\))?[0-9]{3}-[0-9]{4}$",
				result.getValidationErrorMessage());
	}

	@Test
	public void testValidationWithMaxLength() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/HasMaxLength.json");
		assertNotNull(schema.getProperties());
		assertEquals(1, schema.getProperties().size());
		JsonSchema subSchema = schema.getProperties().get("someBoundString");
		assertNotNull(subSchema);
		assertEquals(Type.string, subSchema.getType());
		assertEquals(10L, subSchema.getMaxLength());

		JsonSubject subject = setupSubject();
		subject.toJson().put("someBoundString", "1234567890");

		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertTrue(result.getIsValid());

		// more then ten chars
		subject.toJson().put("someBoundString", "12345678901");
		// call under test
		result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertEquals("expected maxLength: 10, actual: 11", result.getValidationErrorMessage());
	}

	@Test
	public void testValidationWithMinLength() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/HasMinLength.json");
		assertNotNull(schema.getProperties());
		assertEquals(1, schema.getProperties().size());
		JsonSchema subSchema = schema.getProperties().get("someBoundString");
		assertNotNull(subSchema);
		assertEquals(Type.string, subSchema.getType());
		assertEquals(2L, subSchema.getMinLength());

		JsonSubject subject = setupSubject();
		subject.toJson().put("someBoundString", "12");

		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertTrue(result.getIsValid());

		// more less than two chars
		subject.toJson().put("someBoundString", "1");
		// call under test
		result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertEquals("expected minLength: 2, actual: 1", result.getValidationErrorMessage());
	}

	@Test
	public void testIfThenElse() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/IfThenElse.json");
		
		// this case hits the else
		JsonSubject subject = setupSubject();
		subject.toJson().put("skyColor", "red");
		subject.toJson().put("timeOfDay", "night");
		subject.toJson().put("sailors", "delight");
		
		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertTrue(result.getIsValid());
		
		// this case hits the then
		subject = setupSubject();
		subject.toJson().put("skyColor", "red");
		subject.toJson().put("timeOfDay", "morning");
		subject.toJson().put("sailors", "warning");
		
		// call under test
		result = manager.validate(schema, subject);
		assertNotNull(result);
		assertTrue(result.getIsValid());
		
		// this case is invalid
		subject = setupSubject();
		subject.toJson().put("skyColor", "red");
		subject.toJson().put("timeOfDay", "morning");
		subject.toJson().put("sailors", "delight");
		
		// call under test
		result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
	}
	
	/**
	 * This test uses a schema containing if/then/else which was added to JSON schema
	 * in draft-07.  The validation library we are using silently ignores all features
	 * added after draft-04 when the provided JSON schema has a null value for the
	 * $schema field.  This causes unexpected validation results. 
	 * @throws Exception
	 */
	@Test
	public void testNull$Schema() throws Exception {
		// This schema uses if/then/else which was added in JSON schema draft-07.
		JsonSchema schema = loadSchemaFromClasspath("schemas/IfThenElse.json");
		// when the $schema is null we want to default to draft-07
		schema.set$schema(null);
		
		// Draft-07 is required to detect that this case is invalid. 
		JsonSubject subject = setupSubject();
		subject.toJson().put("skyColor", "red");
		subject.toJson().put("timeOfDay", "morning");
		subject.toJson().put("sailors", "delight");
		
		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
	}
	
	@Test
	public void testEmpty$Schema() throws Exception {
		// This schema uses if/then/else which was added in JSON schema draft-07.
		JsonSchema schema = loadSchemaFromClasspath("schemas/IfThenElse.json");
		// when the $schema is empty we want to default to draft-07
		schema.set$schema("");
		
		// Draft-07 is required to detect that this case is invalid. 
		JsonSubject subject = setupSubject();
		subject.toJson().put("skyColor", "red");
		subject.toJson().put("timeOfDay", "morning");
		subject.toJson().put("sailors", "delight");
		
		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
	}


	/**
	 * Test added for JSON schema conditional logic (if, then, else) PLFM-6315. This
	 * test is derived from from
	 * https://json-schema.org/understanding-json-schema/reference/conditionals.html
	 * 
	 * @throws Exception
	 */
	@Test
	public void testHasConditional() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/HasConditional.json");
		assertNotNull(schema.getProperties());
		assertEquals(2, schema.getProperties().size());
		List<JsonSchema> allOf = schema.getAllOf();
		assertNotNull(allOf);
		assertEquals(3, allOf.size());
		JsonSchema usDef = allOf.get(0);
		assertNotNull(usDef);
		JsonSchema _if = usDef.get_if();
		assertNotNull(_if);
		JsonSchema _then = usDef.getThen();
		assertNotNull(_then);
		JsonSchema usPostalCode = _then.getProperties().get("postal_code");
		assertEquals("[0-9]{5}(-[0-9]{4})?", usPostalCode.getPattern());

		// US
		JsonSubject subject = setupSubject();
		subject.toJson().put("street_address", "1600 Pennsylvania Avenue NW");
		subject.toJson().put("country", "United States of America");
		subject.toJson().put("postal_code", "20500");

		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertTrue(result.getIsValid());

		// Canadian
		subject = setupSubject();
		subject.toJson().put("street_address", "24 Sussex Drive");
		subject.toJson().put("country", "Canada");
		subject.toJson().put("postal_code", "K1M 1M4");

		// call under test
		result = manager.validate(schema, subject);
		assertNotNull(result);
		assertTrue(result.getIsValid());

		// Netherlands
		subject = setupSubject();
		subject.toJson().put("street_address", "Adriaan Goekooplaan");
		subject.toJson().put("country", "Netherlands");
		subject.toJson().put("postal_code", "2517 JX");

		// call under test
		result = manager.validate(schema, subject);
		assertNotNull(result);
		assertTrue(result.getIsValid());

		// Invalid combination
		subject = setupSubject();
		subject.toJson().put("street_address", "24 Sussex Drive");
		subject.toJson().put("country", "Canada");
		subject.toJson().put("postal_code", "10000");

		// call under test
		result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertEquals("#: only 1 subschema matches out of 2", result.getValidationErrorMessage());
	}
	
	@Test
	public void testHasMinAndMax() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/HasMinMax.json");
		assertNotNull(schema.getProperties());
		assertEquals(3, schema.getProperties().size());

		// All valid
		JsonSubject subject = setupSubject();
		subject.toJson().put("lessThanOrEqualsToTen", 9);
		subject.toJson().put("greaterThanOrEqualToTwenty", 21);
		subject.toJson().put("betweenThirtyAndForty", 34);

		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertTrue(result.getIsValid());

		// over max
		subject = setupSubject();
		subject.toJson().put("lessThanOrEqualsToTen", 11);

		// call under test
		result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertEquals("11 is not less or equal to 10", result.getValidationErrorMessage());
		
		// under min
		subject = setupSubject();
		subject.toJson().put("greaterThanOrEqualToTwenty", 19);

		// call under test
		result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertEquals("19 is not greater or equal to 20", result.getValidationErrorMessage());
		
		// over forty
		subject = setupSubject();
		subject.toJson().put("betweenThirtyAndForty", 41);

		// call under test
		result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertEquals("41 is not less or equal to 40", result.getValidationErrorMessage());
		
		// under thirty
		subject = setupSubject();
		subject.toJson().put("betweenThirtyAndForty", 29);

		// call under test
		result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertEquals("29 is not greater or equal to 30", result.getValidationErrorMessage());
	}
	
	@Test
	public void testHasNot() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/HasNot.json");
		assertNotNull(schema.getProperties());
		assertEquals(2, schema.getProperties().size());
		
		// red is primary
		JsonSubject subject = setupSubject();
		subject.toJson().put("color", "red");
		subject.toJson().put("isPrimary", "true");

		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertTrue(result.getIsValid());
		
		// red is primary
		subject = setupSubject();
		subject.toJson().put("color", "red");
		subject.toJson().put("isPrimary", "false");

		// call under test
		result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertEquals("#: only 1 subschema matches out of 2", result.getValidationErrorMessage());
		
		// orange is not primary
		subject = setupSubject();
		subject.toJson().put("color", "orange");
		subject.toJson().put("isPrimary", "true");

		// call under test
		result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertEquals("#: only 1 subschema matches out of 2", result.getValidationErrorMessage());
		
		// orange is not primary
		subject = setupSubject();
		subject.toJson().put("color", "orange");
		subject.toJson().put("isPrimary", "false");

		// call under test
		result = manager.validate(schema, subject);
		assertNotNull(result);
		assertTrue(result.getIsValid());
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
