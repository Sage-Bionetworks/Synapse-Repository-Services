package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.AsynchJobType;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ErrorResponseCode;
import org.sagebionetworks.repo.model.JsonSchemaAccessRequirement;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.CreateSubmissionRequest;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateField;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateReference;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateStep;
import org.sagebionetworks.repo.model.dataaccess.schema.SubmissionContext;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaConstants;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * Covers saving and resuming the progress a requester has made answering the schema bound to a
 * {@link JsonSchemaAccessRequirement}. The answers are carried by the same request services the
 * managed ACT flow uses, so the requirement type is the only thing that changes.
 */
@ExtendWith(ITTestExtension.class)
public class ITJsonSchemaDataAccessRequestTest {

	private static final long MAX_WAIT_MS = 1000 * 30;
	private static final String ORGANIZATION_NAME = "test.json.schema.dar.organization";
	private static final String SCHEMA_NAME = "integration.test.JsonSchemaDataAccessRequestSchema";
	private static final String SCHEMA_VERSION = "1.0.0";
	private static final String SCHEMA_ID = ORGANIZATION_NAME + "-" + SCHEMA_NAME + "-" + SCHEMA_VERSION;

	private final SynapseAdminClient adminSynapse;
	private final SynapseClient synapse;

	/**
	 * Templates cannot be deleted and their names are unique, so every test names its templates with
	 * a prefix that is unique to the run.
	 */
	private String namePrefix;

	private Project project;
	private FormTemplate template;
	private JsonSchemaAccessRequirement accessRequirement;
	private String requesterId;

	private Long otherUserId;
	private String submissionId;

	public ITJsonSchemaDataAccessRequestTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}

	@BeforeEach
	public void before() throws SynapseException {
		namePrefix = UUID.randomUUID().toString();

		deleteSchemaAndOrganization();

		synapse.createOrganization(new CreateOrganizationRequest().setOrganizationName(ORGANIZATION_NAME));
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.CreateJsonSchema,
				new CreateSchemaRequest().setSchema(newJsonSchema()),
				(CreateSchemaResponse response) -> assertNotNull(response.getNewVersionInfo()), MAX_WAIT_MS);

		project = synapse.createEntity(new Project());
		template = adminSynapse.createFormTemplate(newTemplate("Standard DAR"));
		accessRequirement = adminSynapse.createAccessRequirement(newAccessRequirement(template.getId(), 1L));
		requesterId = synapse.getMyProfile().getOwnerId();
	}

	@AfterEach
	public void after() throws SynapseException {
		if (submissionId != null) {
			adminSynapse.deleteDataAccessSubmission(submissionId);
			submissionId = null;
		}
		adminSynapse.deleteEntity(project);
		deleteSchemaAndOrganization();
		// The access requirement is left behind on purpose: a saved request references it and the
		// requester's saved progress must not be destroyed by removing the requirement.
		if (otherUserId != null) {
			adminSynapse.deleteUser(otherUserId);
			otherUserId = null;
		}
	}

	@Test
	public void testCRUDWithSchemaData() throws SynapseException, JSONObjectAdapterException {
		// call under test
		RequestInterface blank = synapse.getRequestForUpdate(accessRequirementId());

		assertNull(blank.getId());
		assertNull(blank.getSchemaData());
		assertEquals(accessRequirementId(), blank.getAccessRequirementId());

		String firstSitting = "{\"projectLead\":\"Dr. Lead\"}";

		// call under test
		RequestInterface saved = synapse.createOrUpdateRequest(((Request) blank).setSchemaData(json(firstSitting)));

		assertNotNull(saved.getId());
		assertNotNull(saved.getEtag());
		assertEquals(requesterId, saved.getCreatedBy());
		assertSchemaDataEquals(firstSitting, saved.getSchemaData());
		// The requirement the answers were started against is recorded by the server. An access
		// requirement is versioned from zero.
		assertEquals(0L, saved.getAccessRequirementVersionNumber().longValue());
		// A schema based request answers the bound schema rather than a research project
		assertNull(saved.getResearchProjectId());

		// call under test
		RequestInterface resumed = synapse.getRequestForUpdate(accessRequirementId());

		assertEquals(saved.getId(), resumed.getId());
		assertEquals(saved.getEtag(), resumed.getEtag());
		assertSchemaDataEquals(firstSitting, resumed.getSchemaData());

		String secondSitting = "{\"projectLead\":\"Dr. Lead\",\"institution\":{\"name\":\"Sage\"}}";

		// call under test
		RequestInterface updated = synapse.createOrUpdateRequest(((Request) resumed).setSchemaData(json(secondSitting)));

		assertNotEquals(saved.getEtag(), updated.getEtag());
		assertSchemaDataEquals(secondSitting, updated.getSchemaData());
		assertSchemaDataEquals(secondSitting, synapse.getRequestForUpdate(accessRequirementId()).getSchemaData());
	}

	@Test
	public void testSaveWithSchemaDataTheSchemaWouldReject() throws SynapseException, JSONObjectAdapterException {
		// Answers are only validated when the request is submitted, so saving progress accepts a
		// number where the schema asks for a string and a property the schema does not declare.
		String invalid = "{\"projectLead\":42,\"notASchemaProperty\":[\"anything\"]}";

		// call under test
		RequestInterface saved = createRequest(invalid);

		assertSchemaDataEquals(invalid, saved.getSchemaData());
		assertSchemaDataEquals(invalid, synapse.getRequestForUpdate(accessRequirementId()).getSchemaData());

		// The required property can be left unanswered entirely
		RequestInterface emptied = synapse.createOrUpdateRequest(((Request) saved).setSchemaData(json("{}")));

		assertSchemaDataEquals("{}", emptied.getSchemaData());
	}

	@Test
	public void testUpdateWithNewerAccessRequirementVersion() throws SynapseException {
		RequestInterface saved = createRequest("{\"projectLead\":\"Dr. Lead\"}");

		assertEquals(0L, saved.getAccessRequirementVersionNumber().longValue());

		// The requirement moves onto a new version of the template while the request is being answered
		moveRequirementToNewTemplateVersion();

		// Resuming shows the stamp lagging behind the requirement, which is how a client detects that
		// the form changed underneath the requester.
		Request resumed = (Request) synapse.getRequestForUpdate(accessRequirementId());
		assertEquals(0L, resumed.getAccessRequirementVersionNumber().longValue());

		// The requester answers again after the change. A client sending its own stamp does not get
		// to choose it.
		Request toUpdate = resumed
				.setAccessRequirementVersionNumber(99L)
				.setSchemaData(json("{\"projectLead\":\"Dr. Lead\",\"intendedDataUse\":\"Research\"}"));

		// call under test
		RequestInterface updated = synapse.createOrUpdateRequest(toUpdate);

		// The stamp records the version the answers were last saved against, so a client that warned
		// about the change can stop warning once the requester saves against the current form.
		assertEquals(1L, adminSynapse.getAccessRequirement(accessRequirement.getId()).getVersionNumber().longValue());
		assertEquals(1L, updated.getAccessRequirementVersionNumber().longValue());
	}

	@Test
	public void testGetRequestForUpdateAfterSubmissionApproved() throws SynapseException, JSONObjectAdapterException {
		String answers = "{\"projectLead\":\"Dr. Lead\",\"institution\":{\"name\":\"Sage\"}}";
		RequestInterface saved = synapse.createOrUpdateRequest(new Request()
				.setAccessRequirementId(accessRequirementId())
				.setSchemaData(json(answers))
				.setAccessorChanges(
						List.of(new AccessorChange().setType(AccessType.GAIN_ACCESS).setUserId(requesterId))));

		SubmissionStatus status = synapse.submitRequest(new CreateSubmissionRequest()
				.setRequestId(saved.getId())
				.setRequestEtag(saved.getEtag())
				.setSubjectId(project.getId())
				.setSubjectType(RestrictableObjectType.ENTITY));

		submissionId = status.getSubmissionId();

		// The requirement moves forward before the submission is reviewed
		moveRequirementToNewTemplateVersion();

		Submission submission = adminSynapse.updateSubmissionState(submissionId, SubmissionState.APPROVED, null);

		assertEquals(SubmissionState.APPROVED, submission.getState());

		// call under test
		RequestInterface renewal = synapse.getRequestForUpdate(accessRequirementId());

		assertInstanceOf(Renewal.class, renewal);
		// Answering starts over against whichever version of the requirement is current now
		assertEquals(1L, renewal.getAccessRequirementVersionNumber().longValue());
		// The answers already given are the starting point for the renewal
		assertSchemaDataEquals(answers, renewal.getSchemaData());
	}

	@Test
	public void testSubmitWithSchemaData() throws SynapseException, JSONObjectAdapterException {
		String answers = "{\"projectLead\":\"Dr. Lead\",\"institution\":{\"name\":\"Sage\"}}";
		RequestInterface saved = createSubmittableRequest(answers);

		// call under test
		SubmissionStatus status = submit(saved);

		assertEquals(SubmissionState.SUBMITTED, status.getState());

		Submission submission = adminSynapse.updateSubmissionState(submissionId, SubmissionState.APPROVED, null);

		assertEquals(saved.getId(), submission.getRequestId());
		// A schema based requirement collects no research project, so there is nothing to snapshot
		assertNull(submission.getResearchProjectSnapshot());
		// The reviewer reads the answers as they were validated, including the context the server
		// supplied for them
		assertSchemaDataEquals("{\"projectLead\":\"Dr. Lead\",\"institution\":{\"name\":\"Sage\"},"
				+ "\"" + JsonSchemaConstants.SUBMISSION_CONTEXT_PROPERTY + "\":\""
				+ JsonSchemaConstants.SUBMISSION_CONTEXT_REQUEST + "\"}", submission.getSchemaData());
	}

	@Test
	public void testSubmitWithSchemaDataTheSchemaRejects() throws SynapseException, JSONObjectAdapterException {
		// The required project lead was never answered, which saving allows and submitting does not
		RequestInterface saved = createSubmittableRequest("{\"intendedDataUse\":\"Research\"}");

		SynapseBadRequestException exception = assertThrows(SynapseBadRequestException.class, () -> {
			// call under test
			submit(saved);
		});

		assertEquals("The request data is invalid: #: required key [projectLead] not found",
				exception.getMessage());

		// Nothing was created, so the requester can answer the missing property and submit, which an
		// already open submission would refuse
		RequestInterface answered = synapse.createOrUpdateRequest(((Request) synapse
				.getRequestForUpdate(accessRequirementId())).setSchemaData(json("{\"projectLead\":\"Dr. Lead\"}")));

		assertEquals(SubmissionState.SUBMITTED, submit(answered).getState());
	}

	@Test
	public void testSubmitWithStaleAccessRequirementVersion() throws SynapseException, JSONObjectAdapterException {
		RequestInterface saved = createSubmittableRequest("{\"projectLead\":\"Dr. Lead\"}");

		// The requirement states the terms of access, and they changed after the answers were saved
		moveRequirementToNewTemplateVersion();

		SynapseBadRequestException exception = assertThrows(SynapseBadRequestException.class, () -> {
			// call under test
			submit(saved);
		});

		// A client reports that the requirement changed rather than that the answers are invalid
		assertEquals(ErrorResponseCode.ACCESS_REQUIREMENT_VERSION_OUT_OF_DATE, exception.getErrorResponseCode());
		assertEquals("This request was last saved against version 0 of the access requirement, which is now on"
				+ " version 1. Answer the current form and save the request before submitting it.",
				exception.getMessage());

		// Answering the current form again brings the request forward, and submitting then succeeds
		RequestInterface reanswered = synapse.createOrUpdateRequest((Request) synapse
				.getRequestForUpdate(accessRequirementId()));

		assertEquals(SubmissionState.SUBMITTED, submit(reanswered).getState());
	}

	@Test
	public void testReviewSubmissionAfterAccessRequirementUpdated()
			throws SynapseException, JSONObjectAdapterException {
		String answers = "{\"projectLead\":\"Dr. Lead\"}";
		RequestInterface saved = createSubmittableRequest(answers);

		submit(saved);

		// The requirement moves forward while the submission is still awaiting review
		moveRequirementToNewTemplateVersion();

		// call under test
		Submission awaitingReview = adminSynapse.getDataAccessSubmission(submissionId);

		// The submission stands: it is still open, still carries the answers it was validated with,
		// and still names the version of the requirement it was submitted against
		assertEquals(SubmissionState.SUBMITTED, awaitingReview.getState());
		assertEquals(0L, awaitingReview.getAccessRequirementVersion().longValue());
		assertSchemaDataEquals("{\"projectLead\":\"Dr. Lead\",\""
				+ JsonSchemaConstants.SUBMISSION_CONTEXT_PROPERTY + "\":\""
				+ JsonSchemaConstants.SUBMISSION_CONTEXT_REQUEST + "\"}", awaitingReview.getSchemaData());

		// call under test
		Submission reviewed = adminSynapse.updateSubmissionState(submissionId, SubmissionState.APPROVED, null);

		// Reviewing it needs no resubmission, and approving it does not re-validate the answers
		// against the requirement's current form
		assertEquals(SubmissionState.APPROVED, reviewed.getState());
		assertEquals(awaitingReview.getAccessRequirementVersion(), reviewed.getAccessRequirementVersion());
		assertSchemaDataEquals(((JSONObjectAdapter) awaitingReview.getSchemaData()).toJSONString(),
				reviewed.getSchemaData());
	}

	@Test
	public void testSubmitWithMismatchedSubmissionContext() throws SynapseException, JSONObjectAdapterException {
		// The requester has never been approved, so these answers cannot be a renewal of anything
		RequestInterface saved = createSubmittableRequest("{\"projectLead\":\"Dr. Lead\",\""
				+ JsonSchemaConstants.SUBMISSION_CONTEXT_PROPERTY + "\":\""
				+ JsonSchemaConstants.SUBMISSION_CONTEXT_RENEWAL + "\"}");

		SynapseBadRequestException exception = assertThrows(SynapseBadRequestException.class, () -> {
			// call under test
			submit(saved);
		});

		assertEquals(ErrorResponseCode.SUBMISSION_CONTEXT_MISMATCH, exception.getErrorResponseCode());
		assertEquals("This request is a REQUEST, so its '" + JsonSchemaConstants.SUBMISSION_CONTEXT_PROPERTY
				+ "' cannot be 'RENEWAL'.", exception.getMessage());
	}

	@Test
	public void testGetRequestForUpdateWithAnotherRequester() throws SynapseException, JSONObjectAdapterException {
		RequestInterface saved = createRequest("{\"projectLead\":\"Dr. Lead\"}");

		SynapseClient otherSynapse = new SynapseClientImpl();
		// The request services do not check profile validation, and a validated test user cannot be
		// deleted afterwards because VERIFICATION_STATE restricts deleting its USER_GROUP row.
		otherUserId = SynapseClientHelper.createUser(adminSynapse, otherSynapse, true, false);

		// call under test
		RequestInterface otherRequest = otherSynapse.getRequestForUpdate(accessRequirementId());

		// The other requester starts from a blank request rather than reading the saved answers
		assertNull(otherRequest.getId());
		assertNull(otherRequest.getSchemaData());

		String message = assertThrows(SynapseForbiddenException.class, () -> {
			// call under test
			otherSynapse.createOrUpdateRequest(((Request) saved).setSchemaData(json("{\"projectLead\":\"Imposter\"}")));
		}).getMessage();

		assertEquals("Only owner can perform this action.", message);
		assertSchemaDataEquals("{\"projectLead\":\"Dr. Lead\"}",
				synapse.getRequestForUpdate(accessRequirementId()).getSchemaData());
	}

	private RequestInterface createRequest(String schemaDataJson) throws SynapseException {
		return synapse.createOrUpdateRequest(
				new Request().setAccessRequirementId(accessRequirementId()).setSchemaData(json(schemaDataJson)));
	}

	/**
	 * A saved request carrying the given answers and the one accessor every submission needs.
	 */
	private RequestInterface createSubmittableRequest(String schemaDataJson) throws SynapseException {
		return synapse.createOrUpdateRequest(new Request().setAccessRequirementId(accessRequirementId())
				.setSchemaData(json(schemaDataJson)).setAccessorChanges(
						List.of(new AccessorChange().setType(AccessType.GAIN_ACCESS).setUserId(requesterId))));
	}

	private SubmissionStatus submit(RequestInterface request) throws SynapseException {
		SubmissionStatus status = synapse.submitRequest(new CreateSubmissionRequest()
				.setRequestId(request.getId())
				.setRequestEtag(request.getEtag())
				.setSubjectId(project.getId())
				.setSubjectType(RestrictableObjectType.ENTITY));

		submissionId = status.getSubmissionId();

		return status;
	}

	private String accessRequirementId() {
		return accessRequirement.getId().toString();
	}

	private void moveRequirementToNewTemplateVersion() throws SynapseException {
		FormTemplate secondVersion = adminSynapse.createFormTemplateVersion(
				newTemplate("Standard DAR, revised").setId(template.getId()).setEtag(template.getEtag()));

		accessRequirement = adminSynapse.updateAccessRequirement(accessRequirement
				.setFormTemplateRef(reference(template.getId(), secondVersion.getVersionNumber())));
	}

	/**
	 * The saved answers are an arbitrary JSON document, which compares by identity on the way back
	 * out of the client and is stored with its keys in an unspecified order.
	 */
	private static void assertSchemaDataEquals(String expectedJson, Object actual)
			throws JSONObjectAdapterException {
		JSONObjectAdapter adapter = assertInstanceOf(JSONObjectAdapter.class, actual);
		JSONObject expected = new JSONObject(expectedJson);
		JSONObject actualJson = new JSONObject(adapter.toJSONString());

		assertTrue(expected.similar(actualJson), () -> "expected: " + expected + " but was: " + actualJson);
	}

	private JsonSchemaAccessRequirement newAccessRequirement(String templateId, Long templateVersionNumber) {
		return new JsonSchemaAccessRequirement().setAccessType(ACCESS_TYPE.DOWNLOAD)
				.setName("AR " + UUID.randomUUID())
				.setSubjectIds(List.of(new RestrictableObjectDescriptor().setId(project.getId())
						.setType(RestrictableObjectType.ENTITY)))
				.setFormTemplateRef(reference(templateId, templateVersionNumber));
	}

	private static FormTemplateReference reference(String templateId, Long templateVersionNumber) {
		return new FormTemplateReference().setTemplateId(templateId)
				.setTemplateVersionNumber(templateVersionNumber);
	}

	private void deleteSchemaAndOrganization() throws SynapseException {
		try {
			adminSynapse.deleteSchema(ORGANIZATION_NAME, SCHEMA_NAME);
		} catch (SynapseNotFoundException e) {
			// nothing to clean up
		}
		try {
			adminSynapse.deleteOrganization(synapse.getOrganizationByName(ORGANIZATION_NAME).getId());
		} catch (SynapseNotFoundException e) {
			// nothing to clean up
		}
	}

	/**
	 * The schema the requester answers. '/projectLead' is required, '/institution' is an object
	 * rather than a value, and the rest are optional leaves.
	 */
	private static JsonSchema newJsonSchema() {
		Map<String, JsonSchema> properties = new LinkedHashMap<>();
		properties.put("projectLead", new JsonSchema().setType(Type.string));
		properties.put("intendedDataUse", new JsonSchema().setType(Type.string));
		properties.put("institution", new JsonSchema().setType(Type.object)
				.setProperties(Map.of("name", new JsonSchema().setType(Type.string))));

		// Every schema an access requirement binds must extend the bootstrapped base schema, which is
		// where the submission context convention comes from.
		return new JsonSchema().set$id(SCHEMA_ID)
				.setDescription("A schema based data access request integration test schema").setType(Type.object)
				.setAllOf(List.of(
						new JsonSchema().set$ref(JsonSchemaConstants.ACCESS_REQUIREMENT_BASE_SCHEMA_ID)))
				.setProperties(properties).setRequired(List.of("projectLead"));
	}

	private FormTemplate newTemplate(String name) {
		return new FormTemplate().setName(namePrefix + " " + name).setSchema$id(SCHEMA_ID).setDeprecated(false)
				.setSteps(List.of(new FormTemplateStep().setTitle("The project")
						.setDescription("Tell us about the project")
						.setFields(new ArrayList<>(List.of(field("/projectLead"), field("/institution/name"))))));
	}

	private static FormTemplateField field(String schemaPath) {
		return new FormTemplateField().setSchemaPath(schemaPath).setSubmissionContext(SubmissionContext.ALWAYS)
				.setIsPublic(false).setUiDefinition(json("{\"ui:autofocus\":true}"));
	}

	private static JSONObjectAdapter json(String json) {
		try {
			return new JSONObjectAdapterImpl(json);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
