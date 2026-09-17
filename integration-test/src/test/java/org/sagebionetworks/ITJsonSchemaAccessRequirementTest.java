package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.AsynchJobType;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.JsonSchemaAccessRequirement;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.ManagedACTAccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateField;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateReference;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateStep;
import org.sagebionetworks.repo.model.dataaccess.schema.SubmissionContext;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

@ExtendWith(ITTestExtension.class)
public class ITJsonSchemaAccessRequirementTest {

	private static final long MAX_WAIT_MS = 1000 * 30;
	private static final String ORGANIZATION_NAME = "test.json.schema.ar.organization";
	private static final String SCHEMA_NAME = "integration.test.JsonSchemaAccessRequirementSchema";
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
	private List<Long> accessRequirementsToDelete;

	public ITJsonSchemaAccessRequirementTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}

	@BeforeEach
	public void before() throws SynapseException, InterruptedException {
		namePrefix = UUID.randomUUID().toString();
		accessRequirementsToDelete = new ArrayList<>();

		deleteSchemaAndOrganization();

		synapse.createOrganization(new CreateOrganizationRequest().setOrganizationName(ORGANIZATION_NAME));
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.CreateJsonSchema,
				new CreateSchemaRequest().setSchema(newJsonSchema()),
				(CreateSchemaResponse response) -> assertNotNull(response.getNewVersionInfo()), MAX_WAIT_MS);

		project = synapse.createEntity(new Project());
		template = adminSynapse.createFormTemplate(newTemplate("Standard DAR"));
	}

	@AfterEach
	public void after() throws SynapseException {
		// The requirements are removed before their subject so that they never reference a deleted entity
		for (Long accessRequirementId : accessRequirementsToDelete) {
			adminSynapse.deleteAccessRequirement(accessRequirementId);
		}
		adminSynapse.deleteEntity(project);
		deleteSchemaAndOrganization();
	}

	@Test
	public void testCRUDWithJsonSchemaAccessRequirement() throws SynapseException {
		// call under test
		JsonSchemaAccessRequirement created = createAccessRequirement(template.getId(), 1L);

		assertNotNull(created.getId());
		assertNotNull(created.getEtag());
		assertEquals(1L, created.getVersionNumber().longValue());
		assertEquals(ACCESS_TYPE.DOWNLOAD, created.getAccessType());
		assertEquals(reference(template.getId(), 1L), created.getFormTemplateRef());
		// The defaults are the ones shared with the managed ACT requirement type
		assertFalse(created.getIsDUCRequired());
		assertFalse(created.getIsCertifiedUserRequired());
		assertFalse(created.getIsValidatedProfileRequired());
		assertFalse(created.getIsTwoFaRequired());
		assertEquals(0L, created.getExpirationPeriod().longValue());

		// call under test
		assertEquals(created, adminSynapse.getAccessRequirement(created.getId()));

		// A second version of the template is published and the requirement is moved onto it
		FormTemplate secondVersion = adminSynapse.createFormTemplateVersion(
				newTemplate("Standard DAR, revised").setId(template.getId()).setEtag(template.getEtag()));

		assertEquals(2L, secondVersion.getVersionNumber().longValue());

		created.setFormTemplateRef(reference(template.getId(), secondVersion.getVersionNumber()));

		// call under test
		JsonSchemaAccessRequirement updated = adminSynapse.updateAccessRequirement(created);

		assertEquals(2L, updated.getVersionNumber().longValue());
		assertEquals(reference(template.getId(), 2L), updated.getFormTemplateRef());

		// call under test
		assertEquals(updated, adminSynapse.getAccessRequirement(created.getId()));

		// The status of the requirement is reported the same way as for a managed ACT requirement
		AccessRequirementStatus status = synapse.getAccessRequirementStatus(created.getId().toString());

		assertInstanceOf(ManagedACTAccessRequirementStatus.class, status);
		assertEquals(created.getId().toString(), status.getAccessRequirementId());
		assertFalse(status.getIsApproved());
	}

	@Test
	public void testCreateWithNonExistingFormTemplate() {
		JsonSchemaAccessRequirement accessRequirement = newAccessRequirement("-1", 1L);

		String message = assertThrows(SynapseBadRequestException.class, () -> {
			// call under test
			adminSynapse.createAccessRequirement(accessRequirement);
		}).getMessage();

		assertEquals("Version 1 of the form template with the id '-1' does not exist.", message);
	}

	@Test
	public void testCreateWithNonExistingFormTemplateVersion() {
		JsonSchemaAccessRequirement accessRequirement = newAccessRequirement(template.getId(), 2L);

		String message = assertThrows(SynapseBadRequestException.class, () -> {
			// call under test
			adminSynapse.createAccessRequirement(accessRequirement);
		}).getMessage();

		assertEquals("Version 2 of the form template with the id '" + template.getId() + "' does not exist.", message);
	}

	@Test
	public void testCreateWithDeprecatedFormTemplateVersion() throws SynapseException {
		// A template is retired by publishing a version of it that is deprecated
		FormTemplate deprecated = adminSynapse.createFormTemplateVersion(newTemplate("Retired DAR")
				.setId(template.getId()).setEtag(template.getEtag()).setDeprecated(true));

		JsonSchemaAccessRequirement accessRequirement = newAccessRequirement(template.getId(),
				deprecated.getVersionNumber());

		String message = assertThrows(SynapseBadRequestException.class, () -> {
			// call under test
			adminSynapse.createAccessRequirement(accessRequirement);
		}).getMessage();

		assertEquals("Version 2 of the form template with the id '" + template.getId()
				+ "' is deprecated, so it cannot be referenced by an access requirement.", message);
	}

	@Test
	public void testUpdateWithNonExistingFormTemplateVersion() throws SynapseException {
		JsonSchemaAccessRequirement created = createAccessRequirement(template.getId(), 1L);

		created.setFormTemplateRef(reference(template.getId(), 2L));

		String message = assertThrows(SynapseBadRequestException.class, () -> {
			// call under test
			adminSynapse.updateAccessRequirement(created);
		}).getMessage();

		assertEquals("Version 2 of the form template with the id '" + template.getId() + "' does not exist.", message);
	}

	@Test
	public void testUpdateWithNewFormTemplateVersion() throws SynapseException {
		JsonSchemaAccessRequirement movedForward = createAccessRequirement(template.getId(), 1L);
		JsonSchemaAccessRequirement leftBehind = createAccessRequirement(template.getId(), 1L);

		FormTemplate secondVersion = adminSynapse.createFormTemplateVersion(
				newTemplate("Standard DAR, revised").setId(template.getId()).setEtag(template.getEtag()));

		movedForward.setFormTemplateRef(reference(template.getId(), secondVersion.getVersionNumber()));

		// call under test
		adminSynapse.updateAccessRequirement(movedForward);

		assertEquals(reference(template.getId(), 2L),
				((JsonSchemaAccessRequirement) adminSynapse.getAccessRequirement(movedForward.getId()))
						.getFormTemplateRef());

		// The requirement that was not updated still renders the version it was created with
		assertEquals(reference(template.getId(), 1L),
				((JsonSchemaAccessRequirement) adminSynapse.getAccessRequirement(leftBehind.getId()))
						.getFormTemplateRef());
	}

	@Test
	public void testCreateWithNonACTUser() {
		JsonSchemaAccessRequirement accessRequirement = newAccessRequirement(template.getId(), 1L);

		String message = assertThrows(SynapseForbiddenException.class, () -> {
			// call under test
			synapse.createAccessRequirement(accessRequirement);
		}).getMessage();

		assertEquals("Only ACT member can create an AccessRequirement.", message);
	}

	@Test
	public void testUpdateWithNonACTUser() throws SynapseException {
		JsonSchemaAccessRequirement created = createAccessRequirement(template.getId(), 1L);

		String message = assertThrows(SynapseForbiddenException.class, () -> {
			// call under test
			synapse.updateAccessRequirement(created);
		}).getMessage();

		assertEquals("Only ACT member can perform this action.", message);
	}

	private JsonSchemaAccessRequirement createAccessRequirement(String templateId, Long templateVersionNumber)
			throws SynapseException {
		JsonSchemaAccessRequirement created = adminSynapse
				.createAccessRequirement(newAccessRequirement(templateId, templateVersionNumber));
		accessRequirementsToDelete.add(created.getId());
		return created;
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
	 * The schema that the templates under test render. '/projectLead' is required, '/institution' is
	 * an object rather than a value, and the rest are optional leaves.
	 */
	private static JsonSchema newJsonSchema() {
		Map<String, JsonSchema> properties = new LinkedHashMap<>();
		properties.put("projectLead", new JsonSchema().setType(Type.string));
		properties.put("intendedDataUse", new JsonSchema().setType(Type.string));
		properties.put("institution", new JsonSchema().setType(Type.object)
				.setProperties(Map.of("name", new JsonSchema().setType(Type.string))));

		return new JsonSchema().set$id(SCHEMA_ID).setDescription("A JSON schema access requirement integration test schema")
				.setType(Type.object).setProperties(properties).setRequired(List.of("projectLead"));
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

	private static JSONObjectAdapter uiDefinition(String json) {
		try {
			return new JSONObjectAdapterImpl(json);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
