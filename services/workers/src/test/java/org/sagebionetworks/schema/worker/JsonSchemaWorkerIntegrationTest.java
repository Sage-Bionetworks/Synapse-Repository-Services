package org.sagebionetworks.schema.worker;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.sagebionetworks.repo.model.schema.Type;
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

	public static final long MAX_WAIT_MS = 1000 * 80;

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

	public CreateSchemaResponse registerSchema(JsonSchema schema) throws Exception {
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(schema);
		System.out.println("Creating schema: '" + schema.get$id() + "'");
		return asynchronousJobWorkerHelper
				.assertJobResponse(adminUserInfo, request, (CreateSchemaResponse response) -> {
					assertNotNull(response);
					System.out.println(response.getNewVersionInfo());
				}, MAX_WAIT_MS).getResponse();
	}
	
	public JsonSchema getSchemaFromClasspath(String name) throws Exception {
		String json = loadStringFromClasspath(name);
		return EntityFactory.createEntityFromJSONString(json, JsonSchema.class);
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
		bootstrapAndCreateOrganization("my.organization");
		String[] schemasToRegister = { "pets/PetType.json", "pets/Pet.json", "pets/CatBreed.json", "pets/DogBreed.json",
				"pets/Cat.json", "pets/Dog.json", "pets/PetPhoto.json" };
		for (String fileName : schemasToRegister) {
			JsonSchema schema = getSchemaFromClasspath(fileName);
			registerSchema(schema);
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

	void bootstrapAndCreateOrganization(String organizationName) throws RecoverableMessageException, InterruptedException {
		jsonSchemaManager.truncateAll();
		schemaBootstrap.bootstrapSynapseSchemas();
		CreateOrganizationRequest createOrgRequest = new CreateOrganizationRequest();
		createOrgRequest.setOrganizationName(organizationName);
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
		bootstrapAndCreateOrganization("my.organization");
		String projectId = entityManager.createEntity(adminUserInfo, new Project(), null);
		Project project = entityManager.getEntity(adminUserInfo, projectId, Project.class);

		// create the schema
		String fileName = "schema/SimpleFolder.json";
		JsonSchema schema = getSchemaFromClasspath(fileName);
		CreateSchemaResponse createResponse = registerSchema(schema);
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
		bootstrapAndCreateOrganization("my.organization");
		String projectId = entityManager.createEntity(adminUserInfo, new Project(), null);
		Project project = entityManager.getEntity(adminUserInfo, projectId, Project.class);

		// create the schema
		String fileName = "schema/FolderWithBoolean.json";
		JsonSchema schema = getSchemaFromClasspath(fileName);
		CreateSchemaResponse createResponse = registerSchema(schema);
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
	
	@Test
	public void testNoSemanticVersionSchemaRevalidationWithSchemaChange() throws Exception {
		// PLFM-6757
		bootstrapAndCreateOrganization("my.organization");
		String projectId = entityManager.createEntity(adminUserInfo, new Project(), null);
		Project project = entityManager.getEntity(adminUserInfo, projectId, Project.class);
		
		// create the schema with no semantic version
		String fileName = "schema/FolderWithBoolean.json";
		JsonSchema schema1 = getSchemaFromClasspath(fileName);
		CreateSchemaResponse createResponse = registerSchema(schema1);
		String schema$id1 = createResponse.getNewVersionInfo().get$id();
		
		// bind the schema to the project
		BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
		bindRequest.setEntityId(projectId);
		bindRequest.setSchema$id(schema$id1);
		/*
		 * Note that we prohibit notification messages being sent on the bind because
		 * multiple messages will be sent out in this set up that will trigger validation, but
		 * the validation triggered from this bind in particular will be delayed and can cause this test
		 * to pass when it is not suppose to pass.
		 */
		boolean sendNotificationMessages = false;
		entityManager.bindSchemaToEntity(adminUserInfo, bindRequest, sendNotificationMessages);
		
		// add a folder to the project
		Folder folder = new Folder();
		folder.setParentId(project.getId());
		String folderId = entityManager.createEntity(adminUserInfo, folder, null);
		JSONObject folderJson = entityManager.getEntityJson(folderId);
		// Add the foo annotation to the folder
		folderJson.put("hasBoolean", "true");
		folderJson = entityManager.updateEntityJson(adminUserInfo, folderId, folderJson);
		Folder resultFolder = entityManager.getEntity(adminUserInfo, folderId, Folder.class);
		
		// wait for the folder to be valid against the schema
		waitForValidationResults(adminUserInfo, folderId, (ValidationResults t) -> {
			assertNotNull(t);
			assertTrue(t.getIsValid());
			assertEquals(JsonSchemaManager.createAbsolute$id(schema$id1), t.getSchema$id());
			assertEquals(resultFolder.getId(), t.getObjectId());
			assertEquals(ObjectType.entity, t.getObjectType());
			assertEquals(resultFolder.getEtag(), t.getObjectEtag());
		});
		
		/*
		 * Wait 5 seconds for the possibility of lingering validation work to finish up.
		 * Note that if we comment out the solution to PLFM-6757
		 * then this test consistently fails on timeout from the 2nd wait. In other words,
		 * any lingering revalidation work in-progress or to-be-in-progress before this,
		 * is unlikely to be the reason this test will pass.
		 */
		Thread.sleep(5000);
		
		// Revalidation step. Replace the schema with the same schema + an additional required field.
		JsonSchema schema2 = getSchemaFromClasspath(fileName);
		schema2.setRequired(Arrays.asList("requiredField"));
		CreateSchemaResponse createResponse2 = registerSchema(schema2);
		String schema$id2 = createResponse2.getNewVersionInfo().get$id();
		
		// Should be the same schema being referenced
		assertEquals(schema$id1, schema$id2);
		
		// wait for the folder to be invalid against the schema
		waitForValidationResults(adminUserInfo, folderId, (ValidationResults t) -> {
			assertNotNull(t);
			assertFalse(t.getIsValid());
			assertEquals(JsonSchemaManager.createAbsolute$id(schema$id2), t.getSchema$id());
			assertEquals(resultFolder.getId(), t.getObjectId());
			assertEquals(ObjectType.entity, t.getObjectType());
			assertEquals(resultFolder.getEtag(), t.getObjectEtag());
		});
		
		// clean up
		entityManager.clearBoundSchema(adminUserInfo, projectId);
		waitForValidationResultsToBeNotFound(adminUserInfo, folderId);
	}
	
	@Test
	public void testGetEntityJsonWithBoundJsonSchema() throws Exception {
		// PLFM-6811
		// this test helps demonstrate how annotations are driven by the JsonSchema
		String projectId = entityManager.createEntity(adminUserInfo, new Project(), null);
		Project project = entityManager.getEntity(adminUserInfo, projectId, Project.class);
		// build properties with fooKey single
		Map<String, JsonSchema> properties = new HashMap<>();
		JsonSchema fooType = new JsonSchema();
		fooType.setType(Type.string);
		properties.put("fooKey", fooType);
		basicSchema.setProperties(properties);
		// build a subschema in an allOf with barKey array
		List<JsonSchema> allOfSchemas = new LinkedList<>();
		Map<String, JsonSchema> properties2 = new HashMap<>();
		JsonSchema subSchema = new JsonSchema();
		JsonSchema typeSchemaArray2 = new JsonSchema();
		JsonSchema itemsSchemaArray2 = new JsonSchema();
		itemsSchemaArray2.setType(Type.string);
		typeSchemaArray2.setType(Type.array);
		typeSchemaArray2.setItems(itemsSchemaArray2);
		properties2.put("barKey", typeSchemaArray2);
		subSchema.setProperties(properties2);
		allOfSchemas.add(subSchema);
		basicSchema.setAllOf(allOfSchemas);
		/* 
		 * this is the basicSchema
		 * {
		 * 	"properties": 
		 * 	{ 
		 * 		"fooKey": { "type": "string" }
		 * 	},
		 * 	"allOf": [ { "properties": { "barKey": { "type": "array", "items": { "type": "number" } } } } ]
		 * }
		 */

		// create the schema
		CreateSchemaResponse createResponse = registerSchema(basicSchema);
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
		
		// Add 3 single element array annotations to the folder
		folderJson.put("fooKey", Arrays.asList("foo"));
		folderJson.put("barKey", Arrays.asList("bar"));
		folderJson.put("bazKey", Arrays.asList("baz"));
		folderJson = entityManager.updateEntityJson(adminUserInfo, folderId, folderJson);
		
		waitForValidationResults(adminUserInfo, folderId, (ValidationResults t) -> {
			assertNotNull(t);
			assertTrue(t.getIsValid());
		});
		
		// call under test, will throw an org.json.JSONException if
		// it is not a JSONArray
		folderJson = entityManager.getEntityJson(folderId);
		// barKey is defined as an array in a subschema, it will stay as an array
		assertEquals(folderJson.getJSONArray("barKey").getString(0), "bar");
		// bazKey is not defined in the schema, so it defaults to an array
		assertEquals(folderJson.getJSONArray("bazKey").getString(0), "baz");
		// fooKey is defined as a single in the schema, it will become a single
		assertEquals(folderJson.getString("fooKey"), "foo");
		
		// clean up
		entityManager.clearBoundSchema(adminUserInfo, projectId);
		waitForValidationResultsToBeNotFound(adminUserInfo, folderId);
	}
	
	@Test
	public void testGetEntityJsonWithBoundSchemaContainingReference() throws Exception {
		// PLFM-6934
		String projectId = entityManager.createEntity(adminUserInfo, new Project(), null);
		Project project = entityManager.getEntity(adminUserInfo, projectId, Project.class);
		
		// child schema, key property of with enum
		JsonSchema child = new JsonSchema();
		child.set$id(organizationName + JsonSchemaConstants.PATH_DELIMITER + "child");
		child.setType(Type.string);
		child.set_enum(Arrays.asList("Alabama", "Alaska"));
		
		// reference to child schema
		JsonSchema refToChild = new JsonSchema();
		refToChild.set$ref(child.get$id());
		
		// parent contains a reference to the child
		JsonSchema parent = new JsonSchema();
		parent.set$id(organizationName + JsonSchemaConstants.PATH_DELIMITER + "parent");
		Map<String, JsonSchema> parentProps = new HashMap<>();
		parentProps.put("state", refToChild);
		parent.setProperties(parentProps);
		parent.setRequired(Arrays.asList("state"));

		// create the schemas
		registerSchema(child);
		CreateSchemaResponse createResponse = registerSchema(parent);
		String schema$id = createResponse.getNewVersionInfo().get$id();
		
		// bind the schema to the project
		BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
		bindRequest.setEntityId(projectId);
		bindRequest.setSchema$id(schema$id);
		boolean sendNotificationMessages = false;
		entityManager.bindSchemaToEntity(adminUserInfo, bindRequest, sendNotificationMessages);
		
		// add a folder to the project
		Folder folder = new Folder();
		folder.setParentId(project.getId());
		String folderId = entityManager.createEntity(adminUserInfo, folder, null);
		JSONObject folderJson = entityManager.getEntityJson(folderId);
		
		folderJson.put("state", Arrays.asList("Alabama"));
		folderJson = entityManager.updateEntityJson(adminUserInfo, folderId, folderJson);
		
		// wait till it is valid
		waitForValidationResults(adminUserInfo, folderId, (ValidationResults t) -> {
			assertNotNull(t);
			assertTrue(t.getIsValid());
		});
		
		// call under test
		// should not be an array
		folderJson = entityManager.getEntityJson(folderId);
		assertEquals("Alabama", folderJson.getString("state"));
		
		// clean up
		entityManager.clearBoundSchema(adminUserInfo, projectId);
		waitForValidationResultsToBeNotFound(adminUserInfo, folderId);
	}
	
	@Test
	public void testValidationSchemaIndexWithReindexingAndRevalidation() throws Exception {
		// PLFM-6870: Validate that when a schema is changed, the validation schema index is re-indexed for
		// all schemas that reference the changed schema as well as these schemas that reference those schemas,
		// and that this also triggers revalidation of entities
		
		// child schema, key property of type string
		JsonSchema child = new JsonSchema();
		child.set$id(organizationName + JsonSchemaConstants.PATH_DELIMITER + "child");
		Map<String, JsonSchema> properties = new HashMap<>();
		JsonSchema typeSchema = new JsonSchema();
		typeSchema.setType(Type.string);
		properties.put("key", typeSchema);
		child.setProperties(properties);
		// reference to child schema
		JsonSchema refToChild = new JsonSchema();
		refToChild.set$ref(child.get$id());
		// parent contains a reference to the child
		JsonSchema parent = new JsonSchema();
		parent.set$id(organizationName + JsonSchemaConstants.PATH_DELIMITER + "parent");
		parent.setAllOf(Arrays.asList(refToChild));
		
		// create project
		String projectId = entityManager.createEntity(adminUserInfo, new Project(), null);
		Project project = entityManager.getEntity(adminUserInfo, projectId, Project.class);

		// create child schema
		CreateSchemaResponse createResponse = registerSchema(child);
		// create parent schema
		createResponse = registerSchema(parent);
		// bind parent schema to the project
		String parentSchema$id = createResponse.getNewVersionInfo().get$id();
		BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
		bindRequest.setEntityId(projectId);
		bindRequest.setSchema$id(parentSchema$id);
		// we want to validate on putting annotations, so don't send notifications
		boolean sendNotificationMessages = false;
		entityManager.bindSchemaToEntity(adminUserInfo, bindRequest, sendNotificationMessages);
		
		// add a folder to the project
		Folder folder = new Folder();
		folder.setParentId(project.getId());
		String folderId = entityManager.createEntity(adminUserInfo, folder, null);
		JSONObject folderJson = entityManager.getEntityJson(folderId);
		
		// Add single element annotations to the folder that is valid
		folderJson.put("key", Arrays.asList("foo"));
		folderJson = entityManager.updateEntityJson(adminUserInfo, folderId, folderJson);
		
		// wait till it is valid
		waitForValidationResults(adminUserInfo, folderId, (ValidationResults t) -> {
			assertNotNull(t);
			assertTrue(t.getIsValid());
		});
		
		// change child schema and register the new schema
		typeSchema.setType(Type.number);
		properties.put("key", typeSchema);
		child.setProperties(properties);
		createResponse = registerSchema(child);
		
		// wait till it is not valid
		waitForValidationResults(adminUserInfo, folderId, (ValidationResults t) -> {
			assertNotNull(t);
			assertFalse(t.getIsValid());
		});
		
		// clean up
		entityManager.clearBoundSchema(adminUserInfo, projectId);
		waitForValidationResultsToBeNotFound(adminUserInfo, folderId);
	}
	
	@Test
	public void testDuoSchema() throws Exception {
		bootstrapAndCreateOrganization("ebispot.duo");
		String[] schemasToRegister = { "schema/DUO/hmb.json", "schema/DUO/irb.json", "schema/DUO/duo.json"};
		for (String fileName : schemasToRegister) {
			JsonSchema schema = getSchemaFromClasspath(fileName);
			registerSchema(schema);
		}

		JsonSchema validationSchema = jsonSchemaManager.getValidationSchema("ebispot.duo-duo");
		assertNotNull(schemaBootstrap);
		printJson(validationSchema);
		assertNotNull(validationSchema.getDefinitions());
		assertTrue(validationSchema.getDefinitions().containsKey("ebispot.duo-D0000006"));
		assertTrue(validationSchema.getDefinitions().containsKey("ebispot.duo-D0000021"));


		String validJsonFile = loadStringFromClasspath("schema/DUO/ValidDuoFile.json");
		JSONObject validJson = new JSONObject(validJsonFile);
		JsonSubject mockSubject = Mockito.mock(JsonSubject.class);
		when(mockSubject.toJson()).thenReturn(validJson);
		// this schema should be valid
		ValidationResults result = jsonSchemaValidationManager.validate(validationSchema, mockSubject);
		assertNotNull(result);
		assertTrue(result.getIsValid());
	}
	
	@Test
	public void testDuoSchemaAppliedAgainstValidFileJson() throws Exception {
		bootstrapAndCreateOrganization("ebispot.duo");
		jsonSchemaManager.createOrganziation(adminUserInfo, new CreateOrganizationRequest().setOrganizationName("some.project"));
		String[] schemasToRegister = { 
				"schema/DUO/D0000001.json",
				"schema/DUO/D0000004.json",
				"schema/DUO/D0000006.json",
				"schema/DUO/D0000007.json",
				"schema/DUO/D0000011.json",
				"schema/DUO/D0000012.json",
				"schema/DUO/D0000015.json",
				"schema/DUO/D0000016.json",
				"schema/DUO/D0000018.json",
				"schema/DUO/D0000019.json",
				"schema/DUO/D0000020.json",
				"schema/DUO/D0000021.json",
				"schema/DUO/D0000022.json",
				"schema/DUO/D0000024.json",
				"schema/DUO/D0000025.json",
				"schema/DUO/D0000026.json",
				"schema/DUO/D0000027.json",
				"schema/DUO/D0000028.json",
				"schema/DUO/D0000029.json",
				"schema/DUO/D0000042.json",
				"schema/DUO/D0000043.json",
				"schema/DUO/D0000044.json",
				"schema/DUO/D0000045.json",
				"schema/DUO/D0000046.json",
				"schema/DUO/duo.json",
				"schema/DUO/DuoMainStory.json",
		};
		for (String fileName : schemasToRegister) {
			JsonSchema schema = getSchemaFromClasspath(fileName);
			registerSchema(schema);
		}
		
		JsonSchema duoMain = jsonSchemaManager.getValidationSchema("ebispot.duo-duo");
		assertNotNull(duoMain);
		printJson(duoMain);

		JsonSchema validationSchema = jsonSchemaManager.getValidationSchema("some.project-main");
		assertNotNull(validationSchema);
		printJson(validationSchema);
				
		String[] validJsonFiles = { "schema/DUO/ValidSyn1.json", "schema/DUO/ValidSyn4.json" };
		for (String schemaFile : validJsonFiles) {
			String validJsonFile = loadStringFromClasspath(schemaFile);
			JSONObject validJson = new JSONObject(validJsonFile);
			JsonSubject mockSubject = Mockito.mock(JsonSubject.class);
			when(mockSubject.toJson()).thenReturn(validJson);
			// this schema should be valid
			ValidationResults result = jsonSchemaValidationManager.validate(validationSchema, mockSubject);
			assertNotNull(result);
			assertTrue(result.getIsValid());
		}
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
