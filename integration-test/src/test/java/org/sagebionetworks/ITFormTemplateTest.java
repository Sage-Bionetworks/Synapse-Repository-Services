package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateField;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateSearchRequest;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateSearchResponse;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateStep;
import org.sagebionetworks.repo.model.dataaccess.schema.SubmissionContext;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

@ExtendWith(ITTestExtension.class)
public class ITFormTemplateTest {

	private static final long MAX_WAIT_MS = 1000 * 30;
	private static final String ORGANIZATION_NAME = "test.form.template.organization";
	private static final String SCHEMA_NAME = "integration.test.FormTemplateSchema";
	private static final String SCHEMA_VERSION = "1.0.0";
	private static final String SCHEMA_ID = ORGANIZATION_NAME + "-" + SCHEMA_NAME + "-" + SCHEMA_VERSION;

	private final SynapseAdminClient adminSynapse;
	private final SynapseClient synapse;

	/**
	 * Templates cannot be deleted and their names are unique, so every test names its templates with
	 * a prefix that is unique to the run.
	 */
	private String namePrefix;

	public ITFormTemplateTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}

	@BeforeEach
	public void before() throws SynapseException, InterruptedException {
		namePrefix = UUID.randomUUID().toString();
		deleteSchemaAndOrganization();

		synapse.createOrganization(new CreateOrganizationRequest().setOrganizationName(ORGANIZATION_NAME));
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.CreateJsonSchema,
				new CreateSchemaRequest().setSchema(newJsonSchema()),
				(CreateSchemaResponse response) -> assertNotNull(response.getNewVersionInfo()), MAX_WAIT_MS);
	}

	@AfterEach
	public void after() throws SynapseException {
		deleteSchemaAndOrganization();
	}

	@Test
	public void testCreateFormTemplate() throws Exception {
		FormTemplate template = newTemplate("NF Standard DAR");

		// call under test
		FormTemplate created = adminSynapse.createFormTemplate(template);

		assertNotNull(created.getId());
		assertNotNull(created.getEtag());
		assertEquals(1L, created.getVersionNumber().longValue());
		assertEquals(template.getName(), created.getName());
		assertEquals(SCHEMA_ID, created.getSchema$id());
		assertEquals(template.getSteps().size(), created.getSteps().size());

		// call under test
		assertTemplateEquals(created, synapse.getFormTemplate(created.getId()));

		// call under test
		assertTemplateEquals(created, synapse.getFormTemplateVersion(created.getId(), 1L));
	}


	@Test
	public void testCreateFormTemplateVersion() throws Exception {
		FormTemplate first = adminSynapse.createFormTemplate(newTemplate("NF Standard DAR"));
		FormTemplate secondBody = newTemplate("NF Standard DAR, revised").setId(first.getId())
				.setEtag(first.getEtag());
		secondBody.getSteps().get(0).setTitle("The project, revised");

		// call under test
		FormTemplate second = adminSynapse.createFormTemplateVersion(secondBody);

		assertEquals(2L, second.getVersionNumber().longValue());
		assertNotEquals(first.getEtag(), second.getEtag());
		assertEquals(secondBody.getName(), second.getName());
		assertEquals("The project, revised", second.getSteps().get(0).getTitle());

		// The latest version is the one that was just published.
		assertTemplateEquals(second, synapse.getFormTemplate(first.getId()));

		// The first version is immutable, apart from reporting the current etag.
		assertTemplateEquals(first.setEtag(second.getEtag()),
				synapse.getFormTemplateVersion(first.getId(), 1L));
	}

	@Test
	public void testSearchFormTemplates() throws SynapseException {
		FormTemplate nfOne = adminSynapse.createFormTemplate(newTemplate("NF Standard DAR"));
		FormTemplate nfTwo = adminSynapse.createFormTemplate(newTemplate("NF Renewal DAR"));
		FormTemplate adOne = adminSynapse.createFormTemplate(newTemplate("AD Standard DAR"));

		// call under test
		FormTemplateSearchResponse all = synapse
				.searchFormTemplates(new FormTemplateSearchRequest().setName(namePrefix));

		assertNull(all.getNextPageToken());
		assertEquals(List.of(nfOne.getId(), nfTwo.getId(), adOne.getId()), idsOf(all));

		// call under test — the name filter matches a substring without regard to case
		FormTemplateSearchResponse nfOnly = synapse
				.searchFormTemplates(new FormTemplateSearchRequest().setName(namePrefix + " nf "));

		assertEquals(List.of(nfOne.getId(), nfTwo.getId()), idsOf(nfOnly));
	}

	@Test
	public void testSearchFormTemplatesWithDeprecated() throws SynapseException {
		FormTemplate active = adminSynapse.createFormTemplate(newTemplate("NF Standard DAR"));
		FormTemplate retired = adminSynapse.createFormTemplate(newTemplate("NF Retired DAR"));
		// A template is retired by publishing a version of it that is deprecated.
		adminSynapse.createFormTemplateVersion(newTemplate("NF Retired DAR").setId(retired.getId())
				.setEtag(retired.getEtag()).setDeprecated(true));

		// call under test
		FormTemplateSearchResponse activeOnly = synapse
				.searchFormTemplates(new FormTemplateSearchRequest().setName(namePrefix));

		assertEquals(List.of(active.getId()), idsOf(activeOnly));

		// call under test
		FormTemplateSearchResponse everything = synapse.searchFormTemplates(
				new FormTemplateSearchRequest().setName(namePrefix).setIncludeDeprecated(true));

		assertEquals(List.of(active.getId(), retired.getId()), idsOf(everything));
	}

	private static List<String> idsOf(FormTemplateSearchResponse response) {
		return response.getResults().stream().map(FormTemplate::getId).toList();
	}

	/**
	 * The uiDefinition of a field is an arbitrary JSON object which does not compare by value, so the
	 * templates are compared by their serialized form.
	 */
	private static void assertTemplateEquals(FormTemplate expected, FormTemplate actual)
			throws JSONObjectAdapterException {
		assertEquals(EntityFactory.createJSONStringForEntity(expected),
				EntityFactory.createJSONStringForEntity(actual));
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

		return new JsonSchema().set$id(SCHEMA_ID).setDescription("A form template integration test schema")
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
