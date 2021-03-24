package org.sagebionetworks.schema.worker;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaValidationManager;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
import org.sagebionetworks.repo.manager.schema.SynapseSchemaBootstrap;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.GetValidationSchemaRequest;
import org.sagebionetworks.repo.model.schema.GetValidationSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaConstants;
import org.sagebionetworks.repo.model.schema.ObjectType;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class JsonSchemaWorkerIntegrationTest {

	public static final long MAX_WAIT_MS = 1000 * 30;

	@Autowired
	AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;

	@Autowired
	JsonSchemaManager jsonSchemaManager;

	@Autowired
	private SynapseSchemaBootstrap schemaBootstrap;

	@Autowired
	UserManager userManager;

	@Autowired
	JsonSchemaValidationManager jsonSchemaValidationManager;

	@Autowired
	EntityManager entityManager;

	UserInfo adminUserInfo;
	String organizationName;
	String schemaName;
	String semanticVersion;
	JsonSchema basicSchema;
	Organization organization;

	String projectId;

	@BeforeEach
	public void before() {
		jsonSchemaManager.truncateAll();
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		organizationName = "my.org.net";
		schemaName = "some.schema";
		semanticVersion = "1.1.1";
		CreateOrganizationRequest createOrgRequest = new CreateOrganizationRequest();
		createOrgRequest.setOrganizationName(organizationName);
		organization = jsonSchemaManager.createOrganziation(adminUserInfo, createOrgRequest);
		basicSchema = new JsonSchema();
		basicSchema.set$id(organizationName + JsonSchemaConstants.PATH_DELIMITER + schemaName
				+ JsonSchemaConstants.VERSION_PRFIX + semanticVersion);
		basicSchema.setDescription("basic schema for integration test");
	}

	@AfterEach
	public void after() {
		jsonSchemaManager.truncateAll();
		if (projectId != null) {
			entityManager.deleteEntity(adminUserInfo, projectId);
		}
	}

	@Test
	public void testCreateSchema() throws InterruptedException, AssertionError, AsynchJobFailedException {
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(basicSchema);

		asynchronousJobWorkerHelper.assertJobResponse(adminUserInfo, request, (CreateSchemaResponse response) -> {
			assertNotNull(response);
			assertNotNull(response.getNewVersionInfo());
			assertEquals(adminUserInfo.getId().toString(), response.getNewVersionInfo().getCreatedBy());
			assertEquals(semanticVersion, response.getNewVersionInfo().getSemanticVersion());
		}, MAX_WAIT_MS);

		jsonSchemaManager.deleteSchemaById(adminUserInfo, basicSchema.get$id());
		assertThrows(NotFoundException.class, () -> {
			jsonSchemaManager.deleteSchemaById(adminUserInfo, basicSchema.get$id());
		});
	}

	@Test
	public void testCreateSchemaCycle() throws InterruptedException, AssertionError, AsynchJobFailedException {
		// one
		JsonSchema one = createSchema(organizationName, "one");
		one.setDescription("no cycle yet");
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(one);

		asynchronousJobWorkerHelper.assertJobResponse(adminUserInfo, request, (CreateSchemaResponse response) -> {
			assertNotNull(response);
		}, MAX_WAIT_MS);

		// two
		JsonSchema refToOne = create$RefSchema(one);
		JsonSchema two = createSchema(organizationName, "two");
		two.setDescription("depends on one");
		two.setItems(refToOne);
		request = new CreateSchemaRequest();
		request.setSchema(two);

		asynchronousJobWorkerHelper.assertJobResponse(adminUserInfo, request, (CreateSchemaResponse response) -> {
			assertNotNull(response);
		}, MAX_WAIT_MS);

		// update one to depend on two
		one.setItems(create$RefSchema(two));
		one.setDescription("now has a cycle");
		CreateSchemaRequest cycleRequest = new CreateSchemaRequest();
		cycleRequest.setSchema(one);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			asynchronousJobWorkerHelper.assertJobResponse(adminUserInfo, cycleRequest,
					(CreateSchemaResponse response) -> {
						fail("Should have not receive a response");
					}, MAX_WAIT_MS);
		}).getMessage();

		assertEquals("Schema $id: 'my.org.net-one' has a circular dependency", message);
	}

	public CreateSchemaResponse registerSchemaFromClasspath(String name) throws Exception {
		String json = loadStringFromClasspath(name);
		JsonSchema schema = EntityFactory.createEntityFromJSONString(json, JsonSchema.class);
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(schema);
		System.out.println("Creating schema: '" + schema.get$id() + "'");
		return asynchronousJobWorkerHelper
				.assertJobResponse(adminUserInfo, request, (CreateSchemaResponse response) -> {
					assertNotNull(response);
					System.out.println(response.getNewVersionInfo());
				}, MAX_WAIT_MS).getResponse();
	}

	/**
	 * Load the file contents from the classpath.
	 * 
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public String loadStringFromClasspath(String name) throws Exception {
		try (InputStream in = JsonSchemaWorkerIntegrationTest.class.getClassLoader().getResourceAsStream(name);) {
			if (in == null) {
				throw new IllegalArgumentException("Cannot find: '" + name + "' on the classpath");
			}
			return IOUtils.toString(in, "UTF-8");
		}
	}

	@Test
	public void testMainUseCase() throws Exception {
		bootstrapAndCreateOrganization();
		String[] schemasToRegister = { "pets/PetType.json", "pets/Pet.json", "pets/CatBreed.json", "pets/DogBreed.json",
				"pets/Cat.json", "pets/Dog.json", "pets/PetPhoto.json" };
		for (String fileName : schemasToRegister) {
			registerSchemaFromClasspath(fileName);
		}

		JsonSchema validationSchema = jsonSchemaManager.getValidationSchema("my.organization-pets.PetPhoto");
		assertNotNull(schemaBootstrap);
		printJson(validationSchema);
		assertNotNull(validationSchema.getDefinitions());
		assertTrue(validationSchema.getDefinitions().containsKey("my.organization-pets.PetType-1.0.1"));
		assertTrue(validationSchema.getDefinitions().containsKey("my.organization-pets.Pet"));
		assertTrue(validationSchema.getDefinitions().containsKey("my.organization-pets.Pet-1.0.3"));
		assertTrue(validationSchema.getDefinitions().containsKey("my.organization-pets.dog.Breed"));
		assertTrue(validationSchema.getDefinitions().containsKey("my.organization-pets.cat.Breed"));
		assertTrue(validationSchema.getDefinitions().containsKey("my.organization-pets.cat.Cat"));
		assertTrue(validationSchema.getDefinitions().containsKey("my.organization-pets.dog.Dog"));
		assertTrue(validationSchema.getDefinitions().containsKey("org.sagebionetworks-repo.model.Entity-1.0.0"));
		assertTrue(validationSchema.getDefinitions().containsKey("org.sagebionetworks-repo.model.Versionable-1.0.0"));
		assertTrue(validationSchema.getDefinitions()
				.containsKey("org.sagebionetworks-repo.model.VersionableEntity-1.0.0"));
		assertTrue(validationSchema.getDefinitions().containsKey("org.sagebionetworks-repo.model.FileEntity-1.0.0"));

		String validCatJsonString = loadStringFromClasspath("pets/ValidCat.json");
		JSONObject validCat = new JSONObject(validCatJsonString);
		JsonSubject mockSubject = Mockito.mock(JsonSubject.class);
		when(mockSubject.toJson()).thenReturn(validCat);
		// this schema should be valid
		ValidationResults result = jsonSchemaValidationManager.validate(validationSchema, mockSubject);
		assertNotNull(result);
		assertTrue(result.getIsValid());

		// Changing the petType to dog should cause a schema violation.
		validCat.put("petType", "dog");
		result = jsonSchemaValidationManager.validate(validationSchema, mockSubject);
		assertNotNull(result);
		assertFalse(result.getIsValid());
		assertEquals("#: 0 subschemas matched instead of one", result.getValidationErrorMessage());
		printJson(result);
	}

	void bootstrapAndCreateOrganization() throws RecoverableMessageException {
		jsonSchemaManager.truncateAll();
		schemaBootstrap.bootstrapSynapseSchemas();
		CreateOrganizationRequest createOrgRequest = new CreateOrganizationRequest();
		createOrgRequest.setOrganizationName("my.organization");
		organization = jsonSchemaManager.createOrganziation(adminUserInfo, createOrgRequest);
	}

	@Test
	public void testGetValidationSchemaWorker() throws AssertionError, AsynchJobFailedException {
		CreateSchemaRequest createRequest = new CreateSchemaRequest();
		createRequest.setSchema(basicSchema);

		// First create the schema.
		asynchronousJobWorkerHelper.assertJobResponse(adminUserInfo, createRequest, (CreateSchemaResponse response) -> {
			assertNotNull(response);
			assertNotNull(response.getNewVersionInfo());
			assertEquals(adminUserInfo.getId().toString(), response.getNewVersionInfo().getCreatedBy());
			assertEquals(semanticVersion, response.getNewVersionInfo().getSemanticVersion());
		}, MAX_WAIT_MS);

		GetValidationSchemaRequest getRequest = new GetValidationSchemaRequest();
		getRequest.set$id(basicSchema.get$id());
		// Get the validation schema for this schema
		asynchronousJobWorkerHelper.assertJobResponse(adminUserInfo, getRequest,
				(GetValidationSchemaResponse response) -> {
					assertNotNull(response);
					assertNotNull(response.getValidationSchema());
					// the absolute $id should be returned: PLFM-6515
					basicSchema.set$id(JsonSchemaManager.createAbsolute$id("my.org.net-some.schema-1.1.1"));
					assertEquals(basicSchema, response.getValidationSchema());
				}, MAX_WAIT_MS);
	}

	@Test
	public void testEntitySchemaValidation() throws Exception {
		bootstrapAndCreateOrganization();
		String projectId = entityManager.createEntity(adminUserInfo, new Project(), null);
		Project project = entityManager.getEntity(adminUserInfo, projectId, Project.class);

		// create the schema
		String fileName = "schema/SimpleFolder.json";
		CreateSchemaResponse createResponse = registerSchemaFromClasspath(fileName);
		String schema$id = createResponse.getNewVersionInfo().get$id();
		// bind the schema to the project
		BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
		bindRequest.setEntityId(projectId);
		bindRequest.setSchema$id(schema$id);
		entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

		// add a folder to the project
		Folder folder = new Folder();
		folder.setParentId(project.getId());
		String folderId = entityManager.createEntity(adminUserInfo, folder, null);
		JSONObject folderJson = entityManager.getEntityJson(folderId);
		// Add the foo annotation to the folder
		folderJson.put("foo", "bar");
		folderJson = entityManager.updateEntityJson(adminUserInfo, folderId, folderJson);
		Folder resultFolder = entityManager.getEntity(adminUserInfo, folderId, Folder.class);

		// wait for the folder to be valid.
		waitForValidationResults(adminUserInfo, folderId, (ValidationResults t) -> {
			assertNotNull(t);
			assertTrue(t.getIsValid());
			assertEquals(JsonSchemaManager.createAbsolute$id(schema$id), t.getSchema$id());
			assertEquals(resultFolder.getId(), t.getObjectId());
			assertEquals(ObjectType.entity, t.getObjectType());
			assertEquals(resultFolder.getEtag(), t.getObjectEtag());
		});

		// Removing the binding from the container should trigger removal of the results
		// for the child.
		entityManager.clearBoundSchema(adminUserInfo, projectId);

		waitForValidationResultsToBeNotFound(adminUserInfo, folderId);
	}

	@Test
	public void testCreateWithDryRun() throws Exception {
		CreateSchemaRequest createRequest = new CreateSchemaRequest();
		createRequest.setSchema(basicSchema);
		createRequest.setDryRun(true);

		// First create the schema.
		asynchronousJobWorkerHelper.assertJobResponse(adminUserInfo, createRequest, (CreateSchemaResponse response) -> {
			assertNotNull(response);
			assertNotNull(response.getNewVersionInfo());
			assertEquals(adminUserInfo.getId().toString(), response.getNewVersionInfo().getCreatedBy());
			assertEquals(semanticVersion, response.getNewVersionInfo().getSemanticVersion());
		}, MAX_WAIT_MS);

		// the schema should not exist
		assertThrows(NotFoundException.class, () -> {
			jsonSchemaManager.getSchema(basicSchema.get$id(), true);
		});
	}
	
	@Test
	public void testEntitySchemaValidationWithBoolean() throws Exception {
		bootstrapAndCreateOrganization();
		String projectId = entityManager.createEntity(adminUserInfo, new Project(), null);
		Project project = entityManager.getEntity(adminUserInfo, projectId, Project.class);

		// create the schema
		String fileName = "schema/FolderWithBoolean.json";
		CreateSchemaResponse createResponse = registerSchemaFromClasspath(fileName);
		String schema$id = createResponse.getNewVersionInfo().get$id();
		// bind the schema to the project
		BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
		bindRequest.setEntityId(projectId);
		bindRequest.setSchema$id(schema$id);
		entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

		// add a folder to the project
		Folder folder = new Folder();
		folder.setParentId(project.getId());
		String folderId = entityManager.createEntity(adminUserInfo, folder, null);
		JSONObject folderJson = entityManager.getEntityJson(folderId);
		// Add the foo annotation to the folder
		folderJson.put("hasBoolean", "true");
		folderJson = entityManager.updateEntityJson(adminUserInfo, folderId, folderJson);
		Folder resultFolder = entityManager.getEntity(adminUserInfo, folderId, Folder.class);

		// wait for the folder to be valid.
		waitForValidationResults(adminUserInfo, folderId, (ValidationResults t) -> {
			assertNotNull(t);
			assertTrue(t.getIsValid());
			assertEquals(JsonSchemaManager.createAbsolute$id(schema$id), t.getSchema$id());
			assertEquals(resultFolder.getId(), t.getObjectId());
			assertEquals(ObjectType.entity, t.getObjectType());
			assertEquals(resultFolder.getEtag(), t.getObjectEtag());
		});

		// Removing the binding from the container should trigger removal of the results
		// for the child.
		entityManager.clearBoundSchema(adminUserInfo, projectId);

		waitForValidationResultsToBeNotFound(adminUserInfo, folderId);
	}

	/**
	 * Wait for the validation results
	 * 
	 * @param user
	 * @param entityId
	 * @return
	 */
	public ValidationResults waitForValidationResults(UserInfo user, String entityId,
			Consumer<ValidationResults> consumer) {
		try {
			return TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
				try {
					ValidationResults validationResults = entityManager.getEntityValidationResults(user, entityId);
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
	 * Wait for the validation results to be not found.
	 * 
	 * @param user
	 * @param entityId
	 */
	public void waitForValidationResultsToBeNotFound(UserInfo user, String entityId) {
		try {
			TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
				try {
					ValidationResults validationResults = entityManager.getEntityValidationResults(user, entityId);
					System.out.println("Waiting for expected ValidationResults to be removed...");
					return new Pair<>(Boolean.FALSE, null);
				} catch (NotFoundException e) {
					return new Pair<>(Boolean.TRUE, null);
				}
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void printJson(JSONEntity entity) throws JSONException, JSONObjectAdapterException {
		JSONObject object = new JSONObject(EntityFactory.createJSONStringForEntity(entity));
		System.out.println(object.toString(5));
	}

	/**
	 * Helper to create a schema with the given $id.
	 * 
	 * @param $id
	 * @return
	 */
	public JsonSchema createSchema(String organizationName, String schemaName) {
		JsonSchema schema = new JsonSchema();
		schema.set$id(organizationName + JsonSchemaConstants.PATH_DELIMITER + schemaName);
		return schema;
	}

	/**
	 * Helper to create a $ref to the given schema
	 * 
	 * @param toRef
	 * @return
	 */
	public JsonSchema create$RefSchema(JsonSchema toRef) {
		JsonSchema schema = new JsonSchema();
		schema.set$ref(toRef.get$id());
		return schema;
	}
}
