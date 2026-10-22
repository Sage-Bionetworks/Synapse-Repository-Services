package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.schema.SynapseSchemaBootstrap;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.JsonSchemaAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.CreateSubmissionRequest;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionState;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateField;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateReference;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateStep;
import org.sagebionetworks.repo.model.dataaccess.schema.SubmissionContext;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.dbo.search.OpaqueJsonColumnCodecUtil;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaConstants;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.repo.web.AccessRequirementVersionOutOfDateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Covers submitting the answers a requester gave to the schema bound to a
 * {@link JsonSchemaAccessRequirement}, with the schema, the form template, the requirement and the
 * answers stored for real. The services only need to be called once for the wiring to be proven
 * (see ITJsonSchemaDataAccessRequestTest), so the cases that depend on how these managers behave
 * together live here.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class JsonSchemaAccessRequirementAutoWiredTest {

	private static final String ORGANIZATION_NAME = "test.json.schema.dar.autowired.organization";
	private static final String SCHEMA_NAME = "autowired.test.JsonSchemaAccessRequirementSchema";
	private static final String SCHEMA_ID = ORGANIZATION_NAME + "-" + SCHEMA_NAME + "-1.0.0";

	@Autowired
	private UserManager userManager;
	@Autowired
	private NodeManager nodeManager;
	@Autowired
	private AccessRequirementManager accessRequirementManager;
	@Autowired
	private FormTemplateManager formTemplateManager;
	@Autowired
	private JsonSchemaManager jsonSchemaManager;
	@Autowired
	private SynapseSchemaBootstrap schemaBootstrap;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private RequestManager requestManager;
	@Autowired
	private SubmissionManager submissionManager;
	@Autowired
	private RequestDAO requestDao;
	@Autowired
	private SubmissionDAO submissionDao;

	private UserInfo adminUserInfo;
	private UserInfo requester;

	private String projectId;
	private FormTemplate template;
	private JsonSchemaAccessRequirement accessRequirement;

	private Set<String> submissionIds;
	private Set<String> requestIds;
	private List<String> fileHandleIds;

	/**
	 * Templates cannot be deleted and their names are unique, so every test names its templates with
	 * a prefix that is unique to the run.
	 */
	private String namePrefix;

	@BeforeEach
	public void before() {
		submissionIds = new LinkedHashSet<>();
		requestIds = new LinkedHashSet<>();
		fileHandleIds = new ArrayList<>();
		namePrefix = UUID.randomUUID().toString();

		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		NewUser newUser = new NewUser();
		newUser.setEmail(UUID.randomUUID() + "@test.com");
		newUser.setUserName(UUID.randomUUID().toString());
		requester = userManager.createOrGetTestUser(adminUserInfo, newUser);

		// Other tests truncate the schemas, which takes the base schema that every requirement's
		// schema extends with them.
		jsonSchemaManager.truncateAll();
		schemaBootstrap.bootstrapAccessRequirementBaseSchema(adminUserInfo);
		jsonSchemaManager.createOrganziation(adminUserInfo,
				new CreateOrganizationRequest().setOrganizationName(ORGANIZATION_NAME));
		jsonSchemaManager.createJsonSchema(adminUserInfo, new CreateSchemaRequest().setSchema(newJsonSchema()));

		Node project = new Node();
		project.setName("project " + namePrefix);
		project.setNodeType(EntityType.project);
		projectId = nodeManager.createNewNode(project, adminUserInfo);

		template = formTemplateManager.create(adminUserInfo, newTemplate("Standard DAR"));
		accessRequirement = accessRequirementManager.createAccessRequirement(adminUserInfo,
				newAccessRequirement(template.getId(), 1L));
	}

	@AfterEach
	public void after() {
		// A submission must be deleted before the request it was made from
		for (String id : submissionIds) {
			submissionDao.delete(id);
		}
		for (String id : requestIds) {
			requestDao.delete(id);
		}
		accessRequirementManager.deleteAccessRequirement(adminUserInfo, accessRequirement.getId().toString());
		nodeManager.delete(adminUserInfo, projectId);
		userManager.deletePrincipal(adminUserInfo, requester.getId());
		for (String fileHandleId : fileHandleIds) {
			fileHandleManager.deleteFileHandle(adminUserInfo, fileHandleId);
		}
		jsonSchemaManager.truncateAll();
	}

	@Test
	public void testSubmitWithSchemaDataTheSchemaRejects() {
		// The required project lead was never answered, which saving allows and submitting does not
		RequestInterface saved = createSubmittableRequest("{\"intendedDataUse\":\"Research\"}");

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			submit(saved);
		}).getMessage();

		assertEquals("The request data is invalid: #: required key [projectLead] not found", message);

		// Nothing was created, so the requester can answer the missing property and submit, which an
		// already open submission would refuse
		assertFalse(submissionDao.hasSubmissionWithState(requester.getId().toString(), accessRequirementId(),
				SubmissionState.SUBMITTED));

		RequestInterface answered = requestManager.createOrUpdate(requester,
				((Request) requestManager.getRequestForUpdate(requester, accessRequirementId()))
						.setSchemaData(json("{\"projectLead\":\"Dr. Lead\"}")));

		assertEquals(SubmissionState.SUBMITTED, submit(answered).getState());
	}

	@Test
	public void testSubmitWithStaleAccessRequirementVersion() {
		RequestInterface saved = createSubmittableRequest("{\"projectLead\":\"Dr. Lead\"}");

		// The requirement states the terms of access, and they changed after the answers were saved
		moveRequirementToNewTemplateVersion();

		String message = assertThrows(AccessRequirementVersionOutOfDateException.class, () -> {
			// call under test
			submit(saved);
		}).getMessage();

		assertEquals("This request was last saved against version 0 of the access requirement, which is now on"
				+ " version 1. Answer the current form and save the request before submitting it.", message);

		// Answering the current form again brings the request forward, and submitting then succeeds
		RequestInterface reanswered = requestManager.createOrUpdate(requester,
				(Request) requestManager.getRequestForUpdate(requester, accessRequirementId()));

		assertEquals(Long.valueOf(1L), reanswered.getAccessRequirementVersionNumber());
		assertEquals(SubmissionState.SUBMITTED, submit(reanswered).getState());
	}

	@Test
	public void testReviewSubmissionAfterAccessRequirementUpdated() {
		RequestInterface saved = createSubmittableRequest("{\"projectLead\":\"Dr. Lead\"}");
		String submissionId = submit(saved).getSubmissionId();

		// The requirement moves forward while the submission is still awaiting review
		moveRequirementToNewTemplateVersion();

		// call under test
		Submission awaitingReview = submissionManager.getSubmission(adminUserInfo, submissionId);

		// The submission stands: it is still open, still carries the answers it was validated with,
		// and still names the version of the requirement it was submitted against
		assertEquals(SubmissionState.SUBMITTED, awaitingReview.getState());
		assertEquals(Long.valueOf(0L), awaitingReview.getAccessRequirementVersion());
		String validatedAnswers = "{\"projectLead\":\"Dr. Lead\",\"" + JsonSchemaConstants.SUBMISSION_CONTEXT_PROPERTY
				+ "\":\"" + JsonSchemaConstants.SUBMISSION_CONTEXT_REQUEST + "\"}";
		assertSchemaDataEquals(validatedAnswers, awaitingReview.getSchemaData());

		// call under test
		Submission reviewed = submissionManager.updateStatus(adminUserInfo,
				new SubmissionStateChangeRequest().setSubmissionId(submissionId)
						.setNewState(SubmissionState.APPROVED));

		// Reviewing it needs no resubmission, and approving it does not re-validate the answers
		// against the requirement's current form
		assertEquals(SubmissionState.APPROVED, reviewed.getState());
		assertEquals(awaitingReview.getAccessRequirementVersion(), reviewed.getAccessRequirementVersion());
		assertSchemaDataEquals(validatedAnswers, reviewed.getSchemaData());
	}

	@Test
	public void testSubmitWithSchemaDataFileHandle() {
		String fileHandleId = createFileHandle(requester);

		// call under test
		RequestInterface saved = createSubmittableRequest(
				"{\"projectLead\":\"Dr. Lead\",\"signedDocument\":" + fileHandleId + "}", fileHandleId);

		// The client lists the file handles its answers reference
		assertEquals(List.of(fileHandleId), saved.getSchemaDataAttachments());

		String submissionId = submit(saved).getSubmissionId();

		// The submission references the same file handles, which is what lets a reviewer download them
		assertEquals(List.of(fileHandleId),
				submissionManager.getSubmission(adminUserInfo, submissionId).getSchemaDataAttachments());
	}

	@Test
	public void testSaveWithSchemaDataReferencingAnotherUsersFileHandle() {
		// A file handle the requester did not upload
		String fileHandleId = createFileHandle(adminUserInfo);

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			createSubmittableRequest("{\"projectLead\":\"Dr. Lead\",\"signedDocument\":" + fileHandleId + "}", fileHandleId);
		});
	}

	private String createFileHandle(UserInfo owner) {
		String fileHandleId = fileHandleManager.createExternalFileHandle(owner,
				new ExternalFileHandle().setExternalURL("http://example.com/signed.pdf").setFileName("signed.pdf"))
				.getId();
		fileHandleIds.add(fileHandleId);
		return fileHandleId;
	}

	/**
	 * A request carrying the given answers and the one accessor every submission needs.
	 */
	private Request newSubmittableRequest(String schemaDataJson, String... attachments) {
		return new Request().setAccessRequirementId(accessRequirementId()).setSchemaData(json(schemaDataJson))
				.setSchemaDataAttachments(new ArrayList<>(List.of(attachments)))
				.setAccessorChanges(new ArrayList<>(List.of(
						new AccessorChange().setType(AccessType.GAIN_ACCESS).setUserId(requester.getId().toString()))));
	}

	private RequestInterface createSubmittableRequest(String schemaDataJson, String... attachments) {
		RequestInterface saved = requestManager.createOrUpdate(requester, newSubmittableRequest(schemaDataJson, attachments));
		requestIds.add(saved.getId());
		return saved;
	}

	private SubmissionStatus submit(RequestInterface request) {
		SubmissionStatus status = submissionManager.create(requester,
				new CreateSubmissionRequest().setRequestId(request.getId()).setRequestEtag(request.getEtag())
						.setSubjectId(projectId).setSubjectType(RestrictableObjectType.ENTITY));
		submissionIds.add(status.getSubmissionId());
		return status;
	}

	private String accessRequirementId() {
		return accessRequirement.getId().toString();
	}

	private void moveRequirementToNewTemplateVersion() {
		FormTemplate secondVersion = formTemplateManager.createNewVersion(adminUserInfo,
				newTemplate("Standard DAR, revised").setId(template.getId()).setEtag(template.getEtag()));

		accessRequirement = accessRequirementManager.updateAccessRequirement(adminUserInfo, accessRequirementId(),
				accessRequirement.setFormTemplateRef(reference(template.getId(), secondVersion.getVersionNumber())));
	}

	/**
	 * The saved answers are an arbitrary JSON document that is stored with its keys in an
	 * unspecified order, so they are compared as JSON rather than as text.
	 */
	private static void assertSchemaDataEquals(String expectedJson, Object actual) {
		JSONObject expected = new JSONObject(expectedJson);
		JSONObject actualJson = new JSONObject(OpaqueJsonColumnCodecUtil.serialize(actual, "schemaData"));

		assertTrue(expected.similar(actualJson), () -> "expected: " + expected + " but was: " + actualJson);
	}

	private static JSONObject json(String json) {
		return new JSONObject(json);
	}

	private JsonSchemaAccessRequirement newAccessRequirement(String templateId, Long templateVersionNumber) {
		return new JsonSchemaAccessRequirement().setAccessType(ACCESS_TYPE.DOWNLOAD)
				.setName("AR " + UUID.randomUUID())
				.setSubjectIds(List.of(new RestrictableObjectDescriptor().setId(projectId)
						.setType(RestrictableObjectType.ENTITY)))
				.setFormTemplateRef(reference(templateId, templateVersionNumber));
	}

	private static FormTemplateReference reference(String templateId, Long templateVersionNumber) {
		return new FormTemplateReference().setTemplateId(templateId).setTemplateVersionNumber(templateVersionNumber);
	}

	/**
	 * The schema the requester answers. '/projectLead' is required, '/institution' is an object
	 * rather than a value, '/signedDocument' is a file handle, and the rest are optional leaves.
	 */
	private static JsonSchema newJsonSchema() {
		Map<String, JsonSchema> properties = new LinkedHashMap<>();
		properties.put("projectLead", new JsonSchema().setType(Type.string));
		properties.put("intendedDataUse", new JsonSchema().setType(Type.string));
		properties.put("institution", new JsonSchema().setType(Type.object)
				.setProperties(Map.of("name", new JsonSchema().setType(Type.string))));
		properties.put("signedDocument", new JsonSchema().setType(Type.integer).setFormat("synapse-filehandle-id"));

		// Every schema an access requirement binds must extend the bootstrapped base schema, which is
		// where the submission context convention comes from.
		return new JsonSchema().set$id(SCHEMA_ID)
				.setDescription("A schema based data access request autowired test schema").setType(Type.object)
				.setAllOf(List.of(new JsonSchema().set$ref(JsonSchemaConstants.ACCESS_REQUIREMENT_BASE_SCHEMA_ID)))
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
				.setIsPublic(false).setUiDefinition(uiDefinition("{\"ui:autofocus\":true}"));
	}

	private static JSONObjectAdapterImpl uiDefinition(String json) {
		try {
			return new JSONObjectAdapterImpl(json);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
