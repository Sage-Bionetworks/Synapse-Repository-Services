package org.sagebionetworks.repo.manager.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.everit.json.schema.Schema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

@ExtendWith(MockitoExtension.class)
public class JsonSchemaValidatorFactoryTest {
	
	private JsonSchemaValidatorFactory factory;
	private JsonSchema jsonSchema;

	@BeforeEach
	public void before() {
		factory = new JsonSchemaValidatorFactory();
		jsonSchema = new JsonSchema();
		jsonSchema.setType(Type.object);
		jsonSchema.set$id("org.example-MySchema-1.0.0");
		jsonSchema.setDescription("Test schema");
	}

	@Test
	public void testBuildValidatorWithValidSchema() throws JSONObjectAdapterException {
		jsonSchema.set_default("defaultValue");
		// call under test
		Schema validator = factory.buildValidator(jsonSchema, false);
		
		assertFalse(validator.hasDefaultValue());
	}

	@Test
	public void testBuildValidatorWithUseDefaultsTrue() throws JSONObjectAdapterException {
		jsonSchema.set_default("defaultValue");
		
		// call under test
		Schema validator = factory.buildValidator(jsonSchema, true);
		
		assertEquals("defaultValue", validator.getDefaultValue());

	}

	@Test
	public void testBuildValidatorWithNullSchema() {
		jsonSchema = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			factory.buildValidator(jsonSchema, false);
		}).getMessage();
		
		assertEquals("jsonSchema is required.", message);
	}

	@Test
	public void testBuildValidatorWithMissingSchemaProperty() throws JSONObjectAdapterException {
		// When $schema is not provided, it should default to draft-07
		jsonSchema.set$schema(null);
		
		// call under test
		Schema validator = factory.buildValidator(jsonSchema, false);
		
		assertFalse(validator.hasDefaultValue());
		assertEquals(JsonSchemaValidatorFactory.DRAFT_07, jsonSchema.get$schema());
	}

	@Test
	public void testBuildValidatorWithBlankSchemaProperty() throws JSONObjectAdapterException {
		// When $schema is blank, it should default to draft-07
		jsonSchema.set$schema("");
		
		// call under test
		Schema validator = factory.buildValidator(jsonSchema, false);
		
		assertFalse(validator.hasDefaultValue());
		assertEquals(JsonSchemaValidatorFactory.DRAFT_07, jsonSchema.get$schema());
	}

	@Test
	public void testBuildValidatorWithExplicitDraft07() throws JSONObjectAdapterException {
		// Explicitly set draft-07
		jsonSchema.set$schema("http://json-schema.org/draft-07/schema#");
		
		// call under test
		Schema validator = factory.buildValidator(jsonSchema, false);
		
		assertFalse(validator.hasDefaultValue());
		assertEquals("http://json-schema.org/draft-07/schema#", jsonSchema.get$schema());
	}
	
	@Test
	public void testBuildValidatorWithUnsupportedSchemaVersion() throws JSONObjectAdapterException {
		jsonSchema.set$schema("https://json-schema.org/draft/2019-09/schema");
		
		assertEquals("Unsupported JSON schema version: https://json-schema.org/draft/2019-09/schema", assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			factory.buildValidator(jsonSchema, false);	
		}).getMessage());
	}
	
	@Test
	public void testBuildValidatorWithInvalidRefUrl() throws JSONObjectAdapterException {
		jsonSchema.set$schema("http://json-schema.org/draft-07/schema#");
		jsonSchema.setAllOf(List.of(new JsonSchema().set$ref("invalid-ref")));
		
		assertEquals("Invalid JSON schema: java.net.MalformedURLException: no protocol: invalid-ref", assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			factory.buildValidator(jsonSchema, false);	
		}).getMessage());
	}
	
	@Test
	public void testBuildValidatorWithInvalidSchema() throws JSONObjectAdapterException {
		jsonSchema.set$schema("http://json-schema.org/draft-07/schema#");
		jsonSchema.setAllOf(List.of(new JsonSchema().set$ref("#/definitions/invalid-ref")));
		
		assertEquals("Invalid JSON schema: #: key [definitions] not found", assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			factory.buildValidator(jsonSchema, false);	
		}).getMessage());
	}
}
