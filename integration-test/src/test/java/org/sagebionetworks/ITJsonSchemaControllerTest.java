package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.AsynchJobType;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.GetValidationSchemaRequest;
import org.sagebionetworks.repo.model.schema.GetValidationSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaInfoResponse;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoRequest;
import org.sagebionetworks.repo.model.schema.ListJsonSchemaVersionInfoResponse;
import org.sagebionetworks.repo.model.schema.ListOrganizationsRequest;
import org.sagebionetworks.repo.model.schema.ListOrganizationsResponse;
import org.sagebionetworks.repo.model.schema.ListValidationResultsRequest;
import org.sagebionetworks.repo.model.schema.ListValidationResultsResponse;
import org.sagebionetworks.repo.model.schema.ObjectType;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ITJsonSchemaControllerTest {

	public static final long MAX_WAIT_MS = 1000 * 30;

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userId;

	String organizationName;
	String schemaName;
	CreateOrganizationRequest createOrganizationRequest;

	Organization organization;
	Project project;

	@BeforeAll
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		synapse = new SynapseClientImpl();
		userId = SynapseClientHelper.createUser(adminSynapse, synapse);
		SynapseClientHelper.setEndpoints(synapse);
	}

	@BeforeEach
	public void beforeEach() throws SynapseException {
		organizationName = "test.integeration.organization";
		schemaName = "integration.test.Schema.json";
		try {
			adminSynapse.deleteSchema(organizationName, schemaName);
		} catch (SynapseNotFoundException e) {
			// can ignore
		}
		createOrganizationRequest = new CreateOrganizationRequest();
		createOrganizationRequest.setOrganizationName(organizationName);
		// ensure we start each test without this organization.
		try {
			Organization org = synapse.getOrganizationByName(organizationName);
			adminSynapse.deleteOrganization(org.getId());
		} catch (SynapseNotFoundException e) {
			// can ignore
		}
	}

	@AfterEach
	public void afterEach() throws SynapseException {
		if (project != null) {
			synapse.deleteEntity(project, true);
		}
		try {
			adminSynapse.deleteSchema(organizationName, schemaName);
		} catch (SynapseNotFoundException e) {
			// can ignore
		}
		if (organization != null) {
			try {
				adminSynapse.deleteOrganization(organization.getId());
			} catch (SynapseNotFoundException e) {
				// can ignore
			}
		}
	}

	@Test
	public void testCreateOrganization() throws SynapseException {
		// call under test
		organization = synapse.createOrganization(createOrganizationRequest);
		assertNotNull(organization);
		assertEquals(organizationName, organization.getName());
		assertNotNull(organization.getId());
		assertEquals("" + userId, organization.getCreatedBy());
	}

	@Test
	public void testListOrganization() throws SynapseException {
		organization = synapse.createOrganization(createOrganizationRequest);
		assertNotNull(organization);
		ListOrganizationsRequest request = new ListOrganizationsRequest();
		// call under test
		ListOrganizationsResponse response = synapse.listOrganizations(request);
		assertNotNull(response);
		assertNotNull(response.getPage());
		assertTrue(response.getPage().size() > 0);
	}

	@Test
	public void testGetOrganizationByName() throws SynapseException {
		organization = synapse.createOrganization(createOrganizationRequest);
		assertNotNull(organization);
		// call under test
		Organization fetched = synapse.getOrganizationByName(organizationName);
		assertEquals(organization, fetched);
	}

	@Test
	public void testDeleteOrganization() throws SynapseException {
		organization = synapse.createOrganization(createOrganizationRequest);
		assertNotNull(organization);
		// call under test
		synapse.deleteOrganization(organization.getId());

		assertThrows(SynapseNotFoundException.class, () -> {
			synapse.getOrganizationByName(organizationName);
		});
	}

	@Test
	public void testGetOrganizationAcl() throws SynapseException {
		organization = synapse.createOrganization(createOrganizationRequest);
		assertNotNull(organization);

		// call under test
		AccessControlList acl = synapse.getOrganizationAcl(organization.getId());
		assertNotNull(acl);
	}

	@Test
	public void testUpdateOrganizationAcl() throws SynapseException {
		organization = synapse.createOrganization(createOrganizationRequest);
		assertNotNull(organization);
		AccessControlList acl = synapse.getOrganizationAcl(organization.getId());
		assertNotNull(acl);
		// grant public read
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		ra.setAccessType(Sets.newHashSet(ACCESS_TYPE.READ));
		acl.getResourceAccess().add(ra);
		// call under test
		AccessControlList resultAcl = synapse.updateOrganizationAcl(organization.getId(), acl);
		assertNotNull(resultAcl);
		// etag should have changed
		assertNotEquals(acl.getEtag(), resultAcl.getEtag());
	}

	@Test
	public void testCreateSchemaGetDeleteNullVersion() throws SynapseException, InterruptedException {
		organization = synapse.createOrganization(createOrganizationRequest);
		assertNotNull(organization);
		JsonSchema schema = new JsonSchema();
		schema.set$id(organizationName + "-" + schemaName);
		schema.setDescription("test without a version");
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(schema);
		// Call under test
		waitForSchemaCreate(request, (response) -> {
			assertNotNull(response);
			assertNotNull(response.getNewVersionInfo());
			assertEquals(organizationName, response.getNewVersionInfo().getOrganizationName());
			assertEquals(schemaName, response.getNewVersionInfo().getSchemaName());
		});
		String semanticVersion = null;
		// call under test
		JsonSchema fetched = synapse.getJsonSchema(organizationName, schemaName, semanticVersion);
		schema.set$id(JsonSchemaManager.createAbsolute$id(schema.get$id()));
		assertEquals(schema, fetched);
		// call under test
		synapse.deleteSchema(organizationName, schemaName);
	}

	@Test
	public void testGetValidationSchema() throws SynapseException, InterruptedException {
		organization = synapse.createOrganization(createOrganizationRequest);
		assertNotNull(organization);
		JsonSchema schema = new JsonSchema();
		schema.set$id(organizationName + "-" + schemaName);
		schema.setDescription("test without a version");
		CreateSchemaRequest createRequest = new CreateSchemaRequest();
		createRequest.setSchema(schema);
		waitForSchemaCreate(createRequest, (response) -> {
			assertNotNull(response);
			assertNotNull(response.getNewVersionInfo());
			assertEquals(organizationName, response.getNewVersionInfo().getOrganizationName());
			assertEquals(schemaName, response.getNewVersionInfo().getSchemaName());
		});

		GetValidationSchemaRequest getRequest = new GetValidationSchemaRequest();
		getRequest.set$id(schema.get$id());
		// call under test
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.GetValidationSchema, getRequest,
				(GetValidationSchemaResponse response) -> {
					assertNotNull(response);
					schema.set$id(JsonSchemaManager.createAbsolute$id(schema.get$id()));
					assertEquals(schema, response.getValidationSchema());
				}, MAX_WAIT_MS).getResponse();

		// call under test
		synapse.deleteSchema(organizationName, schemaName);
	}

	@Test
	public void testListJsonSchemas() throws SynapseException, InterruptedException {
		organization = synapse.createOrganization(createOrganizationRequest);
		assertNotNull(organization);
		JsonSchema schema = new JsonSchema();
		schema.set$id(organizationName + "-" + schemaName);
		schema.setDescription("test without a version");
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(schema);

		waitForSchemaCreate(request, (response) -> {
			assertNotNull(response);
		});

		ListJsonSchemaInfoRequest listRequest = new ListJsonSchemaInfoRequest();
		listRequest.setOrganizationName(organizationName);
		// call under test
		ListJsonSchemaInfoResponse result = synapse.listSchemaInfo(listRequest);
		assertNotNull(result);
		assertNotNull(result.getPage());
		assertTrue(result.getPage().size() > 0);
	}

	@Test
	public void testListJsonSchemaVersions() throws SynapseException, InterruptedException {
		organization = synapse.createOrganization(createOrganizationRequest);
		assertNotNull(organization);
		JsonSchema schema = new JsonSchema();
		schema.set$id(organizationName + "-" + schemaName);
		schema.setDescription("test without a version");
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(schema);

		waitForSchemaCreate(request, (response) -> {
			assertNotNull(response);
		});

		ListJsonSchemaVersionInfoRequest listRequest = new ListJsonSchemaVersionInfoRequest();
		listRequest.setOrganizationName(organizationName);
		listRequest.setSchemaName(schemaName);
		// call under test
		ListJsonSchemaVersionInfoResponse result = synapse.listSchemaVersions(listRequest);
		assertNotNull(result);
		assertNotNull(result.getPage());
		assertTrue(result.getPage().size() > 0);
	}

	@Test
	public void testCreateSchemaGetDeleteWithVersion() throws SynapseException, InterruptedException {
		organization = synapse.createOrganization(createOrganizationRequest);
		assertNotNull(organization);
		String semanticVersion = "1.45.67-alpha+beta";
		JsonSchema schema = new JsonSchema();
		schema.set$id(organizationName + "-" + schemaName + "-" + semanticVersion);
		schema.setDescription("test with a version");
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(schema);

		// Call under test
		waitForSchemaCreate(request, (response) -> {
			assertNotNull(response);
			assertNotNull(response.getNewVersionInfo());
			assertEquals(organizationName, response.getNewVersionInfo().getOrganizationName());
			assertEquals(schemaName, response.getNewVersionInfo().getSchemaName());
		});

		// call under test
		JsonSchema fetched = synapse.getJsonSchema(organizationName, schemaName, semanticVersion);
		schema.set$id(JsonSchemaManager.createAbsolute$id(schema.get$id()));
		assertEquals(schema, fetched);
		// call under test
		synapse.deleteSchema(organizationName, schemaName);
	}

	@Test
	public void testDeleteSchemaVersion() throws SynapseException, InterruptedException {
		organization = synapse.createOrganization(createOrganizationRequest);
		assertNotNull(organization);
		String semanticVersion = "1.45.0";
		JsonSchema schema = new JsonSchema();
		schema.set$id(organizationName + "-" + schemaName + "-" + semanticVersion);
		schema.setDescription("test with a version");
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(schema);

		// Call under test
		waitForSchemaCreate(request, (response) -> {
			assertNotNull(response);
		});
		schema.set$id(JsonSchemaManager.createAbsolute$id(schema.get$id()));
		JsonSchema fetched = synapse.getJsonSchema(organizationName, schemaName, semanticVersion);
		assertEquals(schema, fetched);
		// call under test
		synapse.deleteSchemaVersion(organizationName, schemaName, semanticVersion);
	}

	@Test
	public void bindSchemaToEntity() throws Exception {
		organization = synapse.createOrganization(createOrganizationRequest);
		assertNotNull(organization);
		JsonSchema schema = new JsonSchema();
		schema.set$id(organizationName + "-" + schemaName);
		schema.setDescription("schema to bind");
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(schema);

		// Call under test
		JsonSchemaVersionInfo versionInfo = waitForSchemaCreate(request, (response) -> {
			assertNotNull(response);
		}).getNewVersionInfo();


		project = new Project();
		project = synapse.createEntity(project);

		Folder folder = new Folder();
		folder.setName("child");
		folder.setParentId(project.getId());
		folder = synapse.createEntity(folder);

		// will bind the schema to the project
		BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
		bindRequest.setEntityId(project.getId());
		bindRequest.setSchema$id(schema.get$id());
		// Call under test
		JsonSchemaObjectBinding parentBinding = synapse.bindJsonSchemaToEntity(bindRequest);
		assertNotNull(parentBinding);
		assertEquals(versionInfo, parentBinding.getJsonSchemaVersionInfo());

		// the folder should inherit the binding
		// call under test
		JsonSchemaObjectBinding childBinding = synapse.getJsonSchemaBindingForEntity(folder.getId());
		assertNotNull(childBinding);
		assertEquals(parentBinding.getJsonSchemaVersionInfo(), childBinding.getJsonSchemaVersionInfo());
		
		Folder folderToCompare = synapse.getEntity(folder.getId(), Folder.class);
		
		// Wait for the folder to be valid.
		// call under test
		waitForValidationResults(folder.getId(), (ValidationResults t) -> {
			assertEquals(folderToCompare.getId(), t.getObjectId());
			assertEquals(folderToCompare.getEtag(), t.getObjectEtag());
			assertEquals(ObjectType.entity, t.getObjectType());
			assertTrue(t.getIsValid());
		});
		
		// call under test
		ValidationSummaryStatistics stats = synapse.getEntitySchemaValidationStatistics(project.getId());
		assertNotNull(stats);
		assertEquals(project.getId(), stats.getContainerId());
		assertEquals(new Long(1), stats.getTotalNumberOfChildren());
		
		ListValidationResultsRequest listRequest = new ListValidationResultsRequest();
		listRequest.setContainerId(project.getId());
		// call under test
		ListValidationResultsResponse listResponse = synapse.getInvalidValidationResults(listRequest);
		assertNotNull(listResponse);
		assertNotNull(listResponse.getPage());
		assertTrue(listResponse.getPage().isEmpty());

		// clear the binding
		// call under test
		synapse.clearSchemaBindingForEntity(project.getId());

		String folderId = folder.getId();
		assertThrows(SynapseNotFoundException.class, () -> {
			synapse.getJsonSchemaBindingForEntity(folderId);
		});
	}
	
	@Test
	public void testGetEntityJson() throws SynapseException {
		project = new Project();
		project = synapse.createEntity(project);
		// Call under test
		JSONObject projectJSON = synapse.getEntityJson(project.getId());
		assertNotNull(projectJSON);
		assertEquals(project.getName(), projectJSON.get("name"));
		assertEquals(project.getId(), projectJSON.get("id"));
	}
	
	@Test
	public void testUpdateEntityJson() throws SynapseException {
		project = new Project();
		project = synapse.createEntity(project);
		JSONObject projectJSON = synapse.getEntityJson(project.getId());
		assertNotNull(projectJSON);
		projectJSON.put("sample", "some value");
		// call under test
		JSONObject updatedJson = synapse.updateEntityJson(project.getId(), projectJSON);
		assertEquals("some value", updatedJson.getString("sample"));
		Annotations annos = synapse.getAnnotationsV2(project.getId());
		assertNotNull(annos);
		assertNotNull(annos.getAnnotations());
		AnnotationsValue value =  annos.getAnnotations().get("sample");
		assertNotNull(value);
		assertEquals(AnnotationsValueType.STRING, value.getType());
		assertEquals(Lists.newArrayList("some value"), value.getValue());
	}
	
	/**
	 * Wait for the validation results
	 * 
	 * @param user
	 * @param entityId
	 * @return
	 */
	public ValidationResults waitForValidationResults(String entityId,
			Consumer<ValidationResults> consumer) {
		try {
			return TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
				try {
					ValidationResults validationResults = synapse.getEntityValidationResults(entityId);
					consumer.accept(validationResults);
					return new Pair<>(Boolean.TRUE, validationResults);
				} catch (Throwable e) {
					System.out.println("Waiting for expected ValidationResults..." + e.getMessage());
					return new Pair<>(Boolean.FALSE, null);
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	/**
	 * Wait for the schema to be created.
	 * 
	 * @param request
	 * @return
	 * @throws SynapseException
	 * @throws InterruptedException
	 */
	CreateSchemaResponse waitForSchemaCreate(CreateSchemaRequest request, Consumer<CreateSchemaResponse> resultConsumer)
			throws SynapseException, InterruptedException {
		return AsyncJobHelper
				.assertAysncJobResult(synapse, AsynchJobType.CreateJsonSchema, request, resultConsumer, MAX_WAIT_MS)
				.getResponse();
	}
}
