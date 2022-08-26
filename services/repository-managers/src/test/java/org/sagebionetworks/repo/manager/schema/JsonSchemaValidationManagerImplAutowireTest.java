package org.sagebionetworks.repo.manager.schema;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.schema.SchemaTestUtils.loadSchemaFromClasspath;

import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.ObjectType;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.schema.ValidationException;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class JsonSchemaValidationManagerImplAutowireTest {
	
	@Autowired
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
	 * This test uses a schema containing if/then/else which was added to JSON
	 * schema in draft-07. The validation library we are using silently ignores all
	 * features added after draft-04 when the provided JSON schema has a null value
	 * for the $schema field. This causes unexpected validation results.
	 * 
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

	/**
	 * This is a test for PLFM-6701.
	 */
	@Test
	public void testValidateWithBooleanCondition() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/BooleanCondition.json");
		assertNotNull(schema.getProperties());

		JsonSubject subject = setupSubject();
		// when isMultiSpecimen=true then assay is required.
		subject.toJson().put("isMultiSpecimen", true);
		subject.toJson().remove("assay");

		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
		String stackTrace = buildStackTrack(result.getValidationException());
		assertTrue(stackTrace.contains("input is invalid against the \"then\" schema"));
		assertTrue(stackTrace.contains("required key [assay] not found"));
	}

	/**
	 * This is a test for PLFM-6701.
	 */
	@Test
	public void testValidateWithBooleanConditionWithFalse() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/BooleanCondition.json");
		assertNotNull(schema.getProperties());

		JsonSubject subject = setupSubject();
		// when isMultiSpecimen=false then assay is not required.
		subject.toJson().put("isMultiSpecimen", false);
		subject.toJson().remove("assay");

		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertTrue(result.getIsValid());
	}

	/**
	 * This is a test for PLFM-6701.
	 */
	@Test
	public void testValidateWithEnumCondition() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/EnumCondition.json");
		assertNotNull(schema.getProperties());

		JsonSubject subject = setupSubject();
		// when other=1,2,or3 then assay is required.
		subject.toJson().put("other", 3);
		subject.toJson().remove("assay");

		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
		String stackTrace = buildStackTrack(result.getValidationException());
		assertTrue(stackTrace.contains("input is invalid against the \"then\" schema"));
		assertTrue(stackTrace.contains("required key [assay] not found"));
	}

	@Test
	public void testValidateWithEnumConditionWithNoMatch() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/EnumCondition.json");
		assertNotNull(schema.getProperties());

		JsonSubject subject = setupSubject();
		// when other=1,2,or3 then assay is required.
		subject.toJson().put("other", 4);
		subject.toJson().remove("assay");

		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertTrue(result.getIsValid());
	}

	@Test
	public void testCalculateDerivedAnnotationsWithUnconditionalDefaults() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/DerivedUnconditionalDefault.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "defaultString", "foo", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(expected, "defaultLong", "123456", AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(expected, "defaultDouble", "0.0123456", AnnotationsValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(expected, "defaultTimestamp", "222", AnnotationsValueType.TIMESTAMP_MS);
		AnnotationsV2TestUtils.putAnnotations(expected, "defaultBoolean", "true", AnnotationsValueType.BOOLEAN);
		AnnotationsV2TestUtils.putAnnotations(expected, "someConst", "123", AnnotationsValueType.LONG);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithUnconditionalDefaultsAndExistingKey() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/DerivedUnconditionalDefault.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		// since these values exist in the subject, they should not be derived.
		subject.put("defaultString", "bar");
		subject.put("defaultDouble", 1.9);

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "defaultLong", "123456", AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(expected, "defaultTimestamp", "222", AnnotationsValueType.TIMESTAMP_MS);
		AnnotationsV2TestUtils.putAnnotations(expected, "defaultBoolean", "true", AnnotationsValueType.BOOLEAN);
		AnnotationsV2TestUtils.putAnnotations(expected, "someConst", "123", AnnotationsValueType.LONG);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithInvalidSubject() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/DerivedUnconditionalDefault.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		// since the value is not a string this subject will be invalid against the schema.
		subject.put("defaultString", 123);

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		assertEquals(Optional.empty(), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithConditionalDefaults() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/DerivedConditionalDefault.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", false);
		subject.put("secondBoolean", true);

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "someUnconditionalDefault", "456", AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(expected, "someConditional", "someBoolean was false", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(expected, "secondConditional", "secondBoolean was true", AnnotationsValueType.STRING);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithConditionalDefaultsFlipped() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/DerivedConditionalDefault.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", true);
		subject.put("secondBoolean", false);

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "someUnconditionalDefault", "456", AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(expected, "someConditional", "someBoolean was true", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(expected, "secondConditional", "secondBoolean was false", AnnotationsValueType.STRING);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithUnconditionalConst() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/DerivedUnconditionalConst.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		System.out.println(subject.toString(5));
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "constString", "foo", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(expected, "constLong", "123456", AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(expected, "constDouble", "0.0123456", AnnotationsValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(expected, "constTimestamp", "222", AnnotationsValueType.TIMESTAMP_MS);
		AnnotationsV2TestUtils.putAnnotations(expected, "constBoolean", "true", AnnotationsValueType.BOOLEAN);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithUnconditionalConstAndExistingKey() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/DerivedUnconditionalConst.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		// since these values exist in the subject, they should not be derived.
		subject.put("constString", "foo");
		subject.put("constLong", 123456L);

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "constDouble", "0.0123456", AnnotationsValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(expected, "constTimestamp", "222", AnnotationsValueType.TIMESTAMP_MS);
		AnnotationsV2TestUtils.putAnnotations(expected, "constBoolean", "true", AnnotationsValueType.BOOLEAN);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithConditionalConst() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/DerivedConditionalConst.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", false);
		subject.put("secondBoolean", true);
		subject.put("someUnconditionalConst", 456L);

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "someConditional", "someBoolean was false", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(expected, "secondConditional", "secondBoolean was true", AnnotationsValueType.STRING);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithConditionalConstSwitched() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/DerivedConditionalConst.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", true);
		subject.put("secondBoolean", false);
		subject.put("someUnconditionalConst", 456L);

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "someConditional", "someBoolean was true", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(expected, "conditionalLong", "999", AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(expected, "secondConditional", "secondBoolean was false", AnnotationsValueType.STRING);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithConditionalConstNoProperty() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/DerivedConditionalConst.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("anotherProperty", "some value");

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "someUnconditionalConst", "456", AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(expected, "someConditional", "someBoolean was true", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(expected, "conditionalLong", "999", AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(expected, "secondConditional", "secondBoolean was false", AnnotationsValueType.STRING);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithCondionalAndNoThen() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/DerivedConditionalNoThen.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", true);

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		assertEquals(Optional.empty(), annos);
		
		subject = new JSONObject();
		subject.put("someBoolean", false);

		// call under test
		annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "someConditional", "someBoolean was false", AnnotationsValueType.STRING);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithCondionalAndNoElse() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/DerivedConditionalNoElse.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", false);

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		assertEquals(Optional.empty(), annos);
		
		subject = new JSONObject();
		subject.put("someBoolean", true);

		// call under test
		annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "someConditional", "someBoolean was true", AnnotationsValueType.STRING);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithUnconditionalReferences() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/DerivedUnconditionalReferences.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("refToString", "I am a string");
		
		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "refToConstString", "foo", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(expected, "anotherRefToConstString", "foo", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(expected, "refToDefaultLong", "123456", AnnotationsValueType.LONG);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithUnconditionalRefOfRef() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/DerivedUnconditionalRefOfRef.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		
		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "refOfRef", "foo", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(expected, "ref", "foo", AnnotationsValueType.STRING);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithConditionalReferences() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/DerivedConditionalReference.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", true);
		
		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "someConditional", "someBoolean was true", AnnotationsValueType.STRING);
		assertEquals(Optional.of(expected), annos);
		
		subject = new JSONObject();
		subject.put("someBoolean", false);
		
		// call under test
		annos = manager.calculateDerivedAnnotations(schema, subject);
		expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "someConditional", "someBoolean was false", AnnotationsValueType.STRING);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithUnconditionalSingleContains() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/ContainsSingle.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", true);
		
		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "fruit", List.of("apple"), AnnotationsValueType.STRING);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithUnconditionalMultipleContains() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/ContainsMultiple.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", true);
		
		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "integers", List.of("123","456","789"), AnnotationsValueType.LONG);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithUnconditionalMultipleContainsMixedType() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/ContainsMultipleMixedTypes.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", true);

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		// other types are ignored.
		AnnotationsV2TestUtils.putAnnotations(expected, "integers", List.of("123", "not a number", "true", "456"),
				AnnotationsValueType.STRING);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithUnconditionalMultipleContainsDuplicates() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/ContainsMultipleDuplicates.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", true);
		
		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		// other types are ignored.
		AnnotationsV2TestUtils.putAnnotations(expected, "integers", List.of("123","456"), AnnotationsValueType.LONG);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithContainsMultipleConditionalMatchThen() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/ContainsMultipleConditional.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", true);

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "_accessRequirementIds", List.of("456", "111", "222"),
				AnnotationsValueType.LONG);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithContainsMultipleConditionalMatchElse() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/ContainsMultipleConditional.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", false);

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "_accessRequirementIds", List.of("456", "333", "444"),
				AnnotationsValueType.LONG);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithContainsMultipleConditionalReferencesMatchThen() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/ContainsMultipleConditionalReferences.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", true);

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "_accessRequirementIds", List.of("456", "111", "222"),
				AnnotationsValueType.LONG);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithContainsMultipleConditionalReferencesMatchElse() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/ContainsMultipleConditionalReferences.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", false);

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "_accessRequirementIds", List.of("456", "333", "444"),
				AnnotationsValueType.LONG);
		assertEquals(Optional.of(expected), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithContainsConditionalNoMatch() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/ContainsConditional.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		assertEquals(Optional.empty(), annos);
	}
	
	@Test
	public void testCalculateDerivedAnnotationsWithContainsConditionalMatch() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/ContainsConditional.json");
		assertNotNull(schema.getProperties());

		JSONObject subject = new JSONObject();
		subject.put("someBoolean", true);

		// call under test
		Optional<Annotations> annos = manager.calculateDerivedAnnotations(schema, subject);
		Annotations expected = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(expected, "_accessRequirementIds", List.of("111", "222"),
				AnnotationsValueType.LONG);
		assertEquals(Optional.of(expected), annos);
	}

	
	@Test
	public void testContainsSingleConst() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/ContainsSingle.json");
		assertNotNull(schema.getProperties());
		
		JsonSubject subject = setupSubject();
		subject.toJson().put("fruit", new JSONArray(List.of("peach","apple","pear")));
		
		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertTrue(result.getIsValid());
	}
	
	@Test
	public void testContainsSingleConstInvalid() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/ContainsSingle.json");
		assertNotNull(schema.getProperties());
		
		JsonSubject subject = setupSubject();
		subject.toJson().put("fruit", new JSONArray(List.of("peach","apples","pear")));
		
		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertEquals("expected at least one array item to match 'contains' schema", result.getValidationErrorMessage());
		assertEquals(List.of("#/fruit: expected at least one array item to match 'contains' schema"), result.getAllValidationMessages());
	}

	@Test
	public void testContainsMultipleConst() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/ContainsMultiple.json");
		assertNotNull(schema.getProperties());

		JsonSubject subject = setupSubject();
		subject.toJson().put("integers", new JSONArray(List.of(111L, 456L, 789L, 123L, 222L)));

		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertTrue(result.getIsValid());
	}
	
	@Test
	public void testContainsMultipleConstInvalid() throws Exception {
		JsonSchema schema = loadSchemaFromClasspath("schemas/ContainsMultiple.json");
		assertNotNull(schema.getProperties());

		JsonSubject subject = setupSubject();
		subject.toJson().put("integers", new JSONArray(List.of(111L, 456L, 789L, 1234L, 222L)));

		// call under test
		ValidationResults result = manager.validate(schema, subject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertEquals("#: only 1 subschema matches out of 2", result.getValidationErrorMessage());
		assertEquals(List.of("#/integers: expected at least one array item to match 'contains' schema"), result.getAllValidationMessages());

	}

	/**
	 * Helper to build a stack trace for the given exception.
	 * 
	 * @param validationException
	 * @return
	 */
	public String buildStackTrack(ValidationException validationException) {
		StringBuilder builder = new StringBuilder();
		buildStackTrackRecursive(builder, validationException);
		return builder.toString();
	}

	/**
	 * Recursive method to build a stack trace from a ValidationException
	 * 
	 * @param builder
	 * @param validationException
	 */
	void buildStackTrackRecursive(StringBuilder builder, ValidationException validationException) {
		builder.append(validationException.getMessage()).append("\n");
		if (validationException.getCausingExceptions() != null) {
			for (ValidationException e : validationException.getCausingExceptions()) {
				buildStackTrackRecursive(builder, e);
			}
		}
	}
	
	public JSONObject setupJsonObject() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("objectId", objectId);
		jsonObject.put("objectType", objectType);
		jsonObject.put("objectEtag", objectEtag);
		return jsonObject;
	}

	public JsonSubject setupSubject() {
		JSONObject jsonObject = setupJsonObject();

		JsonSubject subject = Mockito.mock(JsonSubject.class);
		when(subject.getObjectId()).thenReturn(objectId);
		when(subject.getObjectEtag()).thenReturn(objectEtag);
		when(subject.getObjectType()).thenReturn(objectType);
		when(subject.toJson()).thenReturn(jsonObject);

		return subject;
	}

}
