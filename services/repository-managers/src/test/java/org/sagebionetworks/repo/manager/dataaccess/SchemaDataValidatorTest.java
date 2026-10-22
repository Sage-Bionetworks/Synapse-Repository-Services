package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaValidatorFactory;
import org.sagebionetworks.repo.model.JsonSchemaAccessRequirement;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateReference;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaConstants;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.web.AccessRequirementVersionOutOfDateException;
import org.sagebionetworks.repo.web.SubmissionContextMismatchException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

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

	private SchemaDataValidator validator;

	private JsonSchemaAccessRequirement accessRequirement;
	private FormTemplate template;
	private JsonSchema schema;

	@BeforeEach
	public void before() {
		// The real factory is used so that the schema features the form templates rely on are
		// actually exercised rather than stubbed away.
		validator = new SchemaDataValidator(mockJsonSchemaManager, mockFormTemplateManager,
				new JsonSchemaValidatorFactory());

		accessRequirement = new JsonSchemaAccessRequirement();
		accessRequirement.setId(99L);
		accessRequirement.setVersionNumber(AR_VERSION);
		accessRequirement.setFormTemplateRef(
				new FormTemplateReference().setTemplateId(TEMPLATE_ID).setTemplateVersionNumber(TEMPLATE_VERSION));

		template = new FormTemplate().setId(TEMPLATE_ID).setVersionNumber(TEMPLATE_VERSION).setSchema$id(SCHEMA_ID);

		schema = newSchema();
	}

	@Test
	public void testValidateWithRequest() {
		setupSchemaLookup();
		RequestInterface request = newRequest(
				new JSONObject().put("projectLead", "Dr. Lead").put("irbApproval", "IRB-1").put("yearsOfAccess", 2));

		// call under test
		JSONObject answers = validator.validate(accessRequirement, request);

		JSONObject expected = new JSONObject().put("projectLead", "Dr. Lead").put("irbApproval", "IRB-1")
				.put("yearsOfAccess", 2).put(CONTEXT, JsonSchemaConstants.SUBMISSION_CONTEXT_REQUEST);
		assertTrue(expected.similar(answers), () -> "expected " + expected + " but was " + answers);
	}

	@Test
	public void testValidateWithRenewal() {
		setupSchemaLookup();
		Renewal renewal = new Renewal();
		renewal.setAccessRequirementId("99");
		renewal.setAccessRequirementVersionNumber(AR_VERSION);
		renewal.setSchemaData(
				new JSONObject().put("projectLead", "Dr. Lead").put("changesSinceLastRenewal", "None"));

		// call under test
		JSONObject answers = validator.validate(accessRequirement, renewal);

		JSONObject expected = new JSONObject().put("projectLead", "Dr. Lead").put("changesSinceLastRenewal", "None")
				.put(CONTEXT, JsonSchemaConstants.SUBMISSION_CONTEXT_RENEWAL);
		assertTrue(expected.similar(answers), () -> "expected " + expected + " but was " + answers);
	}

	@Test
	public void testValidateWithSchemaDataFromTheWire() throws JSONObjectAdapterException {
		setupSchemaLookup();
		// A request taken off the wire carries its answers as an adapter rather than as the
		// JSONObject the database layer reads back.
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(
				"{\"projectLead\":\"Dr. Lead\",\"irbApproval\":\"IRB-1\"}");
		RequestInterface request = new Request().setAccessRequirementId("99")
				.setAccessRequirementVersionNumber(AR_VERSION).setSchemaData(adapter);

		// call under test
		JSONObject answers = validator.validate(accessRequirement, request);

		JSONObject expected = new JSONObject().put("projectLead", "Dr. Lead").put("irbApproval", "IRB-1")
				.put(CONTEXT, JsonSchemaConstants.SUBMISSION_CONTEXT_REQUEST);
		assertTrue(expected.similar(answers), () -> "expected " + expected + " but was " + answers);
	}

	@Test
	public void testValidateWithMatchingSubmissionContext() {
		setupSchemaLookup();
		RequestInterface request = newRequest(new JSONObject().put("projectLead", "Dr. Lead")
				.put("irbApproval", "IRB-1").put(CONTEXT, JsonSchemaConstants.SUBMISSION_CONTEXT_REQUEST));

		// call under test
		JSONObject answers = validator.validate(accessRequirement, request);

		assertEquals(JsonSchemaConstants.SUBMISSION_CONTEXT_REQUEST, answers.get(CONTEXT));
	}

	@Test
	public void testValidateWithRequestClaimingRenewalContext() {
		RequestInterface request = newRequest(new JSONObject().put("projectLead", "Dr. Lead")
				.put("irbApproval", "IRB-1").put(CONTEXT, JsonSchemaConstants.SUBMISSION_CONTEXT_RENEWAL));

		String message = assertThrows(SubmissionContextMismatchException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertEquals("This request is a REQUEST, so its '" + CONTEXT + "' cannot be 'RENEWAL'.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager);
	}

	@Test
	public void testValidateWithRenewalClaimingRequestContext() {
		Renewal renewal = new Renewal();
		renewal.setAccessRequirementVersionNumber(AR_VERSION);
		renewal.setSchemaData(new JSONObject().put("projectLead", "Dr. Lead").put(CONTEXT,
				JsonSchemaConstants.SUBMISSION_CONTEXT_REQUEST));

		String message = assertThrows(SubmissionContextMismatchException.class, () -> {
			// call under test
			validator.validate(accessRequirement, renewal);
		}).getMessage();

		assertEquals("This request is a RENEWAL, so its '" + CONTEXT + "' cannot be 'REQUEST'.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager);
	}

	@Test
	public void testValidateWithStaleAccessRequirementVersion() {
		RequestInterface request = newRequest(new JSONObject().put("projectLead", "Dr. Lead"));
		request.setAccessRequirementVersionNumber(AR_VERSION - 1);

		String message = assertThrows(AccessRequirementVersionOutOfDateException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertEquals("This request was last saved against version 2 of the access requirement, which is now on"
				+ " version 3. Answer the current form and save the request before submitting it.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager);
	}

	@Test
	public void testValidateWithNullAccessRequirementVersion() {
		RequestInterface request = newRequest(new JSONObject().put("projectLead", "Dr. Lead"));
		request.setAccessRequirementVersionNumber(null);

		String message = assertThrows(AccessRequirementVersionOutOfDateException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertEquals("This request was last saved against version null of the access requirement, which is now on"
				+ " version 3. Answer the current form and save the request before submitting it.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager);
	}

	@Test
	public void testValidateWithMissingRequiredProperty() {
		setupSchemaLookup();
		RequestInterface request = newRequest(new JSONObject().put("irbApproval", "IRB-1"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertEquals("The request data is invalid: #: required key [projectLead] not found", message);
	}

	@Test
	public void testValidateWithRequestOnlyRequiredPropertyOmitted() {
		setupSchemaLookup();
		// The schema requires an IRB approval of a first request but not of a renewal.
		RequestInterface request = newRequest(new JSONObject().put("projectLead", "Dr. Lead"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertEquals("The request data is invalid: #: required key [irbApproval] not found", message);
	}

	@Test
	public void testValidateWithRenewalOnlyRequiredPropertyOmitted() {
		setupSchemaLookup();
		// The schema requires a statement of changes of a renewal but not of a first request.
		Renewal renewal = new Renewal();
		renewal.setAccessRequirementVersionNumber(AR_VERSION);
		renewal.setSchemaData(new JSONObject().put("projectLead", "Dr. Lead"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(accessRequirement, renewal);
		}).getMessage();

		assertEquals("The request data is invalid: #: required key [changesSinceLastRenewal] not found", message);
	}

	@Test
	public void testValidateWithRequestOnlyPropertyOnRenewal() {
		setupSchemaLookup();
		// A property that is only required of a first request still satisfies a renewal, which has
		// its own requirement to meet.
		Renewal renewal = new Renewal();
		renewal.setAccessRequirementVersionNumber(AR_VERSION);
		renewal.setSchemaData(new JSONObject().put("projectLead", "Dr. Lead").put("irbApproval", "IRB-1"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(accessRequirement, renewal);
		}).getMessage();

		assertEquals("The request data is invalid: #: required key [changesSinceLastRenewal] not found", message);
	}

	@Test
	public void testValidateWithTypeMismatch() {
		setupSchemaLookup();
		RequestInterface request = newRequest(new JSONObject().put("projectLead", "Dr. Lead")
				.put("irbApproval", "IRB-1").put("yearsOfAccess", "two"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertEquals("The request data is invalid: #/yearsOfAccess: expected type: Integer, found: String", message);
	}

	@Test
	public void testValidateWithInvalidFileHandleId() {
		setupSchemaLookup();
		// A file handle id is a number, so a client that sends the name of the file it uploaded
		// instead of the id it was given is rejected.
		RequestInterface request = newRequest(new JSONObject().put("projectLead", "Dr. Lead")
				.put("irbApproval", "IRB-1").put("attachment", "consent-form.pdf"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertEquals("The request data is invalid: #/attachment: expected type: Integer, found: String", message);
	}

	@Test
	public void testValidateWithMultipleProblems() {
		setupSchemaLookup();
		RequestInterface request = newRequest(
				new JSONObject().put("irbApproval", "IRB-1").put("yearsOfAccess", "two"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertTrue(message.contains("projectLead"), () -> message);
		assertTrue(message.contains("yearsOfAccess"), () -> message);
	}

	@Test
	public void testValidateWithNullSchemaData() {
		setupSchemaLookup();
		RequestInterface request = newRequest(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertEquals("The request data is invalid: #: required key [projectLead] not found"
				+ " #: required key [irbApproval] not found", message);
	}

	@Test
	public void testValidateWithSchemaDataThatIsNotAnObject() {
		RequestInterface request = newRequest(new JSONArray().put("projectLead"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertEquals("schemaData must be a JSON object.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager);
	}

	@Test
	public void testValidateDoesNotModifyTheRequest() {
		setupSchemaLookup();
		JSONObject schemaData = new JSONObject().put("projectLead", "Dr. Lead").put("irbApproval", "IRB-1");
		RequestInterface request = newRequest(schemaData);

		// call under test
		validator.validate(accessRequirement, request);

		// The context is supplied for the submission only, leaving the saved request as the
		// requester left it.
		JSONObject expected = new JSONObject().put("projectLead", "Dr. Lead").put("irbApproval", "IRB-1");
		assertTrue(expected.similar(request.getSchemaData()), () -> "was " + request.getSchemaData());
	}

	@Test
	public void testValidateWithNullFormTemplateRef() {
		accessRequirement.setFormTemplateRef(null);
		RequestInterface request = newRequest(new JSONObject().put("projectLead", "Dr. Lead"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(accessRequirement, request);
		}).getMessage();

		assertEquals("accessRequirement.formTemplateRef is required.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager);
	}

	@Test
	public void testValidateWithNullAccessRequirement() {
		RequestInterface request = newRequest(new JSONObject().put("projectLead", "Dr. Lead"));

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(null, request);
		}).getMessage();

		assertEquals("accessRequirement is required.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager);
	}

	@Test
	public void testValidateWithNullRequest() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			validator.validate(accessRequirement, null);
		}).getMessage();

		assertEquals("request is required.", message);
		verifyNoInteractions(mockFormTemplateManager, mockJsonSchemaManager);
	}

	private void setupSchemaLookup() {
		when(mockFormTemplateManager.getVersion(TEMPLATE_ID, TEMPLATE_VERSION)).thenReturn(template);
		when(mockJsonSchemaManager.getValidationSchema(SCHEMA_ID)).thenReturn(schema);
	}

	private RequestInterface newRequest(Object schemaData) {
		return new Request().setAccessRequirementId(accessRequirement.getId().toString())
				.setAccessRequirementVersionNumber(AR_VERSION).setSchemaData(schemaData);
	}

	/**
	 * A schema shaped like the schema an access requirement binds: it requires the submission
	 * context, and it conditions what else it requires on that context rather than on anything the
	 * form template says.
	 */
	private static JsonSchema newSchema() {
		Map<String, JsonSchema> properties = new LinkedHashMap<>();
		properties.put("projectLead", new JsonSchema().setType(Type.string));
		properties.put("yearsOfAccess", new JsonSchema().setType(Type.integer));
		properties.put("attachment",
				new JsonSchema().setType(Type.integer).setFormat(FormTemplateValidator.FILE_HANDLE_FORMAT));
		properties.put("irbApproval", new JsonSchema().setType(Type.string));
		properties.put("changesSinceLastRenewal", new JsonSchema().setType(Type.string));
		properties.put(CONTEXT, new JsonSchema().setType(Type.string)
				.set_enum(List.of(JsonSchemaConstants.SUBMISSION_CONTEXT_REQUEST,
						JsonSchemaConstants.SUBMISSION_CONTEXT_RENEWAL)));

		return new JsonSchema().set$id(SCHEMA_ID).setType(Type.object).setProperties(properties)
				.setRequired(List.of("projectLead", CONTEXT))
				.setAllOf(List.of(requiredForContext(JsonSchemaConstants.SUBMISSION_CONTEXT_REQUEST, "irbApproval"),
						requiredForContext(JsonSchemaConstants.SUBMISSION_CONTEXT_RENEWAL,
								"changesSinceLastRenewal")));
	}

	private static JsonSchema requiredForContext(String context, String property) {
		return new JsonSchema()
				.set_if(new JsonSchema().setProperties(Map.of(CONTEXT, new JsonSchema().set_const(context)))
						.setRequired(List.of(CONTEXT)))
				.setThen(new JsonSchema().setRequired(List.of(property)));
	}
}
