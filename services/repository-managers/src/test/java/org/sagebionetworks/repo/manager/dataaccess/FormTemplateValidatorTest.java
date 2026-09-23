package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateField;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateStep;
import org.sagebionetworks.repo.model.dataaccess.schema.SubmissionContext;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

@ExtendWith(MockitoExtension.class)
public class FormTemplateValidatorTest {

	private static final String SCHEMA_ID = "org.sagebionetworks.test-DataAccessRequest-1.0.0";
	private static final String INSTITUTION_DEFINITION = "org.sagebionetworks.test-Institution";

	@Mock
	private JsonSchemaManager mockJsonSchemaManager;

	@InjectMocks
	private FormTemplateValidator validator;

	private JsonSchema schema;

	@BeforeEach
	public void before() {
		schema = newSchema();
	}

	@Test
	public void testValidate() {
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(schema);
		FormTemplate template = newTemplate(field("/projectLead"), field("/institution/name"),
				field("/irbApproval").setTemplateFileHandleId("987"));

		// call under test
		validator.validate(template);
	}

	@Test
	public void testValidateWithFileWidget() {
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(schema);
		FormTemplate template = newTemplate(field("/projectLead"),
				field("/irbApproval").setUiDefinition(uiDefinition("{\"ui:widget\":\"file\"}")));

		// call under test
		validator.validate(template);
	}

	@Test
	public void testValidateWithNullTemplate() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(null);
		}).getMessage();

		assertEquals("template is required.", message);
		verify(mockJsonSchemaManager, never()).getValidationSchema(any());
	}

	@Test
	public void testValidateWithBlankName() {
		FormTemplate template = newTemplate(field("/projectLead")).setName(" ");

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("template.name is required and must not be a blank string.", message);
		verify(mockJsonSchemaManager, never()).getValidationSchema(any());
	}

	@Test
	public void testValidateWithNoSteps() {
		FormTemplate template = newTemplate(field("/projectLead")).setSteps(List.of());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("template.steps is required and must not be empty.", message);
		verify(mockJsonSchemaManager, never()).getValidationSchema(any());
	}

	@Test
	public void testValidateWithNoFields() {
		FormTemplate template = newTemplate(field("/projectLead"));
		template.getSteps().get(0).setFields(List.of());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("steps[0].fields is required and must not be empty.", message);
	}

	@Test
	public void testValidateWithSchemaIdWithoutVersion() {
		FormTemplate template = newTemplate(field("/projectLead"))
				.setSchema$id("org.sagebionetworks.test-DataAccessRequest");

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("The schema$id 'org.sagebionetworks.test-DataAccessRequest' must include the semantic version"
				+ " of the schema, for example 'org.example-Schema-1.0.2'.", message);
		verify(mockJsonSchemaManager, never()).getValidationSchema(any());
	}

	@Test
	public void testValidateWithUnknownSchema() {
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenThrow(new NotFoundException("not found"));
		FormTemplate template = newTemplate(field("/projectLead"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("The schema '" + SCHEMA_ID + "' does not exist.", message);
	}

	@Test
	public void testValidateWithUnknownPath() {
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(schema);
		FormTemplate template = newTemplate(field("/projectLead"), field("/notAProperty"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("The form template is invalid: The path '/notAProperty' does not resolve to a property of"
				+ " the schema.", message);
	}

	@Test
	public void testValidateWithPathThatIsNotARelativePointer() {
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(schema);
		FormTemplate template = newTemplate(field("/projectLead"), field("projectLead"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("The form template is invalid: The path 'projectLead' does not resolve to a property of"
				+ " the schema.", message);
	}

	@Test
	public void testValidateWithPointerDelimitersInPropertyNames() {
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(newSchemaWithDelimitedPropertyNames());
		FormTemplate template = newTemplate(field("/a~1b"), field("/~01"));

		// call under test
		validator.validate(template);
	}

	@Test
	public void testValidateWithUnescapedPointerDelimiterInPath() {
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(newSchemaWithDelimitedPropertyNames());
		// A raw '/' reads as a step down into a nested property rather than as part of the name.
		FormTemplate template = newTemplate(field("/a/b"), field("/~01"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("The form template is invalid: The path '/a/b' does not resolve to a property of the schema."
				+ " No field covers the required property '/a~1b'.", message);
	}

	@Test
	public void testValidateWithPathToAnObject() {
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(schema);
		FormTemplate template = newTemplate(field("/projectLead"), field("/institution"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("The form template is invalid: The path '/institution' resolves to an object rather than a"
				+ " single value.", message);
	}

	@Test
	public void testValidateWithDuplicatePath() {
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(schema);
		FormTemplate template = newTemplate(field("/projectLead"), field("/projectLead"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("The form template is invalid: More than one field targets the property '/projectLead'.",
				message);
	}

	@Test
	public void testValidateWithUncoveredRequiredProperty() {
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(schema);
		FormTemplate template = newTemplate(field("/irbApproval").setTemplateFileHandleId("987"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("The form template is invalid: No field covers the required property '/projectLead'.", message);
	}

	@Test
	public void testValidateWithRequiredPropertyOfAReferencedSchema() {
		// The requirement is declared by a schema that the root composes rather than by the root itself.
		schema.setRequired(null).setAllOf(List.of(new JsonSchema().set$ref("#/definitions/" + INSTITUTION_DEFINITION)));
		schema.getDefinitions().get(INSTITUTION_DEFINITION).setRequired(List.of("projectLead"));
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(schema);
		FormTemplate template = newTemplate(field("/irbApproval").setTemplateFileHandleId("987"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("The form template is invalid: No field covers the required property '/projectLead'.", message);
	}

	@Test
	public void testValidateWithFileHandleIdOnANonFileProperty() {
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(schema);
		FormTemplate template = newTemplate(field("/projectLead").setTemplateFileHandleId("987"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("The form template is invalid: The field '/projectLead' uploads a file, so the property it"
				+ " targets must be a number with the format 'synapse-filehandle-id'.", message);
	}

	@Test
	public void testValidateWithFileWidgetOnANonFileProperty() {
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(schema);
		FormTemplate template = newTemplate(
				field("/projectLead").setUiDefinition(uiDefinition("{\"ui:widget\":\"file\"}")));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("The form template is invalid: The field '/projectLead' uploads a file, so the property it"
				+ " targets must be a number with the format 'synapse-filehandle-id'.", message);
	}

	@Test
	public void testValidateWithNonFileWidgetOnAFileProperty() {
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(schema);
		// A file handle id may also be supplied by something other than an upload widget.
		FormTemplate template = newTemplate(field("/projectLead"),
				field("/irbApproval").setUiDefinition(uiDefinition("{\"ui:widget\":\"updown\"}")));

		// call under test
		validator.validate(template);
	}

	@Test
	public void testValidateWithUiDefinitionThatIsNotAnObject() {
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(schema);
		// A uiDefinition sent as a JSON scalar is deserialized as a String rather than an adapter.
		FormTemplate template = newTemplate(field("/projectLead").setUiDefinition("ui:autofocus"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("The form template is invalid: The uiDefinition of the field '/projectLead' must be a JSON"
				+ " object.", message);
	}

	@Test
	public void testValidateWithMultipleProblems() {
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(schema);
		FormTemplate template = newTemplate(field("/notAProperty"), field("/institution"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(template);
		}).getMessage();

		assertEquals("The form template is invalid: The path '/notAProperty' does not resolve to a property of the"
				+ " schema. The path '/institution' resolves to an object rather than a single value."
				+ " No field covers the required property '/projectLead'.", message);
	}

	/**
	 * A validation schema with an inlined definition, mirroring what
	 * {@link JsonSchemaManager#getValidationSchema(String)} returns.
	 */
	private static JsonSchema newSchema() {
		Map<String, JsonSchema> properties = new LinkedHashMap<>();
		properties.put("projectLead", new JsonSchema().setType(Type.string));
		properties.put("irbApproval",
				new JsonSchema().setType(Type.integer).setFormat(FormTemplateValidator.FILE_HANDLE_FORMAT));
		properties.put("institution", new JsonSchema().set$ref("#/definitions/" + INSTITUTION_DEFINITION));

		JsonSchema institution = new JsonSchema().setType(Type.object)
				.setProperties(Map.of("name", new JsonSchema().setType(Type.string)));

		return new JsonSchema().set$id(SCHEMA_ID).setType(Type.object).setProperties(properties)
				.setRequired(List.of("projectLead"))
				.setDefinitions(new LinkedHashMap<>(Map.of(INSTITUTION_DEFINITION, institution)));
	}

	/**
	 * A schema whose required property names contain the RFC 6901 delimiters, so a field must address
	 * 'a/b' as '/a~1b' and '~1' as '/~01'. The name '~1' is the case that pins the order the escapes
	 * are applied in, since unescaping '~0' ahead of '~1' would reduce its token to a '/'.
	 */
	private static JsonSchema newSchemaWithDelimitedPropertyNames() {
		Map<String, JsonSchema> properties = new LinkedHashMap<>();
		properties.put("a/b", new JsonSchema().setType(Type.string));
		properties.put("~1", new JsonSchema().setType(Type.string));

		return new JsonSchema().set$id(SCHEMA_ID).setType(Type.object).setProperties(properties)
				.setRequired(List.of("a/b", "~1"));
	}

	private static FormTemplate newTemplate(FormTemplateField... fields) {
		return new FormTemplate().setName("NF Standard DAR").setSchema$id(SCHEMA_ID)
				.setSteps(List.of(new FormTemplateStep().setTitle("Project").setFields(List.of(fields))));
	}

	private static FormTemplateField field(String schemaPath) {
		return new FormTemplateField().setSchemaPath(schemaPath).setSubmissionContext(SubmissionContext.ALWAYS)
				.setIsPublic(false).setUiDefinition(uiDefinition("{\"ui:autofocus\":true}"));
	}

	private static JSONObjectAdapter uiDefinition(String json) {
		try {
			return new JSONObjectAdapterImpl(json);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
