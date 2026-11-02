package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.everit.json.schema.EmptySchema;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaValidatorFactory;
import org.sagebionetworks.repo.model.JsonSchemaAccessRequirement;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.schema.DataAccessRequestType;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateReference;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaConstants;
import org.sagebionetworks.repo.web.AccessRequirementVersionOutOfDateException;
import org.sagebionetworks.repo.web.SubmissionContextMismatchException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * Covers what {@link SchemaDataValidator} adds around the JSON Schema validator. How the validator
 * itself reports problems is covered by the tests of {@link JsonSchemaValidatorFactory}.
 */
@ExtendWith(MockitoExtension.class)
public class SchemaDataValidatorTest {

	private static final String SCHEMA_ID = "org.example-TestDarSchema-1.0.0";
	private static final String TEMPLATE_ID = "123";
	private static final Long TEMPLATE_VERSION = 2L;
	private static final Long AR_VERSION = 3L;
	private static final String CONTEXT = JsonSchemaConstants.SUBMISSION_CONTEXT_PROPERTY;

	@Mock
	private JsonSchemaManager mockJsonSchemaManager;

	@Mock
	private FormTemplateManager mockFormTemplateManager;

	@Mock
	private JsonSchemaValidatorFactory mockValidatorFactory;

	@Mock
	private Schema mockSchemaValidator;

	@Captor
	private ArgumentCaptor<Object> subjectCaptor;

	private SchemaDataValidator validator;

	private JsonSchemaAccessRequirement accessRequirement;
	private FormTemplate template;
	private JsonSchema schema;

	@BeforeEach
	public void before() {
		validator = new SchemaDataValidator(mockJsonSchemaManager, mockFormTemplateManager, mockValidatorFactory);

		accessRequirement = new JsonSchemaAccessRequirement();
		accessRequirement.setId(99L);
		accessRequirement.setVersionNumber(AR_VERSION);
		accessRequirement.setFormTemplateRef(
				new FormTemplateReference().setTemplateId(TEMPLATE_ID).setTemplateVersionNumber(TEMPLATE_VERSION));

		template = new FormTemplate().setId(TEMPLATE_ID).setVersionNumber(TEMPLATE_VERSION).setSchema$id(SCHEMA_ID);

		schema = new JsonSchema().set$id(SCHEMA_ID);
	}

	@Test
	public void testValidateWithRequest() {
		setupSchemaLookup();
		RequestInterface request = newRequest(adapter("{\"projectLead\":\"Dr. Lead\",\"yearsOfAccess\":2}"));

		// call under test
		JSONObjectAdapter answers = validator.validate(accessRequirement, request);

		JSONObject expected = new JSONObject().put("projectLead", "Dr. Lead").put("yearsOfAccess", 2)
				.put(CONTEXT, DataAccessRequestType.REQUEST.name());
		assertValidated(expected);
		assertSimilar(expected, answers);
	}

	@Test
	public void testValidateWithRenewal() {
		setupSchemaLookup();
		RequestInterface renewal = newRenewal(adapter("{\"projectLead\":\"Dr. Lead\"}"));

		// call under test
		JSONObjectAdapter answers = validator.validate(accessRequirement, renewal);

		JSONObject expected = new JSONObject().put("projectLead", "Dr. Lead")
				.put(CONTEXT, DataAccessRequestType.RENEWAL.name());
		assertValidated(expected);
		assertSimilar(expected, answers);
	}

	@Test
	public void testValidateWithMatchingSubmissionContext() {
		setupSchemaLookup();
		RequestInterface request = newRequest(adapter(
				"{\"projectLead\":\"Dr. Lead\",\"" + CONTEXT + "\":\"" + DataAccessRequestType.REQUEST.name() + "\"}"));

		// call under test
		JSONObjectAdapter answers = validator.validate(accessRequirement, request);

		JSONObject expected = new JSONObject().put("projectLead", "Dr. Lead")
				.put(CONTEXT, DataAccessRequestType.REQUEST.name());
		assertValidated(expected);
		assertSimilar(expected, answers);
	}

	@Test
	public void testValidateWithRenewalAndMatchingSubmissionContext() {
		setupSchemaLookup();
		RequestInterface renewal = newRenewal(adapter(
				"{\"projectLead\":\"Dr. Lead\",\"" + CONTEXT + "\":\"" + DataAccessRequestType.RENEWAL.name() + "\"}"));

		// call under test
		JSONObjectAdapter answers = validator.validate(accessRequirement, renewal);

		JSONObject expected = new JSONObject().put("projectLead", "Dr. Lead")
				.put(CONTEXT, DataAccessRequestType.RENEWAL.name());
		assertValidated(expected);
		assertSimilar(expected, answers);
	}

	@Test
	public void testValidateWithRequestClaimingRenewalContext() {
		RequestInterface request = newRequest(adapter(
				"{\"projectLead\":\"Dr. Lead\",\"" + CONTEXT + "\":\"" + DataAccessRequestType.RENEWAL.name() + "\"}"));

		String message = assertThrows(SubmissionContextMismatchException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertEquals("This request is a REQUEST, so its '" + CONTEXT + "' cannot be 'RENEWAL'.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager, mockValidatorFactory);
	}

	@Test
	public void testValidateWithRenewalClaimingRequestContext() {
		RequestInterface renewal = newRenewal(adapter(
				"{\"projectLead\":\"Dr. Lead\",\"" + CONTEXT + "\":\"" + DataAccessRequestType.REQUEST.name() + "\"}"));

		String message = assertThrows(SubmissionContextMismatchException.class, () -> {
			// call under test
			validator.validate(accessRequirement, renewal);
		}).getMessage();

		assertEquals("This request is a RENEWAL, so its '" + CONTEXT + "' cannot be 'REQUEST'.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager, mockValidatorFactory);
	}

	@Test
	public void testValidateWithStaleAccessRequirementVersion() {
		RequestInterface request = newRequest(adapter("{\"projectLead\":\"Dr. Lead\"}"));
		request.setAccessRequirementVersionNumber(AR_VERSION - 1);

		String message = assertThrows(AccessRequirementVersionOutOfDateException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertEquals("This request was last saved against version 2 of the access requirement, which is now on"
				+ " version 3. Answer the current form and save the request before submitting it.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager, mockValidatorFactory);
	}

	@Test
	public void testValidateWithNullAccessRequirementVersion() {
		RequestInterface request = newRequest(adapter("{\"projectLead\":\"Dr. Lead\"}"));
		request.setAccessRequirementVersionNumber(null);

		String message = assertThrows(AccessRequirementVersionOutOfDateException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertEquals("This request was last saved against version null of the access requirement, which is now on"
				+ " version 3. Answer the current form and save the request before submitting it.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager, mockValidatorFactory);
	}

	@Test
	public void testValidateWithInvalidSchemaData() {
		setupSchemaLookup();
		RequestInterface request = newRequest(adapter("{\"yearsOfAccess\":\"two\"}"));
		doThrow(new ValidationException(EmptySchema.INSTANCE, "2 schema violations found",
				List.of(new ValidationException(EmptySchema.INSTANCE, "first problem"),
						new ValidationException(EmptySchema.INSTANCE, "second problem"))))
				.when(mockSchemaValidator).validate(any());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		// Every problem is reported, not only the first
		assertEquals("The request data is invalid: #: first problem #: second problem", message);
	}

	@Test
	public void testValidateWithNullSchemaData() {
		setupSchemaLookup();
		RequestInterface request = newRequest(null);

		// call under test
		JSONObjectAdapter answers = validator.validate(accessRequirement, request);

		JSONObject expected = new JSONObject().put(CONTEXT, DataAccessRequestType.REQUEST.name());
		assertValidated(expected);
		assertSimilar(expected, answers);
	}

	@Test
	public void testValidateWithSchemaDataNotAnAdapter() {
		RequestInterface request = newRequest(new JSONObject().put("projectLead", "Dr. Lead"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertEquals("schemaData must be a JSON object.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager, mockValidatorFactory);
	}

	@Test
	public void testValidateWithRequestLeavesSchemaDataUnchanged() {
		setupSchemaLookup();
		RequestInterface request = newRequest(adapter("{\"projectLead\":\"Dr. Lead\"}"));

		// call under test
		validator.validate(accessRequirement, request);

		// The context is supplied for the submission only, leaving the saved request as the
		// requester left it.
		assertSimilar(new JSONObject().put("projectLead", "Dr. Lead"), (JSONObjectAdapter) request.getSchemaData());
	}

	@Test
	public void testValidateWithNullFormTemplateRef() {
		accessRequirement.setFormTemplateRef(null);
		RequestInterface request = newRequest(adapter("{\"projectLead\":\"Dr. Lead\"}"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertEquals("accessRequirement.formTemplateRef is required.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager, mockValidatorFactory);
	}

	@Test
	public void testValidateWithNullAccessRequirement() {
		RequestInterface request = newRequest(adapter("{\"projectLead\":\"Dr. Lead\"}"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(null, request);
		}).getMessage();

		assertEquals("accessRequirement is required.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager, mockValidatorFactory);
	}

	@Test
	public void testValidateWithNullRequest() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(accessRequirement, null);
		}).getMessage();

		assertEquals("request is required.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager, mockValidatorFactory);
	}

	private void setupSchemaLookup() {
		when(mockFormTemplateManager.getVersion(TEMPLATE_ID, TEMPLATE_VERSION)).thenReturn(template);
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(schema);
		when(mockValidatorFactory.buildValidator(schema, false)).thenReturn(mockSchemaValidator);
	}

	private void assertValidated(JSONObject expected) {
		verify(mockSchemaValidator).validate(subjectCaptor.capture());
		JSONObject subject = (JSONObject) subjectCaptor.getValue();
		assertTrue(expected.similar(subject), () -> "expected " + expected + " but validated " + subject);
	}

	private static void assertSimilar(JSONObject expected, JSONObjectAdapter actual) {
		JSONObject actualJson = new JSONObject(actual.toJSONString());
		assertTrue(expected.similar(actualJson), () -> "expected " + expected + " but was " + actualJson);
	}

	private RequestInterface newRequest(Object schemaData) {
		return new Request().setAccessRequirementId(accessRequirement.getId().toString())
				.setAccessRequirementVersionNumber(AR_VERSION).setSchemaData(schemaData);
	}

	private RequestInterface newRenewal(JSONObjectAdapter schemaData) {
		Renewal renewal = new Renewal();
		renewal.setAccessRequirementId(accessRequirement.getId().toString());
		renewal.setAccessRequirementVersionNumber(AR_VERSION);
		renewal.setSchemaData(schemaData);
		return renewal;
	}

	private static JSONObjectAdapter adapter(String json) {
		try {
			return new JSONObjectAdapterImpl(json);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
