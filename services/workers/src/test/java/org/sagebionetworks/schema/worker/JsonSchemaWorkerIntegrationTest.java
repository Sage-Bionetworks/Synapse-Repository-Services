package org.sagebionetworks.schema.worker;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
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
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaValidationManager;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
import org.sagebionetworks.repo.manager.schema.SynapseSchemaBootstrap;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.FileHandleObjectHelper;
import org.sagebionetworks.repo.model.helper.TermsOfUseAccessRequirementObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
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
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.ObjectAnnotationDTO;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.ViewColumnModelRequest;
import org.sagebionetworks.repo.model.table.ViewColumnModelResponse;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
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

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class JsonSchemaWorkerIntegrationTest {

	public static final long MAX_WAIT_MS = 1000 * 80;

	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;

	@Autowired
	private JsonSchemaManager jsonSchemaManager;

	@Autowired
	private SynapseSchemaBootstrap schemaBootstrap;

	@Autowired
	private UserManager userManager;

	@Autowired
	private JsonSchemaValidationManager jsonSchemaValidationManager;

	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private VelocityEngine velocityEngine;
	
	@Autowired
	private FileHandleObjectHelper fileHandleObjectHelper;
	
	@Autowired
	private TermsOfUseAccessRequirementObjectHelper termsOfUseAccessRequirementObjectHelper;
	
	@Autowired
	private AccessRequirementManager accessRequirementManager;
	
	@Autowired
	private ColumnModelManager columnModleManager;
	

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
		fileHandleObjectHelper.truncateAll();
		jsonSchemaManager.truncateAll();
		if (projectId != null) {
			entityManager.deleteEntity(adminUserInfo, projectId);
		}
		termsOfUseAccessRequirementObjectHelper.truncateAll();

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
	 * Load a JSON schema from the classpath and apply the provided velocity context before parsing.
	 * @param name
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public JsonSchema getSchemaTemplateFromClasspath(String name, String schemaId, VelocityContext context) throws Exception {
		Template tempalte = velocityEngine.getTemplate(name);
		StringWriter writer = new StringWriter();
		tempalte.merge(context, writer);
		String json = writer.toString();
		JsonSchema schema =  EntityFactory.createEntityFromJSONString(json, JsonSchema.class);
		schema.set$schema("http://json-schema.org/draft-07/schema#");
		schema.set$id(schemaId);
		return schema;
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

	void bootstrapAndCreateOrganization() throws RecoverableMessageException, InterruptedException {
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
		JSONObject folderJson = entityManager.getEntityJson(folderId, false);
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
		JSONObject folderJson = entityManager.getEntityJson(folderId, false);
		// Add the foo annotation to the folder
		folderJson.put("hasBoolean", true);
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
		bootstrapAndCreateOrganization();
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
		JSONObject folderJson = entityManager.getEntityJson(folderId, false);
		// Add the foo annotation to the folder
		folderJson.put("hasBoolean", true);
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
		JSONObject folderJson = entityManager.getEntityJson(folderId, false);
		
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
		folderJson = entityManager.getEntityJson(folderId, false);
		// barKey is defined as an array in a subschema, it will stay as an array
		assertEquals(folderJson.getJSONArray("barKey").getString(0), "bar");
		// bazKey is not defined in the schema, and it has single value. so it's a single value
		assertEquals("baz", folderJson.getString("bazKey"));
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
		JSONObject folderJson = entityManager.getEntityJson(folderId, false);
		
		folderJson.put("state", Arrays.asList("Alabama"));
		folderJson = entityManager.updateEntityJson(adminUserInfo, folderId, folderJson);
		
		// wait till it is valid
		waitForValidationResults(adminUserInfo, folderId, (ValidationResults t) -> {
			assertNotNull(t);
			assertTrue(t.getIsValid());
		});
		
		// call under test
		// should not be an array
		folderJson = entityManager.getEntityJson(folderId, false);
		assertEquals("Alabama", folderJson.getString("state"));
		
		// clean up
		entityManager.clearBoundSchema(adminUserInfo, projectId);
		waitForValidationResultsToBeNotFound(adminUserInfo, folderId);
	}
	
	@Test
	public void testGetEntityJsonWithDerivedAnnotations() throws Exception {
		bootstrapAndCreateOrganization();
		String projectId = entityManager.createEntity(adminUserInfo, new Project(), null);
		Project project = entityManager.getEntity(adminUserInfo, projectId, Project.class);

		// create the schema
		String fileName = "schema/DerivedConditionalConst.json";
		JsonSchema schema = getSchemaFromClasspath(fileName);
		CreateSchemaResponse createResponse = registerSchema(schema);
		String schema$id = createResponse.getNewVersionInfo().get$id();
		// bind the schema to the project
		BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
		bindRequest.setEntityId(projectId);
		bindRequest.setSchema$id(schema$id);
		bindRequest.setEnableDerivedAnnotations(true);
		entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

		// add a folder to the project
		Folder folder = new Folder();
		folder.setParentId(project.getId());
		String folderId = entityManager.createEntity(adminUserInfo, folder, null);
		JSONObject folderJson = entityManager.getEntityJson(folderId, false);
		
		folderJson.put("someBoolean", true);
		folderJson = entityManager.updateEntityJson(adminUserInfo, folderId, folderJson);
		
		waitForValidationResults(adminUserInfo, folderId, (ValidationResults t) -> {
			assertNotNull(t);
			assertTrue(t.getIsValid());
		});
		
		folderJson = entityManager.getEntityJson(folderId, true);
		
		assertEquals(true, folderJson.getBoolean("someBoolean"));
		assertEquals(456, folderJson.getLong("unconditionalDefault"));
		assertEquals("someBoolean was true", folderJson.getString("someConditional"));
		assertEquals(999, folderJson.getLong("conditionalLong"));
	}

	@Test
	public void testGetEntityJsonWithDerivedAnnotationsWithInfiniteLoop() throws Exception {
		bootstrapAndCreateOrganization();
		String projectId = entityManager.createEntity(adminUserInfo, new Project(), null);
		Project project = entityManager.getEntity(adminUserInfo, projectId, Project.class);

		// create the schema
		String fileName = "schema/DerivedConditionalConst.json";
		JsonSchema schema = getSchemaFromClasspath(fileName);
		CreateSchemaResponse createResponse = registerSchema(schema);
		String schema$id = createResponse.getNewVersionInfo().get$id();
		// bind the schema to the project
		BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
		bindRequest.setEntityId(projectId);
		bindRequest.setSchema$id(schema$id);
		bindRequest.setEnableDerivedAnnotations(true);
		entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

		// add a folder to the project
		Folder folder = new Folder();
		folder.setParentId(project.getId());
		String folderId = entityManager.createEntity(adminUserInfo, folder, null);
		JSONObject folderJson = entityManager.getEntityJson(folderId, false);

		folderJson.put("someBoolean", true);
		folderJson = entityManager.updateEntityJson(adminUserInfo, folderId, folderJson);

		ValidationResults previousValidationResults = waitForValidationResults(adminUserInfo, folderId, (ValidationResults t) -> {
			assertNotNull(t);
			assertTrue(t.getIsValid());
		});

		// The purpose of the below loop is to detect if an infinite loop occurs between the EntitySchemaValidator and
		// SchemaValidationWorker when updating annotations for an entity.
		int diffCount = 0;
		for (int i = 0; i < 10; i++) {
			Thread.sleep(1000);
			ValidationResults newValidationResults = entityManager.getEntityValidationResults(adminUserInfo, folderId);

			if (!newValidationResults.getValidatedOn().equals(previousValidationResults.getValidatedOn())) {
				diffCount++;
				previousValidationResults = newValidationResults;
			}
			assertTrue(diffCount <= 3);
		}
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
		JSONObject folderJson = entityManager.getEntityJson(folderId, false);
		
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
	public void testDerivedAnnotationsAndReplication() throws Exception {
		bootstrapAndCreateOrganization();
		String projectId = entityManager.createEntity(adminUserInfo, new Project(), null);
		Project project = entityManager.getEntity(adminUserInfo, projectId, Project.class);
		
		// add a folder to the project
		Folder folder = new Folder();
		folder.setParentId(project.getId());
		String folderId = entityManager.createEntity(adminUserInfo, folder, null);
		JSONObject folderJson = entityManager.getEntityJson(folderId, false);
		folderJson.put("myAnnotation", "myAnnotationValue");
		folderJson = entityManager.updateEntityJson(adminUserInfo, folderId, folderJson);
		
		asynchronousJobWorkerHelper.waitForReplicationIndexData(folderId, data-> {
			assertEquals(List.of(
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "myAnnotation", AnnotationType.STRING, List.of("myAnnotationValue"))
			), 
			data.getAnnotations());
		}, MAX_WAIT_MS);
		
		
		List<ColumnModel> viewColumns = columnModleManager.createColumnModels(adminUserInfo,
				List.of(new ColumnModel().setName("conditionalLong").setColumnType(ColumnType.INTEGER),
						new ColumnModel().setName("myAnnotation").setColumnType(ColumnType.STRING).setMaximumSize(100L),
						new ColumnModel().setName("someConditional").setColumnType(ColumnType.STRING).setMaximumSize(200L),
						new ColumnModel().setName("unconditionalDefault").setColumnType(ColumnType.INTEGER)));
		EntityView view = asynchronousJobWorkerHelper.createEntityView(adminUserInfo, "derivedView", projectId,
				viewColumns.stream().map(ColumnModel::getId).collect(Collectors.toList()), List.of(projectId),
				ViewTypeMask.Folder.getMask(), false);
		
		String sql = String.format("select * from %s where row_id = %d", view.getId(), KeyFactory.stringToKey(folderId));
		asynchronousJobWorkerHelper.assertQueryResult(adminUserInfo, sql, (r)->{
			assertQueryResults(r, null, "myAnnotationValue", null, null);
		}, MAX_WAIT_MS);

		// create the schema
		String fileName = "schema/DerivedConditionalConst.json";
		JsonSchema schema = getSchemaFromClasspath(fileName);
		CreateSchemaResponse createResponse = registerSchema(schema);
		String schema$id = createResponse.getNewVersionInfo().get$id();
		
		// Now bind the schema to the project, this will enable derived annotations from the schema that will eventually be replicated
		BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
		bindRequest.setEntityId(projectId);
		bindRequest.setSchema$id(schema$id);
		bindRequest.setEnableDerivedAnnotations(true);
		
		entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);
		
		asynchronousJobWorkerHelper.waitForReplicationIndexData(folderId, data-> {
			assertEquals(List.of(
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "conditionalLong", AnnotationType.LONG, List.of("999"), true),
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "myAnnotation", AnnotationType.STRING, List.of("myAnnotationValue"), false),
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "someConditional", AnnotationType.STRING, List.of("someBoolean was true"), true),
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "unconditionalDefault", AnnotationType.LONG, List.of("456"), true)
			), 
			data.getAnnotations());
		}, MAX_WAIT_MS);
		
		asynchronousJobWorkerHelper.assertQueryResult(adminUserInfo, sql, (r)->{
			assertQueryResults(r, "999", "myAnnotationValue", "someBoolean was true", "456");
		}, MAX_WAIT_MS);
		
		// Now we put an explicit property that should be replicated as well
		folderJson.put("someBoolean", false);
		folderJson = entityManager.updateEntityJson(adminUserInfo, folderId, folderJson);
				
		asynchronousJobWorkerHelper.waitForReplicationIndexData(folderId, data-> {
			assertEquals(List.of(
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "myAnnotation", AnnotationType.STRING, List.of("myAnnotationValue"), false),
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "someBoolean", AnnotationType.BOOLEAN, List.of("false"), false),
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "someConditional", AnnotationType.STRING, List.of("someBoolean was false"), true),
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "unconditionalDefault", AnnotationType.LONG, List.of("456"), true)
			), 
			data.getAnnotations());
		}, MAX_WAIT_MS);
		
		asynchronousJobWorkerHelper.assertQueryResult(adminUserInfo, sql, (r)->{
			assertQueryResults(r, "999", "myAnnotationValue", "someBoolean was false", "456");
		}, MAX_WAIT_MS);
		
		// Now switch the boolean
		folderJson.put("someBoolean", true);
		folderJson = entityManager.updateEntityJson(adminUserInfo, folderId, folderJson);
		
		asynchronousJobWorkerHelper.waitForReplicationIndexData(folderId, data-> {
			assertEquals(List.of(
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "conditionalLong", AnnotationType.LONG, List.of("999"), true),
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "myAnnotation", AnnotationType.STRING, List.of("myAnnotationValue"), false),
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "someBoolean", AnnotationType.BOOLEAN, List.of("true"), false),
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "someConditional", AnnotationType.STRING, List.of("someBoolean was true"), true),
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "unconditionalDefault", AnnotationType.LONG, List.of("456"), true)
			), 
			data.getAnnotations());
		}, MAX_WAIT_MS);
		
		asynchronousJobWorkerHelper.assertQueryResult(adminUserInfo, sql, (r)->{
			assertQueryResults(r, "999", "myAnnotationValue", "someBoolean was true", "456");
		}, MAX_WAIT_MS);
	}
	
	/**
	 * Helper to assert the expected values for a query result that returns a single row.
	 * @param expected
	 * @param bundle
	 */
	void assertQueryResults(QueryResultBundle bundle, String...values){
		assertNotNull(bundle);
		assertEquals(1L, bundle.getQueryCount());
		assertNotNull(bundle.getQueryResult());
		assertNotNull(bundle.getQueryResult().getQueryResults());
		assertNotNull(bundle.getQueryResult().getQueryResults().getRows());
		assertEquals(1, bundle.getQueryResult().getQueryResults().getRows().size());
		Row row = bundle.getQueryResult().getQueryResults().getRows().get(0);
		assertNotNull(row);
		assertNotNull(row.getValues());
		assertEquals(Arrays.asList(values), row.getValues());
	}
	
	@Test
	public void testDerivedAnnotationsAndViewColumnModelRequest() throws Exception {
		bootstrapAndCreateOrganization();
		String projectId = entityManager.createEntity(adminUserInfo, new Project(), null);
		Project project = entityManager.getEntity(adminUserInfo, projectId, Project.class);
		
		// create the schema
		String fileName = "schema/DerivedConditionalConst.json";
		JsonSchema schema = getSchemaFromClasspath(fileName);
		CreateSchemaResponse createResponse = registerSchema(schema);
		String schema$id = createResponse.getNewVersionInfo().get$id();
		
		// Now bind the schema to the project, this will enable derived annotations from the schema that will eventually be replicated
		BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
		bindRequest.setEntityId(projectId);
		bindRequest.setSchema$id(schema$id);
		bindRequest.setEnableDerivedAnnotations(true);
		
		entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);
		
		// add a folder to the project
		Folder folder = new Folder();
		folder.setParentId(project.getId());
		String folderId = entityManager.createEntity(adminUserInfo, folder, null);
		JSONObject folderJson = entityManager.getEntityJson(folderId, false);
		folderJson.put("myAnnotation", "myAnnotationValue");
		folderJson = entityManager.updateEntityJson(adminUserInfo, folderId, folderJson);
		
		asynchronousJobWorkerHelper.waitForReplicationIndexData(folderId, data-> {
			assertEquals(List.of(
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "conditionalLong", AnnotationType.LONG, List.of("999"), true),
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "myAnnotation", AnnotationType.STRING, List.of("myAnnotationValue"), false),
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "someConditional", AnnotationType.STRING, List.of("someBoolean was true"), true),
				new ObjectAnnotationDTO(KeyFactory.stringToKey(folderId), 1L, "unconditionalDefault", AnnotationType.LONG, List.of("456"), true)
			), 
			data.getAnnotations());
		}, MAX_WAIT_MS);

		ViewScope viewScope = new ViewScope()
			.setViewEntityType(ViewEntityType.entityview)
			.setScope(List.of(projectId))
			.setViewTypeMask(ViewTypeMask.Folder.getMask());
		
		ViewColumnModelRequest request = new ViewColumnModelRequest()
			.setViewScope(viewScope);
						
		asynchronousJobWorkerHelper.assertJobResponse(adminUserInfo, request, (ViewColumnModelResponse response) -> {
			List<ColumnModel> expected = List.of(
				new ColumnModel().setName("myAnnotation").setColumnType(ColumnType.STRING).setMaximumSize(17L)
			);
			assertEquals(expected, response.getResults());
		}, MAX_WAIT_MS);
		
		// now asks to include the derived annotations
		request.setIncludeDerivedAnnotations(true);
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUserInfo, request, (ViewColumnModelResponse response) -> {
			List<ColumnModel> expected = List.of(
				new ColumnModel().setName("conditionalLong").setColumnType(ColumnType.INTEGER),
				new ColumnModel().setName("myAnnotation").setColumnType(ColumnType.STRING).setMaximumSize(17L),
				new ColumnModel().setName("someConditional").setColumnType(ColumnType.STRING).setMaximumSize(20L),
				new ColumnModel().setName("unconditionalDefault").setColumnType(ColumnType.INTEGER)
			);
			assertEquals(expected, response.getResults());
		}, MAX_WAIT_MS);
	}
	
	@Test
	public void testDerivedAnnotationWithAccessRequirementIdBinding() throws Exception {
		bootstrapAndCreateOrganization();
		String projectId = entityManager.createEntity(adminUserInfo, new Project(), null);
		
		AccessRequirement ar1 =  termsOfUseAccessRequirementObjectHelper.create((a)->{a.setName("one");});
		AccessRequirement ar2 =  termsOfUseAccessRequirementObjectHelper.create((a)->{a.setName("two");});
		AccessRequirement ar3 =  termsOfUseAccessRequirementObjectHelper.create((a)->{a.setName("three");});

		// create the schema,
		// DerivedWithAccessRequirementIds.json.vtp(stands for velocity) is not a json file, it's a template used by velocity to create json
		String fileName = "schema/DerivedWithAccessRequirementIds.json.vtp";
		JsonSchema schema = getSchemaTemplateFromClasspath(fileName, "my.organization-DerivedWithAccessRequirementIds",
				new VelocityContext(Map.of("arOne", ar1.getId(), "arTwo", ar2.getId(), "arThree", ar3.getId())));
		CreateSchemaResponse createResponse = registerSchema(schema);
		String schema$id = createResponse.getNewVersionInfo().get$id();

		// Now bind the schema to the project, this will enable derived annotations from
		// the schema that will eventually be replicated
		BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
		bindRequest.setEntityId(projectId);
		bindRequest.setSchema$id(schema$id);
		bindRequest.setEnableDerivedAnnotations(true);

		entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);
		
		// one
		FileEntity fileOne = createFileWithAnnotations(projectId, "one", (c)->{
			c.put("someBoolean", true);
		});
		

		waitForValidationResults(adminUserInfo, fileOne.getId(), (ValidationResults t) -> {
			assertNotNull(t);
			assertTrue(t.getIsValid());
		});

		boolean includeDerivedAnnotations = true;
		Annotations annotations = entityManager.getAnnotations(adminUserInfo, fileOne.getId(),
				includeDerivedAnnotations);
		Annotations expected = new Annotations();
		expected.setId(annotations.getId());
		expected.setEtag(annotations.getEtag());
		AnnotationsV2TestUtils.putAnnotations(expected, "someBoolean", "true", AnnotationsValueType.BOOLEAN);
		AnnotationsV2TestUtils.putAnnotations(expected, "_accessRequirementIds",
				List.of(ar1.getId().toString(), ar2.getId().toString()), AnnotationsValueType.LONG);
		assertEquals(expected, annotations);
		
		Long limit = 50L;
		Long offset = 0L;
		// validate that the ARs are bound to the entity.
		Set<Long> boundArIds = accessRequirementManager.getAccessRequirementsForSubject(adminUserInfo,
				new RestrictableObjectDescriptor().setId(fileOne.getId()).setType(RestrictableObjectType.ENTITY), limit,
				offset).stream().map(AccessRequirement::getId).collect(Collectors.toSet());
		assertEquals(Set.of(ar1.getId(), ar2.getId()), boundArIds);
		
		// two
		FileEntity fileTwo = createFileWithAnnotations(projectId, "two", (c)->{
			c.put("someBoolean", false);
		});

		waitForValidationResults(adminUserInfo, fileTwo.getId(), (ValidationResults t) -> {
			assertNotNull(t);
			assertTrue(t.getIsValid());
		});

		annotations = entityManager.getAnnotations(adminUserInfo, fileTwo.getId(), includeDerivedAnnotations);
		expected = new Annotations();
		expected.setId(annotations.getId());
		expected.setEtag(annotations.getEtag());
		AnnotationsV2TestUtils.putAnnotations(expected, "someBoolean", "false", AnnotationsValueType.BOOLEAN);
		AnnotationsV2TestUtils.putAnnotations(expected, "_accessRequirementIds",
				List.of(ar2.getId().toString(), ar3.getId().toString()), AnnotationsValueType.LONG);
		assertEquals(expected, annotations);
		
		// validate that the ARs are bound to the entity.
		boundArIds = accessRequirementManager.getAccessRequirementsForSubject(adminUserInfo,
				new RestrictableObjectDescriptor().setId(fileTwo.getId()).setType(RestrictableObjectType.ENTITY), limit,
				offset).stream().map(AccessRequirement::getId).collect(Collectors.toSet());
		assertEquals(Set.of(ar2.getId(), ar3.getId()), boundArIds);
		
	}
	
	/**
	 * If a file is invalid against a schema that includes access requirement, it should get bound
	 * to the bootstrapped invalid annotation access requirement. (PLFM-7412).
	 * @throws Exception
	 */
	@Test
	public void testDerivedAnnotationWithAccessRequirmentIdBindingAndInvalidObject() throws Exception {
		bootstrapAndCreateOrganization();
		String projectId = entityManager.createEntity(adminUserInfo, new Project(), null);

		// create the schema
		String fileName = "schema/RequiredWithAccessRequirementIds.json";
		JsonSchema schema = getSchemaFromClasspath(fileName);
		CreateSchemaResponse createResponse = registerSchema(schema);
		String schema$id = createResponse.getNewVersionInfo().get$id();

		// Now bind the schema to the project, this will enable derived annotations from
		// the schema that will eventually be replicated
		BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
		bindRequest.setEntityId(projectId);
		bindRequest.setSchema$id(schema$id);
		bindRequest.setEnableDerivedAnnotations(true);

		entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);
		
		// one
		FileEntity fileOne = createFileWithAnnotations(projectId, "one", (c)->{
			c.put("someBoolean", "not a boolean");
		});
		

		waitForValidationResults(adminUserInfo, fileOne.getId(), (ValidationResults t) -> {
			assertNotNull(t);
			assertFalse(t.getIsValid());
		});
		
		Long limit = 50L;
		Long offset = 0L;
		// the invalid annotations lock should be bound to the entity.
		Set<Long> boundArIds = accessRequirementManager.getAccessRequirementsForSubject(adminUserInfo,
				new RestrictableObjectDescriptor().setId(fileOne.getId()).setType(RestrictableObjectType.ENTITY), limit,
				offset).stream().map(AccessRequirement::getId).collect(Collectors.toSet());
		assertEquals(Set.of(AccessRequirementDAO.INVALID_ANNOTATIONS_LOCK_ID), boundArIds);
	}
	
	/**
	 * Helper to create a FileEntity with the annotations defined by the provider consumer.
	 * @param parentId
	 * @param name
	 * @param consumer
	 * @return
	 */
	public FileEntity createFileWithAnnotations(String parentId, String name, Consumer<JSONObject> consumer) {
		S3FileHandle fileHandle =  fileHandleObjectHelper.createS3(h->{h.setFileName("theFile.txt");});
		FileEntity file = new FileEntity().setName(name).setDataFileHandleId(fileHandle.getId()).setParentId(parentId);
		String id = entityManager.createEntity(adminUserInfo, file, null);
		JSONObject fileJson = entityManager.getEntityJson(id, false);
		consumer.accept(fileJson);
		fileJson = entityManager.updateEntityJson(adminUserInfo, id, fileJson);
		return entityManager.getEntity(adminUserInfo, id, FileEntity.class);
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
